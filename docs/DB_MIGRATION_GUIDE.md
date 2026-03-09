# 데이터베이스 마이그레이션 가이드

## 개요

이 문서는 OpenShift 환경의 PostgreSQL 데이터베이스에 대한 마이그레이션 절차를 설명합니다.

## 마이그레이션 목록

### 1. event_count_summary 테이블 추가 (2026-03-09)

**목적**: 이벤트 카운트 성능 최적화를 위한 사전 집계 테이블 추가

**파일**: `infra/sql/analytics-migration-add-event-count-summary.sql`

**변경 사항**:
- `event_count_summary` 테이블 생성
- 이벤트 타입별 카운트 사전 집계
- 성능 향상을 위한 인덱스 추가

---

## 마이그레이션 실행 방법

### 방법 1: 자동화 스크립트 사용 (권장)

```bash
# 기본 마이그레이션 실행
./scripts/apply-db-migration.sh

# 특정 마이그레이션 파일 지정
./scripts/apply-db-migration.sh infra/sql/analytics-migration-add-event-count-summary.sql

# 다른 네임스페이스 지정
NAMESPACE=my-namespace ./scripts/apply-db-migration.sh
```

### 방법 2: 수동 실행

#### 2.1 PostgreSQL Pod 확인

```bash
# Pod 이름 확인 (올바른 라벨 사용)
oc get pods -n workshop-eda -l deploymentconfig=analytics-db

# 예시 출력:
# NAME                     READY   STATUS    RESTARTS   AGE
# analytics-db-1-2vx6s     1/1     Running   0          2d
```

#### 2.2 마이그레이션 파일 복사

```bash
# Pod 이름을 실제 값으로 변경 (실제 환경 예제)
POD_NAME=analytics-db-1-2vx6s
NAMESPACE=workshop-eda

# 마이그레이션 파일 복사
oc cp infra/sql/analytics-migration-add-event-count-summary.sql \
  $NAMESPACE/$POD_NAME:/tmp/migration.sql
```

#### 2.3 마이그레이션 실행

```bash
# SQL 실행
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -f /tmp/migration.sql
```

#### 2.4 결과 확인

```bash
# 테이블 구조 확인
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "\d event_count_summary"

# 데이터 확인
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "SELECT * FROM event_count_summary ORDER BY count DESC;"
```

---

## 롤백 방법

### event_count_summary 테이블 롤백

마이그레이션을 롤백해야 하는 경우:

```bash
# 롤백 SQL 생성
cat > /tmp/rollback.sql << 'EOF'
-- Rollback: Remove event_count_summary table
DROP TABLE IF EXISTS event_count_summary CASCADE;
SELECT 'Rollback completed successfully' as status;
EOF

# Pod에 복사
oc cp /tmp/rollback.sql $NAMESPACE/$POD_NAME:/tmp/rollback.sql

# 롤백 실행
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -f /tmp/rollback.sql

# 확인
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "\dt event_count_summary"
```

---

## 트러블슈팅

### 문제 1: Pod를 찾을 수 없음

**증상**:
```
Error from server (NotFound): pods "analytics-db-xxx" not found
```

**해결 방법**:
```bash
# 올바른 네임스페이스 확인
oc get namespaces | grep workshop

# Pod 목록 다시 확인 (올바른 라벨 사용)
oc get pods -n workshop-eda -l deploymentconfig=analytics-db

# 또는 모든 analytics-db Pod 검색
oc get pods --all-namespaces | grep analytics-db

# Pod 상태 확인
oc get pods -n workshop-eda -l deploymentconfig=analytics-db -o wide
```

### 문제 2: 권한 오류

**증상**:
```
ERROR: permission denied for table event_count_summary
```

**해결 방법**:
```bash
# postgres 사용자로 실행
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U postgres -d analytics -f /tmp/migration.sql

# 또는 권한 부여
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U postgres -d analytics -c "GRANT ALL ON TABLE event_count_summary TO analytics;"
```

### 문제 3: 테이블이 이미 존재함

**증상**:
```
ERROR: relation "event_count_summary" already exists
```

**해결 방법**:
```bash
# 테이블 존재 여부 확인
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "\dt event_count_summary"

# 이미 존재하면 데이터만 업데이트
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "
    INSERT INTO event_count_summary (event_type, count, last_updated)
    SELECT event_type, COUNT(*), CURRENT_TIMESTAMP
    FROM event_log
    GROUP BY event_type
    ON CONFLICT (event_type) DO UPDATE
    SET count = EXCLUDED.count, last_updated = EXCLUDED.last_updated;
  "
```

