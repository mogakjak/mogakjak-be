package com.mogakjak.mogakjak.global.auth.security.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OAuth2AttributeKeys {

    // 공통 속성
    public static final String ID = "id";
    public static final String EMAIL = "email";
    public static final String NAME = "name";
    public static final String PICTURE = "picture";
    public static final String PROVIDER = "provider";

    // Google 속성
    public static final String SUB = "sub";

    // Kakao 속성
    public static final String KAKAO_ACCOUNT = "kakao_account";
    public static final String PROPERTIES = "properties";
    public static final String NICKNAME = "nickname";
    public static final String PROFILE_IMAGE_URL = "profile_image_url";

    // Naver 속성
    public static final String RESPONSE = "response";
    public static final String PROFILE_IMAGE = "profile_image";

    // 제공자 타입
    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_KAKAO = "kakao";
    public static final String PROVIDER_NAVER = "naver";

}
