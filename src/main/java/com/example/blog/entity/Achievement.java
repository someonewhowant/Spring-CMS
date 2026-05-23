package com.example.blog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "icon_class", nullable = false)
    private String iconClass;

    @Column(name = "condition_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionType conditionType;

    @Column(name = "required_value", nullable = false)
    private Integer requiredValue;

    public enum ConditionType {
        QUIZZES_PASSED,
        PERFECT_SCORES,
        COURSES_COMPLETED
    }
}
