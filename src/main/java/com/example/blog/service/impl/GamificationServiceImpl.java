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
    private final com.example.blog.repository.ChatMessageRepository chatMessageRepository;

    // Formula for XP required for the NEXT level (e.g., Level 1->2 requires 100 XP, 2->3 requires 200)
    private int getXpRequiredForLevel(int level) {
        return Math.max(100, level * 100);
    }

    @Override
    @Transactional
    public void awardXp(Long userId, int amount, String reason) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        int currentXp = user.getExperiencePoints() != null ? user.getExperiencePoints() : 0;
        user.setExperiencePoints(currentXp + amount);

        int currentLevel = user.getLevel() != null && user.getLevel() > 0 ? user.getLevel() : 1;
        user.setLevel(currentLevel);

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
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> unlocked = userAchievementRepository.findByUserId(userId);
        
        java.util.Set<Long> unlockedIds = unlocked.stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(java.util.stream.Collectors.toSet());

        // Pre-fetch quiz results to avoid multiple database calls in the loop
        List<com.example.blog.entity.UserQuizResult> quizResults = userQuizResultRepository.findByUserId(userId);

        for (Achievement achievement : allAchievements) {
            if (unlockedIds.contains(achievement.getId())) {
                continue; // Already unlocked
            }

            boolean shouldUnlock = false;
            
            if (achievement.getConditionType() == Achievement.ConditionType.QUIZZES_PASSED) {
                long passedCount = quizResults.stream()
                        .filter(r -> r.getScore() >= 3)
                        .count();
                if (passedCount >= achievement.getRequiredValue()) {
                    shouldUnlock = true;
                }
            } else if (achievement.getConditionType() == Achievement.ConditionType.PERFECT_SCORES) {
                long perfectCount = quizResults.stream()
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
                unlockedIds.add(achievement.getId()); // Add to set to avoid duplicates in the same run
                
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

    @Override
    @Transactional
    public void resetUserStats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        userQuizResultRepository.deleteByUserId(userId);
        userAchievementRepository.deleteByUserId(userId);
        user.setExperiencePoints(0);
        user.setLevel(1);
        user.setLastOpenedCourseId(null);
        user.setLastOpenedModuleId(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetAllStudentStats() {
        List<User> students = userRepository.findByRole(com.example.blog.entity.Role.STUDENT);
        for (User student : students) {
            resetUserStats(student.getId());
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        userQuizResultRepository.deleteByUserId(userId);
        userAchievementRepository.deleteByUserId(userId);
        notificationService.clearAll(userId);
        chatMessageRepository.deleteBySenderIdOrRecipientId(userId);
        userRepository.delete(user);
    }
}
