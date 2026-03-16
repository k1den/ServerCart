package com.k1den.server_cart.models;

import jakarta.persistence.*;

@Entity
@Table(name = "list_items")
public class ListItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "list_id")
    private Integer listId;

    private String name;

    @Column(name = "is_bought")
    private Boolean isBought = false;

    @Column(name = "assigned_user_id")
    private Integer assignedUserId;

    // --- ДОБАВЛЯЕМ КАТЕГОРИЮ ---
    private String category = "Разное"; // По умолчанию будет "Разное"

    @Column(name = "assignee_name")
    private String assigneeName;

    // Геттер и Сеттер
    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    // Обязательно добавь Геттер и Сеттер:
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getListId() {
        return listId;
    }

    public void setListId(Integer listId) {
        this.listId = listId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsBought() {
        return isBought;
    }

    public void setIsBought(Boolean bought) {
        isBought = bought;
    }

    public Integer getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Integer assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
}
