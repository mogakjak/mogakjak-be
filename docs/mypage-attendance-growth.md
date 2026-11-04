# 마이페이지 출석·성장도 API

관련 이슈: #90 후속. 기존 출석·해금 정책은 [character-growth-policy.md](character-growth-policy.md),
누적 집중시간 원천은 [focus-time-metrics.md](focus-time-metrics.md)를 따른다.

## 출석 현황

`GET /api/mypage/attendance`는 인증된 본인의 출석 현황을 반환한다. 날짜나 사용자 ID를
입력받지 않는다. `referenceDate`는 서버가 요청 처리 시점에 한 번 결정한 KST 날짜다.

- 날짜 오름차순으로 과거 5일, 오늘, 미래 4일의 총 10개 항목을 반환한다.
- 여러 타이머 및 pause/resume의 저장된 `FOCUS`·`NORMAL` 구간을 날짜별로 합산한다.
- 뽀모도로 `BREAK`, 종료 시각 없는 구간, 0초·역전 구간, 조회 시점보다 미래에 종료되는
  구간은 제외한다. 자정을 넘는 구간은 기존 KST DATETIME 해석대로 날짜별 분할한다.
- 당일 합계 1,800초 이상이면 `attended=true`다. 로그인만으로 출석하지 않으며 하루 최대
  1일이다. `today` 강조와 `attended`는 독립적이다.
- 미래 날짜는 항상 `focusTimeInSeconds=0`, `attended=false`, `future=true`다.
- `totalAttendanceDays`는 표시된 10일만의 합이 아닌 전체 누적 인정 출석일이다. 과거 저장
  기록을 소급 계산하며 별도 출석 테이블이나 마이그레이션은 추가하지 않는다.
- 세션 종료 후 재조회하면 저장된 결과를 확인할 수 있다. 진행 중 열린 구간을 실시간으로
  가산하거나, 조회 시 출석·캐릭터를 새로 저장하지 않는다.

응답 예시(`data` 부분):

```json
{
  "referenceDate": "2026-10-19",
  "timeZone": "Asia/Seoul",
  "attendanceThresholdInSeconds": 1800,
  "totalAttendanceDays": 25,
  "days": [
    {"date":"2026-10-14","focusTimeInSeconds":1800,"attended":true,"today":false,"future":false},
    {"date":"2026-10-15","focusTimeInSeconds":0,"attended":false,"today":false,"future":false},
    {"date":"2026-10-16","focusTimeInSeconds":0,"attended":false,"today":false,"future":false},
    {"date":"2026-10-17","focusTimeInSeconds":0,"attended":false,"today":false,"future":false},
    {"date":"2026-10-18","focusTimeInSeconds":1799,"attended":false,"today":false,"future":false},
    {"date":"2026-10-19","focusTimeInSeconds":300,"attended":false,"today":true,"future":false},
    {"date":"2026-10-20","focusTimeInSeconds":0,"attended":false,"today":false,"future":true},
    {"date":"2026-10-21","focusTimeInSeconds":0,"attended":false,"today":false,"future":true},
    {"date":"2026-10-22","focusTimeInSeconds":0,"attended":false,"today":false,"future":true},
    {"date":"2026-10-23","focusTimeInSeconds":0,"attended":false,"today":false,"future":true}
  ]
}
```

## 전체 성장 진행률

`GET /api/mypage/character-basket`의 기존 `growthProgress`에 두 필드를 추가한다.

- `levelProgressRate`: 현재 보유 레벨에서 다음 레벨까지의 구간 진행률(0~100).
- `overallProgressRate`: Lv.1~12의 11개 구간에 대한 전체 진행률(0~100).

현재 레벨 `L`은 서버에서 실제 보유한 최고 레벨이다. 누적 수치만으로 보유 레벨을 올리지
않는다. 두 조건을 만족한 캐릭터 지급은 기존 `POST /api/characters/check-award`가 담당하며
요청 본문은 필요 없다. 프론트는 세션 종료·리뷰 제출 후 이 API를 호출하고 출석·바구니를
재조회한다. 일수나 누적시간을 클라이언트 입력으로 신뢰하지 않는다.

