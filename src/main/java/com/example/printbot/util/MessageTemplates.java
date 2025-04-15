package com.example.printbot.util;

public enum MessageTemplates {
    GREETING("Hello! I'm your print bot. How can I help you?"),
    ORDER_DESCRIPTION_REQUEST("Please, enter the description of your order:"),
    ORDER_PAGES_REQUEST("Please, enter the number of pages:"),
    ORDER_PRINT_TYPE_REQUEST("Please, enter the print type:"),
    ORDER_COLOR_REQUEST("Please, enter the color:"),
    ORDER_PAPER_REQUEST("Please, enter the paper type:"),
    ORDER_FILE_REQUEST("Please, attach the file:"),
    ORDER_CONFIRMATION("Please, confirm your order:\n%s"),
    ORDER_CREATED("Your order has been created. Order number: %s"),
    ORDER_UPDATED("Your order status has been updated to: %s"),
    ORDER_CANCELED("Your order has been canceled."),
    ORDER_CANCELED_COMMENT_REQUEST("Please, enter the reason for canceling the order:"),
    MY_ORDERS("Your orders:\n%s"),
    FILE_SIZE_ERROR("File size is too large. Max file size is 20 MB."),
    VALIDATION_ERROR("Validation error: %s"),
    UNKNOWN_ERROR("Unknown error occurred."),
    EXECUTOR_NEW_ORDER("New order created:\n%s");

    private final String message;

    MessageTemplates(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}