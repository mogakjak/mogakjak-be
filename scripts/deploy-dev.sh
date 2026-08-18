#!/usr/bin/env bash

set -Eeuo pipefail

readonly PROJECT_DIR="/home/ubuntu/mogakjak-be"
readonly DEPLOY_BRANCH="develop"
readonly HEALTH_URL="http://127.0.0.1:8080/actuator/health"
readonly MAX_HEALTH_ATTEMPTS=30
readonly HEALTH_RETRY_SECONDS=2

skip_git_update=false
if [[ "${1:-}" == "--skip-git-update" ]]; then
  skip_git_update=true
fi

cd "${PROJECT_DIR}"

exec 9>/tmp/mogakjak-develop-deploy.lock
if ! flock -n 9; then
  echo "Another develop deployment is already running."
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Deployment aborted: the server worktree contains uncommitted changes."
  git status --short
  exit 1
fi

if [[ "${skip_git_update}" == "false" ]]; then
  git fetch origin "${DEPLOY_BRANCH}"
  git checkout "${DEPLOY_BRANCH}"
  git merge --ff-only "origin/${DEPLOY_BRANCH}"
fi

if [[ "$(git branch --show-current)" != "${DEPLOY_BRANCH}" ]]; then
  echo "Deployment aborted: expected branch ${DEPLOY_BRANCH}."
  exit 1
fi

echo "Building Spring Boot application at $(git rev-parse --short HEAD)..."
./gradlew clean bootJar

echo "Rebuilding the app container..."
docker-compose up -d --build --no-deps app

echo "Waiting for application health check..."
for ((attempt = 1; attempt <= MAX_HEALTH_ATTEMPTS; attempt++)); do
  if curl --fail --silent --show-error "${HEALTH_URL}" | grep -q '"status":"UP"'; then
    echo "Deployment completed successfully."
    docker-compose ps app
    exit 0
  fi

  echo "Health check attempt ${attempt}/${MAX_HEALTH_ATTEMPTS} failed."
  sleep "${HEALTH_RETRY_SECONDS}"
done

echo "Deployment failed: application did not become healthy."
docker-compose ps app
docker-compose logs --tail=100 app
exit 1
