package com.example.demo;

import com.example.printbot.telegram.PrintBot;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.starter.EnableTelegramBots;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@EnableTelegramBots
@SpringBootApplication(exclude = TelegramBotStarterConfiguration.class)
public class DemoApplication implements CommandLineRunner {

  @Value("${NAME:World}")
  String name;

  @Autowired
  private PrintBot printBot;


  @RestController
  class HelloworldController {
    @GetMapping("/")
    String hello() {
      return "Hello " + name + "!";
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

}
