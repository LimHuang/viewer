package com.syspilot.viewer.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Generates test trajectory JSON files on the desktop.
 * Run: mvn exec:java -Dexec.mainClass="com.syspilot.viewer.utility.TestDataGenerator"
 */
public class TestDataGenerator {

    private static final String DESKTOP = System.getProperty("user.home") + "/Desktop/";
    private static final Random RND = new Random(42);
    private static final String[] TOOL_NAMES = {
            "read_file", "write_file", "execute_bash", "search_code",
            "list_directory", "grep_pattern", "web_fetch", "web_search",
            "run_tests", "git_diff", "git_log", "edit_file"
    };
    private static final String[] PROBLEMS = {
            "Implement user authentication with JWT tokens",
            "Fix race condition in database connection pool",
            "Add pagination to the search API endpoint",
            "Refactor the logging system to use structured logging",
            "Implement real-time notification system using WebSockets",
            "Optimize database queries for the reporting dashboard",
            "Migrate the frontend from class components to hooks",
            "Add rate limiting to the public API endpoints"
    };

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        System.out.println("Generating test trajectory files...");

        // 5 large files (200+ steps)
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> traj = generateLargeTrajectory(i);
            mapper.writeValue(new File(DESKTOP + "large_trajectory_" + i + ".json"), traj);
            System.out.println("  Created large_trajectory_" + i + ".json");
        }

        // 5 deep-nested files (~50 nodes, deep nesting)
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> traj = generateDeepTrajectory(i);
            mapper.writeValue(new File(DESKTOP + "deep_trajectory_" + i + ".json"), traj);
            System.out.println("  Created deep_trajectory_" + i + ".json");
        }

        System.out.println("Done! Files are on your desktop.");
    }

    private static Map<String, Object> generateLargeTrajectory(int index) {
        Map<String, Object> traj = new LinkedHashMap<>();
        traj.put("schema_version", "1.0");
        traj.put("task_id", "large-task-" + index);
        traj.put("problem", PROBLEMS[index - 1]);
        traj.put("start_time", "2026-05-27T10:00:00Z");
        traj.put("end_time", "2026-05-27T10:15:00Z");
        traj.put("execution_time_seconds", 600 + RND.nextDouble() * 300);
        traj.put("success", RND.nextBoolean());

        int numSteps = 200 + RND.nextInt(51); // 200-250 steps
        List<Map<String, Object>> steps = new ArrayList<>();

        for (int s = 1; s <= numSteps; s++) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("step_id", s);

            if (s % 5 == 1 || s % 5 == 3) {
                step.put("type", "user");
                step.put("timestamp", "2026-05-27T10:" + String.format("%02d", (s / 4) % 60) + ":00Z");
                step.put("message", generateUserMessage(s));
            } else {
                step.put("type", "agent");
                step.put("timestamp", "2026-05-27T10:" + String.format("%02d", (s / 4) % 60) + ":05Z");
                step.put("reasoning", generateReasoning(s));
                step.put("message", "Agent step " + s + " completed");

                Map<String, Object> modelInfo = new LinkedHashMap<>();
                modelInfo.put("model_name", "claude-sonnet-4-6");
                modelInfo.put("input_tokens", 500 + RND.nextInt(5000));
                modelInfo.put("output_tokens", 100 + RND.nextInt(2000));
                step.put("model_info", modelInfo);

                // Add tool calls
                int numTools = RND.nextInt(4);
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (int t = 0; t < numTools; t++) {
                    Map<String, Object> tc = new LinkedHashMap<>();
                    String toolName = TOOL_NAMES[RND.nextInt(TOOL_NAMES.length)];
                    tc.put("tool_call_id", "tc-" + s + "-" + t);
                    tc.put("tool_name", toolName);
                    tc.put("duration_ms", 50.0 + RND.nextDouble() * 2000);
                    tc.put("result", "Result of " + toolName + " for step " + s);
                    toolCalls.add(tc);
                }

                // Add sub-agents every ~40 steps
                if (s % 40 == 0 && s + 5 <= numSteps) {
                    Map<String, Object> tc = new LinkedHashMap<>();
                    tc.put("tool_call_id", "tc-" + s + "-sub");
                    tc.put("tool_name", "spawn_subagent");
                    tc.put("duration_ms", 100.0 + RND.nextDouble() * 500);

                    Map<String, Object> subagent = new LinkedHashMap<>();
                    subagent.put("agentId", "sub-" + s);
                    subagent.put("label", "SubAgent-" + (s / 40));
                    subagent.put("parentAgentId", "main");
                    subagent.put("status", "completed");

                    List<Map<String, Object>> subSteps = new ArrayList<>();
                    for (int ss = 1; ss <= 3 + RND.nextInt(5); ss++) {
                        subSteps.add(createSimpleStep(s * 1000 + ss, "agent", "Sub-agent step " + ss));
                    }
                    subagent.put("steps", subSteps);
                    tc.put("subagent", subagent);
                    toolCalls.add(tc);
                }

                step.put("tool_calls", toolCalls);
                step.put("step_duration_ms", 200.0 + RND.nextDouble() * 5000);
            }

            steps.add(step);
        }

        traj.put("steps", steps);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_steps", numSteps);
        summary.put("subagent_count", numSteps / 40);
        summary.put("total_tokens_in", numSteps * 800);
        summary.put("total_tokens_out", numSteps * 400);
        summary.put("execution_time_seconds", 600.0);
        traj.put("summary", summary);

        return traj;
    }

    private static Map<String, Object> generateDeepTrajectory(int index) {
        Map<String, Object> traj = new LinkedHashMap<>();
        traj.put("schema_version", "1.0");
        traj.put("task_id", "deep-task-" + index);
        traj.put("problem", "Deep nesting test: " + PROBLEMS[index - 1]);
        traj.put("start_time", "2026-05-27T12:00:00Z");
        traj.put("end_time", "2026-05-27T12:05:00Z");
        traj.put("execution_time_seconds", 200 + RND.nextDouble() * 100);
        traj.put("success", true);

        List<Map<String, Object>> steps = new ArrayList<>();
        int stepCounter = 0;

        // Main agent steps
        for (int s = 1; s <= 8; s++) {
            stepCounter++;
            Map<String, Object> step = createAgentStepWithSubagent(stepCounter, s, 4 + index % 3);
            steps.add(step);
        }

        traj.put("steps", steps);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_steps", stepCounter);
        summary.put("subagent_count", 8);
        traj.put("summary", summary);

        return traj;
    }

    /**
     * Creates an agent step with a deeply nested sub-agent chain.
     * @param nestingDepth remaining nesting levels (decrements recursively)
     */
    private static Map<String, Object> createAgentStepWithSubagent(int stepId, int displayId, int nestingDepth) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("step_id", stepId);
        step.put("type", "agent");
        step.put("timestamp", "2026-05-27T12:00:" + String.format("%02d", stepId % 60) + "Z");
        step.put("reasoning", "Processing deep nesting level " + nestingDepth);
        step.put("message", "Agent step completed at depth " + nestingDepth);

        Map<String, Object> modelInfo = new LinkedHashMap<>();
        modelInfo.put("model_name", "claude-sonnet-4-6");
        modelInfo.put("input_tokens", 100 + RND.nextInt(500));
        modelInfo.put("output_tokens", 50 + RND.nextInt(300));
        step.put("model_info", modelInfo);

        List<Map<String, Object>> toolCalls = new ArrayList<>();

        // Regular tool call
        Map<String, Object> tc1 = new LinkedHashMap<>();
        tc1.put("tool_call_id", "tc-" + stepId + "-1");
        tc1.put("tool_name", TOOL_NAMES[RND.nextInt(TOOL_NAMES.length)]);
        tc1.put("duration_ms", 10.0 + RND.nextDouble() * 100);
        tc1.put("result", "Quick operation result for step " + stepId);
        toolCalls.add(tc1);

        // Sub-agent spawning (recursive nesting)
        if (nestingDepth > 0) {
            Map<String, Object> tcSub = new LinkedHashMap<>();
            tcSub.put("tool_call_id", "tc-" + stepId + "-sub");
            tcSub.put("tool_name", "spawn_subagent");
            tcSub.put("duration_ms", 50.0 + RND.nextDouble() * 200);

            Map<String, Object> subagent = new LinkedHashMap<>();
            subagent.put("agentId", "sub-l" + nestingDepth + "-" + stepId);
            subagent.put("label", "SubAgent-L" + nestingDepth + "-" + displayId);
            subagent.put("parentAgentId", "parent-" + stepId);
            subagent.put("status", "completed");

            List<Map<String, Object>> subSteps = new ArrayList<>();
            int subStepCounter = stepId * 100;

            // 2 steps before deeper nesting
            subSteps.add(createSimpleStep(++subStepCounter, "agent",
                    "Sub-agent analysis at depth " + nestingDepth));
            subSteps.add(createSimpleStep(++subStepCounter, "user",
                    "User instruction at depth " + nestingDepth));

            // Recursive: one step with further nesting
            subSteps.add(createAgentStepWithSubagent(++subStepCounter, displayId, nestingDepth - 1));

            // 2 more steps after deeper nesting
            subSteps.add(createSimpleStep(++subStepCounter, "agent",
                    "Sub-agent summary at depth " + nestingDepth));
            subSteps.add(createSimpleStep(++subStepCounter, "agent",
                    "Sub-agent finalizing at depth " + nestingDepth));

            subagent.put("steps", subSteps);
            tcSub.put("subagent", subagent);
            toolCalls.add(tcSub);
        }

        step.put("tool_calls", toolCalls);
        step.put("step_duration_ms", 100.0 + RND.nextDouble() * 500);

        return step;
    }

    private static Map<String, Object> createSimpleStep(int stepId, String type, String message) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("step_id", stepId);
        step.put("type", type);
        step.put("timestamp", "2026-05-27T12:00:" + String.format("%02d", stepId % 60) + "Z");
        step.put("message", message);

        if ("agent".equals(type)) {
            Map<String, Object> modelInfo = new LinkedHashMap<>();
            modelInfo.put("model_name", "claude-sonnet-4-6");
            modelInfo.put("input_tokens", 50 + RND.nextInt(200));
            modelInfo.put("output_tokens", 20 + RND.nextInt(100));
            step.put("model_info", modelInfo);
            step.put("step_duration_ms", 50.0 + RND.nextDouble() * 200);
        }
        return step;
    }

    private static String generateUserMessage(int stepNum) {
        String[] messages = {
                "Can you analyze this code?",
                "Please fix the bug in the authentication module.",
                "Add unit tests for the new feature.",
                "Refactor the database access layer.",
                "Explain how the caching mechanism works.",
                "Review the PR for the payment integration.",
                "Document the API endpoints.",
                "Optimize the build process.",
                "Set up CI/CD pipeline.",
                "Investigate the memory leak in production."
        };
        return messages[stepNum % messages.length] + " (step " + stepNum + ")";
    }

    private static String generateReasoning(int stepNum) {
        String[] reasoning = {
                "Looking at the code structure, I need to understand the data flow first.",
                "The issue appears to be related to connection pooling limits.",
                "I should check the test coverage before making changes.",
                "Let me examine the query execution plan to find the bottleneck.",
                "The architecture suggests using an event-driven approach.",
                "I'll trace the request lifecycle to identify the slow component.",
                "The error logs indicate a pattern of timeout failures.",
                "Cross-referencing with the API documentation reveals the mismatch."
        };
        return reasoning[stepNum % reasoning.length] + " Continuing analysis for step " + stepNum + ".";
    }
}
