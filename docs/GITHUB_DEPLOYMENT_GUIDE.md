# GitHub 배포 가이드

이 문서는 프로젝트를 GitHub에 푸시하고 GHCR(GitHub Container Registry)에 이미지를 자동으로 빌드하는 방법을 설명합니다.

## 목차
1. [사전 준비](#사전-준비)
2. [Git 초기화 및 커밋](#git-초기화-및-커밋)
3. [GitHub 리포지토리 생성 및 연결](#github-리포지토리-생성-및-연결)
4. [코드 푸시](#코드-푸시)
5. [GitHub Actions 확인](#github-actions-확인)
6. [GHCR 이미지 확인](#ghcr-이미지-확인)
7. [문제 해결](#문제-해결)

## 사전 준비

### 필수 요구사항
- Git 설치 확인
  ```bash
  git --version
  ```
- GitHub 계정
- GitHub CLI (선택사항, 편의를 위해 권장)
  ```bash
  gh --version
  ```

### GitHub 인증 설정
```bash
# GitHub CLI를 사용한 인증 (권장)
gh auth login

# 또는 SSH 키 설정
ssh-keygen -t ed25519 -C "your_email@example.com"
# 생성된 공개키를 GitHub에 등록
cat ~/.ssh/id_ed25519.pub
```

## Git 초기화 및 커밋

### 1. Git 저장소 초기화
```bash
# 프로젝트 디렉토리로 이동
cd /Users/hansol/Workspace/TXC/0903_LG/order

# Git 초기화 (이미 초기화되어 있다면 생략)
git init
```

### 2. 사용자 정보 설정
```bash
# 전역 설정 (한 번만 실행)
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 또는 이 프로젝트에만 적용
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### 3. 파일 추가 및 커밋
```bash
# 모든 파일 스테이징
git add .

# 커밋 생성
git commit -m "Initial commit: Add microservices and GitHub Actions workflow"
```

### 4. 기본 브랜치 이름 확인/변경
```bash
# 현재 브랜치 확인
git branch

# main으로 변경 (필요한 경우)
git branch -M main
```

## GitHub 리포지토리 생성 및 연결

### 방법 1: GitHub CLI 사용 (권장)
```bash
# 새 리포지토리 생성 및 자동 연결
gh repo create omg-dev-tech/workshop-eda --public --source=. --remote=origin

# 또는 private 리포지토리로 생성
gh repo create omg-dev-tech/workshop-eda --private --source=. --remote=origin
```

### 방법 2: 웹 UI 사용
1. GitHub 웹사이트에서 새 리포지토리 생성
   - 리포지토리 이름: `workshop-eda`
   - Organization: `omg-dev-tech` (또는 개인 계정)
   - Public 또는 Private 선택
   - README, .gitignore, license는 추가하지 않음 (이미 존재)

2. 로컬 저장소와 연결
   ```bash
   # HTTPS 사용
   git remote add origin https://github.com/omg-dev-tech/workshop-eda.git
   
   # 또는 SSH 사용 (SSH 키 설정 완료 시)
   git remote add origin git@github.com:omg-dev-tech/workshop-eda.git
   ```

3. 원격 저장소 확인
   ```bash
   git remote -v
   ```

## 코드 푸시

### 첫 번째 푸시
```bash
# main 브랜치를 원격 저장소에 푸시
git push -u origin main
```

### 이후 푸시
```bash
# 변경사항 커밋
git add .
git commit -m "Update: description of changes"

# 푸시
git push
```

## GitHub Actions 확인

### 1. Actions 탭 접속
1. GitHub 리포지토리 페이지로 이동
2. 상단의 **Actions** 탭 클릭
3. "Build and Push to GHCR" 워크플로우 확인

### 2. 워크플로우 실행 확인
- 푸시 직후 자동으로 워크플로우가 실행됩니다
- 각 서비스별로 병렬 빌드가 진행됩니다
- 빌드 상태:
  - 🟡 노란색: 진행 중
  - 🟢 초록색: 성공
  - 🔴 빨간색: 실패

### 3. 빌드 로그 확인
1. 워크플로우 실행 항목 클릭
2. 각 서비스 job 클릭하여 상세 로그 확인
3. 실패 시 에러 메시지 확인

### 4. 수동 실행 (필요 시)
```bash
# GitHub CLI 사용
gh workflow run build-and-push.yml

# 또는 웹 UI에서
# Actions > Build and Push to GHCR > Run workflow 버튼 클릭
```

## GHCR 이미지 확인

### 1. 패키지 페이지 접속
1. GitHub 리포지토리 페이지에서 오른쪽 사이드바의 **Packages** 섹션 확인
2. 또는 직접 URL 접속:
   ```
   https://github.com/orgs/omg-dev-tech/packages?repo_name=workshop-eda
   ```

### 2. 빌드된 이미지 목록
다음 6개 이미지가 생성되어야 합니다:
- `order-service`
- `inventory-service`
- `fulfillment-service`
- `payment-adapter-ext`
- `analytics-service`
- `api-gateway`

### 3. 이미지 태그 확인
각 이미지는 두 가지 태그를 가집니다:
- `latest`: 최신 빌드
- `<commit-sha>`: 특정 커밋 버전 (예: `abc1234`)

### 4. 이미지 Pull 테스트
```bash
# 최신 이미지 Pull
docker pull ghcr.io/omg-dev-tech/workshop-eda/order-service:latest

# 특정 버전 Pull
docker pull ghcr.io/omg-dev-tech/workshop-eda/order-service:abc1234

# 이미지 확인
docker images | grep workshop-eda
```

### 5. 패키지 가시성 설정
기본적으로 패키지는 private입니다. Public으로 변경하려면:
1. 패키지 페이지 접속
2. **Package settings** 클릭
3. **Change visibility** > **Public** 선택
4. 확인

## 문제 해결

### 워크플로우 실패 시

#### 1. 권한 오류
```
Error: denied: permission_denied
```
**해결방법:**
- 리포지토리 Settings > Actions > General
- Workflow permissions에서 "Read and write permissions" 선택
- "Allow GitHub Actions to create and approve pull requests" 체크

#### 2. Dockerfile 경로 오류
```
Error: failed to solve: failed to read dockerfile
```
**해결방법:**
- 각 서비스의 Dockerfile 존재 확인
- `.github/workflows/build-and-push.yml`의 경로 확인

#### 3. 빌드 실패
```
Error: failed to build
```
**해결방법:**
- 로컬에서 빌드 테스트
  ```bash
  cd order-service
  docker build -t test-order-service .
  ```
- 빌드 로그에서 구체적인 에러 확인

### Git 푸시 실패 시

#### 1. 인증 실패
```
remote: Permission denied
```
**해결방법:**
- GitHub 인증 재설정
  ```bash
  gh auth login
  ```
- 또는 SSH 키 확인

#### 2. 원격 저장소 연결 오류
```
fatal: 'origin' does not appear to be a git repository
```
**해결방법:**
- 원격 저장소 재설정
  ```bash
  git remote remove origin
  git remote add origin https://github.com/omg-dev-tech/workshop-eda.git
  ```

#### 3. 브랜치 충돌
```
error: failed to push some refs
```
**해결방법:**
- 원격 변경사항 먼저 가져오기
  ```bash
  git pull origin main --rebase
  git push
  ```

### GHCR 이미지 Pull 실패 시

#### 1. 인증 필요 (Private 패키지)
```
Error: unauthorized
```
**해결방법:**
- GitHub Personal Access Token 생성
  - Settings > Developer settings > Personal access tokens > Tokens (classic)
  - `read:packages` 권한 선택
- Docker 로그인
  ```bash
  echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
  ```

#### 2. 이미지 없음
```
Error: manifest unknown
```
**해결방법:**
- GitHub Actions 워크플로우가 성공적으로 완료되었는지 확인
- 패키지 페이지에서 이미지 존재 확인
- 태그 이름 확인 (대소문자 구분)

## 추가 정보

### GitHub Actions 워크플로우 구조
```yaml
# .github/workflows/build-and-push.yml
- 트리거: main 브랜치 푸시 시 자동 실행
- 병렬 빌드: 6개 서비스 동시 빌드
- 캐싱: GitHub Actions 캐시 사용으로 빌드 시간 단축
- 태그: latest + commit SHA
```

### 유용한 Git 명령어
```bash
# 현재 상태 확인
git status

# 커밋 히스토리 확인
git log --oneline

# 원격 저장소 정보 확인
git remote -v

# 브랜치 목록 확인
git branch -a

# 마지막 커밋 수정
git commit --amend

# 변경사항 임시 저장
git stash

# 임시 저장 복원
git stash pop
```

### 참고 문서
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [GHCR 문서](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [로컬 테스트 가이드](LOCAL_TESTING_GUIDE.md)

## 다음 단계

1. ✅ 코드를 GitHub에 푸시
2. ✅ GitHub Actions 워크플로우 실행 확인
3. ✅ GHCR에 이미지 빌드 확인
4. 🔄 OpenShift/Kubernetes에 배포 (별도 가이드 참조)
5. 🔄 Instana 모니터링 설정 (별도 가이드 참조)

---

**문의사항이나 문제가 있으면 GitHub Issues에 등록해주세요.**