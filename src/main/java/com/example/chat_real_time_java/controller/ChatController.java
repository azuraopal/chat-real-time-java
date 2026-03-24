package com.example.chat_real_time_java.controller;

import com.example.chat_real_time_java.model.ChatMessage;
import com.example.chat_real_time_java.model.ChatRoom;
import com.example.chat_real_time_java.model.Message;
import com.example.chat_real_time_java.model.User;
import com.example.chat_real_time_java.repository.ChatRoomRepository;
import com.example.chat_real_time_java.repository.MessageRepository;
import com.example.chat_real_time_java.repository.UserRepository;
import com.example.chat_real_time_java.security.OnlineUserTracker;
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
import java.util.List;

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

    @Autowired
    private OnlineUserTracker onlineUserTracker;

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
            chatMessage.setProfilePhoto(user.getProfilePhoto());
        });

        // Save to DB (public message)
        try {
            User sender = userRepository.findByUsername(username).orElse(null);
            if (sender != null) {
                Message msg = new Message();
                msg.setSender(sender);
                msg.setContent(chatMessage.getContent());
                msg.setChatRoom(null);
                msg.setStatus("DELIVERED");
                messageRepository.save(msg);
            }
        } catch (Exception e) {
            System.err.println("Failed to save message: " + e.getMessage());
        }

        return chatMessage;
    }

    /**
     * Private message to specific user.
     * Status logic:
     *   - SENT (✓ gray): recipient is OFFLINE
     *   - DELIVERED (✓✓ gray): recipient is ONLINE but hasn't read
     *   - READ (✓✓ blue): recipient has opened the conversation
     */
    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String senderUsername = principal.getName();
        String targetUsername = chatMessage.getRecipient();

        chatMessage.setSender(senderUsername);
        chatMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));

        // Determine status based on recipient online status
        boolean recipientOnline = onlineUserTracker.isOnline(targetUsername);
        chatMessage.setStatus(recipientOnline ? ChatMessage.MessageStatus.DELIVERED : ChatMessage.MessageStatus.SENT);

        User sender = userRepository.findByUsername(senderUsername).orElse(null);
        User target = userRepository.findByUsername(targetUsername).orElse(null);

        if (sender != null) {
            chatMessage.setAvatarColor(sender.getAvatarColor());
            chatMessage.setProfilePhoto(sender.getProfilePhoto());
        }

        if (sender != null && target != null) {
            ChatRoom room = chatRoomRepository.findByUsers(sender, target)
                    .orElseGet(() -> {
                        ChatRoom newRoom = new ChatRoom();
                        newRoom.setUser1(sender);
                        newRoom.setUser2(target);
                        return chatRoomRepository.save(newRoom);
                    });

            chatMessage.setRoomId(room.getId());

            // Save to DB with correct status
            String dbStatus = recipientOnline ? "DELIVERED" : "SENT";
            try {
                Message msg = new Message();
                msg.setSender(sender);
                msg.setContent(chatMessage.getContent());
                msg.setChatRoom(room);
                msg.setStatus(dbStatus);
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
     * Mark messages as READ when user opens a conversation
     */
    @MessageMapping("/chat.markRead")
    public void markRead(@Payload ChatMessage chatMessage, Principal principal) {
        String readerUsername = principal.getName();
        Long roomId = chatMessage.getRoomId();

        if (roomId == null) return;

        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) return;

        // Mark all unread messages in this room (sent by the OTHER user) as READ
        List<Message> unread = messageRepository.findByChatRoomAndStatusNot(room, "READ");
        for (Message msg : unread) {
            if (!msg.getSender().getUsername().equals(readerUsername)) {
                msg.setStatus("READ");
                messageRepository.save(msg);
            }
        }

        // Notify the sender that their messages were read
        String otherUser = room.getUser1().getUsername().equals(readerUsername)
                ? room.getUser2().getUsername()
                : room.getUser1().getUsername();

        ChatMessage readNotif = new ChatMessage();
        readNotif.setType(ChatMessage.MessageType.CHAT);
        readNotif.setRoomId(roomId);
        readNotif.setStatus(ChatMessage.MessageStatus.READ);
        readNotif.setSender("_system_read_");
        readNotif.setRecipient(otherUser);

        messagingTemplate.convertAndSendToUser(otherUser, "/queue/private", readNotif);
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

        // Track online user
        onlineUserTracker.addUser(username);

        userRepository.findByUsername(username).ifPresent(user -> {
            chatMessage.setAvatarColor(user.getAvatarColor());
            chatMessage.setProfilePhoto(user.getProfilePhoto());
        });

        return chatMessage;
    }
}
