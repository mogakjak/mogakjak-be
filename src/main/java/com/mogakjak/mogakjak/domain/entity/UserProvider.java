package com.mogakjak.mogakjak.domain.entity;

import com.mogakjak.mogakjak.domain.enumerate.ProviderType;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"type", "providerId"}),
        @UniqueConstraint(columnNames = {"user_id", "type"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProvider extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    ProviderType type;

    @Column(nullable = false)
    String providerId;
}