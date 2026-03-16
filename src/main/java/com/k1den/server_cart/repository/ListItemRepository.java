package com.k1den.server_cart.repository;

import com.k1den.server_cart.models.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListItemRepository extends JpaRepository<ListItem, Integer> {
    // Найти все продукты внутри конкретного списка
    List<ListItem> findByListIdOrderByIdAsc(Integer listId);
}
