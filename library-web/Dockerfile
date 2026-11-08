# The H5 client is plain static HTML/CSS/JS; no JavaScript build step is
# required. nginx serves the files and proxies /api to the gateway so the
# browser sees a single origin (no CORS in the local topology).
FROM nginx:1.27-alpine

COPY library-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY library-web/src/main/resources/static/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -q -O - http://127.0.0.1/healthz >/dev/null || exit 1

# nginx:alpine already starts as root and drops privileges for workers.
