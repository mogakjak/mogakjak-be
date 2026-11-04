# 공식 라운지 NPC

관련 이슈: #109. 기준: [09차 회의](https://app.notion.com/p/09-3fc3e052eacd81d9b69ceb5fbd3f791f)와 구현 전 확정한 정책.

## 인원과 행동

NPC는 모각이(A), 집중이(B), 동동이(C) 세 개다. 필요한 수는 `max(0, min(3, 정원) - 실제 입실 인원)`이다. 실제 사용자 presence와 NPC 상태는 분리하며, 실제 사용자 입장을 NPC가 막지 않는다. 정원은 회의록의 예시 10명을 하드코딩하지 않고 기존 라운지 설정을 사용한다.

| NPC | 활동 시간 | 재입장 작업 변경 확률 | 작업 목록 |
| --- | --- | --- | --- |
| A·모각이 | 50~90분 | 15% | 코딩테스트 준비, 자격증 공부하기, 사이드 프로젝트 작업, 보고서 작성 |
| B·집중이 | 25~50분 | 30% | 토익 공부하기, 포트폴리오 수정하기, 자기소개서 작성하기, 전공 공부 |
| C·동동이 | 15~35분 | 60% | 독서하기, 업무 계획 세우기, 아이디어 정리, 다이어리 쓰기 |

- 시간 범위 양 끝을 포함해 초 단위로 무작위 선택한다. 최초 작업은 해당 유형의 목록에서 무작위로 선택한다.
- 실제 인원이 늘면 초과 NPC의 퇴장을 C→B→A 순으로 선정해 180~300초 뒤로 예약한다. 지연 동안 합계가 3명을 초과할 수 있다. 표시 인원이 정원을 넘는 경우 초과 NPC는 즉시 감축한다.
- 인원이 부족하면 부재 중인 A→B→C 순으로 충원 대상을 골라 0~120초 뒤로 예약한다.
- 모든 유형은 활동 종료 시 퇴장한다. 부족한 자리가 있으면 충원 규칙으로 재입장하며 타이머를 0부터 시작한다. 작업 변경이 선택되면 직전 작업을 제외하고 고른다. 활동 중에는 작업을 바꾸지 않는다.
- 반복 재계산은 기존 예약을 뒤로 미루지 않는다. 사용자 인원이 바뀌면 불필요한 예약을 취소하고, 실행 직전에도 현재 인원을 재평가한다.
- 최초 활성화나 NPC Redis 키 초기화 시 필요한 NPC를 즉시 배치한다. 이후 활동은 1초 주기로 확인하므로 실제 처리 시점에는 스케줄러 실행 지연이 추가될 수 있다.

## 조회·실시간 응답

다음 응답에 `npcMembers`와 `displayMemberCount`를 추가한다.

- `GET /api/lounge`, `POST /api/lounge/enter`, `DELETE /api/lounge/leave`, `PUT /api/lounge/focus-check`
- `GET /api/groups/my`의 공식 라운지 항목
- `GET /api/groups/{groupId}`의 공식 라운지 상세
- 기존 STOMP `/topic/lounge/presence` 전체 현황

`members`, `currentMemberCount`, `participatingMemberCount`, `totalMemberCount`는 기존 실제 사용자 기준을 유지한다. 공식 라운지 `displayMemberCount`는 `members.length + npcMembers.length`다. 일반 그룹에는 NPC가 없고, 상세 응답의 표시 인원은 기존 참여 인원이다. 내 일반 그룹 목록의 표시 인원은 기존 인원 값이 없는 것과 동일하게 null이다.

라운지 현황의 부분 예시:

```json
{
  "currentMemberCount": 1,
  "displayMemberCount": 3,
  "maxMemberCount": 10,
  "members": ["기존 실제 사용자 객체"],
  "npcMembers": [
    {
      "npcId": "A",
      "npcType": "A",
      "nickname": "모각이",
      "profileUrl": null,
      "level": 1,
      "participationStatus": "PARTICIPATING",
      "enteredAt": "2026-11-04T18:00:00",
      "personalTimerSeconds": 30,
      "todoTitle": "코딩테스트 준비"
    },
    {
      "npcId": "B",
      "npcType": "B",
      "nickname": "집중이",
      "profileUrl": null,
      "level": 1,
      "participationStatus": "PARTICIPATING",
      "enteredAt": "2026-11-04T18:00:00",
      "personalTimerSeconds": 20,
      "todoTitle": "토익 공부하기"
    }
  ]
}
```

