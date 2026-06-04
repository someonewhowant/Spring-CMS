package com.example.blog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                "/css/**",
                                                "/js/**",
                                                "/img/**",
                                                "/uploads/**",
                                                "/h2-console/**",
                                                "/favicon.ico",
                                                "/ws/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/",
                                                "/post/**",
                                                "/search",
                                                "/about",
                                                "/admin/login",
                                                "/register/**",
                                                "/courses",
                                                "/course/**",
                                                "/articles",
                                                "/quiz/**",
                                                "/leaderboard",
                                                "/profile/u/**")
                                        .permitAll()
                                        .requestMatchers("/admin/**")
                                        .hasAnyRole("ADMIN", "TEACHER")
                                        .requestMatchers("/student/**")
                                        .hasRole("STUDENT")
                                        .requestMatchers("/teacher/**")
                                        .hasRole("TEACHER")
                                        .requestMatchers("/api/sandbox/**")
                                        .authenticated()
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        form ->
                                form.loginPage("/admin/login")
                                        .loginProcessingUrl("/admin/login")
                                        .successHandler(
                                                (request, response, authentication) -> {
                                                    for (var auth :
                                                            authentication.getAuthorities()) {
                                                        if (auth.getAuthority()
                                                                .equals("ROLE_ADMIN")) {
                                                            response.sendRedirect(
                                                                    "/admin/dashboard");
                                                            return;
                                                        } else if (auth.getAuthority()
                                                                .equals("ROLE_STUDENT")) {
                                                            response.sendRedirect(
                                                                    "/student/dashboard");
                                                            return;
                                                        } else if (auth.getAuthority()
                                                                .equals("ROLE_TEACHER")) {
                                                            response.sendRedirect(
                                                                    "/teacher/dashboard");
                                                            return;
                                                        }
                                                    }
                                                    response.sendRedirect("/");
                                                })
                                        .permitAll())
                .logout(
                        logout ->
                                logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                        .logoutSuccessUrl("/")
                                        .permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
