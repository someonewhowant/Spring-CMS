package com.example.blog.config;

import com.example.blog.entity.Achievement;
import com.example.blog.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GamificationInitializer implements CommandLineRunner {

    private final AchievementRepository achievementRepository;

    @Override
    public void run(String... args) {
        if (achievementRepository.count() == 0) {
            List<Achievement> initialAchievements = List.of(
                    Achievement.builder()
                            .name("First Quiz")
                            .description("Successfully pass your first academy assessment.")
                            .iconClass("bi bi-lightning-fill")
                            .conditionType(Achievement.ConditionType.QUIZZES_PASSED)
                            .requiredValue(1)
                            .build(),
                    Achievement.builder()
                            .name("Scholar")
                            .description("Demonstrate consistency by passing 5 quizzes.")
                            .iconClass("bi bi-mortarboard-fill")
                            .conditionType(Achievement.ConditionType.QUIZZES_PASSED)
                            .requiredValue(5)
                            .build(),
                    Achievement.builder()
                            .name("Perfect Score")
                            .description("Achieve a perfect 5/5 score on any assessment.")
                            .iconClass("bi bi-stars")
                            .conditionType(Achievement.ConditionType.PERFECT_SCORES)
                            .requiredValue(1)
                            .build()
            );
            
            achievementRepository.saveAll(initialAchievements);
            System.out.println("Initialized default gamification achievements.");
        }
    }
}
