package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.MemberListResDto;
import com.mogakjak.mogakjak.domain.user.controller.dto.UserSearchResponse;
import com.mogakjak.mogakjak.global.enumerate.ProviderType;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserProvider;
import com.mogakjak.mogakjak.domain.user.repository.UserProviderRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

    @Transactional
    public List<UserSearchResponse> searchUsers(String nickname) {
        if (nickname.isBlank()) {
            return List.of();
        }

        return userRepository.findByNameContaining(nickname).stream()
                .map(UserSearchResponse::from)
                .collect(Collectors.toList());
    }

    public List<MemberListResDto> findAll() {
        List<User> members = userRepository.findAll();
        List<MemberListResDto> memberListResDtos = new ArrayList<>();
        for (User m : members){
            MemberListResDto memberListResDto = new MemberListResDto();
            memberListResDto.setId(m.getId());
            memberListResDto.setEmail(m.getEmail());
            memberListResDto.setName(m.getName());
            memberListResDtos.add(memberListResDto);
        }
        return memberListResDtos;
    }
}
