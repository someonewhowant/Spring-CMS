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
                            .name("First Blood")
                            .description("Successfully pass your first quiz.")
                            .iconClass("bi bi-droplet-fill")
                            .conditionType(Achievement.ConditionType.QUIZZES_PASSED)
                            .requiredValue(1)
                            .build(),
                    Achievement.builder()
                            .name("Scholar")
                            .description("Pass 5 quizzes.")
                            .iconClass("bi bi-book-half")
                            .conditionType(Achievement.ConditionType.QUIZZES_PASSED)
                            .requiredValue(5)
                            .build(),
                    Achievement.builder()
                            .name("Perfectionist")
                            .description("Get a perfect score (5/5) on a quiz.")
                            .iconClass("bi bi-star-fill")
                            .conditionType(Achievement.ConditionType.PERFECT_SCORES)
                            .requiredValue(1)
                            .build()
            );
            
            achievementRepository.saveAll(initialAchievements);
            System.out.println("Initialized default gamification achievements.");
        }
    }
}
