package com.k1den.server_cart.repository;

import com.k1den.server_cart.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Integer> {

    // Получить все чаты конкретного пользователя (нативный SQL)
    @Query(value = "SELECT c.* FROM chats c JOIN chat_members cm ON c.id = cm.chat_id WHERE cm.user_id = ?1", nativeQuery = true)
    List<Chat> findChatsByUserId(Integer userId);

    // Добавить пользователя в чат (нативный SQL)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO chat_members (chat_id, user_id, role) VALUES (?1, ?2, ?3)", nativeQuery = true)
    void addMemberToChat(Integer chatId, Integer userId, String role);
}