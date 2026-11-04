package com.mogakjak.mogakjak.domain.lounge.controller;

import com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.lounge.npc.LoungeNpcType;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OfficialLoungeNpcControllerTest {
    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedResponseIncludesNpcAndNpcCannotBeCheerTarget() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);
        CustomUserDetails principal = CustomUserDetails.of(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, "", principal.getAuthorities()));
        OfficialLoungeService service = mock(OfficialLoungeService.class);
        when(service.getSummary(userId)).thenReturn(OfficialLoungeSummaryResponse.builder()
                .currentMemberCount(0L).displayMemberCount(1L).members(List.of())
                .npcMembers(List.of(new LoungeNpcMemberResponse("A", LoungeNpcType.A, "모각이", null, 1,
                        GroupParticipationStatus.PARTICIPATING, LocalDateTime.of(2026, 11, 4, 18, 0), 30L,
                        "코딩테스트 준비"))).build());
        var mvc = MockMvcBuilders.standaloneSetup(new OfficialLoungeController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        mvc.perform(get("/api/lounge").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMemberCount").value(0))
                .andExpect(jsonPath("$.data.displayMemberCount").value(1))
                .andExpect(jsonPath("$.data.members.length()").value(0))
                .andExpect(jsonPath("$.data.npcMembers[0].npcId").value("A"))
                .andExpect(jsonPath("$.data.npcMembers[0].npcType").value("A"))
                .andExpect(jsonPath("$.data.npcMembers[0].enteredAt").value("2026-11-04T18:00:00"))
                .andExpect(jsonPath("$.data.npcMembers[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data.npcMembers[0].personalTimerSeconds").value(30));
        mvc.perform(post("/api/lounge/cheer").contentType("application/json").content("{\"targetUserId\":\"A\"}"))
                .andExpect(status().isBadRequest());
        verify(service).getSummary(userId);
        verify(service, never()).sendCheer(any(), any());
    }
}
