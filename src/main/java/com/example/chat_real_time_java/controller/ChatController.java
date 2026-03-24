package com.example.chat_real_time_java.controller;

import com.example.chat_real_time_java.model.ChatMessage;
import com.example.chat_real_time_java.model.ChatRoom;
import com.example.chat_real_time_java.model.Message;
import com.example.chat_real_time_java.model.User;
import com.example.chat_real_time_java.repository.ChatRoomRepository;
import com.example.chat_real_time_java.repository.MessageRepository;
import com.example.chat_real_time_java.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ChatController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Public chat room message
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String username = principal.getName();
        chatMessage.setSender(username);
        chatMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
        chatMessage.setStatus(ChatMessage.MessageStatus.DELIVERED);

        userRepository.findByUsername(username).ifPresent(user -> {
            chatMessage.setAvatarColor(user.getAvatarColor());
        });

        // Save to DB (public message, no chatRoom)
        try {
            User sender = userRepository.findByUsername(username).orElse(null);
            if (sender != null) {
                Message msg = new Message();
                msg.setSender(sender);
                msg.setContent(chatMessage.getContent());
                msg.setChatRoom(null);
                messageRepository.save(msg);
            }
        } catch (Exception e) {
            System.err.println("Failed to save message: " + e.getMessage());
        }

        return chatMessage;
    }

    /**
     * Private message to specific user
     */
    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String senderUsername = principal.getName();
        String targetUsername = chatMessage.getRecipient();

        chatMessage.setSender(senderUsername);
        chatMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
        chatMessage.setStatus(ChatMessage.MessageStatus.DELIVERED);

        User sender = userRepository.findByUsername(senderUsername).orElse(null);
        User target = userRepository.findByUsername(targetUsername).orElse(null);

        if (sender != null) {
            chatMessage.setAvatarColor(sender.getAvatarColor());
        }

        if (sender != null && target != null) {
            // Find or create chat room
            ChatRoom room = chatRoomRepository.findByUsers(sender, target)
                    .orElseGet(() -> {
                        ChatRoom newRoom = new ChatRoom();
                        newRoom.setUser1(sender);
                        newRoom.setUser2(target);
                        return chatRoomRepository.save(newRoom);
                    });

            chatMessage.setRoomId(room.getId());

            // Save to DB
            try {
                Message msg = new Message();
                msg.setSender(sender);
                msg.setContent(chatMessage.getContent());
                msg.setChatRoom(room);
                messageRepository.save(msg);
            } catch (Exception e) {
                System.err.println("Failed to save private message: " + e.getMessage());
            }

            // Send to both sender and recipient
            messagingTemplate.convertAndSendToUser(targetUsername, "/queue/private", chatMessage);
            messagingTemplate.convertAndSendToUser(senderUsername, "/queue/private", chatMessage);
        }
    }

    /**
     * User join notification
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        String username = principal.getName();
        chatMessage.setSender(username);
        chatMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
        headerAccessor.getSessionAttributes().put("username", username);

        userRepository.findByUsername(username).ifPresent(user -> {
            chatMessage.setAvatarColor(user.getAvatarColor());
        });

        return chatMessage;
    }
}
