# IBM Bob 데모 영상 녹화 당일 체크리스트

**목적**: 녹화 당일 빠르게 확인할 수 있는 실행 체크리스트  
**사용법**: 각 항목을 순서대로 체크하며 진행  
**예상 소요 시간**: 준비 15분, 녹화 10분

---

## 📅 녹화 일정

- **날짜**: _______________
- **시간**: _______________
- **담당자**: _______________

---

## ⏰ 타임라인

```
D-Day 준비 시간표:

09:00 - 09:15  시스템 준비
09:15 - 09:25  IBM Bob 준비
09:25 - 09:30  최종 점검
09:30 - 09:40  본 녹화
09:40 - 09:45  녹화 파일 확인 및 백업
```

---

## 🔧 1단계: 시스템 준비 (15분 전)

### 1.1 하드웨어 체크

```bash
# Mac 전원
- [v] 전원 어댑터 연결
- [v] 배터리 100% 또는 충전 중

# 디스플레이
- [v] 해상도: 1920x1080
- [v] 밝기: 80-100%
- [v] Night Shift: 끄기
- [v] True Tone: 끄기

# 네트워크
- [v] 인터넷 연결 확인
- [v] 속도 테스트 (fast.com)
```

### 1.2 시스템 설정

```bash
# 알림 끄기
- [V] Do Not Disturb 활성화
- [V] Option + 알림 센터 아이콘 클릭

# Dock 설정
- [V] Dock 자동 숨김 활성화

# 화면 보호기/절전 모드
- [V] 화면 보호기: Never
- [V] 절전 모드: 끄기

# 불필요한 앱 종료
- [ ] Slack 종료
- [ ] Teams 종료
- [ ] Mail 종료
- [ ] 기타 백그라운드 앱 종료
```

---

## 🤖 2단계: IBM Bob 준비 (10분 전)

### 2.1 Bob 접속

```bash
# IBM Bob 채팅 인터페이스 접속
- [V] Bob 로그인 완료
- [V] 채팅 화면 정상 작동
- [ ] 프로젝트 컨텍스트 로드됨
- [ ] 네트워크 연결 안정적
```

### 2.2 대화 준비

```bash
# 요청 메시지 준비
- [ ] 메모장에 요청 메시지 작성
- [ ] 복사 준비 완료

요청 메시지:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
LG U+ 주문 시스템에 대한 Instana Synthetic 모니터링 테스트를 만들어주세요.

시스템 정보:
- URL: http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com/
- 주문 시스템: 마이크로서비스 아키텍처
- 서비스: order, inventory, fulfillment, payment, analytics

필요한 테스트:
1. 고객 주문 시나리오 (Selenium IDE 형식)
2. 관리자 대시보드 시나리오 (Selenium IDE 형식)
3. API 엔드포인트 테스트 (JavaScript)

각 테스트는 실제 프로덕션에서 사용 가능한 수준으로 작성해주세요.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 2.3 이전 대화 정리 (선택)

```bash
# Bob 채팅 히스토리
- [ ] 이전 대화 정리 (깔끔한 화면)
- [ ] 또는 새 대화 시작
```

---

## 🌐 3단계: 브라우저 준비 (10분 전)

### 3.1 Chrome 탭 준비

```bash
# Chrome 실행
- [ ] open -a "Google Chrome"

# 필요한 탭만 열기 (순서대로)
- [ ] Tab 1: IBM Bob 채팅 인터페이스
- [ ] Tab 2: https://eum-lguplus.instana.io/#/home
- [ ] Tab 3: http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com/

# 기타 탭 모두 닫기
- [ ] 불필요한 탭 정리
```

### 3.2 Chrome 설정

```bash
# 화면 설정
- [ ] 확대/축소: 100% (Cmd + 0)
- [ ] 북마크 바: 숨기기 (Cmd + Shift + B)
- [ ] 확장 프로그램: 최소화
- [ ] 개발자 도구: 닫기
```

### 3.3 Instana 로그인

```bash
# Instana 탭에서
- [ ] 로그인 완료
- [ ] Synthetic Monitoring 섹션 확인
- [ ] 기존 테스트 3개 확인
```

### 3.4 주문 시스템 확인

```bash
# 주문 시스템 탭에서
- [ ] 웹사이트 로딩 확인
- [ ] 로그인 계정 확인:
  - 고객: client / client
  - 관리자: admin / admin
