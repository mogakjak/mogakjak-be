package com.mogakjak.mogakjak.domain.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberListResDto {
    private UUID id;
    private String name;
    private String email;
    private String imageUrl;
}
