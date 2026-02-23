# 트러블슈팅 가이드

## API Gateway I/O 오류 해결

### 문제 증상
```json
{
  "error": "GATEWAY_ERROR",
  "message": "I/O error on GET request for \"http://order-service:8081/orders\": null"
}
{
  "error": "GATEWAY_ERROR",
  "message": "I/O error on GET request for \"http://fulfillment-service:8083/fulfillments\": null"
}
```

### 근본 원인

#### 1. fulfillment-service: OOMKilled (메모리 부족)
- **증상**: Pod가 CrashLoopBackOff 상태로 계속 재시작
- **원인**: 메모리 제한 512Mi로 부족 (높은 트래픽 처리 시)
- **해결**: 메모리 제한을 1Gi로 증가

#### 2. order-service: OOMKilled + DB Constraint 위반
- **증상**: Pod 재시작 + Kafka 메시지 처리 실패
- **원인 1**: 메모리 부족 (512Mi)
- **원인 2**: DB에 `PAYMENT_FAILED` 상태에 대한 CHECK constraint 누락
- **해결**: 
  - 메모리 제한을 1Gi로 증가
  - DB constraint 추가

### 해결 방법

#### 1. DB 마이그레이션 실행 (order-service)

```bash
# order-db Pod 찾기
ORDER_DB_POD=$(oc get pods -l app=order-db -o jsonpath='{.items[0].metadata.name}')

# 마이그레이션 스크립트 실행
oc exec -i $ORDER_DB_POD -- psql -U postgres -d orderdb < infra/sql/order-migration-add-status-check.sql

# 또는 직접 SQL 실행
oc exec -i $ORDER_DB_POD -- psql -U postgres -d orderdb <<EOF
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
    CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELED', 'INVENTORY_RESERVED', 'INVENTORY_REJECTED', 'PAYMENT_FAILED'));
EOF
```

#### 2. Helm 업그레이드 (메모리 증가 적용)

```bash
# 현재 디렉토리가 프로젝트 루트인지 확인
cd /path/to/order

# Helm 업그레이드
helm upgrade workshop-eda ./helm/workshop-eda \
  --namespace workshop-eda \
  --reuse-values

# 또는 values.yaml 수정 후
helm upgrade workshop-eda ./helm/workshop-eda \
  --namespace workshop-eda \
  -f helm/workshop-eda/values.yaml
```

#### 3. Pod 재시작 확인

```bash
# Pod 상태 확인
oc get pods -l app.kubernetes.io/name=order-service
oc get pods -l app.kubernetes.io/name=fulfillment-service

# 로그 확인
oc logs -f -l app.kubernetes.io/name=order-service
oc logs -f -l app.kubernetes.io/name=fulfillment-service

# 엔드포인트 확인
oc get endpoints order-service
oc get endpoints fulfillment-service
```

#### 4. API 테스트

```bash
# API Gateway URL 확인
API_GATEWAY_URL=$(oc get route api-gateway -o jsonpath='{.spec.host}')

# Order 조회 테스트
curl -X GET "http://${API_GATEWAY_URL}/api/admin/orders"

# Fulfillment 조회 테스트
curl -X GET "http://${API_GATEWAY_URL}/api/admin/fulfillments"
```

### 변경 사항 요약

#### 1. `helm/workshop-eda/templates/apps.yaml`
```yaml
resources:
  requests: { cpu: "100m", memory: "512Mi" }  # 256Mi → 512Mi
  limits:   { cpu: "1000m", memory: "1Gi" }   # 500m/512Mi → 1000m/1Gi
```

#### 2. `infra/sql/order-init.sql`
```sql
CREATE TABLE IF NOT EXISTS orders (
    ...
    CONSTRAINT orders_status_check CHECK (status IN (
        'PENDING', 
        'COMPLETED', 
        'CANCELED', 
        'INVENTORY_RESERVED', 
        'INVENTORY_REJECTED', 
        'PAYMENT_FAILED'  -- 추가됨
    ))
);
```

#### 3. `infra/sql/order-migration-add-status-check.sql` (신규 생성)
- 기존 DB에 constraint를 추가하는 마이그레이션 스크립트

### 예방 조치