```text
출석 구간 비율 = clamp((현재 누적일 - L 요구일) / (다음 요구일 - L 요구일), 0, 1)
시간 구간 비율 = clamp((현재 누적초 - L 요구초) / (다음 요구초 - L 요구초), 0, 1)
구간 비율 = min(출석 구간 비율, 시간 구간 비율)
levelProgressRate = 구간 비율 × 100
overallProgressRate = ((L - 1) + 구간 비율) / 11 × 100
```

서버는 계산 중 소수점을 버리지 않는다. 정수 퍼센트 문구가 필요하면 프론트에서 최종
`overallProgressRate`만 버림한다. 기존 `attendanceProgressRate`·`focusTimeProgressRate`는
0부터 다음 레벨의 누적 기준까지 계산하는 기존 정수 비율이며 의미를 변경하지 않는다.

Lv.4를 획득한 사용자가 현재 60일·120시간인 경우, Lv.5의 75일·140시간까지 출석 구간은
40%, 시간 구간은 60%이므로 전체 진행률은 약 30.909%다.

```json
{
  "currentAttendanceDays": 60,
  "currentFocusTimeInSeconds": 432000,
  "currentLevel": 4,
  "nextLevel": 5,
  "requiredAttendanceDays": 75,
  "requiredFocusTimeInSeconds": 504000,
  "remainingAttendanceDays": 15,
  "remainingFocusTimeInSeconds": 72000,
  "attendanceProgressRate": 80,
  "focusTimeProgressRate": 85,
  "levelProgressRate": 40.0,
  "overallProgressRate": 30.90909090909091,
  "maxLevelReached": false
}
```

- 남은 조건은 각각 최소 0으로 제한한다. 한 조건만 충족해도 해금되는 것은 아니다.
- 기존 보유 캐릭터는 회수하지 않는다. 새 기준보다 높은 레벨을 이미 보유한 사용자는
  그 레벨을 유지하고 해당 구간 진행률을 최소 0으로 제한한다.
- Lv.12 보유 시 두 새 진행률은 100, `maxLevelReached=true`다. 다음 레벨·요구조건·남은
  조건은 `null`이며 이후 누적 출석·집중시간은 계속 증가한다.
- 누적 집중시간은 기존 `focus_session.total_duration` 기준이다. 출석은 완료된 집중
  interval 기준이며, 공통 그룹 타이머는 양쪽에 합산하지 않는다.

## 검증 및 배포

새 DB 컬럼·필수 환경변수는 없다. 기존 인증 설정에서 `/api/**`는 인증이 필요하다.
새 출석 조회는 사용자 ID를 인증 주체에서만 얻으며 기존 캐릭터 지급 경로를 바꾸지 않는다.
NPC·모바일 화면·캐릭터 이미지나 이름 변경은 이 작업의 범위가 아니다.

실제 MySQL 테스트는 실수로 서비스 DB에 연결하지 않도록 아래 전용 루프백 URL 형태만
허용하며, 지정하지 않으면 해당 테스트는 skipped다. 테스트는 스키마를 생성·삭제하므로
폐기용 `mypage_verify` DB에서만 실행한다.

```bash
MYPAGE_MYSQL_URL='jdbc:mysql://127.0.0.1:53490/mypage_verify?useSSL=false&allowPublicKeyRetrieval=true' \
  ./gradlew test --tests 'com.mogakjak.mogakjak.domain.*' \
  --tests 'com.mogakjak.mogakjak.global.*' bootJar
```

기존 루트 `MogakjakApplicationTests.setCors()`는 외부 S3 버킷 설정을 변경하므로 위 검증에
포함하지 않는다. 실제 프론트 화면 연동은 이 저장소의 HTTP·MySQL 검증과 별도다.
