package bo.com.ganadero.animales.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QrImageGeneratorTest {
    private final QrImageGenerator generator = new QrImageGenerator();

    @Test
    void generatesDecodablePng() throws Exception {
        String content = "GANADERO_ANIMAL|payload-de-prueba-12345";
        byte[] png = generator.png(content, 512, false);
        assertThat(png).isNotEmpty();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(512);
        assertThat(image.getHeight()).isEqualTo(512);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.POSSIBLE_FORMATS,
                java.util.List.of(com.google.zxing.BarcodeFormat.QR_CODE)));
        assertThat(result.getText()).isEqualTo(content);
    }

    @Test
    void generatesValidSvgWithQuietZone() {
        String svg = generator.svg("contenido-svg-abc", 512, false);
        assertThat(svg).startsWith("<svg xmlns=");
        assertThat(svg).contains("viewBox=\"0 0 512 512\"");
        assertThat(svg).contains("<rect");
        assertThat(svg).endsWith("</svg>\n");
    }

    @Test
    void marksRetiredQrWithOverlay() {
        String svg = generator.svg("contenido-retirado", 512, true);
        assertThat(svg).contains("QR RETIRADO");
        assertThat(svg).contains("#c81e1e");
        byte[] png = generator.png("contenido-retirado", 512, true);
        assertThat(png).isNotEmpty();
    }

    @Test
    void pngRetiredOverlayStillDecodesContent() throws Exception {
        String content = "GANADERO_ANIMAL|retirado-roundtrip";
        byte[] png = generator.png(content, 1024, true);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.POSSIBLE_FORMATS,
                java.util.List.of(com.google.zxing.BarcodeFormat.QR_CODE)));
        assertThat(result.getText()).isEqualTo(content);
    }
}
