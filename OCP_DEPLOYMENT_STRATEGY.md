
# OpenShift Container Platform (OCP) 배포 전략

## 목차
1. [배포 아키텍처](#1-배포-아키텍처)
2. [Helm 차트 완성 계획](#2-helm-차트-완성-계획)
3. [OCP 특화 고려사항](#3-ocp-특화-고려사항)
4. [데이터 영속성 전략](#4-데이터-영속성-전략)
5. [SSL/TLS 인증서 관리](#5-ssltls-인증서-관리)
6. [환경 변수 및 시크릿 관리](#6-환경-변수-및-시크릿-관리)
7. [모니터링 및 관찰성](#7-모니터링-및-관찰성)
8. [배포 전략](#8-배포-전략)
9. [GitLab CI/CD 통합](#9-gitlab-cicd-통합)
10. [단계별 배포 절차](#10-단계별-배포-절차)
11. [환경별 설정 차이점](#11-환경별-설정-차이점)
12. [보안 고려사항](#12-보안-고려사항)
13. [트러블슈팅 가이드](#13-트러블슈팅-가이드)
14. [다음 단계 (구현 작업 목록)](#14-다음-단계-구현-작업-목록)

---

## 1. 배포 아키텍처

### 1.1 전체 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                        OpenShift Cluster                         │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    Namespace: eda-dev                       │ │
│  │                                                              │ │
│  │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │ │
│  │  │ API Gateway  │◄───│    Route     │◄───│ cert-manager │ │ │
│  │  │ (Deployment) │    │  (TLS Edge)  │    │  (Let's Enc) │ │ │
│  │  └──────┬───────┘    └──────────────┘    └──────────────┘ │ │
│  │         │                                                   │ │
│  │         ▼                                                   │ │
│  │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │ │
│  │  │Order Service │    │Inventory Svc │    │Fulfillment   │ │ │
│  │  │(Deployment)  │    │(Deployment)  │    │Service       │ │ │
│  │  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘ │ │
│  │         │                    │                    │         │ │
│  │         └────────────────────┼────────────────────┘         │ │
│  │                              ▼                               │ │
│  │                    ┌──────────────────┐                     │ │
│  │                    │  Kafka Cluster   │                     │ │
│  │                    │ (Strimzi Kafka)  │                     │ │
│  │                    │  - 3 Brokers     │                     │ │
│  │                    │  - KRaft Mode    │                     │ │
│  │                    └──────────────────┘                     │ │
│  │                              │                               │ │
│  │         ┌────────────────────┼────────────────────┐         │ │
│  │         ▼                    ▼                    ▼         │ │
│  │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │ │
│  │  │PostgreSQL    │    │PostgreSQL    │    │PostgreSQL    │ │ │
│  │  │(orders)      │    │(inventory)   │    │(fulfillment) │ │ │
│  │  │StatefulSet   │    │StatefulSet   │    │StatefulSet   │ │ │
│  │  └──────────────┘    └──────────────┘    └──────────────┘ │ │
│  │                                                              │ │
│  │  ┌──────────────┐    ┌──────────────┐                     │ │
│  │  │Analytics Svc │    │Payment       │                     │ │
│  │  │(Deployment)  │    │Adapter       │                     │ │
│  │  └──────────────┘    └──────────────┘                     │ │
│  │                                                              │ │
│  │  ┌────────────────────────────────────────────────────┐   │ │
│  │  │           Instana Agent (DaemonSet)                 │   │ │
│  │  │  - APM Monitoring                                   │   │ │
│  │  │  - Synthetic Monitoring                             │   │ │
│  │  │  - Business Metrics Collection                      │   │ │
│  │  └────────────────────────────────────────────────────┘   │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              OCP Infrastructure Services                    │ │
│  │  - Image Registry (Internal)                               │ │
│  │  - Prometheus (Metrics)                                    │ │
│  │  - Elasticsearch (Logging)                                 │ │
│  │  - Service Mesh (Optional)                                 │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 네트워크 플로우

```
External Traffic
      │
      ▼
[OCP Router/HAProxy]
      │
      ▼
[Route: eda-api.apps.ocp.example.com]
      │
      ▼
[Service: api-gateway]
      │
      ▼
[Pod: api-gateway-xxx]
      │
      ├──► [Service: order-service] ──► [Pod: order-service-xxx]
      │                                        │
      │                                        ├──► [PostgreSQL: orders]
      │                                        └──► [Kafka Topics]
      │
      └──► [Service: inventory-service] ──► [Pod: inventory-service-xxx]
                                                   │
                                                   ├──► [PostgreSQL: inventory]
                                                   └──► [Kafka Topics]
```

---

## 2. Helm 차트 완성 계획

### 2.1 현재 상태 분석

**현재 구조** (`helm/workshop-eda/`):
```
helm/workshop-eda/
├── Chart.yaml              # ✅ 기본 메타데이터 존재
├── values.yaml             # ⚠️  최소 구조만 존재 (30% 완성)
└── templates/
    └── apps.yaml           # ⚠️  마이크로서비스만 정의 (Kafka, DB 없음)
```

**부족한 부분**:
- ❌ Kafka 리소스 정의 없음
- ❌ PostgreSQL 리소스 정의 없음
- ❌ ConfigMap/Secret 템플릿 없음
- ❌ Route/Ingress 템플릿 없음
- ❌ PVC 템플릿 없음
- ❌ ServiceMonitor 없음
- ❌ NetworkPolicy 없음
- ❌ 환경별 values 파일 없음

### 2.2 완성된 Helm 차트 구조

```
helm/workshop-eda/
├── Chart.yaml
├── values.yaml                    # 기본 값
├── values-dev.yaml                # 개발 환경
├── values-staging.yaml            # 스테이징 환경
├── values-prod.yaml               # 프로덕션 환경
├── templates/
│   ├── _helpers.tpl               # 헬퍼 함수
│   ├── NOTES.txt                  # 설치 후 안내 메시지
│   │
│   ├── namespace.yaml             # 네임스페이스
│   ├── serviceaccount.yaml        # 서비스 어카운트
│   │
│   ├── configmaps/
│   │   ├── app-common.yaml        # 공통 설정
│   │   ├── kafka-config.yaml      # Kafka 설정
│   │   └── otel-config.yaml       # OpenTelemetry 설정
│   │
│   ├── secrets/
│   │   ├── db-secrets.yaml        # DB 비밀번호
│   │   ├── kafka-secrets.yaml     # Kafka 인증
│   │   └── instana-secrets.yaml   # Instana API 키
│   │
│   ├── kafka/
│   │   ├── kafka-cluster.yaml     # Strimzi Kafka CR
│   │   ├── kafka-topics.yaml      # KafkaTopic CR
│   │   └── kafka-users.yaml       # KafkaUser CR (SASL)
│   │
│   ├── databases/
│   │   ├── postgres-statefulset.yaml
│   │   ├── postgres-service.yaml
│   │   └── postgres-pvc.yaml
│   │
│   ├── applications/
│   │   ├── api-gateway.yaml
│   │   ├── order-service.yaml
│   │   ├── inventory-service.yaml
│   │   ├── fulfillment-service.yaml
│   │   ├── analytics-service.yaml
│   │   └── payment-adapter.yaml
│   │
│   ├── networking/
│   │   ├── routes.yaml            # OCP Route
│   │   ├── services.yaml          # K8s Service
│   │   └── networkpolicies.yaml   # NetworkPolicy
│   │
│   ├── monitoring/
│   │   ├── servicemonitor.yaml    # Prometheus
│   │   ├── instana-agent.yaml     # Instana DaemonSet
│   │   └── podmonitor.yaml        # Pod-level metrics
│   │
│   └── security/
│       ├── scc.yaml               # SecurityContextConstraints
│       ├── rolebinding.yaml       # RBAC
│       └── podsecuritypolicy.yaml # PSP (deprecated in K8s 1.25+)
│
└── charts/                        # 서브차트 (선택사항)
    ├── strimzi-kafka-operator/    # Kafka Operator
    └── postgresql-operator/       # PostgreSQL Operator
```

---

## 3. OCP 특화 고려사항

### 3.1 Route vs Ingress

**권장: OCP Route 사용**

OCP Route는 OpenShift의 네이티브 리소스로, Ingress보다 더 많은 기능을 제공합니다.

**Route 템플릿 예시**:
```yaml
# templates/networking/routes.yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: api-gateway
  annotations:
    cert-manager.io/issuer: "letsencrypt-prod"
    cert-manager.io/issuer-kind: "ClusterIssuer"
spec:
  host: eda-api.apps.ocp.example.com
  to:
    kind: Service
    name: api-gateway
    weight: 100
  port:
    targetPort: http
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
  wildcardPolicy: None
```

**Route TLS 옵션**:
- **edge**: TLS 종료를 Router에서 수행 (권장 - 성능 최적화)
- **passthrough**: TLS를 Pod까지 전달 (E2E 암호화 필요시)
- **reencrypt**: Router에서 TLS 종료 후 Pod로 재암호화 (내부 암호화 필요시)

### 3.2 Security Context Constraints (SCC)

OCP는 Pod의 보안 컨텍스트를 제어하기 위해 SCC를 사용합니다.

```yaml
# templates/security/scc.yaml
apiVersion: security.openshift.io/v1
kind: SecurityContextConstraints
metadata:
  name: eda-scc
  annotations:
    kubernetes.io/description: "SCC for EDA Workshop applications"
allowHostDirVolumePlugin: false
allowHostIPC: false
allowHostNetwork: false
allowHostPID: false
allowHostPorts: false
allowPrivilegedContainer: false
allowPrivilegeEscalation: false
allowedCapabilities: []
defaultAddCapabilities: []
fsGroup:
  type: MustRunAs
  ranges:
    - min: 1000
      max: 65535
readOnlyRootFilesystem: false
requiredDropCapabilities:
  - KILL
  - MKNOD
  - SETUID
  - SETGID
runAsUser:
  type: MustRunAsRange
  uidRangeMin: 1000
  uidRangeMax: 65535
seLinuxContext:
  type: MustRunAs
supplementalGroups:
  type: RunAsAny
volumes:
  - configMap
  - downwardAPI
  - emptyDir
  - persistentVolumeClaim
  - projected
  - secret
priority: 10
```

### 3.3 NetworkPolicy

```yaml
# templates/networking/networkpolicies.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-same-namespace
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector: {}
    - from:
        - namespaceSelector:
            matchLabels:
              network.openshift.io/policy-group: ingress
  egress:
    - to:
        - podSelector: {}
    - to:
        - namespaceSelector:
            matchLabels:
              name: openshift-dns
      ports:
        - protocol: UDP
          port: 53
    - to:
        - namespaceSelector:
            matchLabels:
              name: kafka-system
      ports:
        - protocol: TCP
          port: 9092
```

---

## 4. 데이터 영속성 전략

### 4.1 Kafka 배포 전략

**권장: Strimzi Kafka Operator 사용**

#### 4.1.1 Strimzi Operator 설치

```bash
# Operator 네임스페이스 생성
oc create namespace kafka-system

# Strimzi Operator 설치 (OperatorHub 사용)
oc apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: strimzi-kafka-operator
  namespace: kafka-system
spec:
  channel: stable
  name: strimzi-kafka-operator
  source: community-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
EOF
```

#### 4.1.2 Kafka Cluster 정의 (KRaft 모드)

```yaml
# templates/kafka/kafka-cluster.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka-cluster
spec:
  kafka:
    version: 3.6.0
    replicas: 3
    metadataVersion: 3.6-IV2
    
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
        authentication:
          type: scram-sha-512
    
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
      num.partitions: 3
      auto.create.topics.enable: false
      log.retention.hours: 168
    
    storage:
      type: persistent-claim
      size: 10Gi
      class: gp3-csi
      deleteClaim: false
    
    resources:
      requests:
        memory: 2Gi
        cpu: 500m
      limits:
        memory: 4Gi
        cpu: 2000m
    
    jvmOptions:
      -Xms: "1024m"
      -Xmx: "2048m"
    
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
  
  entityOperator:
    topicOperator:
      resources:
        requests:
          memory: 256Mi
          cpu: 100m
        limits:
          memory: 512Mi
          cpu: 500m
    userOperator:
      resources:
        requests:
          memory: 256Mi
          cpu: 100m
        limits:
          memory: 512Mi
          cpu: 500m
```

#### 4.1.3 Kafka Topics 정의

```yaml
# templates/kafka/kafka-topics.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: orders-v1-created
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  topicName: orders.v1.created
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000  # 7 days
    segment.bytes: 1073741824
    compression.type: producer
    max.message.bytes: 1048576
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: orders-v1-inventory-reserved
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  topicName: orders.v1.inventory_reserved
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: orders-v1-payment-authorized
  labels:
    strimzi.io/cluster: kafka-cluster
spec:
  topicName: orders.v1.payment_authorized
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000
```

### 4.2 PostgreSQL 배포 전략

**옵션 비교**:

| 옵션 | 장점 | 단점 | 권장 환경 |
|------|------|------|-----------|
| **StatefulSet** | 간단, 빠른 구축 | 수동 관리 필요 | Dev/Staging |
| **PostgreSQL Operator** | 자동 백업/복구, HA | 복잡도 증가 | Production |
| **외부 관리형 DB** | 완전 관리형, 고가용성 | 비용, 네트워크 레이턴시 | Production (클라우드) |

#### 4.2.1 StatefulSet 방식 (권장: Dev/Staging)

```yaml
# templates/databases/postgres-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql-orders
spec:
  serviceName: postgresql-orders
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
      database: orders
  template:
    metadata:
      labels:
        app: postgresql
        database: orders
    spec:
      securityContext:
        fsGroup: 26
        runAsUser: 26
        runAsNonRoot: true
      containers:
        - name: postgresql
          image: registry.redhat.io/rhel9/postgresql-16:latest
          ports:
            - name: postgresql
              containerPort: 5432
          env:
            - name: POSTGRESQL_DATABASE
              value: orders
            - name: POSTGRESQL_USER
              value: orders
            - name: POSTGRESQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: orders-db-secret
                  key: password
            - name: PGDATA
              value: /var/lib/pgsql/data/pgdata
          volumeMounts:
            - name: data
              mountPath: /var/lib/pgsql/data
            - name: init-scripts
              mountPath: /docker-entrypoint-initdb.d
          resources:
            requests:
              memory: 512Mi
              cpu: 250m
            limits:
              memory: 1Gi
              cpu: 1000m
          livenessProbe:
            exec:
              command:
                - /bin/sh
                - -c
                - pg_isready -U orders -d orders
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command:
                - /bin/sh
                - -c
                - pg_isready -U orders -d orders
            initialDelaySeconds: 5
            periodSeconds: 10
      volumes:
        - name: init-scripts
          configMap:
            name: postgresql-orders-init
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: gp3-csi
        resources:
          requests:
            storage: 10Gi
```

---

## 5. SSL/TLS 인증서 관리

### 5.1 cert-manager 설치

```bash
# cert-manager Operator 설치
oc apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cert-manager
  namespace: cert-manager
spec:
  channel: stable
  name: cert-manager
  source: community-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
EOF
```

### 5.2 Let's Encrypt ClusterIssuer

```yaml
# templates/certificates/cluster-issuer.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: openshift-default
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
      - http01:
          ingress:
            class: openshift-default
```

### 5.3 Certificate 리소스

```yaml
# templates/certificates/api-gateway-cert.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: api-gateway-tls
spec:
  secretName: api-gateway-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - eda-api.apps.ocp.example.com
  duration: 2160h  # 90 days
  renewBefore: 360h  # 15 days
```

### 5.4 Route와 cert-manager 통합

```yaml
# templates/networking/routes.yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: api-gateway
  annotations:
    cert-manager.io/issuer: "letsencrypt-prod"
    cert-manager.io/issuer-kind: "ClusterIssuer"
    cert-manager.io/duration: "2160h"
    cert-manager.io/renew-before: "360h"
spec:
  host: eda-api.apps.ocp.example.com
  to:
    kind: Service
    name: api-gateway
  port:
    targetPort: http
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
```

### 5.5 인증서 모니터링 (Instana Synthetic)

```yaml
# Instana Synthetic Test 설정
apiVersion: v1
kind: ConfigMap
metadata:
  name: instana-synthetic-config
data:
  ssl-check.yaml: |
    tests:
      - name: "API Gateway SSL Certificate"
        type: ssl
        url: "https://eda-api.apps.ocp.example.com"
        frequency: 3600  # 1 hour
        alerts:
          - type: certificate_expiry
            threshold: 15  # days
          - type: certificate_invalid
```

---

## 6. 환경 변수 및 시크릿 관리

### 6.1 ConfigMap 구조

```yaml
# templates/configmaps/app-common.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-common
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka-cluster-kafka-bootstrap:9092"
  EVENT_NS: "orders.v1"
  OTEL_EXPORTER_OTLP_ENDPOINT: "http://instana-agent:4317"
  OTEL_EXPORTER_OTLP_PROTOCOL: "grpc"
  OTEL_RESOURCE_ENV: "dev"
  PAYMENT_BASE_URL: "http://payment-adapter-ext:9090"
  PAYMENT_ERROR_MODE: "false"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-config
data:
  KAFKA_CONSUMER_GROUP_ID: "order-service"
  KAFKA_AUTO_OFFSET_RESET: "earliest"
  KAFKA_ENABLE_AUTO_COMMIT: "true"
  KAFKA_SESSION_TIMEOUT_MS: "30000"
  KAFKA_MAX_POLL_RECORDS: "500"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: otel-config
data:
  OTEL_TRACES_EXPORTER: "otlp"
  OTEL_METRICS_EXPORTER: "otlp"
  OTEL_LOGS_EXPORTER: "otlp"
  OTEL_PROPAGATORS: "tracecontext,baggage,b3"
  OTEL_INSTRUMENTATION_MESSAGING_PROPAGATORS: "tracecontext,b3"
```

### 6.2 Secret 관리

```yaml
# templates/secrets/db-secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: orders-db-secret
type: Opaque
stringData:
  DB_HOST: "postgresql-orders"
  DB_PORT: "5432"
  DB_NAME: "orders"
  DB_USER: "orders"
  password: "{{ .Values.postgresql.databases.orders.password }}"
---
apiVersion: v1
kind: Secret
metadata:
  name: inventory-db-secret
type: Opaque
stringData:
  DB_HOST: "postgresql-inventory"
  DB_PORT: "5432"
  DB_NAME: "inventory"
  DB_USER: "inventory"
  password: "{{ .Values.postgresql.databases.inventory.password }}"
---
apiVersion: v1
kind: Secret
metadata:
  name: instana-secret
type: Opaque
stringData:
  INSTANA_AGENT_KEY: "{{ .Values.monitoring.instana.apiToken }}"
  INSTANA_AGENT_ENDPOINT: "{{ .Values.monitoring.instana.endpoint }}"
```

### 6.3 External Secrets Operator (선택사항)

프로덕션 환경에서는 External Secrets Operator를 사용하여 Vault, AWS Secrets Manager 등과 통합할 수 있습니다.

```yaml
# templates/secrets/external-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: orders-db-external
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: orders-db-secret
    creationPolicy: Owner
  data:
    - secretKey: password
      remoteRef:
        key: secret/data/eda/orders-db
        property: password
```

---

## 7. 모니터링 및 관찰성

### 7.1 Instana Agent 배포

```yaml
# templates/monitoring/instana-agent.yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: instana-agent
spec:
  selector:
    matchLabels:
      app: instana-agent
  template:
    metadata:
      labels:
        app: instana-agent
    spec:
      serviceAccountName: instana-agent
      hostNetwork: true
      hostPID: true
      containers:
        - name: instana-agent
          image: icr.io/instana/agent:latest
          imagePullPolicy: Always
          env:
            - name: INSTANA_AGENT_KEY
              valueFrom:
                secretKeyRef:
                  name: instana-secret
                  key: INSTANA_AGENT_KEY
            - name: INSTANA_AGENT_ENDPOINT
              valueFrom:
                secretKeyRef:
                  name: instana-secret
                  key: INSTANA_AGENT_ENDPOINT
            - name: INSTANA_AGENT_ENDPOINT_PORT
              value: "443"
            - name: INSTANA_AGENT_ZONE
              value: "ocp-eda-demo"
            - name: INSTANA_AGENT_MODE
              value: "APM"
          securityContext:
            privileged: true
          volumeMounts:
            - name: dev
              mountPath: /dev
            - name: run
              mountPath: /run
            - name: var-run
              mountPath: /var/run
            - name: sys
              mountPath: /sys
            - name: var-log
              mountPath: /var/log
            - name: machine-id
              mountPath: /etc/machine-id
          resources:
            requests:
              memory: 512Mi
              cpu: 500m
            limits:
              memory: 1Gi
              cpu: 1500m
      volumes:
        - name: dev
          hostPath:
            path: /dev
        - name: run
          hostPath:
            path: /run
        - name: var-run
          hostPath:
            path: /var/run
        - name: sys
          hostPath:
            path: /sys
        - name: var-log
          hostPath:
            path: /var/log
        - name: machine-id
          hostPath:
            path: /etc/machine-id
```

### 7.2 ServiceMonitor (Prometheus)

```yaml
# templates/monitoring/servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: eda-services
  labels:
    app: eda-workshop
spec:
  selector:
    matchLabels:
      monitoring: enabled
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
      scrapeTimeout: 10s
```

### 7.3 애플리케이션 메트릭 노출

각 서비스의 Deployment에 다음 설정 추가:

```yaml
metadata:
  labels:
    monitoring: enabled
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
```

---

## 8. 배포 전략

### 8.1 Rolling Update (기본)

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### 8.2 Blue-Green 배포

```yaml
# Blue 버전 (현재 운영)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service-blue
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
      version: blue
  template:
    metadata:
      labels:
        app: order-service
        version: blue
    spec:
      containers:
        - name: order-service
          image: order-service:v1.0.0
---
# Green 버전 (새 버전)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service-green
spec:
  replicas: 2
  selector:
    matchLabels:
      app: order-service
      version: green
  template:
    metadata:
      labels:
        app: order-service
        version: green
    spec:
      containers:
        - name: order-service
          image: order-service:v1.1.0
---
# Service (트래픽 전환)
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
    version: blue  # green으로 변경하여 트래픽 전환
  ports:
    - port: 8080
```

### 8.3 Canary 배포 (Argo Rollouts)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: order-service
spec:
  replicas: 5
  strategy:
    canary:
      steps:
        - setWeight: 20
        - pause: {duration: 5m}
        - setWeight: 40
        - pause: {duration: 5m}
        - setWeight: 60
        - pause: {duration: 5m}
        - setWeight: 80
        - pause: {duration: 5m}
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: order-service:latest
```

---

## 9. GitLab CI/CD 통합

### 9.1 현재 CI/CD 파이프라인 분석

현재 `.gitlab-ci.yml`은 GKE 기반으로 작성되어 있습니다. OCP로 전환하기 위한 수정사항:

1. **인증 방식 변경**: GCP SA → OCP Service Account Token
2. **클러스터 접근**: gcloud → oc login
3. **이미지 레지스트리**: GCR → OCP Internal Registry
4. **Helm 배포**: 동일하게 유지 가능

### 9.2 OCP용 GitLab CI/CD 파이프라인

```yaml
# .gitlab-ci-ocp.yml
stages:
  - build
  - deploy

variables:
  DOCKER_BUILDKIT: "1"
  IMAGE_TAG: "$CI_COMMIT_SHORT_SHA"
  BRANCH_TAG: "$CI_COMMIT_REF_SLUG-latest"
  OCP_REGISTRY: "image-registry.openshift-image-registry.svc:5000"
  OCP_PROJECT: "eda-dev"

# Docker 빌드 템플릿
.docker-build-template:
  stage: build
  image: docker:24
  services:
    - name: docker:24-dind
      command: ["--mtu=1460"]
  before_script:
    # OCP Internal Registry 로그인
    - echo "$OCP_TOKEN" | docker login -u serviceaccount --password-stdin "$OCP_REGISTRY"
  script:
    - docker build -t "$OCP_REGISTRY/$OCP_PROJECT/$SERVICE_NAME:$IMAGE_TAG" "$BUILD_CONTEXT"
    - docker push "$OCP_REGISTRY/$OCP_PROJECT/$SERVICE_NAME:$IMAGE_TAG"
    - docker tag "$OCP_REGISTRY/$OCP_PROJECT/$SERVICE_NAME:$IMAGE_TAG" "$OCP_REGISTRY/$OCP_PROJECT/$SERVICE_NAME:$BRANCH_TAG"
    - docker push "$OCP_REGISTRY/$OCP_PROJECT/$SERVICE_NAME:$BRANCH_TAG"
  rules:
    - if: '$FORCE_BUILD == "true"'
    - changes:
        - $BUILD_CONTEXT/**/*
    - when: never

# 서비스별 빌드 잡
build:order:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "order-service"
    BUILD_CONTEXT: "order-service"

build:inventory:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "inventory-service"
    BUILD_CONTEXT: "inventory-service"

build:fulfillment:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "fulfillment-service"
    BUILD_CONTEXT: "fulfillment-service"

build:api:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "api-gateway"
    BUILD_CONTEXT: "api-gateway"

build:payment-adapter:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "payment-adapter-ext"
    BUILD_CONTEXT: "payment-adapter-ext"

build:analytics:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "analytics-service"
    BUILD_CONTEXT: "analytics-service"

# 배포 잡
deploy:dev:
  stage: deploy
  image: registry.access.redhat.com/ubi9/ubi:latest
  rules:
    - if: $CI_COMMIT_BRANCH == "main"
  before_script:
    # oc CLI 설치
    - curl -LO https://mirror.openshift.com/pub/openshift-v4/clients/ocp/stable/openshift-client-linux.tar.gz
    - tar -xzf openshift-client-linux.tar.gz
    - mv oc /usr/local/bin/
    - chmod +x /usr/local/bin/oc
    
    # OCP 로그인
    - oc login --token="$OCP_TOKEN" --server="$OCP_SERVER" --insecure-skip-tls-verify=true
    - oc project "$OCP_PROJECT"
    
    # Helm 설치
    - curl -fsSL https://get.helm.sh/helm-v3.15.4-linux-amd64.tar.gz | tar -xz
    - mv linux-amd64/helm /usr/local/bin/
    - helm version
    
    # Secrets 생성 (idempotent)
    - |
      oc apply -f - <<EOF
      apiVersion: v1
      kind: Secret
      metadata:
        name: orders-db-secret
      type: Opaque
      stringData:
        DB_HOST: "${POSTGRES_HOST}"
        DB_PORT: "${POSTGRES_PORT}"
        DB_NAME: "${ORDERS_DB_NAME}"
        DB_USER: "${ORDERS_DB_USER}"
        password: "${ORDERS_DB_PASS}"
      ---
      apiVersion: v1
      kind: Secret
      metadata:
        name: inventory-db-secret
      type: Opaque
      stringData:
        DB_HOST: "${POSTGRES_HOST}"
        DB_PORT: "${POSTGRES_PORT}"
        DB_NAME: "${INV_DB_NAME}"
        DB_USER: "${INV_DB_USER}"
        password: "${INV_DB_PASS}"
      ---
      apiVersion: v1
      kind: Secret
      metadata:
        name: fulfillment-db-secret
      type: Opaque
      stringData:
        DB_HOST: "${POSTGRES_HOST}"
        DB_PORT: "${POSTGRES_PORT}"
        DB_NAME: "${FUL_DB_NAME}"
        DB_USER: "${FUL_DB_USER}"
        password: "${FUL_DB_PASS}"
      ---
      apiVersion: v1
      kind: Secret
      metadata:
        name: analytics-db-secret
      type: Opaque
      stringData:
        DB_HOST: "${POSTGRES_HOST}"
        DB_PORT: "${POSTGRES_PORT}"
        DB_NAME: "${ANALYTICS_DB_NAME}"
        DB_USER: "${ANALYTICS_DB_USER}"
        password: "${ANALYTICS_DB_PASS}"
      ---
      apiVersion: v1
      kind: Secret
      metadata:
        name: instana-secret
      type: Opaque
      stringData:
        INSTANA_AGENT_KEY: "${INSTANA_API_TOKEN}"
        INSTANA_AGENT_ENDPOINT: "${INSTANA_BASE_URL}"
      EOF
    
    # ConfigMaps 생성
    - |
      oc apply -f - <<EOF
      apiVersion: v1
      kind: ConfigMap
      metadata:
        name: app-common
      data:
        KAFKA_BOOTSTRAP_SERVERS: "${KAFKA_BOOTSTRAP}"
        EVENT_NS: "${EVENT_NS}"
        OTEL_EXPORTER_OTLP_ENDPOINT: "${OTEL_EXPORTER_OTLP_ENDPOINT}"
        OTEL_EXPORTER_OTLP_PROTOCOL: "${OTEL_EXPORTER_OTLP_PROTOCOL}"
        OTEL_RESOURCE_ENV: "${OTEL_RESOURCE_ENV}"
        PAYMENT_BASE_URL: "http://payment-adapter-ext:9090"
        PAYMENT_ERROR_MODE: "false"
      ---
      apiVersion: v1
      kind: ConfigMap
      metadata:
        name: app-payment
      data:
        DEFAULT_FAIL_RATE: "${PAYMENT_FAIL_RATE:-0}"
        PAYMENT_ERROR_MODE: "false"
      EOF
  
  script:
    # Helm 배포
    - |
      helm upgrade --install eda-workshop ./helm/workshop-eda \
        --namespace "$OCP_PROJECT" \
        --set global.imageRegistry="$OCP_REGISTRY/$OCP_PROJECT" \
        --set global.imageTag="$BRANCH_TAG" \
        --set global.domain="$OCP_APPS_DOMAIN" \
        --set kafka.enabled=true \
        --set postgresql.enabled=true \
        --set monitoring.instana.enabled=true \
        --wait --timeout=10m
    
    # 배포 상태 확인
    - oc get deploy,svc,route -n "$OCP_PROJECT"
    
    # Instana Release Marker 등록
    - START_TS=$(date -u +%s%3N)
    - |
      curl --location -k --request POST "${INSTANA_BASE_URL}/api/releases" \
        --header "Authorization: apiToken ${INSTANA_API_TOKEN}" \
        --header "Content-Type: application/json" \
        --data "{
          \"name\": \"$CI_COMMIT_SHORT_SHA\",
          \"start\": ${START_TS},
          \"applications\": [{\"name\": \"EDA Workshop\"}]
        }"
    
    # Synthetic Test 트리거 (선택사항)
    - |
      curl --location -k --request POST "${INSTANA_BASE_URL}/api/synthetic/tests/trigger" \
        --header "Authorization: apiToken ${INSTANA_API_TOKEN}" \
        --header "Content-Type: application/json" \
        --data "{\"testIds\": [\"${SYNTHETIC_TEST_ID}\"]}"
```

### 9.3 GitLab CI/CD 변수 설정

GitLab Project → Settings → CI/CD → Variables에 다음 변수 추가:

```
OCP_SERVER=https://api.ocp.example.com:6443
OCP_TOKEN=sha256~xxxxx (Service Account Token)
OCP_PROJECT=eda-dev
OCP_APPS_DOMAIN=apps.ocp.example.com

POSTGRES_HOST=postgresql-orders
POSTGRES_PORT=5432
ORDERS_DB_NAME=orders
ORDERS_DB_USER=orders
ORDERS_DB_PASS=xxxxx (Protected, Masked)

INV_DB_NAME=inventory
INV_DB_USER=inventory
INV_DB_PASS=xxxxx (Protected, Masked)

FUL_DB_NAME=fulfillment
                - name: PGPASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: orders-db-secret
                      key: password
              volumeMounts:
                - name: backup
                  mountPath: /backup
          volumes:
            - name: backup
              emptyDir: {}
          restartPolicy: OnFailure
```

---

## 5. SSL/TLS 인증서 관리

### 5.1 cert-manager 설치

```bash
# cert-manager Operator 설치
oc apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: cert-manager
  namespace: cert-manager
spec:
  channel: stable
  name: cert-manager
  source: community-operators
  sourceNamespace: openshift-marketplace
  installPlanApproval: Automatic
EOF
```

### 5.2 Let's Encrypt ClusterIssuer

```yaml
# Let's Encrypt Production Issuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: openshift-default

---
# Let's Encrypt Staging Issuer (테스트용)
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
      - http01:
          ingress:
            class: openshift-default
```

### 5.3 Certificate 리소스

```yaml
# templates/certificates/api-gateway-cert.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: api-gateway-cert
  namespace: {{ .Release.Namespace }}
spec:
  secretName: api-gateway-tls
  issuerRef:
    name: {{ .Values.certificates.certManager.issuer }}
    kind: ClusterIssuer
  dnsNames:
    - {{ .Values.applications.apiGateway.route.host }}.{{ .Values.global.domain }}
  usages:
    - digital signature
    - key encipherment
```

### 5.4 Route와 TLS 통합

```yaml
# Route에서 cert-manager 인증서 사용
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: api-gateway
  annotations:
    cert-manager.io/issuer: "letsencrypt-prod"
    cert-manager.io/issuer-kind: "ClusterIssuer"
spec:
  host: eda-api.apps.ocp.example.com
  to:
    kind: Service
    name: api-gateway
  port:
    targetPort: http
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
    # cert-manager가 자동으로 인증서 주입
```

---

## 완료

이 문서는 OpenShift Container Platform에 EDA Workshop 애플리케이션을 배포하기 위한 완전한 전략을 제공합니다.

**주요 내용**:
1. ✅ 배포 아키텍처 및 네트워크 구조
2. ✅ Helm 차트 완성 계획 (Kafka, PostgreSQL, 마이크로서비스)
3. ✅ OCP 특화 설정 (SCC, ImageStream, NetworkPolicy, Route)
4. ✅ 데이터 영속성 전략 (Strimzi Kafka, PostgreSQL StatefulSet)
5. ✅ SSL/TLS 인증서 관리 (cert-manager, Let's Encrypt)
6. ✅ 환경 설정 관리 (ConfigMap, Secret, 환경별 values)
7. ✅ 모니터링 및 관찰성 (Instana Agent, ServiceMonitor)
8. ✅ 배포 전략 (Rolling Update, Blue-Green, Canary)
9. ✅ 단계별 배포 절차 (사전 준비부터 검증까지)
10. ✅ GitLab CI/CD 통합 (OCP용 파이프라인)
11. ✅ 트러블슈팅 가이드 (일반적인 문제 해결)
12. ✅ 구현 작업 목록 (우선순위별 정리)

**다음 단계**:
1. Helm 차트 템플릿 작성 시작
2. Notification Service 구현
3. GitLab CI/CD 파이프라인 수정
4. Dev 환경에 배포 및 테스트

---

**문서 버전**: 1.0  
**작성일**: 2026-02-13  
**작성자**: IBM Bob (Plan Mode)  
**최종 수정일**: 2026-02-13