NPC의 `npcId`는 A/B/C인 고정 문자열이며 사용자 UUID가 아니다. 프론트는 `npcMembers`를 별도 표시 전용 카드로 합쳐 그리고, 응원·메이트·초대·사용자 프로필 이동을 비활성화한다. 기존 사용자 API에 NPC ID를 보내지 않는다. NPC는 사용자·Todo·집중 세션 DB 행, 출석·성장·누적 집중시간, 집중 체크 알림 수신자를 만들지 않는다.

`enteredAt`은 기존 라운지 계약과 같이 KST 로컬 시각이다. `personalTimerSeconds`는 현재 NPC 입장 이후의 경과 초이며, 실제 사용자의 작업 누적시간과 구별한다. 입장 전 시각에는 0, 활동 종료 이후에는 해당 활동 길이로 제한한다. 이미지는 기존 활성 Lv.1 이미지를 공통 재사용하며 없으면 null이다. 프론트는 기본 이미지로 대체한다.

NPC 상태 변경 시 즉시, 타이머는 10초 간격으로 기존 presence 토픽에 전체 현황을 발행한다. NPC 갱신 이벤트의 `eventType`은 `NPC_UPDATE`, `changedUserId`는 null이다. 기존 ENTER/LEAVE/DISCONNECT 등의 메시지에도 같은 NPC 필드를 포함한다.

## 저장·동시성·실패 처리

`official-lounge:npc:state`에 세 NPC의 작업, 입장·활동 종료·예약 시각과 상태 버전을 JSON으로 저장한다. 시각은 epoch milliseconds다. 기존 `official-lounge:presence`에는 실제 사용자 UUID만 유지한다.

상태와 실제 인원수를 읽고 다음 상태를 계산한 뒤, Lua에서 기존 JSON과 현재 실제 인원이 일치할 때만 저장한다. 동시 실행에서 한 번만 적용하며 경쟁 시 최신 값을 읽어 최대 8회 재계산하고 다음 tick으로 넘긴다. 앱 재시작 후 같은 Redis 상태의 예약을 이어받는다. Redis 자체 데이터가 초기화되면 기존 예약 복구 대신 현재 실제 인원으로 초기 배치한다.

NPC 상태 조회 실패 시 NPC 목록은 비우고 실제 사용자 현황을 반환한다. NPC 예약 실패는 사용자 입퇴장을 실패시키지 않으며 스케줄러가 다음 tick에 다시 시도한다. 기존 실제 presence 자체의 장애 처리 의미는 바꾸지 않는다. 로그의 `공식 라운지 NPC 갱신 실패`, `NPC 표시를 읽지 못했습니다`를 확인한다.

기존 Redis Pub/Sub에는 재전송 큐가 없다. 구독 연결이 끊겨 메시지를 놓친 경우 다음 10초 전체 현황이나 REST 재조회로 동기화한다.

## 활성화와 검증

`lounge.npc.enabled` 기본값은 false다. 비활성화 시 NPC 스케줄러 빈을 만들지 않고 NPC Redis 조회도 수행하지 않는다. 응답은 `npcMembers: []`이고 표시 인원은 실제 인원이다. 저장된 NPC 키는 지우지 않으므로 재활성화 시 지난 예약을 현재 인원으로 재평가한다.

개발 서버 배포는 기본 비활성화를 유지한다. 프론트 카드·표시 인원·NPC 상호작용 비활성화 지원을 확인한 후 서버의 기존 `.env`에 `LOUNGE_NPC_ENABLED=true`를 설정하고 배포 절차로 앱 컨테이너를 재생성한다. 해당 변수는 선택값이며 누락하면 Compose가 false를 전달하므로 기존 CI secret 추가는 필요 없다. 비활성화도 false로 바꿔 앱 컨테이너를 재생성한다. DB 마이그레이션은 없다.

정책 단위 테스트는 시간과 난수를 고정한다. 실제 Redis 테스트는 격리된 로컬 인스턴스와 `NPC_REDIS_TEST_PORT`를 사용하며 테스트별 고유 키만 생성·삭제한다.

```sh
NPC_REDIS_TEST_PORT=53909 ./gradlew test --tests '*domain.lounge.*' --tests '*GroupServiceImplTest' --tests '*RedisPubSubServiceTest'
```

회귀 검증에는 인원 0/1/2/3/정원, 동시 예약, 재시작, 예약 취소, 작업 변경 확률, 인증 사용자 응답, Redis 메시지 직렬화·라우팅, 실제 사용자 집계 분리를 포함한다. 실제 개발 서버 확인 시에는 배포 커밋·실행 JAR·health·API 명세·기능 비활성화를 확인한다. 로그인 계정이 제공되지 않은 환경에서는 실제 로그인 사용자 조회나 브라우저 STOMP 수신 확인을 완료했다고 보고하지 않는다.

AI 대화, 시간대별 인원 조절, 별도 NPC 디자인, 프론트 코드 변경은 이번 범위에서 제외한다.
