package com.example.printbot.service;

import com.example.printbot.telegram.BotHandler;
import com.example.printbot.model.Order;
import com.example.printbot.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;

    private final PdfService pdfService;
    
    @Autowired
    public OrderService(OrderRepository orderRepository, PdfService pdfService) {
        this.orderRepository = orderRepository;
        this.pdfService = pdfService;
    }

    public Order createOrder(Order order) {
        order.setOrderNumber(generateOrderNumber());
        log.info("createOrder method start with order: {}", order);
        try {
            if(Objects.isNull(order)){
                log.error("Order is null");
                throw new IllegalArgumentException("Order is null");
            }

            if (Objects.isNull(order.getPages())) {
                if (Objects.nonNull(order.getFileId())){
                    try {
                        log.info("Getting pages from file");
                        order.setPages(pdfService.getPageCount(new File(order.getFileId())));
                    } catch (Exception e) {
                        log.error("Error getting pages from file", e);
                    }
                }else {
                    log.error("Order pages is null, and file is null");
                }
            }
            if(Objects.isNull(order.getPages()) || order.getPages() <= 0){
                log.error("Pages is invalid: {}", order.getPages());
                throw new IllegalArgumentException("Invalid pages");}
            Order savedOrder = orderRepository.save(order);
            log.info("createOrder method end with result: {}", savedOrder);
            return savedOrder;
        } catch (Exception e) {
            log.error("Error in createOrder method", e);
            throw e;
        }
    }

    public List<Order> findOrdersByUserId(Long userId) {
        log.info("findOrdersByUserId method start with userId: {}", userId);
        try {
            List<Order> orders = orderRepository.findAllByUserId(userId);
            log.info("findOrdersByUserId method end with result: {}", orders);
            return orders;
        } catch (Exception e) {
            log.error("Error in findOrdersByUserId method", e);
            throw e;
        }
    }

    public Order updateOrderStatus(Long orderId, Order.Status status) {
        log.info("updateOrderStatus method start with orderId: {}, status: {}", orderId, status);
        if (Objects.isNull(status) || !List.of(Order.Status.values()).contains(status)) {
            log.error("Invalid order status: {}", status);
            throw new IllegalArgumentException("Invalid order status");
        }
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
             try {
                Order order = optionalOrder.get();

                order.setStatus(status);
                Order updatedOrder = orderRepository.save(order);
                log.info("updateOrderStatus method end with result: {}", updatedOrder);
                return updatedOrder;
            } catch (Exception e) {
                log.error("Error in updateOrderStatus method", e);
                throw e;
            }
        } else {
            log.warn("Order with id {} not found", orderId);
            return null;
        }
    }

    public Order findOrderById(Long id) {
        log.info("findOrderById method start with id: {}", id);
        Optional<Order> optionalOrder = orderRepository.findById(id);
        log.info("findOrderById method end with result: {}", optionalOrder.orElse(null));
        return optionalOrder.orElse(null) ;
    }

    public String generateOrderNumber() {
        log.info("generateOrderNumber method start");
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String formattedDateTime = now.format(formatter);
        int randomNumber = new Random().nextInt(1000);
        String orderNumber = "ORDER_" + formattedDateTime + "_" + randomNumber;
        log.info("generateOrderNumber method end with result: {}", orderNumber);
        return orderNumber;
    }

    public Order save(Order order){
        log.info("save method start with order: {}", order);
        Order savedOrder = orderRepository.save(order);
        log.info("save method end with result: {}", savedOrder);
        return savedOrder;
    }
}