package com.teedjay;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.w3c.dom.Document;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.time.LocalDateTime;

public class CreateImageTest {

    int width = 800;
    int height = 480;

    String html = """
            <html>
                <head>
                <style>
                body {
                    width: 780px;
                    height: 460px;
                    margin: 10px;
                    padding: 0px;
                }
                h1 {
                    background-color: yellow;
                }
                div {
                  background-color: lightblue;
                }
                p {
                  background-color: red;
                }
                </style>
                </head>
                <body>
                    <h1>Hello, BMP3!</h1>
                    <div>Togtider</div>
                    <p>something is cooking</p>
                    <p>something is cooking</p>
                    <p>something is cooking</p>
                    <p>something is cooking</p>
                    <div>Trikken</div>
                    <p>something is cooking</p>
                    <p>something is cooking</p>
                    <div>Bussen</div>
                    <p>something is cooking</p>
                    <p>something is cooking</p>
                    <div>%s</div>
                </body>
            </html>
            """.formatted(LocalDateTime.now());


    @Test
    void testCreateImageUsingFlyingSaucer() throws Exception {
        InputSource is = new InputSource(new StringReader(html));
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();;
        Document xmlDoc = builder.parse(is);
        BufferedImage image = new Java2DRenderer(xmlDoc, width, height).getImage();
        File output = new File("output-flyingsaucer.png");
        ImageIO.write(image, "png", output);
    }

    @Test
    void testCreateImageUsingSelenium() throws Exception {

        // use the chrome driver, it downloads automatically and can be forced to silent / headless
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);

        // push HTML content in the Browser
        driver.get("data:text/html," + html);

        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

        FileUtils.copyFile(screenshot, new File("output-chromium.png"));

        makeGrayscale(screenshot);

        makeDithered(screenshot);

        makeMonochrome(screenshot);

        makeBMP(screenshot);

        driver.quit();
    }

    void makeBMP(File file)throws Exception {

        BufferedImage inputImage = ImageIO.read(file);

        byte[] bytes = new byte[width/8 * height];

        int current = 0;
        int pointer = 0;

        // Convert to bytes store the bw image 8 pixels pr byte
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = inputImage.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Compute grayscale value using luminance formula
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);

                // make room for next LSB
                current = current << 1;

                // Apply threshold for monochrome
                if (gray > 128) current = current + 1;
                if ((x % 8) == 7) bytes[pointer++] = (byte) (current & 0xFF);

            }
        }

        writeBMP3_1bpp("output-bm3.bmp", bytes);

    }


    void makeGrayscale(File file) throws Exception {
        BufferedImage inputImage = ImageIO.read(file);
//        int width = inputImage.getWidth();
//        int height = inputImage.getHeight();
        BufferedImage monochromeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Convert each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = inputImage.getRGB(x, y);

                // Extract color components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Compute grayscale value using luminance formula
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);

                // Apply threshold for monochrome
                //int bw = (gray > 128) ? 0xFFFFFF : 0x000000;

                // Set new pixel value GGG
                monochromeImage.setRGB(x, y, (gray << 16) + (gray << 8) + gray);
            }
        }


        // Save the output
        ImageIO.write(monochromeImage, "png", new File("output-grayscale.png"));
    }

    void makeMonochrome(File file) throws Exception {
        BufferedImage inputImage = ImageIO.read(file);
//        int width = inputImage.getWidth();
//        int height = inputImage.getHeight();
        BufferedImage monochromeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Convert each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = inputImage.getRGB(x, y);

                // Extract color components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Compute grayscale value using luminance formula
                int gray = (int)(0.299 * r + 0.587 * g + 0.114 * b);

                // Apply threshold for monochrome
                int bw = (gray > 128) ? 0xFFFFFF : 0x000000;

                // Set new pixel value GGG
                monochromeImage.setRGB(x, y, bw);
            }
        }

        // Save the output
        ImageIO.write(monochromeImage, "png", new File("output-monochrome.png"));
    }

    void makeDithered(File file) throws Exception {
        BufferedImage inputImage = ImageIO.read(file);

//        int width = inputImage.getWidth();
//        int height = inputImage.getHeight();

        // Convert to grayscale array
        double[][] gray = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = inputImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                gray[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }

        // monochrome with Floyd–Steinberg dithering
        BufferedImage ditheredImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double oldPixel = gray[y][x];
                int newPixel = oldPixel < 128 ? 0 : 255;
                double error = oldPixel - newPixel;

                ditheredImage.setRGB(x, y, newPixel == 0 ? 0x000000 : 0xFFFFFF);

                // distribute the error to neighbors
                if (x + 1 < width) gray[y][x + 1] += error * 7 / 16.0;
                if (x - 1 >= 0 && y + 1 < height) gray[y + 1][x - 1] += error * 3 / 16.0;
                if (y + 1 < height) gray[y + 1][x] += error * 5 / 16.0;
                if (x + 1 < width && y + 1 < height) gray[y + 1][x + 1] += error * 1 / 16.0;
            }
        }

        // Save the output
        ImageIO.write(ditheredImage, "png", new File("output-dithered.png"));
    }


    void writeBMP3_1bpp(String filename, byte[] imageData) throws Exception {
        final int width = 800;
        final int height = 480;
        final int bpp = 1;

        int rowBytesUnpadded = (width + 7) / 8;
        int rowBytesPadded = ((rowBytesUnpadded + 3) / 4) * 4;
        int pixelDataSize = rowBytesPadded * height;
        int fileSize = 14 + 40 + 8 + pixelDataSize; // Header + DIB + Palette + Pixel Data
        int dataOffset = 14 + 40 + 8;

        try (FileOutputStream fos = new FileOutputStream(filename)) {

            // === BMP Header (14 bytes) ===
            fos.write(new byte[]{
                    'B', 'M',
                    (byte) fileSize, (byte) (fileSize >> 8), (byte) (fileSize >> 16), (byte) (fileSize >> 24),
                    0, 0, 0, 0, // Reserved
                    (byte) dataOffset, 0, 0, 0
            });

            // === DIB Header (BITMAPINFOHEADER, 40 bytes) ===
            fos.write(new byte[]{
                    40, 0, 0, 0, // Header size
                    (byte) width, (byte) (width >> 8), (byte) (width >> 16), (byte) (width >> 24),
                    (byte) height, (byte) (height >> 8), (byte) (height >> 16), (byte) (height >> 24),
                    1, 0, // Planes
                    1, 0, // Bits per pixel (1 bpp)
                    0, 0, 0, 0, // Compression (BI_RGB)
                    (byte) pixelDataSize, (byte) (pixelDataSize >> 8), (byte) (pixelDataSize >> 16), (byte) (pixelDataSize >> 24),
                    0, 0, 0, 0, // X pixels per meter
                    0, 0, 0, 0, // Y pixels per meter
                    2, 0, 0, 0, // Colors in color table
                    0, 0, 0, 0  // Important colors
            });

            // === Color Palette (2 colors: black and white, BGRA order) ===
            // sRGB assumption: black = (0,0,0), white = (255,255,255)
            fos.write(new byte[]{
                    0, 0, 0, 0,             // Black (BGRA)
                    (byte)255, (byte)255, (byte)255, 0 // White (BGRA)
            });

            // === Pixel Data (bottom-up) ===
            for (int y = height - 1; y >= 0; y--) {
                int rowStart = y * rowBytesUnpadded;
                fos.write(imageData, rowStart, rowBytesUnpadded);

                // Padding if needed
                for (int i = 0; i < rowBytesPadded - rowBytesUnpadded; i++) {
                    fos.write(0);
                }
            }
        }
    }

}
