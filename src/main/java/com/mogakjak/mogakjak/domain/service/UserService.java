package com.mogakjak.mogakjak.domain.service;

import com.mogakjak.mogakjak.domain.entity.User;
import com.mogakjak.mogakjak.domain.entity.UserProvider;
import com.mogakjak.mogakjak.domain.enumerate.ProviderType;
import com.mogakjak.mogakjak.domain.repository.UserProviderRepository;
import com.mogakjak.mogakjak.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;

    @Transactional
    public User createUser(String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public UserProvider createUserProvider(User user, ProviderType providerType, String providerId) {
        UserProvider userProvider = UserProvider.builder()
                .user(user)
                .type(providerType)
                .providerId(providerId)
                .build();

        return userProviderRepository.save(userProvider);
    }
}
