package com.mogakjak.mogakjak.global.auth.security.resolver;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.auth.exception.AuthExceptionCode;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || !(authentication.getPrincipal() instanceof CustomUserDetails cud)
        ) {
            return null;
        }

        return userRepository.findById(cud.getUser().getId())
                .orElseThrow(() -> new CustomException(AuthExceptionCode.USER_NOT_FOUND));
    }
}
