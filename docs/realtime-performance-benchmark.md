# 실시간 메시징 성능 비교 계획

## 목적

현재 단일 애플리케이션 서버에서 사용하는 `STOMP + Redis Pub/Sub` 경로와 Redis를 거치지 않는 로컬 STOMP 경로를 같은 조건에서 비교한다.

비교 결과를 근거로 Redis Pub/Sub 제거 여부를 결정하고, 공용 라운지의 적정 최대 인원과 다음 최적화 우선순위를 정한다.

## 비교 대상

### 기준선: Redis Pub/Sub

```text
Application Service
  -> Redis PUBLISH
  -> Redis MessageListener
  -> Simple STOMP Broker
  -> WebSocket Client
```

### 후보: Local STOMP

```text
Application Service
  -> SimpMessagingTemplate
  -> Simple STOMP Broker
  -> WebSocket Client
```

두 구현은 동일한 목적지와 DTO를 사용해야 한다. 최종 삭제 전까지 아래 설정으로 전환할 수 있도록 구성한다.

```yaml
app:
  realtime:
    transport: redis # redis | local
    lounge-payload: full # full | delta
```

## 관측 환경

관측 스택은 `../../myhouse-observability` 저장소의 Prometheus, Grafana, cAdvisor, node-exporter를 사용한다. GlitchTip 관련 컨테이너는 벤치마크 자원을 사용하므로 실행하지 않는다.

```bash
cd ../../myhouse-observability
docker compose -f compose.yaml -f compose.local.yaml up -d \
  prometheus grafana cadvisor node-exporter
```

모각작 애플리케이션은 호스트의 `8081` 포트로 실행한다. Prometheus의 로컬 Spring 타깃에는 다음 항목을 추가한다.

`/actuator/prometheus`는 기본 실행에서 노출되지 않는다. 벤치마크 Compose 오버레이가 `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,prometheus`를 설정하며, 운영에서 사용할 때는 endpoint가 외부 인터넷에 직접 노출되지 않도록 방화벽이나 reverse proxy 접근 범위를 제한한다.

```yaml
- targets:
    - host.docker.internal:8081
  labels:
    environment: local
    service: mogakjak-be
    service_type: application
    runtime: spring
    server: mogakjak-be-local
```

연결 확인:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus
```

Prometheus의 scrape interval은 비교 중 동일하게 유지한다. 짧은 부하 구간을 관찰하려면 5초를 사용하고, 각 시나리오는 최소 두 번의 scrape보다 충분히 길게 실행한다.

## 실행 방법

애플리케이션과 의존 서비스를 시작한다.

```bash
docker compose -f docker-compose.yml -f compose.benchmark.yml \
  up -d --build db redis app
```

기준선은 기존 Redis Pub/Sub와 전체 라운지 payload를 사용한다.

```bash
APP_REALTIME_TRANSPORT=redis \
APP_REALTIME_LOUNGE_PAYLOAD=full \
docker compose -f docker-compose.yml -f compose.benchmark.yml \
  up -d --force-recreate app
```

후보 구조는 Redis Pub/Sub listener를 만들지 않고 Simple STOMP Broker로 직접 전달한다. 라운지 presence는 인증 토큰과 입실 상태 저장을 위해 Redis를 계속 사용한다.

```bash
APP_REALTIME_TRANSPORT=local \
APP_REALTIME_LOUNGE_PAYLOAD=delta \
docker compose -f docker-compose.yml -f compose.benchmark.yml \
  up -d --force-recreate app
```

부하 테스트는 네이티브 STOMP endpoint인 `/connect-native`를 사용한다. `K6_ACCESS_TOKENS`에 쉼표로 구분한 여러 사용자의 access token을 전달하면 실제 라운지 멤버 수를 늘릴 수 있다. 토큰 하나만 사용하면 동일 사용자의 다중 WebSocket fan-out을 측정한다.

```bash
K6_ACCESS_TOKENS='<token-1>,<token-2>' \
K6_CONNECTIONS=20 \
K6_DURATION=10m \
docker compose -f docker-compose.yml -f compose.benchmark.yml \
  --profile load-test run --rm k6 run /scripts/realtime/lounge.js
