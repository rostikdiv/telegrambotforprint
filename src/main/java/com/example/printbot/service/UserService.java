package com.example.printbot.service;

import com.example.printbot.model.User;
import com.example.printbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createOrUpdateUser(User user) {
        User existingUser = userRepository.findByTelegramId(user.getTelegramId());
        if (existingUser != null) {
            existingUser.setUsername(user.getUsername());
            existingUser.setContactInfo(user.getContactInfo());
            return userRepository.save(existingUser);
        } else {
            return userRepository.save(user);
        }
    }

    public User findUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}