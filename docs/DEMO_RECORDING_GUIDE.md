# IBM Bob 데모 영상 녹화 가이드

**목적**: IBM Bob 채팅 인터페이스 화면 녹화 가이드 (음성 없음)  
**대상**: 녹화 담당자  
**예상 소요 시간**: 준비 15분, 녹화 10분

---

## 📋 목차

1. [하드웨어 요구사항](#하드웨어-요구사항)
2. [소프트웨어 준비](#소프트웨어-준비)
3. [IBM Bob 준비](#ibm-bob-준비)
4. [QuickTime 녹화 설정](#quicktime-녹화-설정)
5. [녹화 진행](#녹화-진행)
6. [후반 작업](#후반-작업)
7. [문제 해결](#문제-해결)

---

## 🖥️ 하드웨어 요구사항

### 최소 사양
- **Mac**: macOS 10.14 이상
- **RAM**: 8GB 이상
- **저장공간**: 20GB 이상
- **디스플레이**: 1920x1080 이상
- **네트워크**: 안정적인 인터넷 연결

### 권장 사양
- **Mac**: macOS 12 이상
- **RAM**: 16GB 이상
- **저장공간**: SSD 50GB 이상
- **디스플레이**: 2560x1440 (Retina)

---

## 💿 소프트웨어 준비

### 1. QuickTime Player (기본 설치됨)

```bash
# QuickTime 실행 확인
open -a "QuickTime Player"
```

### 2. 브라우저 (Chrome 권장)

```bash
# Chrome 설치 확인
open -a "Google Chrome"
```

**필요한 탭**:
1. IBM Bob 채팅 인터페이스 (메인)
2. Instana 콘솔
3. 주문 시스템 웹사이트

### 3. 파일 탐색기 (Finder)

```bash
# 생성된 파일 위치
open /Users/hansol/Workspace/TXC/0903_LG/order/instana-synthetic
```

---

## 🤖 IBM Bob 준비

### 1. Bob 접속

```bash
# IBM Bob 채팅 인터페이스 접속
# (브라우저 또는 전용 앱)
```

**확인 사항**:
- [ ] Bob 로그인 완료
- [ ] 채팅 인터페이스 정상 작동
- [ ] 프로젝트 컨텍스트 로드됨
- [ ] 이전 대화 히스토리 정리 (선택)

### 2. 대화 준비

**데모용 요청 메시지 미리 작성**:

```
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
```

**팁**: 메모장에 미리 작성해두고 복사/붙여넣기 사용

### 3. 생성된 파일 확인

```bash
# 파일 존재 확인
ls -la /Users/hansol/Workspace/TXC/0903_LG/order/instana-synthetic/

# 예상 출력:
# client-scenario.side
# admin-scenario.side
# api-test.js
# README.md
```

### 4. Instana 준비

```bash
# Instana 콘솔 접속
open https://eum-lguplus.instana.io/#/home

# 로그인 확인
# Synthetic Monitoring 섹션 확인
```

### 5. 주문 시스템 준비

```bash
# 주문 시스템 접속
open http://web-workshop-eda.apps.itz-12fl8d.infra01-lb.syd05.techzone.ibm.com/

# 로그인 계정 확인:
# - 고객: client / client
# - 관리자: admin / admin
```

---

## 🎥 QuickTime 녹화 설정

### 1. 화면 녹화 시작

```bash
# 단축키
Shift + Cmd + 5
```

### 2. 녹화 옵션

**컨트롤 바에서 설정**:
```
✓ Record Entire Screen (전체 화면)
✓ Options 클릭:
  - Microphone: None (마이크 끄기)
  - Show Mouse Clicks: 체크
  - Save to: Movies 폴더
  - Timer: None
```

### 3. 화면 설정

```bash
# 디스플레이 해상도
System Preferences → Displays
Resolution: 1920x1080 (Scaled)

# 알림 끄기
System Preferences → Notifications & Focus
Do Not Disturb 활성화

# Dock 자동 숨김
System Preferences → Dock & Menu Bar
✓ Automatically hide and show the Dock
```

### 4. 브라우저 설정

**Chrome 설정**:
- 확대/축소: 100% (Cmd + 0)
- 북마크 바: 숨기기 (Cmd + Shift + B)
- 확장 프로그램: 최소화
- 개발자 도구: 닫기

---

## 🎬 녹화 진행

### 녹화 전 최종 체크

```bash
# 1. 알림 끄기
- [ ] Do Not Disturb 활성화

# 2. 불필요한 앱 종료
- [ ] Slack, Teams, Mail 등 종료

# 3. 배터리 확인
- [ ] 전원 연결 (MacBook)

# 4. 화면 정리
- [ ] 바탕화면 정리
- [ ] 브라우저 탭 정리 (3개만)
- [ ] Dock 자동 숨김

# 5. Bob 준비
- [ ] Bob 채팅 화면 열림
- [ ] 요청 메시지 준비됨
- [ ] 네트워크 연결 확인
```

### 녹화 시작

```bash
# 1. Shift + Cmd + 5
# 2. Options 확인
# 3. "Record" 클릭
# 4. 3초 대기
# 5. 데모 시작
```

### 녹화 순서

#### [00:00 - 00:30] 인트로
- [ ] Bob 채팅 인터페이스 전체 화면 보여주기
- [ ] 3초 정지 (자막 삽입 여유)

#### [00:30 - 02:00] Bob에게 요청
- [ ] 채팅창에 요청 메시지 붙여넣기
- [ ] Enter 키 누르기
- [ ] Bob 응답 대기

#### [02:00 - 04:00] Bob 작업 과정
- [ ] Bob의 작업 진행 메시지 표시
- [ ] 파일 생성 과정 확인
- [ ] 완료 메시지 대기

#### [04:00 - 05:30] 생성된 파일 확인
- [ ] Finder로 전환 (Cmd + Tab)
- [ ] instana-synthetic 폴더 열기
- [ ] 3개 파일 확인
- [ ] 각 파일 간단히 열어보기 (5초씩)

#### [05:30 - 07:00] Instana 확인
- [ ] Chrome에서 Instana 탭으로 전환
- [ ] Synthetic Monitoring 섹션
- [ ] 업로드된 테스트 3개 확인
- [ ] 하나의 테스트 상세 보기

#### [07:00 - 08:00] 실제 웹사이트 확인
- [ ] 주문 시스템 탭으로 전환
- [ ] 고객 로그인 (client/client)
- [ ] 주문 생성 시연
- [ ] 관리자 로그인 (admin/admin)
- [ ] 대시보드 확인

#### [08:00 - 08:30] 마무리
- [ ] Bob 채팅으로 돌아오기
- [ ] Bob의 최종 메시지 확인
- [ ] 3초 정지

### 녹화 종료

```bash
# 1. Cmd + Control + Esc (녹화 중지)
# 2. 자동 저장 확인 (Movies 폴더)
# 3. 파일명 변경: "IBM_Bob_Demo_Raw.mov"
# 4. 즉시 백업
```

---

## ✂️ 후반 작업

### 1. 영상 확인

```bash
# QuickTime으로 재생
open ~/Movies/IBM_Bob_Demo_Raw.mov

# 확인 사항:
- [ ] Bob 채팅 화면이 선명한가?
- [ ] 텍스트가 읽기 쉬운가?
- [ ] 화면 전환이 부드러운가?
- [ ] 마우스 클릭이 보이는가?
```

### 2. iMovie로 편집

#### 기본 편집

```
1. iMovie 실행
2. 프로젝트 생성
3. 녹화 파일 가져오기
4. 시작/끝 트리밍
5. 불필요한 부분 컷
```

#### 자막 추가

**주요 자막**:
```
00:00 - "IBM Bob - AI 기반 개발 어시스턴트"
00:30 - "자연어로 요청만 하면"
02:00 - "Bob이 자동으로 생성합니다"
04:00 - "프로덕션 레벨의 완전한 코드"
05:30 - "Instana에서 즉시 사용 가능"
08:00 - "개발 시간 95% 단축"
```

#### 화면 확대 (Ken Burns)

**확대가 필요한 부분**:
1. Bob의 작업 진행 메시지 (02:00-04:00)
2. 생성된 파일 목록 (04:00-04:30)
3. Instana 테스트 성공률 (06:00-06:30)

**설정**:
```
1. 클립 선택
2. Crop 버튼 → Ken Burns
3. Start: 확대할 영역 작게
4. End: 전체 화면 또는 동일
5. Duration: 2-3초
6. Apply
```

#### 내보내기

```
File → Share → File
├── Resolution: 1080p
├── Quality: High
└── Format: Video and Audio

저장: ~/Movies/IBM_Bob_Demo_Final.mp4
```

---

## 🔧 문제 해결

### Bob 관련 문제

**문제**: Bob이 응답하지 않음
```bash
# 해결:
1. 네트워크 연결 확인
2. Bob 페이지 새로고침
3. 브라우저 재시작
4. 다른 브라우저 시도
```

**문제**: Bob 응답이 너무 느림
```bash
# 해결:
1. 네트워크 속도 확인
2. 다른 시간대에 시도
3. 요청 메시지 단순화
```

### 녹화 문제

**문제**: 화면이 흐릿함
```bash
# 해결:
1. 디스플레이 해상도 확인 (1920x1080)
2. 브라우저 확대/축소 100%
3. Retina 디스플레이 사용
```

**문제**: 파일 크기가 너무 큼
```bash
# 해결:
1. HandBrake로 압축
brew install --cask handbrake
2. Preset: Fast 1080p30
3. Quality: RF 20-22
```

### 환경 문제

**문제**: Instana 접속 불가
```bash
# 해결:
1. 계정 정보 재확인
2. 브라우저 캐시 삭제
3. 시크릿 모드 시도
4. VPN 연결 확인
```

**문제**: 주문 시스템 접속 불가
```bash
# 해결:
1. URL 확인
2. 네트워크 연결 확인
3. 시스템 상태 확인 (OCP)
```

---

## 📚 참고 자료

### 공식 문서
- QuickTime: https://support.apple.com/guide/quicktime-player/welcome/mac
- iMovie: https://support.apple.com/imovie

### 도구
- HandBrake (압축): https://handbrake.fr/
- IINA (플레이어): https://iina.io/

---

## 🎯 녹화 체크리스트 요약

### 녹화 전
- [ ] Bob 채팅 인터페이스 접속
- [ ] 요청 메시지 준비
- [ ] Instana 로그인
- [ ] 주문 시스템 확인
- [ ] 생성된 파일 확인
- [ ] QuickTime 설정
- [ ] 알림 끄기
- [ ] 화면 정리

### 녹화 중
- [ ] Bob 채팅 화면 중심
- [ ] 요청 메시지 입력
- [ ] Bob 응답 대기
- [ ] 파일 확인
- [ ] Instana 확인
- [ ] 웹사이트 확인
- [ ] 마무리 메시지

### 녹화 후
- [ ] 파일 저장 확인
- [ ] 백업
- [ ] 영상 재생 확인
- [ ] iMovie 편집
- [ ] 자막 추가
- [ ] 최종 내보내기

---

**문서 버전**: 2.0  
**작성일**: 2026-03-03  
**작성자**: IBM Bob Demo Team  
**업데이트**: IBM Bob 채팅 인터페이스 중심으로 간소화