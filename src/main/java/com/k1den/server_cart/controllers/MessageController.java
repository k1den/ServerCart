package com.k1den.server_cart.controllers;

import com.k1den.server_cart.models.Message;
import com.k1den.server_cart.repository.MessageRepository;
import com.k1den.server_cart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

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

        Message savedMsg = messageRepository.save(msg);

        userRepository.findById(userId).ifPresent(user -> {
            savedMsg.setSenderName(user.getUsername());
            savedMsg.setSenderColor(user.getAvatarColor());
        });

        return ResponseEntity.ok(savedMsg);
    }

    // Получить историю сообщений чата
    @GetMapping("/{chatId}")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Integer chatId) {
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        for (Message m : messages) {
            userRepository.findById(m.getUserId()).ifPresent(user -> {
                m.setSenderName(user.getUsername());
                m.setSenderColor(user.getAvatarColor());
            });
        }

        return ResponseEntity.ok(messages);
    }
}