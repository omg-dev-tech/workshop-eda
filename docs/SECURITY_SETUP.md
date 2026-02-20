# 민감 정보 보안 설정 가이드

## 방법 1: HashiCorp Vault + Spring Cloud Vault (추천)

### 1.1 Vault 설치 및 설정

```bash
# Docker로 Vault 실행
docker run -d --name vault \
  --cap-add=IPC_LOCK \
  -e 'VAULT_DEV_ROOT_TOKEN_ID=myroot' \
  -p 8200:8200 \
  vault:latest

# Vault CLI 설치 (macOS)
brew install vault

# Vault 환경변수 설정
export VAULT_ADDR='http://localhost:8200'
export VAULT_TOKEN='myroot'
```

### 1.2 Vault에 시크릿 저장

```bash
# KV v2 시크릿 엔진 활성화
vault secrets enable -path=secret kv-v2

# DB 비밀번호 저장
vault kv put secret/order-service \
  db.password=orderspw \
  db.username=orders

vault kv put secret/inventory-service \
  db.password=invpw \
  db.username=inventory

vault kv put secret/fulfillment-service \
  db.password=fulpw \
  db.username=fulfillment

vault kv put secret/analytics-service \
  db.password=analyticspw \
  db.username=analytics

vault kv put secret/notification-service \
  db.password=notificationpw \
  db.username=notifications

# OTEL/Instana 키 저장
vault kv put secret/observability \
  otel.endpoint=https://your-otel-endpoint \
  instana.key=your-instana-key
```

### 1.3 Spring Boot 의존성 추가

각 서비스의 `build.gradle`에 추가:

```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-vault-config:4.1.0'
}
```

### 1.4 bootstrap.properties 설정

각 서비스에 `src/main/resources/bootstrap.properties` 생성:

**order-service/src/main/resources/bootstrap.properties:**
```properties
spring.application.name=order-service
spring.cloud.vault.uri=http://localhost:8200
spring.cloud.vault.token=${VAULT_TOKEN:myroot}
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.default-context=order-service
```

### 1.5 application.properties 수정

민감한 정보를 Vault 참조로 변경:

```properties
# 기존
spring.datasource.password=${DB_PASS:orderspw}

# 변경 후 (Vault에서 자동 주입)
spring.datasource.password=${db.password}
spring.datasource.username=${db.username}
```

---

## 방법 2: Jasypt (Java Simplified Encryption)

간단하고 빠르게 적용 가능한 방법입니다.

### 2.1 의존성 추가

각 서비스의 `build.gradle`에 추가:

```gradle
dependencies {
    implementation 'com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5'
}
```

### 2.2 암호화 키 생성 및 설정

```bash
# 암호화 마스터 키 생성 (안전한 곳에 보관)
export JASYPT_ENCRYPTOR_PASSWORD="your-strong-master-key-here"
```

### 2.3 비밀번호 암호화

```bash
# Jasypt CLI 다운로드
wget https://github.com/jasypt/jasypt/releases/download/jasypt-1.9.3/jasypt-1.9.3-dist.zip
unzip jasypt-1.9.3-dist.zip

# 비밀번호 암호화
cd jasypt-1.9.3/bin
./encrypt.sh input="orderspw" password="your-strong-master-key-here" algorithm=PBEWithMD5AndDES

# 출력 예시:
# ----ENVIRONMENT-----------------
# Runtime: Oracle Corporation Java HotSpot(TM) 64-Bit Server VM 11.0.12+8-LTS-237
# ----ARGUMENTS-------------------
# input: orderspw
# password: your-strong-master-key-here
# ----OUTPUT----------------------
# 5fJQq7fXYNqKwvLY8K9XZA==
```

### 2.4 .env 파일 수정

```properties
# 기존
ORDER_DB_PASS=orderspw

# 변경 후
ORDER_DB_PASS=ENC(5fJQq7fXYNqKwvLY8K9XZA==)
INV_DB_PASS=ENC(암호화된_값)
FUL_DB_PASS=ENC(암호화된_값)
```

### 2.5 application.properties 설정

```properties
# Jasypt 설정
jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}
jasypt.encryptor.algorithm=PBEWithMD5AndDES
jasypt.encryptor.iv-generator-classname=org.jasypt.iv.NoIvGenerator

# 기존 설정 유지 (ENC()로 감싸진 값은 자동 복호화됨)
spring.datasource.password=${DB_PASS}
```

### 2.6 Docker Compose 수정

```yaml
order-service:
  environment:
    JASYPT_ENCRYPTOR_PASSWORD: ${JASYPT_ENCRYPTOR_PASSWORD}
    DB_PASS: ${ORDER_DB_PASS}
```

### 2.7 실행

```bash
# 마스터 키를 환경변수로 설정하고 실행
export JASYPT_ENCRYPTOR_PASSWORD="your-strong-master-key-here"
docker-compose up -d
```

---

