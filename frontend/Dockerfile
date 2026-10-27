# ---------- Etapa de construccion ----------
FROM node:24-alpine AS build
WORKDIR /front

# Primero dependencias para aprovechar la cache
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build        # genera /front/dist

# ---------- Etapa de ejecucion ----------
# Nginx sirve el build de Angular y reenvia /api al backend
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /front/dist/ /usr/share/nginx/html/
EXPOSE 80
