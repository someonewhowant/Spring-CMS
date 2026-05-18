package com.example.blog.config;

import com.example.blog.entity.*;
import com.example.blog.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(CategoryRepository categoryRepository, 
                                      TagRepository tagRepository,
                                      PostRepository postRepository,
                                      UserRepository userRepository,
                                      CourseRepository courseRepository,
                                      CourseModuleRepository moduleRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // Инициализация пользователя
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin")) // Пароль: admin
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
            }

            // Инициализация категорий
            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(Arrays.asList(
                    Category.builder().name("Backend").slug("backend").build(),
                    Category.builder().name("Frontend").slug("frontend").build(),
                    Category.builder().name("DevOps").slug("devops").build(),
                    Category.builder().name("Architecture").slug("architecture").build()
                ));
            }

            // Инициализация тегов
            if (tagRepository.count() == 0) {
                tagRepository.saveAll(Arrays.asList(
                    Tag.builder().name("Java").slug("java").build(),
                    Tag.builder().name("Spring Boot").slug("spring-boot").build(),
                    Tag.builder().name("Docker").slug("docker").build(),
                    Tag.builder().name("Security").slug("security").build(),
                    Tag.builder().name("Microservices").slug("microservices").build(),
                    Tag.builder().name("TypeScript").slug("typescript").build()
                ));
            }

            // Инициализация тестовых постов
            if (postRepository.count() == 0) {
                Category backend = categoryRepository.findBySlug("backend").orElse(null);
                Category architecture = categoryRepository.findBySlug("architecture").orElse(null);
                
                postRepository.save(Post.builder()
                        .title("Mastering Spring Boot Performance")
                        .body("Optimizing the critical path in your Spring Boot applications requires a deep understanding of the JVM, garbage collection, and thread pool management. In this post, we explore techniques to squeeze every millisecond of performance.")
                        .category(backend)
                        .imageUrl("https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&q=80&w=1000")
                        .build());
                
                postRepository.save(Post.builder()
                        .title("Microservices Architecture: The Hard Parts")
                        .body("While microservices offer scalability and independence, they also introduce complexity in data consistency and network reliability. We discuss how to handle distributed transactions and service discovery effectively.")
                        .category(architecture)
                        .imageUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=1000")
                        .build());

                postRepository.save(Post.builder()
                        .title("Building a Scalable Data Pipeline with Kafka")
                        .body("Apache Kafka has become the de-facto standard for event-driven architectures. Learn how to design a resilient data pipeline that handles millions of events per second with ease.")
                        .category(categoryRepository.findBySlug("devops").orElse(null))
                        .imageUrl("https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&q=80&w=1000")
                        .build());
            }

            // Инициализация курсов
            if (courseRepository.count() == 0) {
                Course architectureCourse = Course.builder()
                        .title("Zero to One: System Architecture")
                        .description("A comprehensive guide to designing scalable, reliable, and maintainable systems from scratch.")
                        .content("In this course, we cover the fundamental principles of system design, including CAP theorem, Load Balancing, and Database Sharding.")
                        .level("Advanced")
                        .duration("15 Hours")
                        .imageUrl("https://images.unsplash.com/photo-1508921334112-4c6ef99ce26d?auto=format&fit=crop&q=80&w=1000")
                        .build();
                
                courseRepository.save(architectureCourse);

                moduleRepository.save(CourseModule.builder()
                        .title("Introduction to Scalability")
                        .content("Scalability is the ability of a system to handle a growing amount of work by adding resources.")
                        .orderIndex(0)
                        .course(architectureCourse)
                        .build());

                moduleRepository.save(CourseModule.builder()
                        .title("Database Partitioning Strategies")
                        .content("Horizontal vs Vertical scaling. Sharding mechanisms and rebalancing.")
                        .orderIndex(1)
                        .course(architectureCourse)
                        .build());

                Course backendCourse = Course.builder()
                        .title("Backend Mastery: Java & Spring Boot")
                        .description("Master the core concepts of Spring Framework and build enterprise-grade applications.")
                        .content("Deep dive into Spring Core, Security, Data, and Cloud. Learn how to build production-ready APIs.")
                        .level("Beginner")
                        .duration("20 Hours")
                        .imageUrl("https://images.unsplash.com/photo-1587620962725-abab7fe55159?auto=format&fit=crop&q=80&w=1000")
                        .build();
                
                courseRepository.save(backendCourse);

                moduleRepository.save(CourseModule.builder()
                        .title("Getting Started with Dependency Injection")
                        .content("Understanding the Inversion of Control principle.")
                        .orderIndex(0)
                        .course(backendCourse)
                        .build());
            }
        };
    }
}
