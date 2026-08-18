# Oracle 개발 서버 배포

`develop` 브랜치에 push 또는 merge가 발생하면
`.github/workflows/deploy-develop.yml`이 Oracle 개발 서버에 자동 배포합니다.

## 배포 대상

- 서버 저장소: `/home/ubuntu/mogakjak-be`
- Git 브랜치: `develop`
- Docker Compose 서비스: `app`
- 헬스체크: `http://127.0.0.1:8080/actuator/health`

MySQL, Redis, Prometheus, Grafana 컨테이너는 재생성하지 않습니다.
서버의 `.env`와 `src/main/resources/application-dev.yml`도 기존 파일을 그대로 사용합니다.

## GitHub Actions Secrets

Repository Actions secrets에 다음 값이 필요합니다.

- `ORACLE_HOST`: Oracle 개발 서버 주소
- `ORACLE_USER`: SSH 사용자
- `ORACLE_SSH_PRIVATE_KEY`: CI 전용 SSH 개인키
- `ORACLE_KNOWN_HOSTS`: `ssh-keyscan`으로 확인한 서버 호스트 키

개인 사용자 SSH 키를 Actions secret으로 재사용하지 않습니다.

## 배포 절차

1. 서버 작업 트리가 깨끗한지 확인합니다.
2. `origin/develop`을 fetch하고 fast-forward로만 반영합니다.
3. `./gradlew clean bootJar`로 실행 JAR을 빌드합니다.
4. `docker-compose up -d --build --no-deps app`으로 app 컨테이너만 재생성합니다.
5. 최대 60초 동안 Actuator health가 `UP`인지 확인합니다.

작업 트리가 dirty 상태이거나 fast-forward가 불가능하거나 헬스체크에 실패하면 workflow가 실패합니다.

## 수동 실행

GitHub Actions의 `Deploy develop to Oracle` workflow에서 `Run workflow`를 실행하거나,
서버에서 다음 명령을 사용할 수 있습니다.

```bash
cd /home/ubuntu/mogakjak-be
bash scripts/deploy-dev.sh
```

## 확인

```bash
ssh mogakjak-oracle
cd /home/ubuntu/mogakjak-be
docker-compose ps app
docker-compose logs --tail=200 app
curl http://127.0.0.1:8080/actuator/health
```
