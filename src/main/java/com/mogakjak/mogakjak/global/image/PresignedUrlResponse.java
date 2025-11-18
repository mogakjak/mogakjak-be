package com.mogakjak.mogakjak.global.image;

public record PresignedUrlResponse(
        String presignedUrl,
        String imageUrl
) {}