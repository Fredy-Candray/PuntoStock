FROM node:24-alpine AS build
WORKDIR /src
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build
FROM nginxinc/nginx-unprivileged:1.28-alpine
COPY --from=build /src/dist/puntostock/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
