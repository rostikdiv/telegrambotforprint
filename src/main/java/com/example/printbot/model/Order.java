package com.example.printbot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private Integer pages;
    private String printType;
    private String color;
    private String paper;
    private String fileId;
    private Long userId;
    private Double cost;
    private String orderNumber;
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        CANCELED,
        ACCEPTED,
        PAID,
        COMPLETED
    }

    public Order() {
    }

    public Order(String description, Integer pages, String printType, String color, String paper, String fileId, Long userId, Double cost, String orderNumber, Status status) {
        this.description = description;
        this.pages = pages;
        this.printType = printType;
        this.color = color;
        this.paper = paper;
        this.fileId = fileId;
        this.userId = userId;
        this.cost = cost;
        this.orderNumber = orderNumber;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPaper() {
        return paper;
    }

    public void setPaper(String paper) {
        this.paper = paper;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}