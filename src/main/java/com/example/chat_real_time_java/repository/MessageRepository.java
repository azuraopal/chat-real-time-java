package com.example.chat_real_time_java.repository;

import com.example.chat_real_time_java.model.ChatRoom;
import com.example.chat_real_time_java.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Public messages (no chatRoom)
    List<Message> findTop50ByChatRoomIsNullOrderByTimestampAsc();

    // Private messages for a chat room
    List<Message> findTop100ByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);

    // Last message in a chat room (for sidebar preview)
    Message findTopByChatRoomOrderByTimestampDesc(ChatRoom chatRoom);

    // Find unread messages in a room (for marking as READ)
    List<Message> findByChatRoomAndStatusNot(ChatRoom chatRoom, String status);
}
