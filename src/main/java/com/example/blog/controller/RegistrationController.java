package com.example.blog.controller;

import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("")
    public String registerChoice() {
        return "register";
    }

    @GetMapping("/{role}")
    public String registrationForm(@PathVariable String role, Model model) {
        if (!role.equalsIgnoreCase("student") && !role.equalsIgnoreCase("teacher")) {
            return "redirect:/register";
        }
        model.addAttribute("role", role.toLowerCase());
        return "register-form";
    }

    @PostMapping("/{role}")
    public String registerUser(@PathVariable String role, 
                               @RequestParam String username, 
                               @RequestParam String password,
                               Model model) {
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists");
            model.addAttribute("role", role);
            return "register-form";
        }

        Role userRole = role.equalsIgnoreCase("teacher") ? Role.TEACHER : Role.STUDENT;
        
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(userRole)
                .build();
        
        userRepository.save(user);
        return "redirect:/admin/login?registered";
    }
}
