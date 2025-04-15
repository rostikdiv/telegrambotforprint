package com.example.printbot.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import jakarta.annotation.PostConstruct;
@Component
public class PrintBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final BotHandler botHandler;

    private final TelegramBotsApi telegramBotsApi;

    public PrintBot(@Value("${telegram.bot.username}") String botUsername,
                    @Value("${telegram.bot.token}") String botToken,
                    BotHandler botHandler, TelegramBotsApi telegramBotsApi) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.botHandler = botHandler;
        this.telegramBotsApi = telegramBotsApi;
    }
    @PostConstruct
    public void init() throws TelegramApiException {
        telegramBotsApi.registerBot(this);


    }

    @Override
    public void onUpdateReceived(Update update) {
        botHandler.handleUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}