#### 1. 리소스 모니터링
```bash
# Pod 리소스 사용량 확인
oc adm top pods -l app.kubernetes.io/name=order-service
oc adm top pods -l app.kubernetes.io/name=fulfillment-service

# 메모리 사용량이 80% 이상이면 증가 고려
```

#### 2. DB Schema 검증
- 애플리케이션 코드의 Enum과 DB Constraint가 일치하는지 확인
- 새로운 상태 추가 시 DB 마이그레이션 스크립트 작성

#### 3. Health Check 설정
```yaml
readinessProbe:
  initialDelaySeconds: 100  # 충분한 시작 시간 제공
  periodSeconds: 60
livenessProbe:
  initialDelaySeconds: 120
  periodSeconds: 60
```

### 클러스터 메모리 부족 시 대안

#### 옵션 1: JVM 힙 메모리 최적화 (권장)
메모리 제한을 유지하면서 JVM 설정을 최적화합니다.

**values.yaml 수정:**
```yaml
apps:
  order-service:
    env:
      JAVA_OPTS: "-Xms256m -Xmx384m -XX:MaxMetaspaceSize=128m"
      # 기존 환경변수들...
  
  fulfillment-service:
    env:
      JAVA_OPTS: "-Xms256m -Xmx384m -XX:MaxMetaspaceSize=128m"
      # 기존 환경변수들...
```

**templates/apps.yaml 수정:**
```yaml
containers:
  - name: {{ $name }}
    image: "{{ $global.imageRegistry }}/{{ $name }}:{{ $global.imageTag }}"
    {{- if $cfg.env.JAVA_OPTS }}
    env:
      - name: JAVA_OPTS
        value: {{ $cfg.env.JAVA_OPTS | quote }}
    {{- end }}
    resources:
      requests: { cpu: "100m", memory: "256Mi" }  # 원래대로 유지
      limits:   { cpu: "500m", memory: "512Mi" }   # 원래대로 유지
```

#### 옵션 2: 불필요한 서비스 스케일 다운
```bash
# analytics-service 스케일 다운 (필수 아님)
oc scale deployment analytics-service --replicas=0

# load-generator 중지
oc scale deployment load-generator --replicas=0
```

#### 옵션 3: DB Constraint만 수정 (임시 해결)
메모리 증가 없이 DB constraint만 수정하여 order-service 크래시 방지:

```bash
# 1. DB 마이그레이션만 실행
ORDER_DB_POD=$(oc get pods -l app=order-db -o jsonpath='{.items[0].metadata.name}')
oc exec -i $ORDER_DB_POD -- psql -U postgres -d orderdb <<EOF
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELED', 'INVENTORY_RESERVED', 'INVENTORY_REJECTED', 'PAYMENT_FAILED'));
EOF

# 2. order-service만 재시작
oc rollout restart deployment/order-service

# 3. fulfillment-service는 스케일 다운 (조회 기능만 사용 안 함)
oc scale deployment fulfillment-service --replicas=0
```

#### 옵션 4: 클러스터 리소스 확인 및 정리
```bash
# 1. 노드별 리소스 사용량 확인
oc adm top nodes

# 2. 네임스페이스별 리소스 사용량
oc adm top pods --all-namespaces --sort-by=memory

# 3. 불필요한 Pod 정리
oc get pods --all-namespaces | grep -E 'Completed|Error|CrashLoopBackOff'

# 4. 리소스 쿼터 확인
oc get resourcequota -n workshop-eda
oc describe resourcequota -n workshop-eda
```

#### 옵션 5: Horizontal Pod Autoscaler 비활성화
```bash
# HPA가 있다면 삭제
oc delete hpa --all -n workshop-eda
```

#### 권장 순서
1. **먼저 DB Constraint 수정** (옵션 3) - 즉시 효과
2. **불필요한 서비스 스케일 다운** (옵션 2) - 메모리 확보
3. **JVM 최적화 적용** (옵션 1) - 장기적 해결
4. **클러스터 리소스 정리** (옵션 4) - 필요시

### 참고 자료
- [Kubernetes Resource Management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)
- [PostgreSQL CHECK Constraints](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-CHECK-CONSTRAINTS)
- [Spring Kafka Error Handling](https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html)
- [JVM Memory Tuning](https://docs.oracle.com/en/java/javase/21/gctuning/)

---
Made with Bob