### 문제 4: 연결 타임아웃

**증상**:
```
Error: connection timeout
```

**해결 방법**:
```bash
# Pod 상태 확인
oc describe pod $POD_NAME -n $NAMESPACE

# Pod 로그 확인
oc logs $POD_NAME -n $NAMESPACE

# 필요시 Pod 재시작
oc delete pod $POD_NAME -n $NAMESPACE
# (StatefulSet/Deployment가 자동으로 재생성)
```

### 문제 5: 마이그레이션 중 데이터 불일치

**증상**:
```
ERROR: duplicate key value violates unique constraint
```

**해결 방법**:
```bash
# 기존 데이터 확인
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "SELECT * FROM event_count_summary;"

# 테이블 비우고 다시 시도
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "TRUNCATE TABLE event_count_summary;"

# 마이그레이션 재실행
./scripts/apply-db-migration.sh
```

---

## 마이그레이션 검증

### 1. 테이블 구조 검증

```bash
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "
    SELECT 
      column_name, 
      data_type, 
      is_nullable,
      column_default
    FROM information_schema.columns
    WHERE table_name = 'event_count_summary'
    ORDER BY ordinal_position;
  "
```

### 2. 인덱스 검증

```bash
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "
    SELECT 
      indexname, 
      indexdef
    FROM pg_indexes
    WHERE tablename = 'event_count_summary';
  "
```

### 3. 데이터 정합성 검증

```bash
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "
    -- event_log와 event_count_summary 비교
    SELECT 
      el.event_type,
      COUNT(*) as actual_count,
      ecs.count as summary_count,
      COUNT(*) - ecs.count as difference
    FROM event_log el
    LEFT JOIN event_count_summary ecs ON el.event_type = ecs.event_type
    GROUP BY el.event_type, ecs.count
    HAVING COUNT(*) != COALESCE(ecs.count, 0);
  "
```

### 4. 성능 검증

```bash
# 쿼리 실행 계획 확인
oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c "
    EXPLAIN ANALYZE
    SELECT event_type, count FROM event_count_summary
    WHERE event_type = 'ORDER_CREATED';
  "
```

---

## 베스트 프랙티스

### 1. 마이그레이션 전 백업

```bash
# 데이터베이스 백업
oc exec -n $NAMESPACE $POD_NAME -- \
  pg_dump -U analytics analytics > backup_$(date +%Y%m%d_%H%M%S).sql

# 특정 테이블만 백업
oc exec -n $NAMESPACE $POD_NAME -- \
  pg_dump -U analytics -t event_log analytics > event_log_backup.sql
```

### 2. 트랜잭션 사용

마이그레이션 스크립트에 트랜잭션 추가:

```sql
BEGIN;

-- 마이그레이션 작업
CREATE TABLE IF NOT EXISTS event_count_summary (...);

-- 검증
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM event_count_summary LIMIT 1) THEN
    RAISE EXCEPTION 'Migration validation failed';
  END IF;
END $$;

COMMIT;
```

### 3. 단계별 실행

복잡한 마이그레이션은 단계별로 분리:

```bash
# 1단계: 테이블 생성
./scripts/apply-db-migration.sh infra/sql/step1-create-table.sql

# 2단계: 데이터 마이그레이션
./scripts/apply-db-migration.sh infra/sql/step2-migrate-data.sql

# 3단계: 인덱스 생성
./scripts/apply-db-migration.sh infra/sql/step3-create-indexes.sql
```

### 4. 모니터링

```bash
# 마이그레이션 중 DB 상태 모니터링
watch -n 2 "oc exec -n $NAMESPACE $POD_NAME -- \
  psql -U analytics -d analytics -c 'SELECT COUNT(*) FROM event_count_summary;'"
```

---

## 참고 자료

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [OpenShift CLI 가이드](https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.html)
- 프로젝트 README: `../README.md`
- 배포 가이드: `./OCP_DEPLOYMENT_GUIDE.md`

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 2026-03-09 | 1.1 | Pod 라벨을 실제 환경에 맞게 수정 (deploymentconfig=analytics-db) | Bob |
| 2026-03-09 | 1.0 | 초기 문서 작성 및 event_count_summary 마이그레이션 추가 | Bob |