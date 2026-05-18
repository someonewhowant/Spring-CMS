package com.example.blog.controller;

import com.example.blog.entity.User;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final UserRepository userRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());

        List<UserQuizResult> quizResults = userQuizResultRepository.findByUserId(user.getId());
        model.addAttribute("quizResults", quizResults);
        
        int completedQuizzesCount = quizResults.size();
        model.addAttribute("completedQuizzesCount", completedQuizzesCount);
        
        double averageScore = 0.0;
        if (completedQuizzesCount > 0) {
            double totalScore = 0.0;
            for (UserQuizResult res : quizResults) {
                totalScore += res.getScore();
            }
            averageScore = Math.round((totalScore / completedQuizzesCount) * 10.0) / 10.0;
        }
        model.addAttribute("averageScore", averageScore);

        long totalCoursesCount = courseRepository.count();
        long totalQuizzesCount = quizRepository.count();
        model.addAttribute("totalCoursesCount", totalCoursesCount);
        model.addAttribute("totalQuizzesCount", totalQuizzesCount);

        model.addAttribute("message", "Добро пожаловать в командный центр студента!");
        model.addAttribute("title", "Student Cabinet");
        
        return "student/dashboard";
    }
}
