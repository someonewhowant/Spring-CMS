package com.example.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    private String fullName;

    @Email(message = "Please provide a valid email address")
    private String email;

    private String title;
    private String bio;
    private String githubUrl;
    private String linkedinUrl;
    private MultipartFile avatarFile;

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
