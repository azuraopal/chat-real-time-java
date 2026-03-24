package com.example.chat_real_time_java.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String displayName;

    @Column(length = 7)
    private String avatarColor;

    @Column(length = 200)
    private String bio;

    @Column(columnDefinition = "LONGTEXT")
    private String profilePhoto;
}
