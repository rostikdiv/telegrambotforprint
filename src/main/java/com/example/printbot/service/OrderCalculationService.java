package com.example.printbot.service;

import com.example.printbot.model.Order;
import org.springframework.stereotype.Service;

@Service
public class OrderCalculationService {

    public Double calculateCost(Order order) {
        return order.getPages() * 1.0;
    }
}