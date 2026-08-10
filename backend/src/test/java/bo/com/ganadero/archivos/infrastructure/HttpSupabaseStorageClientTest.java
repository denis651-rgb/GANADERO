package bo.com.ganadero.archivos.infrastructure;

import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpSupabaseStorageClientTest {

    private static final String URL = "https://proyecto.supabase.co";
    private static final String BUCKET = "ganadero-private";

    @Test
    void rejectsUploadWhenKeyIsMissing() {
        var storage = client("");
        assertThatThrownBy(() -> storage.upload("empresas/x/documentos/animals/foto.jpg",
                new byte[]{1, 2, 3}, "image/jpeg"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_NOT_CONFIGURED));
    }

    @Test
    void rejectsSignedUrlWhenKeyIsMissing() {
        var storage = client("");
        assertThatThrownBy(() -> storage.signedUrl("empresas/x/documentos/animals/foto.jpg"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_NOT_CONFIGURED));
    }

    private static final String ENC_PATH = "empresas%2Fx%2Fdocumentos%2Fanimals%2Ffoto.jpg";

    @Test
    void uploadsWhenStorageRespondsOk() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var storage = client(builder, "clave-secreta");
        server.expect(requestTo(URL + "/storage/v1/object/" + BUCKET + "/" + ENC_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "clave-secreta"))
                .andExpect(header("Authorization", "Bearer clave-secreta"))
                .andRespond(withSuccess());
        storage.upload("empresas/x/documentos/animals/foto.jpg", new byte[]{1, 2, 3}, "image/jpeg");
        server.verify();
    }

    @Test
    void translatesUpstreamFailureToStorageUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var storage = client(builder, "clave-secreta");
        server.expect(requestTo(URL + "/storage/v1/object/" + BUCKET + "/" + ENC_PATH))
                .andRespond(withServerError());
        assertThatThrownBy(() -> storage.upload("empresas/x/documentos/animals/foto.jpg",
                new byte[]{1, 2, 3}, "image/jpeg"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_UNAVAILABLE));
        server.verify();
    }

    @Test
    void translatesMissingSignedUrlToStorageUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var storage = client(builder, "clave-secreta");
        server.expect(requestTo(URL + "/storage/v1/object/sign/" + BUCKET + "/" + ENC_PATH))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> storage.signedUrl("empresas/x/documentos/animals/foto.jpg"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_UNAVAILABLE));
        server.verify();
    }

    private HttpSupabaseStorageClient client(String key) {
        return client(RestClient.builder(), key);
    }

    private HttpSupabaseStorageClient client(RestClient.Builder builder, String key) {
        return new HttpSupabaseStorageClient(URL, key, builder, properties());
    }

    private AppProperties properties() {
        return new AppProperties(new AppProperties.Bootstrap(false, ""),
                new AppProperties.SystemStatus(false), "http://localhost",
                new AppProperties.Storage(BUCKET, 5_242_880, Duration.ofMinutes(10),
                        List.of("image/jpeg", "image/png", "image/webp"), List.of("jpg", "jpeg", "png", "webp")));
    }
}
