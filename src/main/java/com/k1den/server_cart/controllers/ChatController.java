package com.k1den.server_cart.controllers;

import com.k1den.server_cart.models.Chat;
import com.k1den.server_cart.models.ChatInvitation;
import com.k1den.server_cart.models.User;
import com.k1den.server_cart.repository.ChatInvitationRepository;
import com.k1den.server_cart.repository.ChatRepository;
import com.k1den.server_cart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatInvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    // Отправить инвайт по никнейму
    @PostMapping("/{chatId}/invite")
    public ResponseEntity<?> inviteUser(@PathVariable Integer chatId, @RequestParam String username, @RequestParam String chatTitle) {
        // Ищем пользователя по никнейму
        User invitee = userRepository.findByUsername(username).orElse(null);
        if (invitee == null) {
            return ResponseEntity.badRequest().body("Пользователь не найден");
        }

        ChatInvitation invite = new ChatInvitation();
        invite.setChatId(chatId);
        invite.setInviteeId(invitee.getId());
        invite.setChatTitle(chatTitle);
        invitationRepository.save(invite);

        return ResponseEntity.ok("Приглашение отправлено!");
    }

    // Принять инвайт
    @PostMapping("/invitations/{inviteId}/accept")
    public ResponseEntity<?> acceptInvite(@PathVariable Integer inviteId) {
        ChatInvitation invite = invitationRepository.findById(inviteId).orElse(null);
        if (invite != null) {
            // Добавляем юзера в чат
            chatRepository.addMemberToChat(invite.getChatId(), invite.getInviteeId(), "MEMBER");
            // Удаляем инвайт
            invitationRepository.delete(invite);
            return ResponseEntity.ok("Принято");
        }
        return ResponseEntity.badRequest().build();
    }

    // 1. Создать новый чат
    @PostMapping("/create")
    public ResponseEntity<Chat> createChat(@RequestParam String title, @RequestParam Integer userId) {
        // Создаем сам чат
        Chat chat = new Chat();
        chat.setTitle(title);
        Chat savedChat = chatRepository.save(chat);

        // Привязываем создателя к чату с правами ADMIN
        chatRepository.addMemberToChat(savedChat.getId(), userId, "ADMIN");

        return ResponseEntity.ok(savedChat);
    }

    // 2. Получить список чатов (понадобится нам в следующем шаге)
    @GetMapping("/my")
    public ResponseEntity<List<Chat>> getMyChats(@RequestParam Integer userId) {
        List<Chat> chats = chatRepository.findChatsByUserId(userId);
        return ResponseEntity.ok(chats);
    }

    // Получить список приглашений для пользователя
    @GetMapping("/invitations/{userId}")
    public ResponseEntity<List<ChatInvitation>> getMyInvites(@PathVariable Integer userId) {
        return ResponseEntity.ok(invitationRepository.findByInviteeId(userId));
    }

    // Отклонить (удалить) инвайт
    @DeleteMapping("/invitations/{inviteId}/decline")
    public ResponseEntity<?> declineInvite(@PathVariable Integer inviteId) {
        invitationRepository.deleteById(inviteId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Integer chatId) {
        chatRepository.deleteById(chatId);
        return ResponseEntity.ok().build();
    }
}