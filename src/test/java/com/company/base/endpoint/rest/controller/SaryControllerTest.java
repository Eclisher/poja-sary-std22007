package com.company.base.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.company.base.endpoint.rest.controller.health.SaryController;
import com.company.base.file.BucketComponent;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

public class SaryControllerTest {
  private SaryController saryController;
  private BucketComponent bucketComponent;

  @BeforeEach
  void setUp() {
    bucketComponent = mock(BucketComponent.class);
    saryController = new SaryController(bucketComponent);
  }

  @Test
  void testGetImageUrls() throws IOException {
    // Given
    String id = "testId";
    String originalImageUrl = "http://example.com/original.jpg";
    String grayscaleImageUrl = "http://example.com/grayscale.jpg";
    when(bucketComponent.presign("blacks/" + id, Duration.ofMinutes(5)))
        .thenReturn(new URL(originalImageUrl));
    when(bucketComponent.presign("grayscale/" + id + "_bw.jpg", Duration.ofMinutes(5)))
        .thenReturn(new URL(grayscaleImageUrl));

    // When
    ResponseEntity<SaryController.ImageUrls> response = saryController.getImageUrls(id);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    SaryController.ImageUrls imageUrls = response.getBody();
    assertNotNull(imageUrls);
    assertEquals(originalImageUrl, imageUrls.getOriginalImageUrl());
    assertEquals(grayscaleImageUrl, imageUrls.getGrayscaleImageUrl());
  }

  @Test
  void testUploadImageToBucket_ImageDownloadFailure() throws IOException {
    // Given
    String id = "testId";
    MockMultipartFile image =
        new MockMultipartFile(
            "image", "testImage.jpg", "image/jpeg", "test image content".getBytes());
    String expectedErrorMessage = "Error uploading image to bucket.";
    when(bucketComponent.upload(any(), any()))
        .thenThrow(new RuntimeException(expectedErrorMessage));

    // When
    ResponseEntity<?> response = saryController.uploadAndConvertToBlackAndWhite(id, image);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedErrorMessage, response.getBody());
  }
}
