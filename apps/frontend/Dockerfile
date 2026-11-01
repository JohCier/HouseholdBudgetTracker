# syntax=docker/dockerfile:1
# Build context: repository root (the build needs the npm workspace and packages/shared-types)

# Node major version, source is apps/frontend/.nvmrc. The image build in ci.yml passes it from there; compose.yaml
# (local stack, E2E tests) uses this default, which the job "frontend" in ci.yml checks against .nvmrc.
ARG NODE_VERSION=22

# --- Build: Angular-App bauen -------------------------------------------------
FROM node:${NODE_VERSION}-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
COPY apps/frontend/package.json apps/frontend/
COPY packages/shared-types/package.json packages/shared-types/
RUN --mount=type=cache,target=/root/.npm npm ci
COPY packages/shared-types/ packages/shared-types/
COPY apps/frontend/ apps/frontend/
RUN npm run build --workspace=frontend

# --- Runtime: nginx liefert die App aus und leitet /api an die Dienste weiter ---
FROM nginx:1.31-alpine
COPY apps/frontend/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/apps/frontend/dist/frontend/browser/ /usr/share/nginx/html/
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD ["wget", "-q", "--spider", "http://127.0.0.1/"]
