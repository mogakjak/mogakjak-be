package com.mogakjak.mogakjak.global.image;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final AmazonS3 amazonS3;

    @Value("${application.bucket.name}")
    private String bucket;

    @Value("${cloud.aws.s3.public-endpoint}")
    private String publicEndpoint;

    @Value("${cloud.aws.s3.namespace}")
    private String namespace;

    /**
     * Presigned URL 발급
     * @param prefix 파일 경로 (예: profile/, group/)
     * @param fileName 원본 파일명
     * @return presignedUrl, fileKey(실제 저장될 경로)
     */
    public PresignedUrlResponse getPresignedUrl(String prefix, String fileName) {
        // 1. 파일 경로 생성
        String fileKey = prefix + "/" + UUID.randomUUID() + "_" + fileName;

        // 2. Presigned URL 생성 (업로드용)
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 2; // 2분 유효
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileKey)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(expiration);

        // 중요: 누구나 조회할 수 있도록 PublicRead 권한 부여
        generatePresignedUrlRequest.addRequestParameter(Headers.S3_CANNED_ACL,
                CannedAccessControlList.PublicRead.toString());

        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

        // 3. 조회용 Public URL 생성 (DB 저장용)
        // 형식: https://kr.object.ncloudstorage.com/버킷명/파일키
        String imageUrl = publicEndpoint + "/n/" + namespace + "/b/" + bucket + "/o/" + fileKey;

        return new PresignedUrlResponse(url.toString(), imageUrl);
    }
}