```

---

## 📁 4단계: 파일 준비 (30분 전)

### 4.1 ⚠️ 파일 준비 (매우 중요!)

**데모의 핵심**: Bob이 파일을 "새로 생성"하는 것처럼 보여야 하므로, 기존 파일을 반드시 삭제해야 합니다.

**1. 기존 파일 백업 (복원용)**
```bash
# 백업 디렉토리 생성
mkdir -p instana-synthetic-backup

# 기존 파일 백업
cp instana-synthetic/client-scenario.side instana-synthetic-backup/
cp instana-synthetic/admin-scenario.side instana-synthetic-backup/
cp instana-synthetic/api-test.js instana-synthetic-backup/

# 백업 확인
ls -la instana-synthetic-backup/
```

**2. 데모용 파일 삭제 (필수!)**
```bash
# 기존 파일 삭제 - Bob이 새로 생성할 파일들
rm instana-synthetic/client-scenario.side
rm instana-synthetic/admin-scenario.side
rm instana-synthetic/api-test.js

# 삭제 확인 - README.md만 남아있어야 함
ls -la instana-synthetic/
# 출력 예상: README.md만 표시됨
```

**3. VSCode에서 확인**
- VSCode 탐색기에서 `instana-synthetic` 폴더 확인
- `README.md`만 있고 `.side`, `.js` 파일이 없어야 함
- 이 상태가 데모 시작 화면!

**💡 중요**: 이 단계를 건너뛰면 Bob이 "파일이 이미 존재합니다" 메시지를 표시하여 데모가 실패합니다!

---

## 🎥 5단계: QuickTime 설정 (5분 전)

### 5.1 QuickTime 실행

```bash
# 화면 녹화 컨트롤 열기
- [ ] Shift + Cmd + 5
```

### 5.2 녹화 옵션 확인

```bash
# Options 클릭 후 확인
- [ ] Microphone: None (마이크 끄기)
- [ ] Show Mouse Clicks: 체크
- [ ] Save to: Movies 폴더
- [ ] Timer: None

# 녹화 영역
- [ ] "Record Entire Screen" 선택
```

---

## 🧪 6단계: 테스트 녹화 (5분 전)

### 6.1 짧은 테스트 (30초)

```bash
# 녹화 시작
- [ ] "Record" 버튼 클릭
- [ ] 3초 대기

# 테스트 동작
- [ ] Bob 채팅 화면 보여주기
- [ ] Chrome 탭 전환 (Cmd + Tab)
- [ ] Finder 열기
- [ ] 마우스 클릭 테스트

# 녹화 중지
- [ ] Cmd + Control + Esc
```

### 6.2 테스트 파일 확인

```bash
# 녹화 파일 재생
- [ ] Movies 폴더에서 파일 찾기
- [ ] QuickTime으로 재생
- [ ] 화면 선명도 확인
- [ ] 마우스 클릭 표시 확인
- [ ] 텍스트 가독성 확인
```

---

## 🎬 7단계: 본 녹화 (정시)

### 7.1 최종 점검

```bash
# 환경 최종 확인
- [ ] Bob 채팅 화면 열림
- [ ] 요청 메시지 준비됨
- [ ] 브라우저 탭 3개 준비
- [ ] Finder 준비
- [ ] 알림 끄기 (Do Not Disturb)
- [ ] Dock 자동 숨김

# 화면 정리
- [ ] 바탕화면 정리
- [ ] 불필요한 창 모두 닫기
```

### 7.2 녹화 시작

```bash
# 1. 심호흡 (긴장 풀기)
- [ ] 3회 심호흡

