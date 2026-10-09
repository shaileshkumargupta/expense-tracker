package com.expense_tracker.service.ai;

import com.expense_tracker.dto.ai.AiChatResponse;
import com.expense_tracker.dto.ai.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private final GeminiClient geminiClient;
    private final ChatMemoryService chatMemoryService;
    private final FinancialToolsCallback toolsCallback;
    private final ObjectMapper objectMapper;

    /**
     * Executes the conversational AI flow with Redis memory and deterministic tool calling.
     */
    public AiChatResponse chat(String email, String userPrompt) {
        log.info("Processing AI Chat request for user: '{}', prompt: '{}'", email, userPrompt);

        List<String> executedTools = new ArrayList<>();

        // 1. Fetch conversational history from Redis sliding window
        List<ChatMessageDto> history = chatMemoryService.getHistory(email);

        // 2. Build Gemini contents structure
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessageDto msg : history) {
            if ("user".equalsIgnoreCase(msg.getRole())) {
                contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", msg.getContent()))));
            } else if ("model".equalsIgnoreCase(msg.getRole())) {
                contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", msg.getContent()))));
            }
        }

        // Add current user prompt
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt))));

        // Persist user prompt in Redis memory
        chatMemoryService.appendMessage(email, ChatMessageDto.builder()
                .role("user")
                .content(userPrompt)
                .timestamp(LocalDateTime.now())
                .build());

        String finalAssistantMessage;

        try {
            List<Map<String, Object>> tools = toolsCallback.getGeminiTools();
            Map<String, Object> geminiResponse = geminiClient.generateContent(contents, tools);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResponse.get("candidates");
            Map<String, Object> firstCandidate = (candidates != null && !candidates.isEmpty()) ? candidates.get(0) : null;
            Map<String, Object> functionCall = extractFunctionCall(geminiResponse);

            if (functionCall != null && firstCandidate != null) {
                String toolName = (String) functionCall.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> toolArgs = (Map<String, Object>) functionCall.getOrDefault("args", Collections.emptyMap());

                log.info("Gemini invoked tool: '{}' with args: {}", toolName, toolArgs);
                if (!executedTools.contains(toolName)) {
                    executedTools.add(toolName);
                }

                // Execute matched tool callback
                Map<String, Object> toolExecutionResult = executeTool(email, toolName, toolArgs);

                // Add assistant function-call turn (preserving thoughtSignature and call ID)
                @SuppressWarnings("unchecked")
                Map<String, Object> modelContent = (Map<String, Object>) firstCandidate.get("content");
                contents.add(modelContent);

                // Add function response turn (Google Gemini API expects role 'user' for function responses)
                Map<String, Object> functionResponsePart = Map.of(
                        "functionResponse", Map.of(
                                "name", toolName,
                                "response", toolExecutionResult
                        )
                );
                contents.add(Map.of("role", "user", "parts", List.of(functionResponsePart)));

                // Call Gemini again to format conversational response
                Map<String, Object> followupResponse = geminiClient.generateContent(contents, tools);
                finalAssistantMessage = extractTextResponse(followupResponse);

                if (finalAssistantMessage == null || finalAssistantMessage.isBlank()) {
                    finalAssistantMessage = formatToolResultFallback(toolName, toolExecutionResult);
                }
            } else {
                finalAssistantMessage = extractTextResponse(geminiResponse);
                if (finalAssistantMessage == null || finalAssistantMessage.isBlank()) {
                    finalAssistantMessage = "I have processed your request. How else can I assist you with your budget, expenses, income, or categories?";
                }
            }
        } catch (Exception e) {
            log.warn("Gemini AI API call failed [{}]. Reason: {}. Falling back to resilient local NLP intent parser.",
                    e.getClass().getSimpleName(), e.getMessage());
            finalAssistantMessage = handleGracefulFallback(email, userPrompt, executedTools);
        }

        // Persist model response in Redis memory
        chatMemoryService.appendMessage(email, ChatMessageDto.builder()
                .role("model")
                .content(finalAssistantMessage)
                .timestamp(LocalDateTime.now())
                .build());

        return AiChatResponse.builder()
                .response(finalAssistantMessage)
                .executedTools(executedTools)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private Map<String, Object> executeTool(String email, String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "recordExpense" -> toolsCallback.recordExpense(email, args);
            case "getAllExpenses" -> toolsCallback.getAllExpenses(email, args);
            case "recordIncome" -> toolsCallback.recordIncome(email, args);
            case "getAllIncomes" -> toolsCallback.getAllIncomes(email, args);
            case "checkBudgetStatus" -> toolsCallback.checkBudgetStatus(email, args);
            case "setBudget" -> toolsCallback.setBudget(email, args);
            case "getAllCategories" -> toolsCallback.getAllCategories(email, args);
            case "addCategory" -> toolsCallback.addCategory(email, args);
            case "getDashboardOverview" -> toolsCallback.getDashboardOverview(email, args);
            case "getMonthlySummary" -> toolsCallback.getMonthlySummary(email, args);
            default -> Map.of("error", "Unrecognized tool: " + toolName);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFunctionCall(Map<String, Object> response) {
        if (response == null || !response.containsKey("candidates")) {
            return null;
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Map<String, Object> firstCandidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        if (content == null || !content.containsKey("parts")) {
            return null;
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        for (Map<String, Object> part : parts) {
            if (part.containsKey("functionCall")) {
                return (Map<String, Object>) part.get("functionCall");
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractTextResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("candidates")) {
            return null;
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Map<String, Object> firstCandidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        if (content == null || !content.containsKey("parts")) {
            return null;
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> part : parts) {
            if (part.containsKey("text")) {
                sb.append(part.get("text"));
            }
        }

        return sb.toString().trim();
    }

    /**
     * Defensive fallback mechanism if external Gemini service is unreachable or API key is unconfigured.
     * Accurately routes to Expense, Income, Budget, Category, or Dashboard tools based on user intent.
     */
    private String handleGracefulFallback(String email, String userPrompt, List<String> executedTools) {
        String lower = userPrompt.toLowerCase();

        // 1. Categories
        if (lower.contains("categor")) {
            if (lower.contains("add") || lower.contains("create") || lower.contains("new")) {
                String catName = lower.replace("add category", "").replace("create category", "").replace("new category", "").trim();
                if (catName.isBlank()) catName = "Custom Category";
                Map<String, Object> res = toolsCallback.addCategory(email, Map.of("categoryName", catName, "type", "EXPENSE"));
                if (!executedTools.contains("addCategory")) executedTools.add("addCategory");
                return (String) res.get("message");
            } else {
                Map<String, Object> res = toolsCallback.getAllCategories(email, Collections.emptyMap());
                if (!executedTools.contains("getAllCategories")) executedTools.add("getAllCategories");
                return String.format("Here are your categories:\n- **Expense:** %s\n- **Income:** %s",
                        res.get("expenseCategories"), res.get("incomeCategories"));
            }
        }

        // 2. Incomes
        if (lower.contains("income") || lower.contains("salary") || lower.contains("earnings") || lower.contains("earned")) {
            if (lower.contains("received") || lower.contains("got") || lower.contains("add") || lower.contains("earned") || lower.contains("credited")) {
                Double amount = extractAmountFromPrompt(userPrompt);
                String source = lower.contains("salary") ? "Salary" : (lower.contains("freelance") ? "Freelance" : "General Income");
                Map<String, Object> res = toolsCallback.recordIncome(email, Map.of(
                        "amount", amount,
                        "source", source,
                        "description", userPrompt
                ));
                if (!executedTools.contains("recordIncome")) executedTools.add("recordIncome");
                return "I've recorded your income: " + res.get("message");
            } else {
                Map<String, Object> res = toolsCallback.getAllIncomes(email, Collections.emptyMap());
                if (!executedTools.contains("getAllIncomes")) executedTools.add("getAllIncomes");
                return formatIncomeResult(res);
            }
        }

        // 3. Budget
        if (lower.contains("budget")) {
            if (lower.contains("set") || lower.contains("update") || lower.contains("create")) {
                Double amount = extractAmountFromPrompt(userPrompt);
                LocalDate now = LocalDate.now();
                Map<String, Object> res = toolsCallback.setBudget(email, Map.of(
                        "amount", amount,
                        "month", now.getMonthValue(),
                        "year", now.getYear()
                ));
                if (!executedTools.contains("setBudget")) executedTools.add("setBudget");
                return (String) res.get("message");
            } else {
                LocalDate now = LocalDate.now();
                Map<String, Object> budgetStatus = toolsCallback.checkBudgetStatus(email, Map.of(
                        "month", now.getMonthValue(),
                        "year", now.getYear()
                ));
                if (!executedTools.contains("checkBudgetStatus")) executedTools.add("checkBudgetStatus");
                if (Boolean.TRUE.equals(budgetStatus.get("success"))) {
                    return String.format("Budget check for %s/%s:\n- **Allowance:** ₹%.2f\n- **Total Spent:** ₹%.2f\n- **Remaining:** ₹%.2f\n- **Status:** %s",
                            budgetStatus.get("month"), budgetStatus.get("year"), budgetStatus.get("budgetAmount"),
                            budgetStatus.get("totalExpense"), budgetStatus.get("remainingAmount"), budgetStatus.get("status"));
                } else {
                    return (String) budgetStatus.get("message");
                }
            }
        }

        // 4. Dashboard / Balance / Financial Overview
        if (lower.contains("dashboard") || lower.contains("balance") || lower.contains("overview") || lower.contains("financial status")) {
            LocalDate now = LocalDate.now();
            Map<String, Object> dashboard = toolsCallback.getDashboardOverview(email, Map.of(
                    "month", now.getMonthValue(),
                    "year", now.getYear()
            ));
            if (!executedTools.contains("getDashboardOverview")) executedTools.add("getDashboardOverview");
            return String.format("Consolidated Dashboard (%s/%s):\n- **Total Income:** ₹%.2f\n- **Total Expense:** ₹%.2f\n- **Net Balance:** ₹%.2f\n- **Monthly Budget:** ₹%.2f (Remaining: ₹%.2f)",
                    dashboard.get("month"), dashboard.get("year"), dashboard.get("totalIncome"),
                    dashboard.get("totalExpense"), dashboard.get("balance"), dashboard.get("budget"),
                    dashboard.get("remainingBudget"));
        }

        // 5. Querying Expenses (optionally filtered by month)
        if (lower.contains("all expenses") || lower.contains("show expenses") || lower.contains("get expenses") ||
                lower.contains("list expenses") || lower.contains("give me all expenses") || lower.contains("view expenses") ||
                lower.contains("what expenses") || lower.contains("what did i spend") || lower.contains("my expenses")) {

            LocalDate now = LocalDate.now();
            Map<String, Object> queryArgs = new HashMap<>();
            boolean isThisMonth = lower.contains("this month") || lower.contains("current month") || lower.contains("month");
            if (isThisMonth) {
                queryArgs.put("month", now.getMonthValue());
                queryArgs.put("year", now.getYear());
            }

            Map<String, Object> expensesResult = toolsCallback.getAllExpenses(email, queryArgs);
            if (!executedTools.contains("getAllExpenses")) executedTools.add("getAllExpenses");

            String period = isThisMonth ? (now.getMonth().name() + " " + now.getYear()) : "All-Time";
            return formatExpenseResult(expensesResult, period);
        }

        // 6. Monthly Summary / Report
        if (lower.contains("summary") || lower.contains("report")) {
            LocalDate now = LocalDate.now();
            Map<String, Object> summary = toolsCallback.getMonthlySummary(email, Map.of(
                    "month", now.getMonthValue(),
                    "year", now.getYear()
            ));
            if (!executedTools.contains("getMonthlySummary")) executedTools.add("getMonthlySummary");
            return String.format("Monthly Financial Summary (%s/%s):\n- **Income:** ₹%.2f\n- **Expenses:** ₹%.2f\n- **Net Savings:** ₹%.2f\n- **Category Breakdown:** %s",
                    summary.get("month"), summary.get("year"), summary.get("totalIncome"), summary.get("totalExpense"),
                    summary.get("netSavings"), summary.get("categoryWiseExpense"));
        }

        // 7. Spend actions (creating an expense)
        if (lower.contains("spent") || lower.contains("paid") || lower.contains("bought") ||
                lower.contains("purchased") || lower.contains("add expense") || lower.contains("new expense")) {

            Double amount = extractAmountFromPrompt(userPrompt);
            String category = detectCategoryFromPrompt(lower);
            String title = detectTitleFromPrompt(userPrompt, category);

            Map<String, Object> result = toolsCallback.recordExpense(email, Map.of(
                    "amount", amount,
                    "categoryName", category,
                    "title", title,
                    "description", userPrompt
            ));
            if (!executedTools.contains("recordExpense")) executedTools.add("recordExpense");
            return "I've recorded your transaction: " + result.get("message");
        }

        return "I am your AI Financial Assistant. You can manage Expenses (record or list), Income (add or view), Budget (set or check status), Categories, or ask for a consolidated Dashboard overview!";
    }

    @SuppressWarnings("unchecked")
    private String formatExpenseResult(Map<String, Object> res, String period) {
        Number totalSpentNum = (Number) res.getOrDefault("totalSpent", 0.0);
        double totalSpent = totalNumToDouble(totalSpentNum);
        int count = res.get("totalCount") instanceof Number n ? n.intValue() : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Here are your expenses for %s:\n\n", period));
        sb.append(String.format("- **Total Spent:** ₹%.2f across **%d** transactions\n\n", totalSpent, count));

        Map<String, ?> categories = (Map<String, ?>) res.get("categoryBreakdown");
        if (categories != null && !categories.isEmpty()) {
            sb.append("**Category Breakdown:**\n");
            categories.forEach((cat, amt) -> {
                double val = totalNumToDouble(amt);
                sb.append(String.format("- %s: ₹%.2f\n", cat, val));
            });
            sb.append("\n");
        }

        List<Map<String, Object>> recent = (List<Map<String, Object>>) res.get("recentTransactions");
        if (recent != null && !recent.isEmpty()) {
            sb.append("**Recent Transactions:**\n");
            for (Map<String, Object> item : recent) {
                sb.append(String.format("- **%s**: ₹%s *(Category: %s)*\n",
                        item.get("title"), item.get("amount"), item.get("category")));
            }
        }

        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String formatIncomeResult(Map<String, Object> res) {
        Number totalIncomeNum = (Number) res.getOrDefault("totalIncome", 0.0);
        double totalIncome = totalNumToDouble(totalIncomeNum);
        int count = res.get("totalCount") instanceof Number n ? n.intValue() : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Here is your recorded income:\n\n- **Total Income:** ₹%.2f across **%d** transactions\n\n", totalIncome, count));

        Map<String, ?> breakdown = (Map<String, ?>) res.get("sourceBreakdown");
        if (breakdown != null && !breakdown.isEmpty()) {
            sb.append("**Source Breakdown:**\n");
            breakdown.forEach((src, amt) -> {
                double val = totalNumToDouble(amt);
                sb.append(String.format("- %s: ₹%.2f\n", src, val));
            });
            sb.append("\n");
        }

        List<Map<String, Object>> recent = (List<Map<String, Object>>) res.get("recentIncomes");
        if (recent != null && !recent.isEmpty()) {
            sb.append("**Recent Incomes:**\n");
            for (Map<String, Object> item : recent) {
                sb.append(String.format("- **%s**: ₹%s *(Source: %s)*\n",
                        item.get("source"), item.get("amount"), item.get("source")));
            }
        }

        return sb.toString().trim();
    }

    private String formatToolResultFallback(String toolName, Map<String, Object> toolExecutionResult) {
        return switch (toolName) {
            case "getAllExpenses" -> formatExpenseResult(toolExecutionResult, "Current Period");
            case "getAllIncomes" -> formatIncomeResult(toolExecutionResult);
            case "recordExpense", "recordIncome", "setBudget", "addCategory" ->
                    (String) toolExecutionResult.getOrDefault("message", "Operation completed successfully.");
            case "checkBudgetStatus" ->
                    String.format("Budget check for %s/%s: Allowance: ₹%s, Spent: ₹%s, Remaining: ₹%s, Status: %s",
                            toolExecutionResult.get("month"), toolExecutionResult.get("year"),
                            toolExecutionResult.get("budgetAmount"), toolExecutionResult.get("totalExpense"),
                            toolExecutionResult.get("remainingAmount"), toolExecutionResult.get("status"));
            case "getDashboardOverview" ->
                    String.format("Dashboard Overview: Income: ₹%s, Expenses: ₹%s, Balance: ₹%s, Budget: ₹%s",
                            toolExecutionResult.get("totalIncome"), toolExecutionResult.get("totalExpense"),
                            toolExecutionResult.get("balance"), toolExecutionResult.get("budget"));
            case "getMonthlySummary" ->
                    String.format("Monthly Summary: Income: ₹%s, Expenses: ₹%s, Net Savings: ₹%s",
                            toolExecutionResult.get("totalIncome"), toolExecutionResult.get("totalExpense"),
                            toolExecutionResult.get("netSavings"));
            default -> {
                try {
                    yield objectMapper.writeValueAsString(toolExecutionResult);
                } catch (Exception e) {
                    yield "Operation completed.";
                }
            }
        };
    }

    private double totalNumToDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj != null) {
            try {
                return Double.parseDouble(obj.toString());
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private Double extractAmountFromPrompt(String prompt) {
        Pattern pattern = Pattern.compile("(?i)(?:(?:rs\\.?|inr|₹|\\$)\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:rupees|rs\\.?|inr|dollars|bucks|eur)?|(?:spent|paid|cost|earned|received|budget)\\s+(\\d+(?:\\.\\d+)?))");
        Matcher matcher = pattern.matcher(prompt);
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String val = matcher.group(i);
                if (val != null && !val.isBlank()) {
                    try {
                        double parsed = Double.parseDouble(val);
                        if (parsed > 0) return parsed;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        Matcher genericMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(prompt);
        if (genericMatcher.find()) {
            try {
                return Double.parseDouble(genericMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private String detectCategoryFromPrompt(String lower) {
        if (lower.contains("chocolate") || lower.contains("lunch") || lower.contains("dinner") ||
                lower.contains("breakfast") || lower.contains("food") || lower.contains("coffee") ||
                lower.contains("tea") || lower.contains("snack") || lower.contains("burger") ||
                lower.contains("pizza") || lower.contains("restaurant") || lower.contains("cafe")) {
            return "Food";
        }
        if (lower.contains("uber") || lower.contains("ola") || lower.contains("metro") ||
                lower.contains("cab") || lower.contains("bus") || lower.contains("train") ||
                lower.contains("flight") || lower.contains("petrol") || lower.contains("fuel") ||
                lower.contains("travel") || lower.contains("auto")) {
            return "Transport";
        }
        if (lower.contains("grocery") || lower.contains("groceries") || lower.contains("milk") ||
                lower.contains("vegetable") || lower.contains("fruit") || lower.contains("market") ||
                lower.contains("supermarket")) {
            return "Groceries";
        }
        if (lower.contains("movie") || lower.contains("netflix") || lower.contains("cinema") ||
                lower.contains("game") || lower.contains("concert") || lower.contains("outing")) {
            return "Entertainment";
        }
        if (lower.contains("bill") || lower.contains("electricity") || lower.contains("water") ||
                lower.contains("wifi") || lower.contains("internet") || lower.contains("recharge") ||
                lower.contains("rent")) {
            return "Utilities";
        }
        return "General";
    }

    private String detectTitleFromPrompt(String prompt, String category) {
        String lower = prompt.toLowerCase();
        if (lower.contains("chocolate")) return "Chocolates";
        if (lower.contains("lunch")) return "Lunch";
        if (lower.contains("dinner")) return "Dinner";
        if (lower.contains("breakfast")) return "Breakfast";
        if (lower.contains("coffee")) return "Coffee";
        if (lower.contains("tea")) return "Tea";
        if (lower.contains("snack")) return "Snacks";
        if (lower.contains("petrol") || lower.contains("fuel")) return "Fuel";
        if (lower.contains("uber") || lower.contains("ola") || lower.contains("cab")) return "Cab Ride";
        if (lower.contains("groceries") || lower.contains("grocery")) return "Groceries";
        return category + " Expense";
    }
}
