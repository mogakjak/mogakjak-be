package com.mogakjak.mogakjak.domain.quote.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote extends BaseSchema {

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    public void update(String content, String author) {
        this.content = content;
        this.author = author;
    }
}

