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
    private UserRepository userRepository;

    private final Map<String, String> verificationCodes = new HashMap<>();

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/request-code")
    public ResponseEntity<Void> requestCode(@RequestParam String email, @RequestParam String username) {

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
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<User> verifyCode(
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "#CCCCCC") String avatarColor) {

        String savedCode = verificationCodes.get(email);
        if (savedCode != null && savedCode.equals(code)) {
            verificationCodes.remove(email);

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(username);
                newUser.setAvatarColor(avatarColor);
                return userRepository.save(newUser);
            });
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.badRequest().build();
    }
}
