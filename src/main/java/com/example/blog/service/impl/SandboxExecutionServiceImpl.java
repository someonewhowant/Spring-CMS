package com.example.blog.service.impl;

import com.example.blog.service.SandboxExecutionService;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Executes user code via the Piston API — a free, open-source code execution engine.
 * Supports 50+ languages including Python, Java, JavaScript, C++, etc.
 * Public endpoint: https://emkc.org/api/v2/piston
 *
 * @see <a href="https://github.com/engineer-man/piston">Piston on GitHub</a>
 */
@Slf4j
@Service
public class SandboxExecutionServiceImpl implements SandboxExecutionService {

    @Value("${sandbox.piston.api-url:https://emkc.org/api/v2/piston}")
    private String pistonApiUrl;

    private final RestTemplate restTemplate;

    public SandboxExecutionServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Map<String, Object> executeCode(String language, String code, String stdin) {
        Map<String, Object> result = new HashMap<>();

        try {
            String langId = mapLanguage(language);
            String version = getLanguageVersion(language);

            // Build the Piston API request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("language", langId);
            requestBody.put("version", version);
            requestBody.put("files", new Object[]{Map.of("content", code)});
            if (stdin != null && !stdin.isBlank()) {
                requestBody.put("stdin", stdin);
            }
            // Safety limits
            requestBody.put("compile_timeout", 10000);
            requestBody.put("run_timeout", 5000);
            requestBody.put("compile_memory_limit", 256000000);
            requestBody.put("run_memory_limit", 256000000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    pistonApiUrl + "/execute",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> run = (Map<String, Object>) body.get("run");
                if (run != null) {
                    result.put("stdout", run.getOrDefault("stdout", ""));
                    result.put("stderr", run.getOrDefault("stderr", ""));
                    result.put("exitCode", run.getOrDefault("code", -1));
                    result.put("timedOut", Boolean.FALSE);
                } else {
                    // Compilation error case
                    Map<String, Object> compile = (Map<String, Object>) body.get("compile");
                    if (compile != null) {
                        result.put("stdout", "");
                        result.put("stderr", compile.getOrDefault("stderr", compile.getOrDefault("output", "Compilation error")));
                        result.put("exitCode", compile.getOrDefault("code", 1));
                        result.put("timedOut", Boolean.FALSE);
                    }
                }
            } else {
                result.put("stdout", "");
                result.put("stderr", "Sandbox API returned status: " + response.getStatusCode());
                result.put("exitCode", -1);
                result.put("timedOut", Boolean.FALSE);
            }
        } catch (Exception e) {
            log.error("Sandbox execution failed for language={}: {}", language, e.getMessage());
            result.put("stdout", "");
            result.put("stderr", "Execution service unavailable: " + e.getMessage());
            result.put("exitCode", -1);
            result.put("timedOut", Boolean.FALSE);
        }

        return result;
    }

    /** Maps our language names to Piston API language identifiers. */
    private String mapLanguage(String language) {
        return switch (language.toLowerCase()) {
            case "python", "python3" -> "python";
            case "java" -> "java";
            case "javascript", "js" -> "javascript";
            case "c", "c99" -> "c";
            case "cpp", "c++" -> "c++";
            default -> language.toLowerCase();
        };
    }

    /** Returns a reasonable default version for each language. */
    private String getLanguageVersion(String language) {
        return switch (language.toLowerCase()) {
            case "python", "python3" -> "3.10.0";
            case "java" -> "15.0.2";
            case "javascript", "js" -> "18.15.0";
            case "c", "c99" -> "10.2.0";
            case "cpp", "c++" -> "10.2.0";
            default -> "*";
        };
    }
}
