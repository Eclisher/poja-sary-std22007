package com.company.base.endpoint.rest.controller.health;

import com.company.base.file.BucketComponent;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import  java.time.Duration;
@RestController
@RequestMapping("/blacks")
@AllArgsConstructor
public class SaryController {
    private final BucketComponent bucketComponent;

    private static final String BLACKS_KEY = "blacks/";
    private static final String GRAYSCALE_KEY = "grayscale/";

    @PutMapping("/{id}")
    public ResponseEntity<?> uploadAndConvertToBlackAndWhite(@PathVariable String id, @RequestParam("image") MultipartFile image) {
        try {
            String originalImageUrl = uploadImageToBucket(image, id);
            String grayscaleImageUrl = convertToBlackAndWhite(originalImageUrl);
            return ResponseEntity.ok().body(new ImageUrls(originalImageUrl, grayscaleImageUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image.");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<ImageUrls> getImageUrls(@PathVariable String id) {
        URL originalImageUrl = bucketComponent.presign(BLACKS_KEY + id, Duration.ofMinutes(5));
        URL grayscaleImageUrl = bucketComponent.presign(GRAYSCALE_KEY + id + "_bw.jpg", Duration.ofMinutes(5));
        ImageUrls imageUrls = new ImageUrls(originalImageUrl.toString(), grayscaleImageUrl.toString());
        return ResponseEntity.ok().body(imageUrls);
    }

    private String uploadImageToBucket(MultipartFile image, String id) throws IOException {
        String fileSuffix = getFileExtension(image.getOriginalFilename());
        String filePrefix = id + "/";
        String fileBucketKey = BLACKS_KEY + filePrefix + fileSuffix;
        File tempFile = File.createTempFile("tempImage", fileSuffix);
        image.transferTo(tempFile);
        return bucketComponent.upload(tempFile, fileBucketKey).toString();
    }

    private String convertToBlackAndWhite(String originalImageUrl) {
        try {
            BufferedImage originalImage = ImageIO.read(new URL(originalImageUrl));
            BufferedImage bwImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D graphics = bwImage.createGraphics();
            graphics.drawImage(originalImage, 0, 0, null);
            graphics.dispose();
            File outputFile = File.createTempFile("tempImage", ".jpg");
            ImageIO.write(bwImage, "jpg", outputFile);
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
          throw new RuntimeException();
        }
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return filename.substring(lastIndex);
    }

    public static class ImageUrls {
        private final String originalImageUrl;
        private final String grayscaleImageUrl;

        public ImageUrls(String originalImageUrl, String grayscaleImageUrl) {
            this.originalImageUrl = originalImageUrl;
            this.grayscaleImageUrl = grayscaleImageUrl;
        }

        public String getOriginalImageUrl() {
            return originalImageUrl;
        }

        public String getGrayscaleImageUrl() {
            return grayscaleImageUrl;
        }
    }
}

