FROM node:22-alpine AS build

WORKDIR /app

RUN npm install -g pnpm@11.16.0

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY index.html tsconfig.json tsconfig.app.json tsconfig.node.json vite.config.ts ./
COPY public ./public
COPY src ./src

RUN pnpm test
RUN VITE_API_BASE_URL="" pnpm run build

FROM nginx:alpine

COPY nginx.container.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 18082
