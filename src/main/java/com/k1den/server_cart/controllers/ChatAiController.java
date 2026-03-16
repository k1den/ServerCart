package com.k1den.server_cart.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.k1den.server_cart.models.ListItem;
import com.k1den.server_cart.models.ShoppingList;
import com.k1den.server_cart.repository.ListItemRepository;
import com.k1den.server_cart.repository.ShoppingListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-ai")
public class ChatAiController {

    @Autowired
    private ShoppingListRepository listRepository;

    @Autowired
    private ListItemRepository itemRepository;

    // Инструмент для чтения JSON от ИИ
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    @PostMapping("/process-message")
    public ResponseEntity<String> processMessage(@RequestParam Integer chatId, @RequestParam String messageText) {

        System.out.println("Получено сообщение для ИИ: " + messageText);

        // 1. Формируем запрос к ИИ (Промпт + Сообщение пользователя)
        String prompt = "Ты умный ассистент для покупок. Извлеки продукты, которые НУЖНО купить из текста: '" + messageText + "'. " +
                "ИГНОРИРУЙ то, что просят НЕ покупать. " +
                "Верни ТОЛЬКО чистый JSON массив. Твой ответ ОБЯЗАТЕЛЬНО должен начинаться с '[' и заканчиваться на ']'. " +
                "Пример: [{\"name\":\"Молоко\",\"category\":\"Молочные продукты\"}].";

        // 2. ОТПРАВЛЯЕМ ЗАПРОС К ИИ (Здесь происходит магия)
        String aiJsonResponse = callAiApi(prompt);

        if (aiJsonResponse == null || aiJsonResponse.isEmpty()) {
            return ResponseEntity.badRequest().body("Ошибка: ИИ не смог обработать запрос");
        }

        try {
            // Читаем ответ ИИ
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(aiJsonResponse);
            java.util.List<com.fasterxml.jackson.databind.JsonNode> itemsToSave = new java.util.ArrayList<>();

            // Сценарий 1: Пришел чистый массив [ {...}, {...} ]
            if (rootNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                    itemsToSave.add(node);
                }
            }
            // Сценарий 2: Пришел объект {...}
            else if (rootNode.isObject()) {
                // Если внутри сразу есть "name" — значит Оллама вернула ровно один товар! (ТВОЙ СЛУЧАЙ)
                if (rootNode.has("name")) {
                    itemsToSave.add(rootNode);
                } else {
                    // Если "name" нет, значит массив спрятан внутри { "products": [ {...} ] }
                    for (com.fasterxml.jackson.databind.JsonNode child : rootNode) {
                        if (child.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode node : child) {
                                itemsToSave.add(node);
                            }
                            break;
                        }
                    }
                }
            }

            // Если товары так и не нашли
            if (itemsToSave.isEmpty()) {
                return ResponseEntity.ok("ИИ не нашел товаров для покупки.");
            }

            // 4. Находим или создаем список покупок для этого чата
            List<ShoppingList> existingLists = listRepository.findByChatId(chatId);
            ShoppingList targetList;
            if (existingLists.isEmpty()) {
                targetList = new ShoppingList();
                targetList.setChatId(chatId);
                targetList = listRepository.save(targetList);
            } else {
                targetList = existingLists.get(0);
            }

            // 5. Проходимся по списку и сохраняем товары в базу
            int addedCount = 0;
            for (com.fasterxml.jackson.databind.JsonNode itemNode : itemsToSave) {
                if (itemNode.has("name")) {
                    String name = itemNode.get("name").asText();
                    String category = itemNode.has("category") ? itemNode.get("category").asText() : "Разное";

                    ListItem item = new ListItem();
                    item.setListId(targetList.getId());
                    item.setName(name);
                    item.setCategory(category);
                    itemRepository.save(item);
                    addedCount++;
                }
            }

            return ResponseEntity.ok("ИИ успешно добавил " + addedCount + " товаров!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ошибка при чтении ответа ИИ: " + e.getMessage());
        }
    }

    // --- МЕТОД ДЛЯ СВЯЗИ С РЕАЛЬНЫМ ИИ ---
    // В самом начале класса, рядом с ObjectMapper, добавь:
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    // --- НАСТОЯЩИЙ МЕТОД ДЛЯ СВЯЗИ С OLLAMA ---
    private String callAiApi(String prompt) {
        String ollamaUrl = "http://localhost:11434/api/generate";

        try {
            System.out.println("Отправляем запрос в Ollama...");

            // 1. Формируем тело запроса для Ollama
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", "my-llama"); // То самое имя, которое мы задали в терминале!
            requestBody.put("prompt", prompt);
            requestBody.put("format", "json"); // ЗАСТАВЛЯЕМ модель отвечать строго в JSON
            requestBody.put("stream", false);  // Ждем полный ответ, а не по буквам

            // 2. Настраиваем заголовки (Headers)
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // 3. Упаковываем и отправляем HTTP POST запрос
            org.springframework.http.HttpEntity<java.util.Map<String, Object>> request = new org.springframework.http.HttpEntity<>(requestBody, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(ollamaUrl, request, String.class);

            // 4. Достаем ответ (Ollama возвращает JSON, внутри которого есть поле "response" с ответом ИИ)
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
            String aiResult = root.get("response").asText();

            System.out.println("Ответ от Ollama: " + aiResult);
            return aiResult;

        } catch (Exception e) {
            System.err.println("Ошибка при запросе к Ollama: " + e.getMessage());
            return null;
        }
    }
}
