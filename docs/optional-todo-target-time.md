# 할 일 목표시간 선택값 (#98)

## API 계약

- `POST /api/todos`: `targetTimeInSeconds` 생략 또는 `null`이면 목표시간 미설정.
- `PUT /api/todos/{todoId}`: 전체 수정 API이므로 생략 또는 `null`이면 기존 목표시간을 해제한다. 기존 목표를 유지하려면 현재 값을 함께 전달한다.
- 값이 있을 때는 기존과 같이 60~86400초 범위를 허용한다. `0`을 미설정 값으로 보내지 않는다.
- 할 일 조회의 `targetTimeInSeconds` 및 타이머 시작 응답의 `todo.targetTimeInSeconds`는 미설정이면 `null`이다.
- 할 일 및 개인·그룹 내 개인 타이머 응답의 `progressRate`는 목표시간 미설정이면 `null`이다. 목표가 있고 누적 작업시간이 없으면 `0`, 목표가 있으면 기존처럼 누적 작업시간으로 계산하고 최대 100으로 제한한다.
- 프론트는 `null` 달성률을 `—` 또는 미설정 상태로 표시하고, 실제 0%와 구분한다.
- 카운트다운의 `targetSeconds`는 할 일 목표시간과 별개이며 계속 필수다. 뽀모도로 집중·휴식 시간 및 반복 횟수도 기존 필수 설정을 유지한다.
- 모든 개인 타이머 모드에서 `todoId`는 계속 필수이며 소유권·삭제 검증도 유지한다. 그룹 공용 타이머는 변경하지 않는다.

목표시간 없는 생성 요청:

```json
{
  "categoryId": "7f000001-9a3d-1f34-819a-3d92e3800001",
  "task": "독서",
  "date": "2026-10-05"
}
```

할 일 응답의 관련 필드:

```json
{
  "targetTimeInSeconds": null,
  "actualTimeInSeconds": 600,
  "progressRate": null
}
```

이 할 일로 카운트다운을 실행할 때도 `todoId`, `targetSeconds`, `participationType`을 전달한다. 목표시간이 없어도 작업시간 누적과 타이머 완료 알림은 정상적으로 동작한다.

## 기존 MySQL DB 적용

Hibernate `ddl-auto: update`에 기존 NOT NULL 제약 제거를 의존하지 않는다. 기존 DB에는 **새 애플리케이션 배포 전에** `scripts/sql/98-optional-todo-target-time.sql`을 해당 DB에서 실행한다. 신규 DB는 변경된 엔티티로 nullable 컬럼을 생성한다.

1. 대상 DB와 `SHOW CREATE TABLE todo`의 컬럼 타입이 `INT`인지 확인하고 기존 운영 절차로 백업한다.
2. 적용 전 행 수·목표시간·누적 작업시간을 확인한다.
3. MySQL 클라이언트의 기존 인증 방식으로 대상 DB에 연결해 위 SQL을 실행한다. 비밀번호를 명령줄에 노출하지 않는다.
4. SQL의 확인 쿼리에서 `IS_NULLABLE = YES`와 `DATA_TYPE = int`를 확인한다.
5. 새 애플리케이션을 배포하고 목표시간 없는 생성·조회·타이머 시작을 확인한다.

SQL은 컬럼의 null 허용 여부만 변경하며 기존 값, 행 및 집중 기록을 삭제·변경하지 않는다. 동일 SQL의 재실행도 동일 스키마를 유지한다. 이번 PR은 DB에 직접 적용하거나 배포하지 않는다.

## 복구

프론트도 nullable 응답 계약에 맞춰 배포한다. 이전 앱 버전으로 복귀해야 한다면 신규 쓰기를 중단하고 다음을 확인한다.

```sql
SELECT COUNT(*) AS unset_target_count
FROM todo
WHERE target_time_in_seconds IS NULL;
```

- 0인 경우 `scripts/sql/98-optional-todo-target-time-rollback.sql`로 NOT NULL을 복원한 후 이전 앱을 배포할 수 있다.
- 1건 이상이면 NOT NULL 복원을 실행하지 않는다. 미설정 목표를 0 또는 임의 시간으로 자동 치환하면 사용자 의미를 훼손한다. nullable 스키마를 유지하고, 사용자와 합의한 데이터 복구 또는 호환 앱 버전을 준비한다.
- 이번 변경으로 새로 기록된 세션에는 `progressRate = null`이 있을 수 있으므로 이전 프론트와의 호환성도 확인한다.

## 검증 방법

다음 명령으로 domain/global 패키지의 회귀 테스트를 실행한다. 요청 검증·JSON 응답·할 일 생성/수정·타이머 생명주기와 기존 회귀 테스트를 포함한다.

```bash
./gradlew test --tests 'com.mogakjak.mogakjak.domain.*' --tests 'com.mogakjak.mogakjak.global.*'
```

기존 루트 패키지의 `MogakjakApplicationTests.setCors()`는 외부 S3 버킷의 CORS 설정을 실제 변경하는 테스트이므로 위 실행에서 제외한다. 전체 `./gradlew test` 실행은 이 테스트의 외부 서비스 설정을 요구한다.

MySQL 마이그레이션 테스트는 별도의 폐기 가능한 `issue98_verify` 데이터베이스에서만 실행한다. 테스트가 `todo` 테이블을 생성·삭제하므로 실제 애플리케이션 DB를 연결하지 않는다. 환경변수가 없으면 해당 테스트는 건너뛴다.

```bash
ISSUE98_MYSQL_URL='jdbc:mysql://127.0.0.1:53398/issue98_verify?useSSL=false&allowPublicKeyRetrieval=true' \
  ./gradlew test --tests '*OptionalTodoTargetMigrationTest'
```

이 테스트는 로컬 임시 MySQL의 root / 빈 비밀번호 접속을 사용한다. 기존 NOT NULL 상태에서 NULL 저장 거절, SQL 적용 및 재실행, 기존 목표·누적시간 보존, NULL 저장·수정, NULL이 있을 때 복구 거절, NULL이 없을 때 복구를 확인한다.

## 후속 범위

목표시간 단독 변경은 #99, 최근 작업일은 #100, 선택 후 조회 보완은 #101, 검색은 #102에서 처리한다.
