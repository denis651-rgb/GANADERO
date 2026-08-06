package bo.com.ganadero.archivos.application;
public interface SupabaseStorageClient{void upload(String path,byte[] content,String contentType);String signedUrl(String path);void delete(String path);}
