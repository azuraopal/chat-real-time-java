package com.example.chat_real_time_java.security;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users are currently connected via WebSocket.
 * Used for determining message delivery status (SENT vs DELIVERED).
 */
@Component
public class OnlineUserTracker {

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public void addUser(String username) {
        onlineUsers.add(username);
    }

    public void removeUser(String username) {
        onlineUsers.remove(username);
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    public Set<String> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers);
    }

    public int getOnlineCount() {
        return onlineUsers.size();
    }
}
