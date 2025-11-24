package com.mogakjak.mogakjak.global.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@ToString(exclude = {"accessToken", "refreshToken", "sessionId"})
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenRefreshResponse {

//    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String accessToken;

//    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String refreshToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sessionId;

    private LoginResponse userInfo;
}
