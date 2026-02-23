# CI/CD 파이프라인 설정 가이드

이 문서는 GitHub Actions를 통한 자동화된 CI/CD 파이프라인 설정 방법을 설명합니다.

## 📋 목차

1. [개요](#개요)
2. [파이프라인 아키텍처](#파이프라인-아키텍처)
3. [GitHub Secrets 설정](#github-secrets-설정)
4. [Instana 설정](#instana-설정)
5. [워크플로우 실행](#워크플로우-실행)
6. [트러블슈팅](#트러블슈팅)

## 개요

### 파이프라인 기능

- ✅ **자동 이미지 빌드**: 모든 마이크로서비스 이미지를 GHCR에 자동 빌드 및 푸시
- ✅ **OCP 자동 배포**: Helm을 통한 OpenShift 자동 배포
- ✅ **Health Check**: 배포 후 자동 헬스 체크
- ✅ **Instana Synthetic 테스트**: 자동화된 E2E 테스트 실행
- ✅ **자동 롤백**: 실패 시 이전 버전으로 자동 롤백
- ✅ **알림**: Slack 알림 (선택사항)

### 트리거 조건

- `main` 브랜치에 push
- 수동 실행 (workflow_dispatch)

## 파이프라인 아키텍처

```
┌─────────────────┐
│  Git Push       │
│  (main branch)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Build Images    │ ◄── 모든 서비스 이미지 빌드
│ Push to GHCR    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Deploy to OCP   │ ◄── Helm upgrade 실행
│ (Helm upgrade)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Health Check    │ ◄── Pod 상태 및 API 확인
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Instana Tests   │ ◄── Synthetic 테스트 실행
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│Success │ │ Failed │
└────────┘ └───┬────┘
               │
               ▼
         ┌──────────┐
         │ Rollback │ ◄── 자동 롤백
         └──────────┘
```

## GitHub Secrets 설정

### 1. GitHub Repository Settings 접속

1. GitHub 리포지토리 페이지로 이동
2. **Settings** 탭 클릭
3. 왼쪽 메뉴에서 **Secrets and variables** → **Actions** 클릭
4. **New repository secret** 버튼 클릭

### 2. 필수 Secrets 설정

#### OpenShift 인증 정보

| Secret Name | 설명 | 예시 |
|------------|------|------|
| `OCP_SERVER` | OpenShift API 서버 URL | `https://api.your-cluster.com:6443` |
| `OCP_TOKEN` | OpenShift 로그인 토큰 | `sha256~xxxxxxxxxxxxx` |
| `OCP_PROJECT` | OpenShift 프로젝트 이름 | `workshop-eda` |

**OCP_TOKEN 획득 방법:**

```bash
# OpenShift 웹 콘솔에서:
# 1. 우측 상단 사용자 이름 클릭
# 2. "Copy login command" 선택
# 3. "Display Token" 클릭
# 4. token 값 복사

# 또는 CLI에서:
oc whoami -t
```

#### Instana 인증 정보

| Secret Name | 설명 | 예시 |
|------------|------|------|
| `INSTANA_API_TOKEN` | Instana API 토큰 | `xxxxxxxxxxxxxxxx` |
| `INSTANA_BASE_URL` | Instana API Base URL | `https://your-domain.instana.io/api` |
| `INSTANA_LOCATION_ID` | Instana 테스트 Location ID | `9B25iaaJLgJzWT9d3zI6` |
| `INSTANA_CLIENT_TEST_ID` | Client Scenario 테스트 ID | `wHPrxlLUSqaGUlDsCioc` |
| `INSTANA_ADMIN_TEST_ID` | Admin Scenario 테스트 ID | `test-id-2` |

**Instana API Token 생성 방법:**

1. Instana 콘솔 접속
2. **Settings** → **Team Settings** → **API Tokens**
3. **New API Token** 클릭
4. 권한 설정:
   - `Configuration of Synthetic Monitoring` 체크
   - `Access Synthetic Monitoring` 체크
5. 생성된 토큰 복사

**Instana Test ID 확인 방법:**

1. Instana 콘솔에서 **Synthetic Monitoring** 메뉴로 이동
2. 등록된 테스트 클릭
3. URL에서 테스트 ID 확인
   - 예: `https://your-tenant.instana.io/synthetics/tests/abc123def456`
   - Test ID: `abc123def456`

#### 알림 설정 (선택사항)

| Secret Name | 설명 | 예시 |
|------------|------|------|
| `SLACK_WEBHOOK_URL` | Slack Webhook URL | `https://hooks.slack.com/services/...` |

**Slack Webhook URL 생성 방법:**

1. Slack 워크스페이스에서 **Apps** 검색
2. **Incoming Webhooks** 앱 추가
3. 채널 선택 및 Webhook URL 생성
4. 생성된 URL 복사

### 3. Secrets 설정 확인

모든 Secrets가 올바르게 설정되었는지 확인:

```bash
# GitHub CLI 사용 (선택사항)
gh secret list
```

## Instana 설정

### 1. Synthetic Monitoring 스크립트 등록

#### Client Scenario 등록

1. Instana 콘솔 → **Synthetic Monitoring** 메뉴
2. **Add script** 클릭
3. **Upload file** 선택
4. `instana-synthetic/client-scenario.side` 파일 업로드
5. 설정:
   - **Name**: `Workshop EDA - Client Scenario`
   - **Frequency**: 5분
   - **Locations**: 원하는 위치 선택
   - **Timeout**: 300초
6. **Save** 클릭

#### Admin Scenario 등록

1. **Add script** 클릭
2. **Upload file** 선택
3. `instana-synthetic/admin-scenario.side` 파일 업로드
4. 설정:
   - **Name**: `Workshop EDA - Admin Scenario`
   - **Frequency**: 10분
   - **Locations**: 원하는 위치 선택
   - **Timeout**: 300초
5. **Save** 클릭

### 2. 스크립트 URL 수정

**중요**: 업로드한 스크립트의 URL을 실제 OCP Route URL로 수정해야 합니다.

1. 등록된 테스트 클릭
2. **Edit** 버튼 클릭
3. 스크립트 내의 URL 찾기:
   ```
   현재: http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com
   ```
4. 실제 OCP Route URL로 변경:
   ```bash
   # OCP에서 Route URL 확인
   oc get route web -n workshop-eda -o jsonpath='{.spec.host}'
   ```
5. 변경 후 **Save**

### 3. 알림 설정 (선택사항)

1. 테스트 설정에서 **Alerting** 탭
2. **Add Alert** 클릭
3. 조건 설정:
   - 연속 2회 실패 시 알림
   - 응답 시간 10초 초과 시 경고
4. 알림 채널 설정 (Email, Slack 등)

## 워크플로우 실행

### 자동 실행

`main` 브랜치에 코드를 push하면 자동으로 실행됩니다:

```bash
git add .
git commit -m "Update application"
git push origin main
```

### 수동 실행

1. GitHub 리포지토리 페이지로 이동
2. **Actions** 탭 클릭
3. **Deploy to OpenShift with Instana Testing** 워크플로우 선택
4. **Run workflow** 버튼 클릭
5. 옵션 선택:
   - **Skip Instana Synthetic Tests**: 테스트를 건너뛰려면 `true` 입력
6. **Run workflow** 클릭

### 실행 모니터링

1. **Actions** 탭에서 실행 중인 워크플로우 클릭
2. 각 Job의 진행 상황 확인:
   - ✅ Build and Push Images
   - ✅ Deploy to OCP
   - ✅ Health Check
   - ✅ Run Instana Synthetic Tests
   - ✅ Success Notification (성공 시)
   - ❌ Rollback (실패 시)

### 로그 확인

각 단계의 상세 로그를 확인하려면:

1. Job 이름 클릭
2. Step 이름 클릭하여 상세 로그 확인

## 트러블슈팅

### 1. OCP 로그인 실패

**증상:**
```
Error: Login failed
```

**해결 방법:**
1. `OCP_TOKEN`이 만료되지 않았는지 확인
2. `OCP_SERVER` URL이 올바른지 확인
3. 토큰 재생성:
   ```bash
   oc login --token=<new-token> --server=<server>
   oc whoami -t  # 새 토큰 확인
   ```

### 2. Helm 배포 실패

**증상:**
```
Error: UPGRADE FAILED
```

**해결 방법:**
1. Helm 차트 문법 확인:
   ```bash
   helm lint helm/workshop-eda
   ```
2. 수동 배포 테스트:
   ```bash
   helm upgrade workshop-eda helm/workshop-eda \
     --namespace workshop-eda \
     --dry-run --debug
   ```
3. 기존 릴리스 상태 확인:
   ```bash
   helm list -n workshop-eda
   helm history workshop-eda -n workshop-eda
   ```

### 3. Health Check 실패

**증상:**
```
Error: API Gateway health check failed
```

**해결 방법:**
1. Pod 상태 확인:
   ```bash
   oc get pods -n workshop-eda
   oc logs -f deployment/api-gateway -n workshop-eda
   ```
2. Route 확인:
   ```bash
   oc get route -n workshop-eda
   ```
3. 수동 헬스 체크:
   ```bash
   ROUTE_URL=$(oc get route api-gateway -n workshop-eda -o jsonpath='{.spec.host}')
   curl http://$ROUTE_URL/actuator/health
   ```

### 4. Instana 테스트 실패

**증상:**
```
Error: Test timeout or failed
```

**해결 방법:**
1. Instana 콘솔에서 테스트 결과 확인
2. 테스트 스크립트의 URL이 올바른지 확인
3. Web UI가 정상적으로 작동하는지 확인:
   ```bash
   ROUTE_URL=$(oc get route web -n workshop-eda -o jsonpath='{.spec.host}')
   curl http://$ROUTE_URL
   ```
4. 테스트 타임아웃 증가 (필요시):
   - 워크플로우 파일에서 `--timeout 600` → `--timeout 900`

### 5. 롤백 실패

**증상:**
```
Error: Rollback verification failed
```

**해결 방법:**
1. Helm 히스토리 확인:
   ```bash
   helm history workshop-eda -n workshop-eda
   ```
2. 수동 롤백:
   ```bash
   helm rollback workshop-eda <revision> -n workshop-eda
   ```
3. Pod 상태 확인:
   ```bash
   oc get pods -n workshop-eda
   ```

### 6. 이미지 빌드 실패

**증상:**
```
Error: failed to solve: failed to build
```

**해결 방법:**
1. Dockerfile 문법 확인
2. 로컬에서 빌드 테스트:
   ```bash
   docker build -t test-image ./order-service
   ```
3. GHCR 권한 확인:
   - Repository Settings → Actions → General
   - Workflow permissions: "Read and write permissions" 선택

## 성공 기준

### 배포 성공 조건

- ✅ 모든 이미지 빌드 성공
- ✅ GHCR 푸시 성공
- ✅ Helm upgrade 성공
- ✅ 모든 Pod Running 상태
- ✅ Health check 통과
- ✅ Instana Synthetic 테스트 통과

### 롤백 트리거 조건

- ❌ Helm upgrade 실패
- ❌ Pod가 5분 내 Ready 상태 미달성
- ❌ Health check 실패
- ❌ Instana 테스트 실패

## 추가 리소스

- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Helm 문서](https://helm.sh/docs/)
- [OpenShift CLI 문서](https://docs.openshift.com/container-platform/latest/cli_reference/openshift_cli/getting-started-cli.html)
- [Instana Synthetic Monitoring 문서](https://www.ibm.com/docs/en/instana-observability/current?topic=instana-synthetic-monitoring)

## 문의

문제가 지속되면 다음을 확인하세요:

1. GitHub Actions 로그
2. OpenShift Pod 로그
3. Instana 테스트 결과
4. Helm 릴리스 히스토리

---

**Made with Bob** 🤖