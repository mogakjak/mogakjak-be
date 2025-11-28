# 모여서 각자 작업하는 시간 , 모각작🍅

> 🔗 Link : [https://mogakjak-fe.vercel.app](https://mogakjak-fe.vercel.app/)

![모각작 장표](https://github.com/user-attachments/assets/34debf9e-ca5e-40c8-91d4-be1807d009ee)


> **팀명**
>
> ### 🤿 몰딥브 (More Deep Dive)
>
> 몰입에 **Deep Dive** 하겠다는 포부와,  
> 휴양지 **Maldives**처럼 편안하고 즐거운 몰입 환경을 만들겠다는  
> **중의적 의미를 담은 팀명입니다.**



<br>

# 🔖 서비스 개요

###  👤 유저 리서치
![모각작 장표](https://github.com/user-attachments/assets/fbbe6732-514f-48ae-960e-21d5ab1a3e37)

#### 기존의 서비스들에 유저들은 압박이 필요한 몰입에 부담을 느끼고 있었습니다

### 🐥 PainPoint
![모각작 장표_1](https://github.com/user-attachments/assets/b9ca55b2-c3f6-43fd-865a-23dce83400b8)

#### 강제적인 경쟁이 아닌 자율성 속의 연대를 원합니다


<br>

# 💻 서비스 주요 기능
- **그룹 실시간 상태 공유**  
  멤버들의 집중/휴식 상태 표시, ‘콕 찌르기’ 알림 제공

- **몰입 지원 기능**  
  공개 설정, 그룹 타이머, 목표 달성률, 캐릭터 성장 시스템

- **웹 최적화 기능**  
  PIP 모드, 그룹 알림, 주·월 집중 리포트 시각화

![모각작 장표_2](https://github.com/user-attachments/assets/ef21d7eb-e428-45a1-a9cd-ac7d1b8940fe)

<br>
<br>

# 💼 API 명세서
[![Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?logo=swagger&logoColor=white)](https://mogakjak.site/swagger-ui/index.html#/)

<br>
<br>

# 🗂️ ERD
  <img width="1844" height="1041" alt="image" src="https://github.com/user-attachments/assets/eb9e2918-27b4-488c-a05a-b8df07a8395d" />

<br>
<br>

# 🏗️ 시스템 아키텍처
<img width="7900" height="4840" alt="image" src="https://github.com/user-attachments/assets/67f727ff-8dee-4c98-8ab0-d6136b0e7b8b" />

<br>
<br>

# 🛠️ 개발 환경 및 사용 기술 스택

###  **백엔드**

- **Spring Boot 3.3.2 + Java 21**  
  LTS 기반으로 성능과 타입 안정성을 확보했습니다.

- **Spring Data JPA**  
  메서드 네이밍 쿼리, JPQL, Native Query를 사용하고  
  `JOIN FETCH`로 N+1 문제를 해결했습니다.

- **MySQL**  
  안정적인 관계형 데이터 구조에 적합하여 선택했습니다.

- **Spring Security + JWT (jjwt 0.13.0)**  
  `OncePerRequestFilter` 기반 JWT 필터 체인과 Stateless 세션으로  
  보안성과 확장성을 확보했습니다.

- **Redis + Pub/Sub**  
  다음 8개 채널을 Pub/Sub 구조로 운영하며 실시간 메시징을 처리합니다:  
  `chat`, `focus-notification`, `group-member-status`, `timer-completion`,  
  `poke-notification`, `cheer-notification`, `group-timer-event`, `user-active-status`

- **WebSocket (STOMP + SockJS)**  
  토큰 기반 인증 인터셉터와 Simple Broker로  
  그룹 타이머·채팅·알림 등 실시간 기능을 제공합니다.

- **AWS S3 SDK (NCP Object Storage 호환)**  
  이미지 저장소로 사용했습니다.


<br>

# 🤖 백엔드 CI/CD 요약

- **CI 자동화**: PR 생성 시 컴파일·테스트·정적 분석을 자동 실행하고, 실패 시 merge 차단  
- **CD 자동화**: `develop → staging`, `main → production` 환경으로 자동 배포  
- **무중단 배포(블루/그린)**: Docker 이미지 빌드 → 배포 → 헬스체크 후 트래픽 전환, 실패 시 자동 롤백  

<br>

# 🧩 백엔드 트러블슈팅 요약

### 1) 트랜잭션 커밋 전 브로드캐스트 문제
- **문제**: 커밋 전에 WebSocket 브로드캐스트 → 프론트에서 최신 상태 미반영  
- **해결**: `afterCommit()` 사용해 **트랜잭션 완료 후** 브로드캐스트 수행

### 2) LocalDateTime 직렬화 오류
- **문제**: Redis Pub/Sub 전송 시 Java 8 시간 타입 직렬화 실패  
- **해결**: `JavaTimeModule` 등록 + 타임스탬프 비활성화

### 3) N+1 쿼리 성능 문제
- **문제**: 그룹/메이트 조회 등에서 N+1 발생  
- **해결**: **JOIN FETCH** + Native Query로 집계 처리

### 4) 타이머 완료 스케줄링 비효율
- **문제**: 1초마다 전체 타이머 조회 → 서버 부하  
- **해결**: 종료 시점 기반 **이벤트 스케줄링**으로 전환

### 5) 읽기 전용 트랜잭션 미적용
- **문제**: 조회 쿼리도 기본 트랜잭션 사용 → 락·오버헤드 증가  
- **해결**: `@Transactional(readOnly = true)` 적용

### 6) WebSocket 수평 확장 문제
- **문제**: 단일 서버만 브로드캐스트 가능  
- **해결**: **Redis Pub/Sub**으로 모든 인스턴스 동기화

### 7) 복잡한 대시보드 집계 성능 문제
- **문제**: 여러 쿼리 + 애플리케이션 집계로 응답 지연  
- **해결**: Native Query로 **한 번에 집계 조회**


