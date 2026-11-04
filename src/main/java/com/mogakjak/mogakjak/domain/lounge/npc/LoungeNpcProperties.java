package com.mogakjak.mogakjak.domain.lounge.npc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lounge.npc")
public class LoungeNpcProperties {
    private boolean enabled = false;
}
