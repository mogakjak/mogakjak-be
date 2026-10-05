# 할 일별 최근 작업 시각 (#100)

## API 계약

기존 `TodoResponse`에 nullable `lastWorkedAt`을 추가한다.

- 전체 목록 `GET /api/todos/my`, 날짜별 카테고리 목록 `GET /api/todos/today`, `GET /api/todos?date=...`에 포함된다.
- 전체 수정, 목표시간 PATCH, 완료 토글 응답에도 같은 실제 작업 시각을 유지한다.
- 생성 응답은 작업 이력이 없으므로 null이다.
- 예: `"lastWorkedAt": "2026-10-05T00:05:00+09:00"`.
- 날짜·수정 시각·제목이 아니라 요청 사용자와 동일한 `todoId`의 작업 기록을 기준으로 한다.
- 타이머 응답용 `TodoSummary`는 이번 이슈 범위 밖이다. 사이드바 조회 계약은 #101에서 확장한다.

## 산출 기준

`FocusSession`과 `FocusInterval`을 조회 집계한다. 개인 및 그룹 내 개인 세션을 모두 포함하고, 할 일이 없는 그룹 공용 기록은 포함하지 않는다.

| 상태 | 반환 기준 |
| --- | --- |
| 종료된 집중 구간 | NORMAL/FOCUS 중 종료 > 시작이고 종료 <= 조회 시각인 구간의 가장 최근 종료 시각 |
| 현재 집중 중 | RUNNING이고 해당 사용자 활성 세션과 일치하는 최신 열린 NORMAL/FOCUS 구간이 실제로 시작된 경우 조회 시각 |
| 일시정지·세션 종료 | 가장 최근 종료된 집중 구간 시각 |
| 뽀모도로 휴식 | 휴식 이전의 가장 최근 집중 구간 종료 시각 |
| 시작·재개 직후 | 조회 시각과 시작 시각이 같으면 새 작업으로 계산하지 않음. 이후 실제 양의 집중 시간이 생기면 현재 시각 |
| 작업 없음 | null |

휴식, 단순 조회, 제목·날짜·목표시간 변경, 완료 토글은 최근 작업 시각을 갱신하지 않는다. 활성 연결 없는 오래된 열린 구간, 다른 사용자의 활성 연결, 최신 구간보다 오래된 열린 집중 구간은 현재 작업으로 취급하지 않는다. 잘못된 음수/0 길이 및 미래에 종료된 구간도 제외한다.

## 시간대와 프론트 표시

타이머는 `LocalDateTime.now()`로 JVM 로컬 시각을 DATETIME에 기록하고 있다. 조회도 같은 기준으로 비교하고, 응답 생성에서 JVM 로컬 시각의 실제 순간을 Asia/Seoul로 변환한다. UTC 값을 단순히 +09:00로 라벨링하지 않는다.

2026-10-05 별도 확인에서 호스트와 컨테이너 OS 및 JVM 초기 user.timezone 속성은 UTC였으나, `MogakjakApplication.started()`의 `@PostConstruct`가 기본 TimeZone을 Asia/Seoul로 변경한다. **OS의 UTC를 기존 작업 기록의 시간대로 단정하지 않는다.** 정상 앱에서 기록한 `2026-10-05 00:05:00`은 `2026-10-05T00:05:00+09:00`으로 반환된다. UTC 기본 시간대인 별도 실행 환경에서는 동일한 실제 순간인 `2026-10-04 15:05:00`을 위 한국 시각으로 변환한다. 초기 시스템 속성은 TimeZone.setDefault 이후의 유효 기본 시간대와 다를 수 있다.

기존 기록이 있는 앱 JVM의 유효 시간대를 바꿀 때는 DATETIME 기록의 해석도 함께 검토해야 한다. 서로 다른 시간대로 생성된 과거 기록은 이 필드만으로 자동 판별할 수 없다.

프론트는 다음 계약으로 원본 시각을 표시한다.

