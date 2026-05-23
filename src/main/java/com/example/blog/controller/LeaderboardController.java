package com.example.blog.controller;

import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class LeaderboardController {

    private final UserRepository userRepository;

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        List<User> topStudents = userRepository.findByRole(Role.STUDENT).stream()
                .sorted((u1, u2) -> {
                    int xp1 = u1.getExperiencePoints() != null ? u1.getExperiencePoints() : 0;
                    int xp2 = u2.getExperiencePoints() != null ? u2.getExperiencePoints() : 0;
                    return Integer.compare(xp2, xp1);
                })
                .limit(50)
                .collect(Collectors.toList());

        model.addAttribute("topStudents", topStudents);
        return "leaderboard";
    }
}
