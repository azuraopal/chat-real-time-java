package com.example.chat_real_time_java.repository;

import com.example.chat_real_time_java.model.ChatRoom;
import com.example.chat_real_time_java.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1 = :u1 AND cr.user2 = :u2) OR (cr.user1 = :u2 AND cr.user2 = :u1)")
    Optional<ChatRoom> findByUsers(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.user1 = :user OR cr.user2 = :user ORDER BY cr.createdAt DESC")
    List<ChatRoom> findByUser(@Param("user") User user);
}
