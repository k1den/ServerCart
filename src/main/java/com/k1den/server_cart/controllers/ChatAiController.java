package com.k1den.server_cart.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.k1den.server_cart.models.ListItem;
import com.k1den.server_cart.models.ShoppingList;
import com.k1den.server_cart.repository.ListItemRepository;
import com.k1den.server_cart.repository.ShoppingListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat-ai")
public class ChatAiController {

    @Autowired
    private ShoppingListRepository listRepository;

    @Autowired
    private ListItemRepository itemRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/process-message")
    public ResponseEntity<String> processMessage(@RequestParam Integer chatId, @RequestParam String messageText) {

        System.out.println("════════════════════════════════════════");
        System.out.println("📩 Входящее сообщение: " + messageText);

        List<Command> commands = parseCommands(messageText);
        System.out.println("📝 Найдено команд: " + commands.size());
        for (int i = 0; i < commands.size(); i++) {
            System.out.println("   Команда " + (i + 1) + ": " + commands.get(i).action + " → " + commands.get(i).text);
        }

        List<Map<String, String>> itemsToAdd = new ArrayList<>();
        List<String> itemsToExclude = new ArrayList<>();

        for (Command cmd : commands) {
            if (cmd.isNegative()) {
                List<String> excluded = extractItemsFromText(cmd.text);
                itemsToExclude.addAll(excluded);
                System.out.println("   ⛔ Исключить: " + excluded);
            } else {
                List<Map<String, String>> items = processPhrase(cmd.text);
                itemsToAdd.addAll(items);
                System.out.println("   ✅ Добавить: " + items.size() + " товаров");
            }
        }

        System.out.println("📊 Всего к добавлению: " + itemsToAdd.size());
        System.out.println("📊 Всего к исключению: " + itemsToExclude.size());

        List<Map<String, String>> filteredItems = new ArrayList<>();
        for (Map<String, String> item : itemsToAdd) {
            String name = item.get("name");
            if (name == null) continue;

            boolean shouldExclude = false;
            for (String excluded : itemsToExclude) {
                if (name.equalsIgnoreCase(excluded)) {
                    shouldExclude = true;
                    System.out.println("⛔ ИСКЛЮЧЁН: " + name);
                    break;
                }
            }

            if (!shouldExclude) {
                filteredItems.add(item);
            }
        }

        if (filteredItems.isEmpty()) {
            return ResponseEntity.ok("ИИ не нашел товаров для покупки (все исключены).");
        }

        List<ShoppingList> existingLists = listRepository.findByChatId(chatId);
        ShoppingList targetList;
        if (existingLists.isEmpty()) {
            targetList = new ShoppingList();
            targetList.setChatId(chatId);
            targetList = listRepository.save(targetList);
        } else {
            targetList = existingLists.get(0);
        }

        int addedCount = 0;
        for (Map<String, String> itemData : filteredItems) {
            String name = itemData.get("name");
            if (name == null || name.trim().isEmpty()) continue;

            String normalizedName = normalizeProductName(name.trim());
            String category = itemData.getOrDefault("category", "Разное");

            ListItem item = new ListItem();
            item.setListId(targetList.getId());
            item.setName(normalizedName);
            item.setCategory(category.trim());
            itemRepository.save(item);
            addedCount++;

            System.out.println("✅ Добавлен товар: " + normalizedName + " (" + category + ")");
        }

        System.out.println("🎉 Итого добавлено: " + addedCount);
        System.out.println("════════════════════════════════════════");
        return ResponseEntity.ok("✅ Добавлено товаров: " + addedCount);
    }

    static class Command {
        String text;
        String action;
        boolean isNegative;

        Command(String text, String action, boolean isNegative) {
            this.text = text;
            this.action = action;
            this.isNegative = isNegative;
        }

        boolean isNegative() {
            return isNegative;
        }
    }

