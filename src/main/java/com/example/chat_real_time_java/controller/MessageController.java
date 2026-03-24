package com.example.chat_real_time_java.controller;

import com.example.chat_real_time_java.model.Message;
import com.example.chat_real_time_java.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping
    public ResponseEntity<?> getPublicMessages() {
        List<Message> messages = messageRepository.findTop50ByChatRoomIsNullOrderByTimestampAsc();

        List<Map<String, Object>> result = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sender", msg.getSender().getUsername());
            map.put("content", msg.getContent());
            map.put("timestamp", msg.getTimestamp().format(TIME_FORMATTER));
            map.put("avatarColor", msg.getSender().getAvatarColor() != null ? msg.getSender().getAvatarColor() : "#00A884");
            map.put("status", msg.getStatus() != null ? msg.getStatus() : "DELIVERED");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
