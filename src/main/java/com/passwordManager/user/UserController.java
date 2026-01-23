package com.passwordManager.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/username-availability")
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam String username) {
        return ResponseEntity.ok(!userService.doesUserExistByUsername(username));
    }

}
