package com.example.blog.controller;

import com.example.blog.dto.UserRegisterRequest;
import com.example.blog.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

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
    public String registerUser(
            @PathVariable String role,
            @Valid @ModelAttribute("registerRequest") UserRegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model) {
        if (!role.equalsIgnoreCase("student") && !role.equalsIgnoreCase("teacher")) {
            return "redirect:/register";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("role", role);
            return "register-form";
        }

        registerRequest.setRole(role);

        try {
            userService.register(registerRequest);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("role", role);
            return "register-form";
        }

        return "redirect:/admin/login?registered";
    }
}