## 방법 3: AWS Secrets Manager / Azure Key Vault (클라우드 환경)

### 3.1 AWS Secrets Manager 사용

**의존성 추가:**
```gradle
dependencies {
    implementation 'com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:1.0.8'
}
```

**application.properties:**
```properties
spring.datasource.url=jdbc-secretsmanager:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=orders
spring.datasource.driver-class-name=com.amazonaws.secretsmanager.sql.AWSSecretsManagerPostgreSQLDriver
```

### 3.2 Azure Key Vault 사용

**의존성 추가:**
```gradle
dependencies {
    implementation 'com.azure.spring:spring-cloud-azure-starter-keyvault-secrets:5.8.0'
}
```

**application.properties:**
```properties
spring.cloud.azure.keyvault.secret.endpoint=https://your-keyvault.vault.azure.net/
spring.datasource.password=${db-password}
```

---

## 방법 4: git-crypt (Git 레벨 암호화)

### 4.1 설치

```bash
# macOS
brew install git-crypt

# Ubuntu
sudo apt-get install git-crypt
```

### 4.2 초기화 및 설정

```bash
# 프로젝트 루트에서
cd /Users/hansol/Workspace/TXC/0903_LG/order

# git-crypt 초기화
git-crypt init

# GPG 키 생성 (없는 경우)
gpg --gen-key

# 사용자 추가
git-crypt add-gpg-user your-email@example.com

# .gitattributes 파일 생성
echo ".env filter=git-crypt diff=git-crypt" >> .gitattributes
echo "*.key filter=git-crypt diff=git-crypt" >> .gitattributes
echo "*.pem filter=git-crypt diff=git-crypt" >> .gitattributes

# 커밋
git add .gitattributes
git commit -m "Add git-crypt configuration"

# 암호화 적용
git-crypt lock
```

### 4.3 사용법

```bash
# 암호화된 파일 잠금
git-crypt lock

# 암호화된 파일 해제
git-crypt unlock

# 다른 개발자에게 접근 권한 부여
git-crypt add-gpg-user other-developer@example.com
```

---

## 방법 5: SOPS (Secrets OPerationS) - Mozilla

### 5.1 설치

```bash
# macOS
brew install sops

# 또는 직접 다운로드
curl -LO https://github.com/mozilla/sops/releases/download/v3.8.1/sops-v3.8.1.darwin.amd64
chmod +x sops-v3.8.1.darwin.amd64
sudo mv sops-v3.8.1.darwin.amd64 /usr/local/bin/sops
```

### 5.2 .env 파일 암호화

```bash
# GPG 키로 암호화
sops -e -i .env

# 복호화하여 보기
sops .env

# 복호화하여 실행
sops exec-env .env 'docker-compose up -d'
```

### 5.3 .sops.yaml 설정

```yaml
creation_rules:
  - path_regex: \.env$
    pgp: 'YOUR_GPG_FINGERPRINT'
  - path_regex: \.key$
    pgp: 'YOUR_GPG_FINGERPRINT'
```

---

## 권장 사항

### 개발 환경
- **로컬 개발**: Jasypt (간단하고 빠름)
- **팀 협업**: git-crypt 또는 SOPS

### 프로덕션 환경
- **온프레미스**: HashiCorp Vault
- **AWS**: AWS Secrets Manager
- **Azure**: Azure Key Vault
- **GCP**: Google Secret Manager

### 보안 체크리스트

1. ✅ `.env` 파일을 `.gitignore`에 추가 (이미 완료)
2. ✅ `.env.example`에는 실제 값 대신 플레이스홀더 사용
3. ✅ 프로덕션 환경에서는 환경변수나 시크릿 관리 도구 사용
4. ✅ 정기적으로 비밀번호 로테이션
5. ✅ 접근 권한 최소화 (Principle of Least Privilege)
6. ✅ 감사 로그 활성화

---

## 빠른 시작 (Jasypt 적용)

가장 빠르게 적용할 수 있는 Jasypt 방법:

```bash
# 1. 마스터 키 생성 및 저장
echo "JASYPT_ENCRYPTOR_PASSWORD=MySecureKey123!" >> ~/.bash_profile
source ~/.bash_profile

# 2. 각 서비스 build.gradle에 의존성 추가
# implementation 'com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5'

# 3. 비밀번호 암호화 (온라인 도구 사용 가능)
# https://www.devglan.com/online-tools/jasypt-online-encryption-decryption

# 4. .env 파일 업데이트
# ORDER_DB_PASS=ENC(암호화된값)

# 5. 실행
docker-compose up -d
```

---

## 추가 리소스

- [HashiCorp Vault 문서](https://www.vaultproject.io/docs)
- [Jasypt 문서](http://www.jasypt.org/)
- [git-crypt GitHub](https://github.com/AGWA/git-crypt)
- [SOPS GitHub](https://github.com/mozilla/sops)
- [Spring Cloud Vault](https://spring.io/projects/spring-cloud-vault)