    private List<Command> parseCommands(String text) {
        List<Command> commands = new ArrayList<>();

        Pattern buyPattern = Pattern.compile("(купи|возьми|надо|нужно|добавь|положи)\\s+", Pattern.CASE_INSENSITIVE);
        Pattern negativePattern = Pattern.compile("(не\\s*покупай|не\\s*надо|не\\s*бери|без|исключи|кроме)\\s+", Pattern.CASE_INSENSITIVE);

        List<int[]> segments = new ArrayList<>();

        Matcher buyMatcher = buyPattern.matcher(text);
        while (buyMatcher.find()) {
            segments.add(new int[]{buyMatcher.start(), buyMatcher.end(), 1}); // 1 = positive
        }

        Matcher negMatcher = negativePattern.matcher(text);
        while (negMatcher.find()) {
            segments.add(new int[]{negMatcher.start(), negMatcher.end(), -1}); // -1 = negative
        }

        segments.sort(Comparator.comparingInt(a -> a[0]));

        for (int i = 0; i < segments.size(); i++) {
            int[] current = segments.get(i);
            int start = current[0];
            int end = (i + 1 < segments.size()) ? segments.get(i + 1)[0] : text.length();

            String cmdText = text.substring(current[1], end).trim();
            String action = text.substring(current[0], current[1]).trim().toLowerCase();
            boolean isNegative = current[2] == -1;

            if (!cmdText.isEmpty()) {
                commands.add(new Command(cmdText, action, isNegative));
            }
        }

        if (commands.isEmpty()) {
            commands.add(new Command(text.trim(), "купи", false));
        }

        return commands;
    }

    private List<String> extractItemsFromText(String text) {
        List<String> items = new ArrayList<>();

        // Разбиваем по запятым, союзам
        String[] parts = text.split("[,;]|\\s+(и|а|также|но)\\s+");

        for (String part : parts) {
            String trimmed = part.trim().toLowerCase();
            if (!trimmed.isEmpty() && trimmed.length() > 2) {
                items.add(trimmed);
            }
        }

        return items;
    }