- Asia/Seoul의 달력 날짜 차이를 사용한다. 경과 초를 86400으로 나누지 않는다.
- 예: 한국 시각 10월 5일 00:10에 10월 4일 23:50 작업은 1일 전이다.
- null은 미작업/표시 없음으로 처리하고 오늘 작업을 null과 혼동하지 않는다.
- 한 달 이내는 n일 전, 그 이후는 한국 날짜를 표시한다.
- 한 달을 30일 또는 달력상 한 달로 볼지, 경계 포함 여부와 오늘의 문구는 프론트에서 확정해야 한다. 백엔드는 상대 문구를 생성하지 않는다.

## 조회 및 기존 데이터

새 타임스탬프 컬럼이나 백필이 없고 기존 구간 기록이 즉시 조회된다. 누적 시간만 있고 구간이 없는 기록의 최근 날짜를 임의 추정하지 않는다.

- 목록에 포함된 todoId들을 한 번에 전달한다.
- 종료 구간 MAX 집계 1회 + 현재 집중 중 todoId 조회 1회. 할 일 수에 비례하는 최근 시각 개별 조회 없음.
- 빈 목록은 구간 조회를 수행하지 않는다.
- 한 번 캡처한 조회 시각을 두 쿼리에서 공통 사용한다.
- 기존 목록 순서·카테고리 그룹·소유권·soft delete 필터는 유지한다.

엔티티에 다음 인덱스를 정의했다. 개발 DB는 확인 당시 두 테이블에 PRIMARY만 있었으므로 전체 기록 증가에 대비한다.

- `focus_session(user_id, todo_id)`: 사용자·할 일 목록 범위 제한
- `focus_interval(session_id, phase_type, ended_at)`: 세션별 유효 집중 구간 집계
- `focus_interval(session_id, started_at)`: 최신 구간 및 이전 열린 구간 제외

개발 환경의 ddl-auto=update로 배포 시 인덱스를 생성할 수 있다. 배포 후 `SHOW INDEX`로 위 인덱스 이름(`idx_focus_session_user_todo`, `idx_focus_interval_session_phase_end`, `idx_focus_interval_session_started`)을 확인한다. 스키마 자동 갱신을 쓰지 않는 환경은 동일 정의로 인덱스를 별도 생성한다. 데이터가 큰 환경에서는 인덱스 DDL 락/부하를 검토하고 기존 중복 인덱스도 확인한다.

## 검증

실제 서비스 DB에는 테스트를 실행하지 않는다. 세 개의 MySQL 통합 테스트는 각각 폐기 가능한 전용 DB를 사용한다. 환경변수가 없으면 해당 통합 테스트는 생략된다.

```bash
ISSUE98_MYSQL_URL='jdbc:mysql://127.0.0.1:53400/issue98_verify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul' \
ISSUE99_MYSQL_URL='jdbc:mysql://127.0.0.1:53400/issue99_verify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul' \
ISSUE100_MYSQL_URL='jdbc:mysql://127.0.0.1:53400/issue100_verify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul' \
JAVA_TOOL_OPTIONS='-Duser.timezone=Asia/Seoul' \
./gradlew test --tests 'com.mogakjak.mogakjak.domain.*' --tests 'com.mogakjak.mogakjak.global.*' bootJar
```

새 테스트는 이력 없음, 이름이 같은 서로 다른 할 일, 타 사용자, 기존 개인/그룹 기록, 집중/휴식/일시정지/종료/고아 구간, 시작 직후, 기존 목록·수정 응답, 두 번의 배치 조회, 한국 자정 경계 및 +09:00 JSON 직렬화를 검증한다. 변환 테스트는 기본 TimeZone을 UTC/Asia/Seoul 각각으로 설정·복원하여 동일 순간 반환을 확인한다. 전체 JPA 회귀 테스트는 앱의 유효 기본 시간대인 Asia/Seoul과 JDBC를 일치시킨다.

루트 `MogakjakApplicationTests.setCors()`는 실제 외부 S3 버킷을 변경하므로 위 실행 대상에서 제외한다.
