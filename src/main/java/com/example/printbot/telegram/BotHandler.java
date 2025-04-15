package com.example.printbot.telegram;

import com.example.printbot.model.Order;
import com.example.printbot.model.User;
import com.example.printbot.service.OrderCalculationService;
import com.example.printbot.service.OrderService;
import com.example.printbot.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Component
public class BotHandler {

    private final UserService userService;
    private final OrderService orderService;
    private final OrderCalculationService orderCalculationService;

    public BotHandler(UserService userService, OrderService orderService, OrderCalculationService orderCalculationService) {
        this.userService = userService;
        this.orderService = orderService;
        this.orderCalculationService = orderCalculationService;
    }

    public SendMessage handleUpdate(Update update)  {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long telegramId = update.getMessage().getFrom().getId();
            User user = userService.findUserByTelegramId(telegramId);
            if (user == null) {
                user = new User();
                user.setTelegramId(telegramId);
                user.setUsername(update.getMessage().getFrom().getUserName());
                userService.save(user);
            }
            String messageText = update.getMessage().getText();
            SendMessage message = new SendMessage();
            message.setChatId(update.getMessage().getChatId().toString());
            if (messageText.equals("/start")) {
                message.setText("Hello!");
            } else if (messageText.startsWith("/create_order")) {
                Order order = new Order();
                order.setUserId(user.getId());
                order.setStatus(Order.Status.ACCEPTED.name());
                orderService.createOrder(order);
                message.setText("Order created successfully!");
            } else if (messageText.startsWith("/update_status")) {
                String[] parts = messageText.split(" ");
                if (parts.length == 3) {
                    Long orderId = Long.parseLong(parts[1]);
                    Order.Status status = Order.Status.valueOf(parts[2]);
                    orderService.updateOrderStatus(orderId, status);
                    message.setText("Order status updated successfully!");
                } else {
                    message.setText("Invalid command format. Use /update_status <orderId> <status>");
                }
            } else if (messageText.equals("/my_orders")) {
                List<Order> orders = orderService.findOrdersByUserId(user.getId());
                String ordersList = orders.stream()
                        .map(order -> "Order ID: " + order.getId() + ", Status: " + order.getStatus())
                        .collect(Collectors.joining("\n"));
                message.setText(ordersList.isEmpty() ? "No orders found." : ordersList);
            }
            else {
                message.setText(messageText);
            } 
            return message;
        }
        return null;
    }
}