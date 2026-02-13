package com.mogakjak.mogakjak.global.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AgreementGateFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/api/users/agreements")
                || path.startsWith("/api/groups/meta/")
                || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails cud)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!cud.getUser().isAgreementCompleted()) {
            handleException(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleException(HttpServletResponse response) throws IOException {
        ApiResponse<?> apiResponse = ApiResponse.error(ErrorCode.REQUIRED_AGREEMENT_MISSING);

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.getWriter().flush();
    }
}
