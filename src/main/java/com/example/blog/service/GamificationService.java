package com.example.blog.service;

import com.example.blog.entity.User;
import com.example.blog.entity.Achievement;
import java.util.List;

public interface GamificationService {
    void awardXp(Long userId, int amount, String reason);
    void evaluateAchievements(Long userId);
    List<Achievement> getUnlockedAchievements(Long userId);
}
