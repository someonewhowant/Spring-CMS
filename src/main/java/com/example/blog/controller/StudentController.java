package com.example.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("message", "Вы вошли как Студент");
        model.addAttribute("title", "Student Cabinet");
        return "student/dashboard";
    }
}
