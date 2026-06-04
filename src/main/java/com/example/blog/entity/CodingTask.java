package com.example.blog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "coding_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /** The programming language for this task (e.g., javascript, python, java). */
    @Column(nullable = false)
    private String language;

    /** Starter code that pre-populates the editor. */
    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    /**
     * JSON array of test cases. Each test case is an object with:
     * - "input": the input string passed to the program's stdin
     * - "expectedOutput": the expected stdout output (trimmed)
     * - "label": human-readable description of the test
     * Example: [{"input":"5","expectedOutput":"25","label":"Square of 5"}]
     */
    @Column(name = "test_cases_json", columnDefinition = "TEXT", nullable = false)
    private String testCasesJson;

    /** XP reward for successfully passing all test cases. */
    @Column(name = "points_reward", nullable = false)
    private int pointsReward;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;
}
