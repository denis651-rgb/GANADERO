package bo.com.ganadero.archivos.application;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Valida que un archivo subido sea realmente una imagen (Tarea 9.3).
 *
 * <p>No confía en la extensión ni en el MIME declarado: lee la cabecera
 * (magic bytes) y decodifica la imagen con ImageIO (que incluye el lector
 * WebP de TwelveMonkeys). Rechaza archivos corruptos y ejecutables
 * renombrados a extensión de imagen.</p>
 */
@Component
public final class ImagenValidador {

    private static final int MAX_DIMENSION = 100_000;

    public record Resultado(int ancho, int alto, String formato) {}

    public Resultado validar(byte[] content, String mimeDeclarado) {
        String formato = sniff(content, mimeDeclarado);
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) throw invalid();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                int ancho = reader.getWidth(0);
                int alto = reader.getHeight(0);
                if (ancho <= 0 || alto <= 0 || ancho > MAX_DIMENSION || alto > MAX_DIMENSION) throw invalid();
                BufferedImage imagen = reader.read(0);
                if (imagen == null || imagen.getWidth() != ancho || imagen.getHeight() != alto) throw invalid();
                return new Resultado(ancho, alto, formato);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalid();
        }
    }

    private String sniff(byte[] content, String mimeDeclarado) {
        if (content == null || content.length < 12) throw invalid();
        String detected;
        if (content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) {
            detected = "image/jpeg";
        } else if (content[0] == (byte) 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            detected = "image/png";
        } else if (content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            detected = "image/webp";
        } else {
            throw invalid();
        }
        String declared = mimeDeclarado == null ? "" : mimeDeclarado.toLowerCase(Locale.ROOT);
        if (!detected.equals(declared)) throw invalid();
        return detected;
    }

    private BusinessException invalid() {
        return new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
    }
}
