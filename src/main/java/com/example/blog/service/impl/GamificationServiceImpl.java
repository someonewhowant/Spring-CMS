package com.example.blog.service.impl;

import com.example.blog.entity.Achievement;
import com.example.blog.entity.User;
import com.example.blog.entity.UserAchievement;
import com.example.blog.repository.AchievementRepository;
import com.example.blog.repository.UserAchievementRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.service.GamificationService;
import com.example.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final NotificationService notificationService;

    // Formula for XP required for the NEXT level (e.g., Level 1->2 requires 100 XP, 2->3 requires 150)
    private int getXpRequiredForLevel(int level) {
        return level * 100;
    }

    @Override
    @Transactional
    public void awardXp(Long userId, int amount, String reason) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setExperiencePoints(user.getExperiencePoints() + amount);

        int requiredXp = getXpRequiredForLevel(user.getLevel());
        
        boolean leveledUp = false;
        while (user.getExperiencePoints() >= requiredXp) {
            user.setExperiencePoints(user.getExperiencePoints() - requiredXp);
            user.setLevel(user.getLevel() + 1);
            requiredXp = getXpRequiredForLevel(user.getLevel());
            leveledUp = true;
        }

        userRepository.save(user);

        if (leveledUp) {
            notificationService.createNotification(
                    user, 
                    "Level Up! You have reached Level " + user.getLevel(), 
                    "/profile"
            );
        }

        // Check if any new achievements were unlocked due to this action
        evaluateAchievements(userId);
    }

    @Override
    @Transactional
    public void evaluateAchievements(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> unlocked = userAchievementRepository.findByUserId(userId);
        
        List<Long> unlockedIds = unlocked.stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toList());

        for (Achievement achievement : allAchievements) {
            if (unlockedIds.contains(achievement.getId())) {
                continue; // Already unlocked
            }

            boolean shouldUnlock = false;
            
            if (achievement.getConditionType() == Achievement.ConditionType.QUIZZES_PASSED) {
                long passedCount = userQuizResultRepository.findByUserId(userId).stream()
                        .filter(r -> r.getScore() >= 3)
                        .count();
                if (passedCount >= achievement.getRequiredValue()) {
                    shouldUnlock = true;
                }
            } else if (achievement.getConditionType() == Achievement.ConditionType.PERFECT_SCORES) {
                long perfectCount = userQuizResultRepository.findByUserId(userId).stream()
                        .filter(r -> r.getScore() == 5)
                        .count();
                if (perfectCount >= achievement.getRequiredValue()) {
                    shouldUnlock = true;
                }
            }
            
            if (shouldUnlock) {
                UserAchievement ua = UserAchievement.builder()
                        .user(user)
                        .achievement(achievement)
                        .build();
                userAchievementRepository.save(ua);
                
                notificationService.createNotification(
                        user,
                        "Achievement Unlocked: " + achievement.getName(),
                        "/profile"
                );
            }
        }
    }

    @Override
    public List<Achievement> getUnlockedAchievements(Long userId) {
        return userAchievementRepository.findByUserId(userId).stream()
                .map(UserAchievement::getAchievement)
                .collect(Collectors.toList());
    }
}