# 2. 녹화 시작
- [ ] Shift + Cmd + 5
- [ ] Options 재확인
- [ ] "Record" 클릭
- [ ] 3초 대기

# 3. 데모 시작
- [ ] DEMO_VIDEO_SCRIPT.md 참고
```

### 7.3 녹화 진행

```bash
# [00:00 - 00:30] 인트로
- [ ] Bob 채팅 화면 전체 보여주기
- [ ] 3초 정지

# [00:30 - 02:00] Bob에게 요청
- [ ] 요청 메시지 붙여넣기
- [ ] Enter 키
- [ ] Bob 응답 대기

# [02:00 - 04:00] Bob 작업 과정
- [ ] 작업 진행 메시지 확인
- [ ] 파일 생성 과정 확인
- [ ] 완료 메시지 대기

# [04:00 - 05:30] 파일 확인
- [ ] Finder로 전환 (Cmd + Tab)
- [ ] instana-synthetic 폴더
- [ ] 3개 파일 확인
- [ ] 각 파일 간단히 열기 (5초씩)

# [05:30 - 07:00] Instana 확인
- [ ] Chrome에서 Instana 탭
- [ ] Synthetic Monitoring
- [ ] 테스트 3개 확인
- [ ] 하나의 테스트 상세 보기

# [07:00 - 08:00] 웹사이트 확인
- [ ] 주문 시스템 탭
- [ ] 고객 로그인 (client/client)
- [ ] 주문 생성
- [ ] 관리자 로그인 (admin/admin)
- [ ] 대시보드 확인

# [08:00 - 08:30] 마무리
- [ ] Bob 채팅으로 돌아오기
- [ ] 최종 메시지 확인
- [ ] 3초 정지
```

### 7.4 녹화 종료

```bash
# 녹화 중지
- [ ] Cmd + Control + Esc

# 파일 확인
- [ ] Movies 폴더에서 파일 찾기
- [ ] 파일명 변경: "IBM_Bob_Demo_Raw_2026-03-03.mov"
```

---

## 💾 8단계: 즉시 백업 (녹화 직후)

### 8.1 로컬 백업

```bash
# 외장 하드로 복사
- [ ] 외장 하드 연결
- [ ] 파일 복사
- [ ] 복사 완료 확인

# 다른 위치에도 복사
- [ ] Desktop에 복사
- [ ] Documents에 복사
```

### 8.2 클라우드 백업

```bash
# iCloud Drive
- [ ] iCloud Drive 폴더로 복사
- [ ] 업로드 진행 확인

# 또는 Google Drive
- [ ] Google Drive 폴더로 복사
- [ ] 업로드 진행 확인
```

---

## ✅ 9단계: 녹화 파일 검증

### 9.1 전체 재생 확인

```bash
# QuickTime으로 전체 재생
- [ ] 처음부터 끝까지 재생
- [ ] Bob 채팅 화면 선명
- [ ] 텍스트 가독성 확인
- [ ] 마우스 클릭 표시 확인
- [ ] 화면 전환 부드러움
```

### 9.2 주요 구간 확인

```bash
# 핵심 구간 확인
- [ ] 00:30 - Bob 요청 메시지 읽기 가능
- [ ] 02:00 - Bob 작업 진행 메시지 명확
- [ ] 04:00 - 생성된 파일 목록 선명
- [ ] 05:30 - Instana 화면 선명
- [ ] 07:00 - 웹사이트 화면 선명
```

### 9.3 문제 발견 시

```bash
# 경미한 문제 (편집으로 해결 가능)
- [ ] 시작/끝 부분 트리밍 필요
- [ ] 자막 추가 필요
- [ ] 화면 확대 필요

# 심각한 문제 (재녹화 필요)
- [ ] 화면이 흐릿함
- [ ] Bob 응답이 잘림
- [ ] 중요 부분 누락
- [ ] 화면 전환 오류

# 재녹화 결정
- [ ] 문제 심각도 평가
- [ ] 재녹화 일정 조율
```

---

## 📊 10단계: 완료 보고

### 10.1 녹화 정보 기록

```
녹화 완료 정보:

