package bo.com.ganadero.animales.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Component
public class QrImageGenerator {
    private static final Map<EncodeHintType, Object> HINTS = Map.of(
            EncodeHintType.MARGIN, 2,
            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M
    );

    public byte[] png(String content, int size, boolean retired) {
        BufferedImage image = render(content, size);
        if (retired) drawRetiredOverlay(image);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar la imagen PNG del QR", exception);
        }
    }

    public String svg(String content, int size, boolean retired) {
        BitMatrix matrix = encode(content, size);
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(size)
                .append("\" height=\"").append(size)
                .append("\" viewBox=\"0 0 ").append(size).append(' ').append(size)
                .append("\" shape-rendering=\"crispEdges\">\n");
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        int width = matrix.getWidth();
        for (int y = 0; y < matrix.getHeight(); y++) {
            int x = 0;
            while (x < width) {
                if (matrix.get(x, y)) {
                    int start = x;
                    while (x < width && matrix.get(x, y)) x++;
                    sb.append("<rect x=\"").append(start).append("\" y=\"").append(y)
                            .append("\" width=\"").append(x - start).append("\" height=\"1\" fill=\"#000000\"/>\n");
                } else {
                    x++;
                }
            }
        }
        if (retired) {
            sb.append(retiredOverlaySvg(size));
        }
        sb.append("</svg>\n");
        return sb.toString();
    }

    private BufferedImage render(String content, int size) {
        BitMatrix matrix = encode(content, size);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        g.dispose();
        return image;
    }

    private BitMatrix encode(String content, int size) {
        try {
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, HINTS);
        } catch (WriterException exception) {
            throw new IllegalArgumentException("El contenido del QR no cabe en el tamaño solicitado", exception);
        }
    }

    private void drawRetiredOverlay(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.rotate(Math.toRadians(-30), w / 2.0, h / 2.0);
        g.setColor(new Color(200, 30, 30, 110));
        g.fillRect(0, h / 2 - h / 40, w, h / 20);
        g.setColor(new Color(200, 30, 30));
        g.setFont(new Font("Arial", Font.BOLD, Math.max(10, h / 12)));
        FontMetrics fm = g.getFontMetrics();
        String text = "QR RETIRADO";
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = h / 2 + (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
        g.dispose();
    }

    private String retiredOverlaySvg(int size) {
        int bandHeight = Math.max(2, size / 20);
        int fontSize = Math.max(10, size / 12);
        return "<g transform=\"rotate(-30 " + size / 2.0 + " " + size / 2.0 + ")\">"
                + "<rect x=\"0\" y=\"" + (size / 2.0 - bandHeight / 2.0) + "\" width=\"" + size
                + "\" height=\"" + bandHeight + "\" fill=\"#c81e1e\" opacity=\"0.45\"/>"
                + "<text x=\"" + size / 2.0 + "\" y=\"" + size / 2.0
                + "\" text-anchor=\"middle\" dominant-baseline=\"middle\" font-family=\"Arial, sans-serif\""
                + " font-size=\"" + fontSize + "\" font-weight=\"bold\" fill=\"#c81e1e\">QR RETIRADO</text></g>\n";
    }
}
