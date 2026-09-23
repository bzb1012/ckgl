# ---- 构建阶段：Vite 打包 ----
FROM node:20-alpine AS build
WORKDIR /app

# 国内加速：npmmirror
RUN npm config set registry https://registry.npmmirror.com
COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---- 运行阶段：nginx 托管静态文件并反代后端 ----
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
