package com.k1den.server_cart.repository;

import com.k1den.server_cart.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email); // Поиск юзера по email

    Optional<User> findByUsername(String username);
}
