package com.example.chat_real_time_java.controller;

import com.example.chat_real_time_java.model.ChatRoom;
import com.example.chat_real_time_java.model.Message;
import com.example.chat_real_time_java.model.User;
import com.example.chat_real_time_java.repository.ChatRoomRepository;
import com.example.chat_real_time_java.repository.MessageRepository;
import com.example.chat_real_time_java.repository.UserRepository;
import com.example.chat_real_time_java.security.OnlineUserTracker;
import com.example.chat_real_time_java.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/chat")
public class ChatRoomController {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @PostMapping("/room/{targetUsername}")
    public ResponseEntity<?> getOrCreateRoom(@PathVariable String targetUsername, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        User targetUser = userRepository.findByUsername(targetUsername).orElse(null);

        if (currentUser == null || targetUser == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        if (currentUser.getId().equals(targetUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot chat with yourself"));
        }

        ChatRoom room = chatRoomRepository.findByUsers(currentUser, targetUser)
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setUser1(currentUser);
                    newRoom.setUser2(targetUser);
                    return chatRoomRepository.save(newRoom);
                });

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", room.getId());
        result.put("targetUsername", targetUser.getUsername());
        result.put("targetDisplayName", targetUser.getDisplayName() != null ? targetUser.getDisplayName() : targetUser.getUsername());
        result.put("targetAvatarColor", targetUser.getAvatarColor() != null ? targetUser.getAvatarColor() : "#00A884");
        result.put("targetBio", targetUser.getBio() != null ? targetUser.getBio() : "");
        result.put("targetProfilePhoto", targetUser.getProfilePhoto() != null ? targetUser.getProfilePhoto() : "");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<?> getRoomMessages(@PathVariable Long roomId, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);

        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();

        if (!room.getUser1().getId().equals(currentUser.getId()) &&
            !room.getUser2().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
        }

        List<Message> messages = messageRepository.findTop100ByChatRoomOrderByTimestampAsc(room);

        List<Map<String, Object>> result = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sender", msg.getSender().getUsername());
            map.put("content", msg.getContent());
            map.put("timestamp", msg.getTimestamp().format(TIME_FORMATTER));
            map.put("avatarColor", msg.getSender().getAvatarColor() != null ? msg.getSender().getAvatarColor() : "#00A884");
            map.put("status", msg.getStatus() != null ? msg.getStatus() : "DELIVERED");
            map.put("profilePhoto", msg.getSender().getProfilePhoto() != null ? msg.getSender().getProfilePhoto() : "");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);

        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));

        List<ChatRoom> rooms = chatRoomRepository.findByUser(currentUser);

        List<Map<String, Object>> result = rooms.stream().map(room -> {
            User other = room.getUser1().getId().equals(currentUser.getId()) ? room.getUser2() : room.getUser1();
            Message lastMsg = messageRepository.findTopByChatRoomOrderByTimestampDesc(room);

            Map<String, Object> map = new HashMap<>();
            map.put("roomId", room.getId());
            map.put("username", other.getUsername());
            map.put("displayName", other.getDisplayName() != null ? other.getDisplayName() : other.getUsername());
            map.put("avatarColor", other.getAvatarColor() != null ? other.getAvatarColor() : "#00A884");
            map.put("bio", other.getBio() != null ? other.getBio() : "");
            map.put("profilePhoto", other.getProfilePhoto() != null ? other.getProfilePhoto() : "");
            map.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "");
            map.put("lastTime", lastMsg != null ? lastMsg.getTimestamp().format(TIME_FORMATTER) : "");
            map.put("online", onlineUserTracker.isOnline(other.getUsername()));
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<User> users = userRepository.findAll();

        List<Map<String, Object>> result = users.stream()
                .filter(u -> !u.getUsername().equals(userDetails.getUsername()))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("username", u.getUsername());
                    map.put("displayName", u.getDisplayName() != null ? u.getDisplayName() : u.getUsername());
                    map.put("avatarColor", u.getAvatarColor() != null ? u.getAvatarColor() : "#00A884");
                    map.put("bio", u.getBio() != null ? u.getBio() : "");
                    map.put("profilePhoto", u.getProfilePhoto() != null ? u.getProfilePhoto() : "");
                    map.put("online", onlineUserTracker.isOnline(u.getUsername()));
                    return map;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
