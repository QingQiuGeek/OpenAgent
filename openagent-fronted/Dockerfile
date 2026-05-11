# ===== Stage 1: Build =====
FROM node:20-alpine AS builder
WORKDIR /build

COPY package*.json ./
RUN npm ci --no-audit --no-fund

COPY . .

# 通过构建参数注入 API BASE URL（docker-compose 会传）
# 默认走 nginx 反代 /api，让前端与后端走同源，避免 CORS
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ===== Stage 2: Nginx =====
FROM nginx:1.27-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
