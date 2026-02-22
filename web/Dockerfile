FROM nginx:alpine

# Non-root 사용자로 실행하기 위한 설정
RUN chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid && \
    chown -R nginx:nginx /usr/share/nginx/html

# 정적 파일 복사
COPY --chown=nginx:nginx index.html /usr/share/nginx/html/
COPY --chown=nginx:nginx app.js /usr/share/nginx/html/
COPY --chown=nginx:nginx style.css /usr/share/nginx/html/
COPY --chown=nginx:nginx config.js /usr/share/nginx/html/

# Nginx 설정 복사
COPY --chown=nginx:nginx nginx.conf /etc/nginx/conf.d/default.conf

USER nginx

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]