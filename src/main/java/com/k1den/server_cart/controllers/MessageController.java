package com.k1den.server_cart.controllers;

import com.k1den.server_cart.models.Message;
import com.k1den.server_cart.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    // Сохранить новое сообщение
    @PostMapping("/send")
    public ResponseEntity<Message> sendMessage(
            @RequestParam Integer chatId,
            @RequestParam Integer userId,
            @RequestParam String content) {

        Message msg = new Message();
        msg.setChatId(chatId);
        msg.setUserId(userId);
        msg.setContent(content);

        return ResponseEntity.ok(messageRepository.save(msg));
    }

    // Получить историю сообщений чата
    @GetMapping("/{chatId}")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Integer chatId) {
        return ResponseEntity.ok(messageRepository.findByChatIdOrderByCreatedAtAsc(chatId));
    }
}