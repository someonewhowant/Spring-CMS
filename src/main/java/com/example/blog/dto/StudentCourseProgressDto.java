package com.example.blog.dto;

import com.example.blog.entity.Course;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StudentCourseProgressDto {
    private final Course course;
    private final int progressPercentage;
    private final String status;
    private final int passedQuizzesCount;
    private final int totalQuizzesCount;
}
