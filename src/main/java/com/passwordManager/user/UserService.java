package com.passwordManager.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    public Optional<User> findUserByUsername(String username) {
       return userRepository.findByUsername(username);
    }
    public boolean doesUserExistByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
