#!/bin/bash
# Prepares a Claude Code cloud session so that builds and all tests run:
# JDK 25 (the image only ships 21), a current Node (the image's is too old for the
# Angular CLI), a Docker daemon for Testcontainers, and the npm and Maven dependencies. Idempotent; does nothing outside cloud sessions.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR"
ENV_FILE="${CLAUDE_ENV_FILE:-/dev/null}"

# JDK 25 for apps/backend and apps/users
JDK=/usr/lib/jvm/java-25-openjdk-amd64
if [ ! -x "$JDK/bin/java" ]; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get install -y -qq openjdk-25-jdk-headless >/dev/null \
    || { apt-get update -qq && apt-get install -y -qq openjdk-25-jdk-headless >/dev/null; }
fi
export JAVA_HOME="$JDK"
echo "export JAVA_HOME=$JDK" >> "$ENV_FILE"
echo "export PATH=$JDK/bin:\$PATH" >> "$ENV_FILE"

# Docker daemon for the @IntegrationTest tests (Testcontainers)
if ! docker info >/dev/null 2>&1; then
  setsid nohup dockerd >/tmp/dockerd.log 2>&1 < /dev/null &
  for _ in $(seq 1 30); do
    docker info >/dev/null 2>&1 && break
    sleep 1
  done
fi
if docker info >/dev/null 2>&1; then
  docker image inspect postgres:18-alpine >/dev/null 2>&1 || docker pull -q postgres:18-alpine >/dev/null || true
else
  echo "Docker did not start (see /tmp/dockerd.log); only ./mvnw test works, not ./mvnw verify" >&2
fi

# Latest Node of the major version in apps/frontend/.nvmrc, from the npm registry
# (nodejs.org is not reachable); npm itself keeps coming from the image
NODE_DIR=/opt/node-current/node_modules/node-linux-x64/bin
if [ ! -x "$NODE_DIR/node" ]; then
  npm install --prefix /opt/node-current --no-save --no-package-lock --no-audit --no-fund \
    --no-update-notifier --loglevel=error "node-linux-x64@$(cat apps/frontend/.nvmrc)" >/dev/null
fi
export PATH="$NODE_DIR:$PATH"
echo "export PATH=$NODE_DIR:\$PATH" >> "$ENV_FILE"

# npm workspace (frontend, shared-types); ci instead of install leaves package-lock.json untouched
npm ci --no-audit --no-fund --no-update-notifier --loglevel=error

# Maven dependencies and plugins of both Java apps
for app in backend users; do
  (cd "apps/$app" && ./mvnw -B -q dependency:go-offline >/dev/null) \
    || echo "Could not prefetch Maven dependencies of $app" >&2
done
