package com.example.blog.service;

import java.util.Map;

/**
 * Service responsible for executing user-submitted code in a safe sandbox environment.
 * Uses the Piston API (https://github.com/engineer-man/piston) for remote execution
 * of Python, Java, and other server-side languages.
 * JavaScript is executed client-side in an isolated iframe sandbox.
 */
public interface SandboxExecutionService {

    /**
     * Executes code with the given language and stdin input.
     *
     * @param language the programming language (e.g., "python", "java", "javascript")
     * @param code     the source code to execute
     * @param stdin    the standard input to provide
     * @return a map with keys "stdout", "stderr", "exitCode", "timedOut"
     */
    Map<String, Object> executeCode(String language, String code, String stdin);
}
