# 할 일 선택 후 사이드바 조회 (#101)

## 조회 계약

`GET /api/todos/{todoId}`는 인증한 본인의 미삭제 할 일만 조회한다. 타인/미존재/삭제 할 일은 기존 접근 오류 계약인 403으로 처리한다. 삭제된 카테고리와 비정상적인 다른 소유자의 카테고리도 노출하지 않는다.

응답은 `ApiResponse<TodoDetailResponse>`이며 기존 목록·수정 응답의 구조는 유지한다. 새 API는 읽기 전용이고 타이머 시작/정지, 공개 설정 또는 누적시간을 저장하지 않는다.

| 화면 요소 | 응답/기존 API |
| --- | --- |
| 선택한 할 일 제목·ID | todo.task, todo.id |
| 카테고리 | category.id/name/color (기존 CategoryResponse) |
| 저장된 목표시간 | todo.targetTimeInSeconds (null 허용, 수정 가능) |
| 할 일 누적 집중시간 | accumulatedTimeInSeconds (진행 중 구간 포함) |
| 현재 달성률 | 최상위 progressRate (진행 중 구간 포함) |
| 최근 작업 시각 | todo.lastWorkedAt (+09:00 ISO 문자열, 없으면 null) |
| 현재 타이머 모드·상태·세션 ID | activeSession.mode/status/sessionId |
| 현재 세션 집중시간 | activeSession.focusTimeInSeconds |
| 현재 단계·라운드와 시간 | activeSession.phaseType/round/phaseElapsedTimeInSeconds |
| 할 일/누적시간 공개 토글 | isTaskPublic/isTimerPublic + 기존 PUT /api/timers/{sessionId}/visibility |

인증 사용자의 활성 연결과 세션 소유자·todoId·RUNNING/PAUSED 상태가 모두 일치할 때만 activeSession을 반환한다. 다른 할 일의 세션·타인 세션·종료 세션·끊어진 활성 연결은 null이다. 개인 및 그룹 내 개인 타이머를 동일하게 조회한다. 그룹 공용 타이머는 포함하지 않는다.

프로필·캐릭터·응원 수·기본 타이머 설정과 화면 배치는 기존 API/프론트 상태의 범위이며 이 할 일 상세 DTO에 별도 추가하지 않는다.

세션 없을 때 공개 기본값은 둘 다 true다. 공개 설정은 할 일별 영구 저장이 아니라 현재 세션 설정이다. 시작 전 토글을 변경했다면 프론트가 시작 요청의 기존 isTaskPublic/isTimerPublic에 전달한다. 시작 후에는 기존 visibility API를 사용하고 상세를 재조회한다. 본인 상세 화면은 비공개 상태라도 자신의 제목/시간을 볼 수 있으며, 공개 토글은 타인에게 공개하는 여부다.

## 시간 필드의 구분

- `todo.actualTimeInSeconds`: DB에 저장된 전체 할 일 누적시간. 조회만으로 증가하지 않는다.
- `todo.progressRate`: 저장된 누적시간 기준 달성률. 기존 TodoResponse 의미를 유지한다.
- `accumulatedTimeInSeconds`: 저장된 전체 누적시간 + 최신 열린 RUNNING 집중 구간의 아직 저장되지 않은 시간.
- 최상위 `progressRate`: 표시용 누적시간을 목표시간으로 나눈 비율(내림, 최대 100). 목표 미설정이면 null.
- `activeSession.focusTimeInSeconds`: 현재 세션의 NORMAL/FOCUS 구간 합계. 이전 세션 및 BREAK를 제외한다.
- `activeSession.phaseElapsedTimeInSeconds`: 현재 phaseType·round의 구간 합계. 일시정지/재개의 여러 구간을 합산한다. BREAK일 때는 휴식 경과시간이다.
- `activeSession.isCounting`: 최신 열린 구간이 RUNNING이며 시작 시각이 스냅샷 시각 이후가 아닌 경우 true. BREAK도 휴식 시계는 진행하므로 true일 수 있다.
- `serverTime`: 한 번 캡처해 계산과 최근 작업 시각 집계에 공통 사용한 스냅샷 시각. ISO +09:00 문자열이다.

누적 할 일 시간은 기존 TodoAccumulatedTimeCalculator를 재사용한다. 현재 세션 집중시간은 totalDuration 필드를 그대로 복사하지 않고 구간 기록으로 계산한다. 기존 totalDuration에는 과거/기존 처리에서 휴식이 섞일 수 있으므로 새 상세 응답의 집중시간과 혼동하지 않는다.

상태가 PAUSED이거나 최신 구간이 닫혀 있으면 열린 구간을 추가하지 않는다. 최신 구간보다 오래된 고아 열린 구간, 미래 시작/종료 및 음수 길이 구간은 현재 집중시간으로 더하지 않는다.

## 응답 예시

