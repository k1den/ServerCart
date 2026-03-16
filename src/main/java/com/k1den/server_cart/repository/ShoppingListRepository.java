package com.k1den.server_cart.repository;

import com.k1den.server_cart.models.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Integer> {
    // Найти все списки, привязанные к конкретному чату
    List<ShoppingList> findByChatId(Integer chatId);

    // Найти списки пользователя, которые НЕ привязаны к чату
    List<ShoppingList> findByUserIdAndChatIdIsNull(Integer userId);
}
