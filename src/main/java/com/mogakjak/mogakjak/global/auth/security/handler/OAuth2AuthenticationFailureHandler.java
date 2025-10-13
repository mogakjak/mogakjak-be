package com.mogakjak.mogakjak.global.auth.security.handler;

import com.mogakjak.mogakjak.global.auth.security.util.RedirectValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final RedirectValidator redirectValidator;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws ServletException, IOException {
        log.error("OAuth2 로그인 실패", exception);

        String targetBaseUrl = resolveRedirectOnFailure(request);

        String errorRedirectUrl = String.format(
                "%s/auth/error#code=%s",
                targetBaseUrl,
                "OAUTH2_FAILURE"
        );

        getRedirectStrategy().sendRedirect(request, response, errorRedirectUrl);
    }

    private String resolveRedirectOnFailure(HttpServletRequest request) {
        String redirectUriParam = request.getParameter("redirect_uri");
        if (redirectUriParam != null) {
            String decoded = URLDecoder.decode(redirectUriParam, StandardCharsets.UTF_8);
            if (redirectValidator.isAuthorized(decoded)) {
                return decoded;
            } else {
                log.warn("비허용 redirect_uri 요청 차단됨: {}", decoded);
            }
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("redirect_uri".equals(cookie.getName())) {
                    String decoded = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                    if (redirectValidator.isAuthorized(decoded)) {
                        return decoded;
                    }
                }
            }
        }
        return frontendBaseUrl;
    }
}
