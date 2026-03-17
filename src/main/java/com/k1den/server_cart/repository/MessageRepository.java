package com.k1den.server_cart.repository;

import com.k1den.server_cart.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByChatIdOrderByCreatedAtAsc(Integer chatId);
}