package com.k1den.server_cart.controllers;

import com.k1den.server_cart.models.ListItem;
import com.k1den.server_cart.models.ShoppingList;
import com.k1den.server_cart.repository.ListItemRepository;
import com.k1den.server_cart.repository.ShoppingListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class ShoppingListController {

    @Autowired
    private ShoppingListRepository listRepository;

    @Autowired
    private ListItemRepository itemRepository;


    // Создать новый список для чата
    @PostMapping("/create")
    public ResponseEntity<ShoppingList> createList(@RequestParam Integer chatId) {
        ShoppingList list = new ShoppingList();
        list.setChatId(chatId);
        // Пока без даты, добавим ее выбор позже
        return ResponseEntity.ok(listRepository.save(list));
    }

    // Получить списки чата
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<ShoppingList>> getListsByChat(@PathVariable Integer chatId) {
        return ResponseEntity.ok(listRepository.findByChatId(chatId));
    }

    // Добавить продукт в список
    @PostMapping("/items/add")
    public ResponseEntity<ListItem> addItem(
            @RequestParam Integer listId,
            @RequestParam String name,
            @RequestParam(defaultValue = "Разное") String category) {

        ListItem item = new ListItem();
        item.setListId(listId);
        item.setName(name);
        item.setCategory(category);
        return ResponseEntity.ok(itemRepository.save(item));
    }

    // Получить все продукты списка
    @GetMapping("/{listId}/items")
    public ResponseEntity<List<ListItem>> getItems(@PathVariable Integer listId) {
        return ResponseEntity.ok(itemRepository.findByListIdOrderByIdAsc(listId));
    }

    // Переключить статус (Куплено / Не куплено)
    @PutMapping("/items/{itemId}/toggle")
    public ResponseEntity<ListItem> toggleItemStatus(@PathVariable Integer itemId) {
        ListItem item = itemRepository.findById(itemId).orElseThrow();
        item.setIsBought(!item.getIsBought());
        return ResponseEntity.ok(itemRepository.save(item));
    }

    // Создать одиночный список
    @PostMapping("/create/standalone")
    public ResponseEntity<ShoppingList> createStandaloneList(@RequestParam Integer userId) {
        ShoppingList list = new ShoppingList();
        list.setUserId(userId);
        return ResponseEntity.ok(listRepository.save(list));
    }

    // Получить одиночные списки пользователя
    @GetMapping("/standalone/{userId}")
    public ResponseEntity<List<ShoppingList>> getStandaloneLists(@PathVariable Integer userId) {
        return ResponseEntity.ok(listRepository.findByUserIdAndChatIdIsNull(userId));
    }

    // Удалить продукт из списка
    @DeleteMapping("/items/{itemId}/delete")
    public org.springframework.http.ResponseEntity<Void> deleteItem(@PathVariable Integer itemId) {
        itemRepository.deleteById(itemId);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    // Назначить ответственного за покупку
    @PutMapping("/items/{itemId}/assign")
    public ResponseEntity<ListItem> assignItem(
            @PathVariable Integer itemId,
            @RequestParam(required = false) String assigneeName) {

        ListItem item = itemRepository.findById(itemId).orElseThrow();
        item.setAssigneeName(assigneeName);
        return ResponseEntity.ok(itemRepository.save(item));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(@PathVariable Integer listId) {
        listRepository.deleteById(listId);
        return ResponseEntity.ok().build();
    }
}
