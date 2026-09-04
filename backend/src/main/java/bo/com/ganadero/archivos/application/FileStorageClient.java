package bo.com.ganadero.archivos.application;

public interface FileStorageClient {
    void upload(String path, byte[] content, String contentType);

    String url(String path);

    void delete(String path);
}