    private List<Map<String, String>> processPhrase(String phrase) {
        System.out.println("🤖 Обрабатываем фразу: " + phrase);

        String prompt = """
        Ты парсер списка покупок. Извлеки ВСЕ товары из фразы:
        "%s"
        
        ПРАВИЛА:
        1. Верни все упомянутые продукты
        2. Названия в именительном падеже, первая буква заглавная
        3. Формат: [{"name":"...", "category":"..."}]
        4. Если товаров нет — верни пустой массив []
        
        Категории: Фрукты, Овощи, Молочные продукты, Мясо, Хлеб, Бакалея, Напитки, Другое
        
        Примеры:
        "молоко хлеб" → [{"name":"Молоко","category":"Молочные продукты"},{"name":"Хлеб","category":"Хлеб"}]
        "помидоры огурцы сыр" → [{"name":"Помидоры","category":"Овощи"},{"name":"Огурцы","category":"Овощи"},{"name":"Сыр","category":"Молочные продукты"}]
        
        Ответ ТОЛЬКО JSON:
        """.formatted(phrase);

        String aiResponse = callAiApi(prompt);

        if (aiResponse == null || aiResponse.isEmpty()) {
            System.out.println("⚠️ ИИ не вернул ответ для фразы");
            return new ArrayList<>();
        }

        String jsonStr = extractJsonArray(aiResponse);
        if (jsonStr == null) {
            jsonStr = aiResponse.trim();
        }

        try {
            List<Map<String, String>> items;
            if (jsonStr.startsWith("[")) {
                items = objectMapper.readValue(jsonStr, new TypeReference<List<Map<String, String>>>() {});
            } else {
                Map<String, String> singleItem = objectMapper.readValue(jsonStr, new TypeReference<Map<String, String>>() {});
                items = Collections.singletonList(singleItem);
            }

            System.out.println("   ✅ Найдено товаров: " + items.size());
            return items;

        } catch (Exception e) {
            System.out.println("   ❌ Ошибка парсинга: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String callAiApi(String prompt) {
        String ollamaUrl = "http://localhost:11434/api/chat";

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "my-llama");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "Ты JSON парсер. Возвращай ТОЛЬКО валидный JSON массив. Никакого текста кроме JSON."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                    "temperature", 0.1,
                    "top_p", 0.9,
                    "num_predict", 300
            ));

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> request =
                    new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(ollamaUrl, request, String.class);

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("message").path("content").asText();

        } catch (Exception e) {
            System.err.println("Ошибка Ollama (chat): " + e.getMessage());
            return callAiApiGenerate(prompt);
        }
    }

    private String callAiApiGenerate(String prompt) {
        String ollamaUrl = "http://localhost:11434/api/generate";

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "my-llama");
            requestBody.put("prompt", prompt);
            requestBody.put("format", "json");
            requestBody.put("stream", false);
            requestBody.put("options", Map.of("temperature", 0.1, "num_predict", 300));

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> request =
                    new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(ollamaUrl, request, String.class);

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("response").asText();

        } catch (Exception e) {
            System.err.println("Ошибка Ollama (generate): " + e.getMessage());
            return null;
        }
    }

    private String normalizeProductName(String name) {
        if (name == null || name.isEmpty()) return name;

        name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();

        Map<String, String> corrections = Map.ofEntries(
                Map.entry("мяса", "Мясо"), Map.entry("мясу", "Мясо"), Map.entry("мясом", "Мясо"), Map.entry("мясе", "Мясо"),
                Map.entry("банана", "Бананы"), Map.entry("бананов", "Бананы"), Map.entry("банану", "Бананы"), Map.entry("бананом", "Бананы"),
                Map.entry("помидора", "Помидоры"), Map.entry("помидоров", "Помидоры"), Map.entry("помидору", "Помидоры"), Map.entry("помидором", "Помидоры"),
                Map.entry("огурца", "Огурцы"), Map.entry("огурцов", "Огурцы"), Map.entry("огурцу", "Огурцы"), Map.entry("огурцом", "Огурцы"),
                Map.entry("яблока", "Яблоки"), Map.entry("яблок", "Яблоки"), Map.entry("яблоку", "Яблоки"), Map.entry("яблоком", "Яблоки"),
                Map.entry("молока", "Молоко"), Map.entry("молоку", "Молоко"), Map.entry("молоком", "Молоко"), Map.entry("молоке", "Молоко"),
                Map.entry("хлеба", "Хлеб"), Map.entry("хлебу", "Хлеб"), Map.entry("хлебом", "Хлеб"), Map.entry("хлебе", "Хлеб"),
                Map.entry("колбасы", "Колбаса"), Map.entry("колбасу", "Колбаса"), Map.entry("колбасой", "Колбаса"), Map.entry("колбасе", "Колбаса"),
                Map.entry("сосисок", "Сосиски"), Map.entry("сосиски", "Сосиски"), Map.entry("сосискам", "Сосиски"), Map.entry("сосисками", "Сосиски"),
                Map.entry("рыбы", "Рыба"), Map.entry("рыбу", "Рыба"), Map.entry("рыбой", "Рыба"), Map.entry("рыбе", "Рыба"),
                Map.entry("курицы", "Курица"), Map.entry("курицу", "Курица"), Map.entry("курицей", "Курица"), Map.entry("курице", "Курица"),
                Map.entry("сыра", "Сыр"), Map.entry("сыру", "Сыр"), Map.entry("сыром", "Сыр"), Map.entry("сыре", "Сыр"),
                Map.entry("яиц", "Яйца"), Map.entry("яйца", "Яйца"), Map.entry("яйцам", "Яйца"), Map.entry("яйцами", "Яйца"),
                Map.entry("картошки", "Картошка"), Map.entry("картошку", "Картошка"), Map.entry("картошкой", "Картошка"), Map.entry("картошке", "Картошка"),
                Map.entry("картофеля", "Картофель"), Map.entry("картофелю", "Картофель"), Map.entry("картофелем", "Картофель"), Map.entry("картофеле", "Картофель")
        );

        String lowerName = name.toLowerCase();
        if (corrections.containsKey(lowerName)) {
            return corrections.get(lowerName);
        }

        return name;
    }

    private String extractJsonArray(String text) {
        if (text == null) return null;
        text = text.trim();

        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');

        if (start != -1 && end != -1 && end > start) {
            String json = text.substring(start, end + 1);
            if (json.matches("\\[.*\\]")) {
                return json;
            }
        }
        return null;
    }
}