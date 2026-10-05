# 개발 DB 확인 및 #98 적용 기록 (2026-10-05)

사용자 요청에 따라 개발 서버에 SSH로 접속해 읽기 전용으로 확인한 뒤, **별도 사용자 승인**을 받아 #98의 목표시간 nullable SQL만 적용했다. 인증 정보나 사용자 개별 기록은 이 문서에 남기지 않는다.

## 확인 결과

- 서버 checkout: PR #104 머지 커밋 `22ce812dac83f2cde3e7ee69c7c9335943529074`.
- 앱 컨테이너 실행 중, MySQL 8.0 컨테이너 healthy.
- 대상: `mogakjak.todo.target_time_in_seconds`.
- 적용 전: INT, IS_NULLABLE=NO, 기본값 없음, 컬럼 comment 없음.
- 할 일 436행, 목표 미설정 0건.
- 집중 구간 795건 중 양의 길이를 가진 NORMAL/FOCUS 종료 구간 763건.
- focus_session/focus_interval에는 PRIMARY만 존재. #100에서 조회용 인덱스를 정의한다.
- 호스트와 앱 컨테이너 UTC, DB global/session time_zone=SYSTEM, system_time_zone=UTC.
- 앱 TZ/JAVA_TOOL_OPTIONS 비어 있고 JAVA_OPTS에는 메모리 설정만 존재.
- OS 시각 및 JVM 초기 user.timezone 속성과 별도로, 앱의 `@PostConstruct`가 TimeZone.setDefault(Asia/Seoul)를 호출한다. 타이머의 JVM 로컬 기록은 정상 실행 시 한국 시각으로 해석한다.

## 실행 및 보존 검증

todo 테이블을 single-transaction mysqldump로 백업한 뒤 다음 SQL을 실행했다.

```sql
ALTER TABLE todo MODIFY COLUMN target_time_in_seconds INT NULL;
```

변경 후 information_schema에서 DATA_TYPE=int, IS_NULLABLE=YES를 확인했다. 별도 기록 갱신이나 테스트 데이터 삽입은 하지 않았다.

| 검증 항목 | 전 | 후 |
| --- | ---: | ---: |
| 행 수 | 436 | 436 |
| 목표시간 합계 | 5,052,202 | 5,052,202 |
| 누적 작업시간 합계 | 20,393,371 | 20,393,371 |
| ID·목표시간·누적시간 CRC32 XOR 검증값 | 3,320,775,558 | 3,320,775,558 |

백업은 개발 서버 `/home/ubuntu/mogakjak-db-backups/20261005-issue98-todo-before-nullable.sql`에 보관했다. 디렉터리 권한 700, 파일 권한 600으로 접근 제한했으며 컨테이너 재생성과 무관하게 호스트에 유지된다. 개인정보가 포함될 수 있으므로 저장소에는 백업 내용을 추가하지 않는다.

이번 변경은 NOT NULL 제약만 완화한다. 기존 436행을 삭제하거나 목표시간/누적시간 값을 변경하지 않았다. #100 인덱스 및 새 코드는 아직 이 기록의 DB 적용 대상이 아니다.

## 복구 주의

nullable 값이 새로 저장된 이후에는 NOT NULL을 바로 복원할 수 없다. `docs/optional-todo-target-time.md`의 복구 절차에 따라 NULL 건수를 확인해야 한다. 임의의 목표시간으로 치환하거나 전체 백업 복원으로 이후 정상 작업을 덮어쓰지 않는다.
