# ---- Stage 1: build ----
FROM node:20-alpine AS build
WORKDIR /app

# Install deps separately for better caching
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund

# Copy source and build
COPY . .
# Provide defaults that get baked into the bundle. Override with build args if needed.
ARG VITE_API_URL=
ARG VITE_WS_URL=
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_WS_URL=${VITE_WS_URL}
RUN npm run build

# ---- Stage 2: serve ----
FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