```

동일한 토큰과 연결 수로 `APP_REALTIME_TRANSPORT`와 `APP_REALTIME_LOUNGE_PAYLOAD`만 변경해 반복한다. access token은 저장소나 결과 파일에 기록하지 않는다.

## 수집 지표

### 기본 애플리케이션 지표

- HTTP 요청 처리량과 p50, p95, p99 응답 시간
- JVM process CPU와 system CPU
- heap 사용량과 GC pause
- live thread 수
- HikariCP active, pending, max connection
- 컨테이너 CPU, 메모리, 네트워크 송수신량

### 실시간 기능 커스텀 지표

| Metric | Type | 설명 |
| --- | --- | --- |
| `mogakjak_websocket_sessions` | Gauge | 현재 WebSocket 연결 수 |
| `mogakjak_realtime_messages_total` | Counter | transport, channel, stage별 메시지 수 |
| `mogakjak_realtime_publish_seconds` | Timer | 애플리케이션의 메시지 발행 처리 시간 |
| `mogakjak_realtime_payload_bytes` | DistributionSummary | channel별 payload 크기 |
| `mogakjak_realtime_deliveries_total` | Counter | 실제 목적지 전송 횟수 |
| `mogakjak_lounge_members` | Gauge | 현재 공용 라운지 입실 인원 |

메트릭 라벨에는 `userId`, `groupId`, `sessionId`처럼 값이 계속 증가하는 식별자를 사용하지 않는다. 허용 라벨은 `transport`, `channel`, `stage`, `result`처럼 값의 종류가 제한된 항목으로 한정한다.

서버 메트릭만으로는 브로커 이후의 네트워크 지연을 알 수 없다. 부하 발생기는 이벤트 발행 시점부터 클라이언트 수신 시점까지의 end-to-end 지연도 별도로 기록한다.

## 부하 시나리오

모든 시나리오는 동일한 애플리케이션 CPU·메모리 제한과 동일한 DB 데이터에서 수행한다.

| 단계 | 동시 연결 | 목적 |
| ---: | ---: | --- |
| 1 | 20 | 현재 공식 라운지 제한에서 정상 동작 확인 |
| 2 | 100 | 단일 서버의 일반 부하 확인 |
| 3 | 300 | 전체 목록 브로드캐스트 병목 확인 |
| 4 | 500 | 델타 이벤트 전환 필요 시점 확인 |

각 연결 단계에서 다음 부하를 분리해 실행한다.

1. 연결만 유지하는 idle WebSocket
2. 짧은 시간의 동시 연결과 라운지 입장
3. 타이머 시작, 일시정지, 재개, 종료
4. 공용 라운지 응원 이벤트 연속 전송
5. WebSocket 연결 해제와 재접속
6. 여러 이벤트가 섞인 실제 사용 패턴

## 실행 규칙

- 구현별로 2분 이상 워밍업한다.
- 측정 구간은 시나리오당 10분 이상 유지한다.
- 각 시나리오는 최소 3회 반복한다.
- 실행 순서에 따른 영향을 줄이기 위해 Redis와 local 순서를 번갈아 실행한다.
- 실행 전 컨테이너를 재시작하고 같은 초기 데이터와 리소스 제한을 적용한다.
- 측정 중 모니터링 이외의 컨테이너와 로컬 작업을 최소화한다.
- 평균값만 보지 않고 p95, p99와 최악 구간을 함께 기록한다.

## 결과 기록표

| Transport | Connections | Scenario | E2E p95 | E2E p99 | App CPU | Heap max | Network TX | Error rate |
| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Redis | 20 |  |  |  |  |  |  |  |
| Local | 20 |  |  |  |  |  |  |  |
| Redis | 100 |  |  |  |  |  |  |  |
| Local | 100 |  |  |  |  |  |  |  |
| Redis | 300 |  |  |  |  |  |  |  |
| Local | 300 |  |  |  |  |  |  |  |
| Redis | 500 |  |  |  |  |  |  |  |
| Local | 500 |  |  |  |  |  |  |  |

## 판단 기준

다음 조건을 모두 만족하면 단일 서버 운영에서 Redis Pub/Sub 제거를 우선 검토한다.

- local 전송에서 기능 누락이나 메시지 라우팅 차이가 없다.
- 목표 동시 접속 구간에서 p95와 p99 지연이 같거나 개선된다.
- 오류율과 비정상 WebSocket 종료가 증가하지 않는다.
- CPU, heap, 네트워크 사용량이 같거나 감소한다.
- 서버 재시작 시 유실 가능한 일시 상태의 범위가 제품 요구사항에 부합한다.

Redis Pub/Sub 제거와 공용 라운지 확장은 별도로 판단한다. 현재 공용 라운지는 상태 변경마다 전체 멤버 목록을 모든 구독자에게 보내므로 인원이 늘수록 payload와 fan-out 비용이 함께 증가한다. 100명 이상을 목표로 할 때는 최초 REST 조회 후 변경된 멤버만 보내는 델타 이벤트 구조를 함께 비교한다.

## 작업 순서

- [x] Actuator Prometheus endpoint에서 모각작 기본 지표 수집 확인
- [x] 실시간 기능 커스텀 지표 추가
- [x] `RealtimeEventPublisher` 경계 도입
- [ ] Redis transport 기준선 측정
- [x] local STOMP transport 구현
- [ ] 동일 부하 시나리오 재측정
- [ ] Redis Pub/Sub 유지 또는 제거 결정 기록
- [x] 공용 라운지 전체 스냅샷과 델타 이벤트 전환 옵션 구현
- [ ] 공용 라운지 전체 스냅샷과 델타 이벤트 측정
- [ ] 검증된 최대 인원을 공식 라운지 설정에 반영
