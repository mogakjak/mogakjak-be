# 모각작 프로젝트 (Mogakjak Project)

## 시작하기 (Getting Started)

이 프로젝트를 로컬 환경에서 실행하기 위한 안내입니다.

### 사전 요구 사항 (Prerequisites)

* **Docker Desktop** 이 설치되어 있어야 합니다.

### 환경 설정 (Environment Setup)

1.  프로젝트를 클론합니다.
    ```bash
    git clone [https://github.com/mogakjak/mogakjak-be.git](https://github.com/mogakjak/mogakjak-be.git)
    cd mogakjak
    ```

2.  `.env` 파일을 생성합니다.
    프로젝트 루트에 있는 `.env.example` 파일을 복사하여 `.env` 파일을 생성한 후, 필요한 환경 변수 값을 채워주세요.

### 실행하기 (Running the Application)

1.  애플리케이션을 빌드합니다. (Gradle 기준)
    ```bash
    ./gradlew bootJar
    ```

2.  Docker Compose를 사용하여 모든 서비스를 실행합니다.
    ```bash
    docker-compose up -d --build
    ```

3.  애플리케이션이 `http://localhost:8080` 에서 정상적으로 실행되었는지 확인합니다.
    * **Swagger UI**: `http://localhost:8080/swagger-ui.html`
    * **Grafana**: `http://localhost:3001`
    * **Prometheus**: `http://localhost:9090`