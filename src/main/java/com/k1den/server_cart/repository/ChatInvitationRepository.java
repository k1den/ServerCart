package com.k1den.server_cart.repository;

import com.k1den.server_cart.models.ChatInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Integer> {
    List<ChatInvitation> findByInviteeId(Integer inviteeId); // Найти инвайты, отправленные мне
}
