package com.example.demoprometheusexporter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@RestController
public class ReroutePrometheusController {
    private final String appInstanceGuid;
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .requestFactory(new TrustEverythingClientHttpRequestFactory())
            .errorHandler(new NoErrorsResponseErrorHandler())
            .build();

    public ReroutePrometheusController(@Value("${vcap.application.application_id:}") String appInstanceGuid) {
        this.appInstanceGuid = appInstanceGuid;
    }

    @GetMapping(path = "/prometheus-cf")
    public ResponseEntity<String> reroute(RequestEntity<?> request, UriComponentsBuilder builder) {
        UriComponents target = builder.path("prometheus").build();
        String host = target.getHost();
        String instanceIndex = host.split("-")[0];
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.add("X-CF-APP-INSTANCE", this.appInstanceGuid + ":" + instanceIndex);
        URI prometheusUri = target.toUri();
        return this.restTemplate.exchange(prometheusUri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }


    private static final class NoErrorsResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false;
        }
    }

    private static final class TrustEverythingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        @Override
        protected HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
            HttpURLConnection connection = super.openConnection(url, proxy);
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(getSslContext(new TrustEverythingTrustManager()).getSocketFactory());
                httpsConnection.setHostnameVerifier(new TrustEverythingHostNameVerifier());
            }
            return connection;
        }

        private static SSLContext getSslContext(TrustManager trustManager) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                return sslContext;
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static final class TrustEverythingHostNameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    private static final class TrustEverythingTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
