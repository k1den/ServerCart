package com.k1den.server_cart.controllers;

import com.k1den.server_cart.models.User;
import com.k1den.server_cart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private final Map<String, String> verificationCodes = new HashMap<>();

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/request-code")
    public ResponseEntity<?> requestCode(@RequestParam String email, @RequestParam String username) {
        email = email.trim().toLowerCase();
        username = username.trim();

        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        Optional<User> existingUserByUsername = userRepository.findByUsername(username);

        if (existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            if (!user.getUsername().equals(username)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Пользователь с такой почтой уже зарегистрирован под другим именем."));
            }
        } else {
            if (existingUserByUsername.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Это имя пользователя уже занято."));
            }
        }

        Random random = new Random();
        String code = String.format("%06d", random.nextInt(999999));

        verificationCodes.put(email, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("louisecaezgf0@rambler.ru");
        message.setTo(email);
        message.setSubject("Код подтверждения - Совместные покупки");
        message.setText("Привет, " + username + "!\nВаш код для входа: " + code);

        try {
            mailSender.send(message);
            System.out.println("Код " + code + " успешно отправлен на " + email);
            return ResponseEntity.ok(Map.of("message", "Код отправлен на почту"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Ошибка отправки письма"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "#CCCCCC") String avatarColor) {

        email = email.trim().toLowerCase();
        username = username.trim();

        String savedCode = verificationCodes.get(email);

        if (savedCode == null || !savedCode.equals(code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Неверный код или срок действия истек"));
        }

        verificationCodes.remove(email);

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.getUsername().equals(username)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Конфликт данных пользователя"));
            }
        } else {
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Имя пользователя уже занято"));
            }

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(username);
            newUser.setAvatarColor(avatarColor);
            user = userRepository.save(newUser);
        }

        return ResponseEntity.ok(user);
    }
}
