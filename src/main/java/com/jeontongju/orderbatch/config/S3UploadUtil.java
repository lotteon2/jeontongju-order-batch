package com.jeontongju.orderbatch.config;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class S3UploadUtil {

  private final AmazonS3Client amazonS3Client;

  @Value("${cloud.aws.s3.bucket}")
  public String bucket;  // S3 버킷

  // S3 파일 업로드
  public String uploadFile(MultipartFile multipartFile, String fileName) {
    String key = "settlement" + "/" + fileName;

    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setContentType(multipartFile.getContentType());

    try (InputStream inputStream = multipartFile.getInputStream()) {
      amazonS3Client.putObject(new PutObjectRequest(bucket, key, inputStream, objectMetadata));
    } catch (IOException e) {
      throw new RuntimeException("S3에 파일 업로드 중 오류 발생", e);
    }

    return amazonS3Client.getUrl(bucket, key).toString();
  }

}