날짜: _______________
시간: _______________
파일명: IBM_Bob_Demo_Raw_2026-03-03.mov
파일 크기: _______________ MB
재생 시간: _______________ 분
해상도: 1920x1080

백업 위치:
- [ ] 외장 하드: _______________
- [ ] iCloud Drive: _______________
- [ ] Google Drive: _______________

품질 평가:
- Bob 화면 선명도: ⭐⭐⭐⭐⭐
- 텍스트 가독성: ⭐⭐⭐⭐⭐
- 화면 전환: ⭐⭐⭐⭐⭐
- 데모 진행: ⭐⭐⭐⭐⭐

재녹화 필요: [ ] Yes  [ ] No

특이사항:
_______________________________________________
_______________________________________________
```

### 10.2 다음 단계

```bash
# 편집 작업 예정
- [ ] iMovie로 편집 시작일: _______________
- [ ] 편집 담당자: _______________
- [ ] 예상 완료일: _______________

# 최종 파일 목표
- [ ] 파일명: IBM_Bob_Demo_Final.mp4
- [ ] 길이: 8-10분
- [ ] 자막: 한글
- [ ] 화면 확대: Ken Burns 효과 적용
```

---

## 🚨 비상 연락처

```
기술 지원:
- IBM 지원팀: support@ibm.com

프로젝트 팀:
- 프로젝트 매니저: [이름] ([전화번호])
- 기술 리드: [이름] ([전화번호])
- 백업 녹화 담당자: [이름] ([전화번호])
```

---

## 📝 녹화 후 정리

### 녹화 후 복원

**데모 완료 후 원본 파일 복원:**

```bash
# 백업 파일 복원
cp instana-synthetic-backup/client-scenario.side instana-synthetic/
cp instana-synthetic-backup/admin-scenario.side instana-synthetic/
cp instana-synthetic-backup/api-test.js instana-synthetic/

# 복원 확인
ls -la instana-synthetic/

# 백업 폴더 삭제
rm -rf instana-synthetic-backup

echo "✅ 원본 파일 복원 완료"
```

**복원 확인:**
- [ ] client-scenario.side 존재
- [ ] admin-scenario.side 존재
- [ ] api-test.js 존재
- [ ] 백업 폴더 삭제됨

### 환경 정리

```bash
# 시스템 설정 복원
- [ ] Do Not Disturb 끄기
- [ ] Dock 자동 숨김 끄기 (선호에 따라)
- [ ] 절전 모드 설정 복원

# 파일 정리
- [ ] 테스트 녹화 파일 삭제
- [ ] 불필요한 파일 정리
```

---

## ✨ 성공 기준

녹화가 성공적으로 완료되었다고 판단하는 기준:

- [x] 전체 데모 시나리오 완료 (8-10분)
- [x] Bob 채팅 화면이 선명하고 텍스트가 읽기 쉬움
- [x] Bob의 응답이 모두 포함됨
- [x] 생성된 파일 3개 확인됨
- [x] Instana 화면이 선명함
- [x] 웹사이트 시연이 포함됨
- [x] 파일이 안전하게 백업됨
- [x] 재생 시 문제 없음

---

## 🎯 핵심 포인트

### 데모의 핵심
1. **IBM Bob 채팅 인터페이스** - 메인 화면 (80% 시간)
2. **자연어 대화** - 복잡한 요청을 간단하게
3. **자동 생성** - Bob이 모든 것을 처리
4. **즉시 사용** - 프로덕션 레벨 결과물

### 강조할 부분
- Bob의 작업 진행 메시지
- 생성된 파일 개수 (3개)
- 코드 라인 수 (283 lines)
- "Made with Bob" 서명
- Instana 테스트 성공률 100%
- 개발 시간 95% 단축

---

**문서 버전**: 2.0  
**작성일**: 2026-03-03  
**작성자**: IBM Bob Demo Team  
**업데이트**: IBM Bob 채팅 인터페이스 중심으로 간소화