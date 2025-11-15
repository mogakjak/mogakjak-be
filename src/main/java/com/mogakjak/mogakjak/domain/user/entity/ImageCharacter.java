package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageCharacter extends BaseSchema {

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isActive;

    /**
     * 캐릭터 해금 조건 (총 누적 집중 시간 - 초 단위)
     * (e.g., 10시간 = 36000)
     */
    @Column(nullable = false)
    private Integer unlockTimeInSeconds = 0;

    public void update(Integer level, String name, String imageUrl, Boolean isActive, Integer unlockTimeInSeconds) {
        this.level = level;
        this.name = name;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.unlockTimeInSeconds = unlockTimeInSeconds;
    }
}