```json
{
  "statusCode": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "todo": {
      "id": "7f000001-9a3d-1f34-819a-3d92e3800001",
      "categoryId": "7f000001-9a3d-1f34-819a-3d92e3800002",
      "task": "독서",
      "date": "2026-10-05",
      "targetTimeInSeconds": 3600,
      "actualTimeInSeconds": 900,
      "isCompleted": false,
      "progressRate": 25,
      "lastWorkedAt": "2026-10-05T12:35:00+09:00"
    },
    "category": {
      "id": "7f000001-9a3d-1f34-819a-3d92e3800002",
      "name": "기본",
      "color": "GREEN",
      "displayOrder": 1,
      "isExpanded": true
    },
    "accumulatedTimeInSeconds": 960,
    "progressRate": 26,
    "isTaskPublic": true,
    "isTimerPublic": false,
    "activeSession": {
      "sessionId": "7f000001-9a3d-1f34-819a-3d92e3800003",
      "mode": "TIMER",
      "status": "RUNNING",
      "participationType": "INDIVIDUAL",
      "groupId": null,
      "startedAt": "2026-10-05T12:20:00+09:00",
      "targetDuration": 1800,
      "focusDuration": null,
      "breakDuration": null,
      "repeatCount": null,
      "phaseType": "NORMAL",
      "round": 0,
      "currentIntervalStartedAt": "2026-10-05T12:34:00+09:00",
      "focusTimeInSeconds": 360,
      "phaseElapsedTimeInSeconds": 360,
      "isCounting": true
    },
    "serverTime": "2026-10-05T12:35:00+09:00"
  }
}
```

활성 세션이 없으면 activeSession은 null, accumulatedTimeInSeconds는 저장된 누적시간, 공개 기본값은 true다. 목표가 없으면 todo.targetTimeInSeconds, todo.progressRate, 최상위 progressRate가 null이다. 최근 작업 이력이 없으면 todo.lastWorkedAt도 null이다. Swagger에는 목표시간/활성 세션 없는 응답 예시를 제공한다.

## 프론트 시간 표시와 재조회

새 개인용 매초 브로드캐스트를 만들지 않는다. 조회 스냅샷과 단조 증가 클라이언트 시계(performance.now 등)를 기준으로 표시하고, 변동 이벤트에서 최신 스냅샷을 다시 받는다.

1. 할 일 선택 시 GET 상세로 저장된 목표·공개 설정·타이머 정보를 구성한다.
2. isCounting=true이면 수신 후 실제 경과한 초를 phaseElapsedTimeInSeconds에 더한다.
3. NORMAL/FOCUS일 때만 accumulatedTimeInSeconds와 focusTimeInSeconds도 늘린다. BREAK에서는 휴식 시계만 증가시킨다.
4. 목표가 있으면 표시용 누적시간으로 달성률을 다시 계산한다. null 목표를 0으로 치환하지 않는다.
5. TIMER 남은 시간은 max(targetDuration - focusTimeInSeconds, 0), STOPWATCH는 focusTimeInSeconds, POMODORO는 현재 phase 설정시간 - phaseElapsedTimeInSeconds로 표시한다.
6. 시작/일시정지/재개/종료/뽀모도로 전환/목표 수정/공개 수정 후 GET 상세를 재조회한다. API 실패 시 변경 성공으로 간주하지 않는다.
7. 앱 복귀, 재연결, 타이머 완료 알림 등에서도 재조회한다. 다른 탭/기기 변경까지 동기화하려면 주기적 재조회도 사용할 수 있다.

기존 개인 완료 알림 경로 `/topic/user/{userId}/timer-completion`을 재조회 신호로 활용할 수 있다. 그룹 멤버 상태와 공식 라운지 presence 갱신은 기존 경로를 유지한다. 표시 시계가 0이 됐다는 이유만으로 서버 FINISHED/다음 phase 상태를 추정하지 않고 기존 명령 API 결과를 기준으로 한다.

## 정확한 누적시간 저장 보완

새 상세 화면의 전체 누적시간이 현재 세션/휴식과 섞이지 않도록 기존 개인·그룹 내 개인 타이머 저장 경로를 보완했다.

- 집중 일시정지/종료: 이번에 닫은 NORMAL/FOCUS 구간만 할 일에 추가한다.
- 휴식 일시정지/종료: 할 일 집중시간에 추가하지 않는다.
- 뽀모도로 단계 전환: 방금 닫은 FOCUS 구간을 해당 시점에 저장한다. 중간 집중 라운드도 즉시 반영된다.
- 이미 일시정지로 저장한 구간은 단계 전환/최종 완료에서 다시 추가하지 않는다.
- 공개 토글·조회·목표 변경은 누적시간을 저장하지 않는다.

새 DB 컬럼이나 수동 SQL은 없다. 과거에 잘못 저장된 누적값을 임의 재계산하거나 실제 DB 데이터를 수정하지 않는다. 기존 앱으로 진행 중이던 뽀모도로 세션의 이전 중간 구간 등 과거 반영 누락/중복은 별도 검토가 필요하며 이번 PR에서 자동 복구하지 않는다.

## 검증

단위/HTTP 테스트와 실제 MySQL에서 본인/삭제 조건, 모든 모드, 목표 설정/해제, 기존 visibility API 연계, 진행 중 계산, 읽기 후 저장값 불변, 휴식 제외, 일시정지/재개 및 마지막 라운드 중복 방지를 확인한다.

```bash
ISSUE101_MYSQL_URL='jdbc:mysql://127.0.0.1:53401/issue101_verify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul' \
JAVA_TOOL_OPTIONS='-Duser.timezone=Asia/Seoul' \
./gradlew test --tests '*TodoSidebarRepositoryTest'
```

이 통합 테스트는 로컬 임시 MySQL root/빈 비밀번호의 폐기 가능한 전용 DB에만 연결한다. create-drop을 사용하므로 실제 서비스 DB를 연결하지 않는다. 전체 domain/global 회귀에서는 #98~#100 테스트의 전용 DB도 각각 준비한다. 실제 외부 버킷 CORS를 수정하는 루트 테스트는 제외한다.

다음 이슈 #102는 이 목록/상세 및 최근 작업 시각 계약을 유지하면서 할 일 실시간 초성·부분 검색을 보완한다.
