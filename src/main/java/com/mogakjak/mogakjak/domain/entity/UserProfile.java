package com.mogakjak.mogakjak.domain.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseSchema {

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_character_id")
    private ImageCharacter mainImageCharacter;

    public void updateMainCharacter(ImageCharacter imageCharacter) {
        this.mainImageCharacter = imageCharacter;
    }
}