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
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository; // ДОБАВИЛИ РЕПОЗИТОРИЙ

    private final Map<String, String> verificationCodes = new HashMap<>();

    @Autowired
    private JavaMailSender mailSender;

    // 1. Принимаем запрос от Android на отправку кода
    @PostMapping("/request-code")
    public ResponseEntity<Void> requestCode(@RequestParam String email, @RequestParam String username) {

        // Генерируем 6-значный код
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(999999));

        // Сохраняем код в памяти
        verificationCodes.put(email, code);

        // Отправляем реальное письмо
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("louisecaezgf0@rambler.ru"); // Тот же email, что в настройках
        message.setTo(email); // Почта, которую ввел юзер в Android
        message.setSubject("Код подтверждения - Совместные покупки");
        message.setText("Привет, " + username + "!\nВаш код для входа: " + code);

        try {
            mailSender.send(message);
            System.out.println("Код " + code + " успешно отправлен на " + email);
            return ResponseEntity.ok().build(); // Возвращаем пустой успешный ответ 200
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // Возвращаем ошибку 500
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestParam String email,
                                        @RequestParam String username,
                                        @RequestParam String code) {
        String savedCode = verificationCodes.get(email);

        if (savedCode != null && savedCode.equals(code)) {
            // КОД ВЕРНЫЙ! Ищем пользователя или создаем нового
            verificationCodes.remove(email);

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                // Если юзера нет, создаем нового (РЕГИСТРАЦИЯ)
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(username);
                return userRepository.save(newUser); // Сохраняем в PostgreSQL
            });

            // Возвращаем пользователя в Android (он преобразуется в JSON автоматически)
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.badRequest().body("Invalid code");
        }
    }
}
