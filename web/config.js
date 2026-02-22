window.__CONFIG__ = {
  // 동적 API Gateway URL 생성
  // 로컬 환경: localhost:8080 사용
  // OCP 환경: 브라우저 URL 기반으로 api-gateway URL 자동 생성
  //   예: web-xxx.apps.ocp.com → api-gateway-xxx.apps.ocp.com
  BASE_URL: (function() {
    const host = window.location.host;
    const protocol = window.location.protocol;
    
    // 로컬 환경 감지 (localhost 또는 127.0.0.1)
    if (host.includes('localhost') || host.includes('127.0.0.1')) {
      return 'http://localhost:8080';
    }
    
    // OCP 환경: web- 를 api-gateway- 로 변경
    const apiHost = host.replace('web-', 'api-gateway-');
    return protocol + '//' + apiHost;
  })()
};