package com.mogakjak.mogakjak.global.websocket.controller;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.websocket.dto.ChatMessageDto;
import com.mogakjak.mogakjak.global.websocket.dto.ChatRoomListResDto;
import com.mogakjak.mogakjak.global.websocket.dto.MyChatListResDto;
import com.mogakjak.mogakjak.global.websocket.service.ChatService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

//    그룹채팅방 개설
    @PostMapping("/room/group/create")
    public ResponseEntity<?> createGroupRoom(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestParam String roomName
    ){
        chatService.createGroupRoom(user.getId(), roomName);
        return ResponseEntity.ok().build();
    }

//    그룹채팅목록조회
    @GetMapping("/room/group/list")
    public ResponseEntity<?> getGroupChatRooms(){
        List<ChatRoomListResDto> chatRooms = chatService.getGroupchatRooms();
        return new ResponseEntity<>(chatRooms, HttpStatus.OK);
    }

//    그룹채팅방참여
    @PostMapping("/room/group/{roomId}/join")
    public ResponseEntity<?> joinGroupChatRoom(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID roomId
    ){
        chatService.addParticipantToGroupChat(user.getId(), roomId);
        return ResponseEntity.ok().build();
    }

//    이전 메시지 조회
    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getChatHistory(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID roomId
    ){
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(user.getId(), roomId);
        return new ResponseEntity<>(chatMessageDtos, HttpStatus.OK);
    }

//    채팅메시지 읽음처리
    @PostMapping("/room/{roomId}/read")
    public ResponseEntity<?> messageRead(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID roomId
    ){
        chatService.messageRead(user.getId(), roomId);
        return ResponseEntity.ok().build();
    }

//    내채팅방목록조회 : roomId, roomName, 그룹채팅여부, 메시지읽음개수
    @GetMapping("/my/rooms")
    public ResponseEntity<?> getMyChatRooms(
            @Parameter(hidden = true) @CurrentUser User user
    ){
        List<MyChatListResDto> myChatListResDtos = chatService.getMyChatRooms(user.getId());
        return new ResponseEntity<>(myChatListResDtos, HttpStatus.OK);
    }

//    채팅방 나가기
    @DeleteMapping("/room/group/{roomId}/leave")
    public ResponseEntity<?> leaveGroupChatRoom(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID roomId
    ){
        chatService.leaveGroupChatRoom(user.getId(), roomId);
        return ResponseEntity.ok().build();
    }

//    개인 채팅방 개설 또는 기존roomId return
    @PostMapping("/room/private/create")
    public ResponseEntity<?> getOrCreatePrivateRoom(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestParam UUID otherMemberId
    ){
        UUID roomId = chatService.getOrCreatePrivateRoom(user.getId(), otherMemberId);
        return new ResponseEntity<>(roomId, HttpStatus.OK);
    }
}
