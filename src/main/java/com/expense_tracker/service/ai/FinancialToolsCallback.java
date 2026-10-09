package com.expense_tracker.service.ai;

import com.expense_tracker.dto.budget.BudgetRequest;
import com.expense_tracker.dto.budget.BudgetSummaryResponse;
import com.expense_tracker.dto.category.CategoryRequest;
import com.expense_tracker.dto.category.CategoryResponse;
import com.expense_tracker.dto.dashboard.DashboardResponse;
import com.expense_tracker.dto.expense.ExpenseRequest;
import com.expense_tracker.dto.expense.ExpenseResponse;
import com.expense_tracker.dto.income.IncomeRequest;
import com.expense_tracker.dto.income.IncomeResponse;
import com.expense_tracker.dto.income.IncomeSummaryResponse;
import com.expense_tracker.dto.report.MonthlyReportResponse;
import com.expense_tracker.entity.category.Category;
import com.expense_tracker.entity.category.CategoryType;
import com.expense_tracker.entity.expense.Expense;
import com.expense_tracker.entity.user.User;
import com.expense_tracker.exception.ETMException;
import com.expense_tracker.repository.category.CategoryRepository;
import com.expense_tracker.repository.expense.ExpenseRepository;
import com.expense_tracker.repository.user.UserRepository;
import com.expense_tracker.service.budget.BudgetService;
import com.expense_tracker.service.category.CategoryService;
import com.expense_tracker.service.dashboard.DashboardService;
import com.expense_tracker.service.expense.ExpenseService;
import com.expense_tracker.service.income.IncomeService;
import com.expense_tracker.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialToolsCallback {

    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // ==========================================
    // 1. EXPENSE TOOLS
    // ==========================================

    public Map<String, Object> recordExpense(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [recordExpense] for user: {} with args: {}", email, args);
        try {
            Double amount = parseDouble(args.get("amount"));
            if (amount == null || amount <= 0) {
                return Map.of("success", false, "error", "Invalid or missing expense amount.");
            }

            String categoryName = args.get("categoryName") != null ? args.get("categoryName").toString().trim() : "General";
            String title = args.get("title") != null ? args.get("title").toString().trim() : categoryName + " Expense";
            String description = args.get("description") != null ? args.get("description").toString().trim() : title;

            // Ensure category exists for user
            ensureCategoryExists(email, categoryName, CategoryType.EXPENSE);

            ExpenseRequest request = new ExpenseRequest();
            request.setAmount(amount);
            request.setCategory(categoryName);
            request.setTitle(title);
            request.setDescription(description);

            expenseService.addExpense(request, email);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", String.format("Recorded expense of %.2f for '%s' under '%s'.", amount, title, categoryName));
            result.put("amount", amount);
            result.put("category", categoryName);
            result.put("title", title);
            result.put("recordedAt", LocalDate.now().toString());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute recordExpense tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not record expense: " + e.getMessage());
        }
    }

    public Map<String, Object> getAllExpenses(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [getAllExpenses] for user: {} with args: {}", email, args);
        try {
            Integer month = args.get("month") != null ? parseInteger(args.get("month"), null) : null;
            Integer year = args.get("year") != null ? parseInteger(args.get("year"), null) : null;

            List<ExpenseResponse> expenseResponses;
            if (month != null && month > 0 && year != null && year > 0) {
                User user = userRepository.findByEmailId(email)
                        .orElseThrow(() -> new ETMException(404, "User not found with email: " + email));
                List<Expense> expenses = expenseRepository.findByUserAndMonthAndYear(user, month, year);
                expenseResponses = expenses.stream().map(e -> {
                    ExpenseResponse resp = new ExpenseResponse();
                    resp.setId(e.getId());
                    resp.setTitle(e.getTitle());
                    resp.setAmount(e.getAmount());
                    resp.setCategory(e.getCategory());
                    resp.setDateTime(e.getDateTime() != null ? e.getDateTime().toString() : null);
                    resp.setDescription(e.getDescription());
                    return resp;
                }).toList();
            } else {
                expenseResponses = expenseService.getExpenses(email);
            }

            double totalSpent = expenseResponses.stream().mapToDouble(ExpenseResponse::getAmount).sum();

            Map<String, Double> categoryBreakdown = expenseResponses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getCategory() != null ? e.getCategory() : "General",
                            LinkedHashMap::new,
                            Collectors.summingDouble(ExpenseResponse::getAmount)
                    ));

            List<Map<String, Object>> recentExpenses = expenseResponses.stream()
                    .sorted((a, b) -> {
                        if (a.getId() != null && b.getId() != null) return b.getId().compareTo(a.getId());
                        return 0;
                    })
                    .limit(10)
                    .map(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", e.getId());
                        item.put("title", e.getTitle());
                        item.put("amount", e.getAmount());
                        item.put("category", e.getCategory());
                        item.put("date", e.getDateTime());
                        return item;
                    })
                    .toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            if (month != null && month > 0 && year != null && year > 0) {
                result.put("month", month);
                result.put("year", year);
            }
            result.put("totalCount", expenseResponses.size());
            result.put("totalSpent", totalSpent);
            result.put("categoryBreakdown", categoryBreakdown);
            result.put("recentTransactions", recentExpenses);
            result.put("expenses", recentExpenses);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute getAllExpenses tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Failed to retrieve expenses: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. INCOME TOOLS
    // ==========================================

    public Map<String, Object> recordIncome(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [recordIncome] for user: {} with args: {}", email, args);
        try {
            Double amount = parseDouble(args.get("amount"));
            if (amount == null || amount <= 0) {
                return Map.of("success", false, "error", "Invalid or missing income amount.");
            }

            String source = args.get("source") != null ? args.get("source").toString().trim() : "Salary";
            String description = args.get("description") != null ? args.get("description").toString().trim() : source + " Income";

            IncomeRequest request = new IncomeRequest();
            request.setAmount(amount);
            request.setSource(source);
            request.setDescription(description);
            request.setDateTime(LocalDate.now());

            incomeService.addIncome(request, email);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", String.format("Recorded income of %.2f from '%s' (%s).", amount, source, description));
            result.put("amount", amount);
            result.put("source", source);
            result.put("recordedAt", LocalDate.now().toString());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute recordIncome tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not record income: " + e.getMessage());
        }
    }

    public Map<String, Object> getAllIncomes(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [getAllIncomes] for user: {}", email);
        try {
            List<IncomeResponse> incomes = incomeService.getIncomes(email);
            IncomeSummaryResponse summary = incomeService.getIncomeSummaryReport(email);

            List<Map<String, Object>> recentIncomes = incomes.stream()
                    .sorted((a, b) -> {
                        if (a.getId() != null && b.getId() != null) return b.getId().compareTo(a.getId());
                        return 0;
                    })
                    .limit(10)
                    .map(inc -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", inc.getId());
                        item.put("source", inc.getSource());
                        item.put("amount", inc.getAmount());
                        item.put("date", inc.getDateTime() != null ? inc.getDateTime().toString() : "");
                        return item;
                    })
                    .toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("totalCount", incomes.size());
            result.put("totalIncome", summary.getTotalAmount());
            result.put("sourceBreakdown", summary.getSourceSummary());
            result.put("recentIncomes", recentIncomes);
            result.put("incomes", recentIncomes);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute getAllIncomes tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Failed to retrieve incomes: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. BUDGET TOOLS
    // ==========================================

    public Map<String, Object> checkBudgetStatus(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [checkBudgetStatus] for user: {} with args: {}", email, args);
        try {
            LocalDate now = LocalDate.now();
            int month = args.get("month") != null ? parseInteger(args.get("month"), now.getMonthValue()) : now.getMonthValue();
            int year = args.get("year") != null ? parseInteger(args.get("year"), now.getYear()) : now.getYear();

            try {
                BudgetSummaryResponse summary = budgetService.getBudgetSummary(month, year, email);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("month", month);
                result.put("year", year);
                result.put("budgetAmount", summary.getBudgetAmount());
                result.put("totalExpense", summary.getTotalExpense());
                result.put("remainingAmount", summary.getRemainingAmount());
                result.put("status", summary.getStatus());
                return result;
            } catch (ETMException etm) {
                return Map.of(
                        "success", false,
                        "status", "NO_BUDGET_CONFIGURED",
                        "message", "No budget has been configured for month " + month + "/" + year + "."
                );
            }
        } catch (Exception e) {
            log.error("Failed to execute checkBudgetStatus tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not check budget status: " + e.getMessage());
        }
    }

    public Map<String, Object> setBudget(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [setBudget] for user: {} with args: {}", email, args);
        try {
            Double amount = parseDouble(args.get("amount"));
            if (amount == null || amount <= 0) {
                return Map.of("success", false, "error", "Invalid or missing budget amount.");
            }

            LocalDate now = LocalDate.now();
            int month = args.get("month") != null ? parseInteger(args.get("month"), now.getMonthValue()) : now.getMonthValue();
            int year = args.get("year") != null ? parseInteger(args.get("year"), now.getYear()) : now.getYear();

            BudgetRequest request = new BudgetRequest();
            request.setAmount(amount);
            request.setMonth(month);
            request.setYear(year);

            budgetService.setBudget(request, email);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", String.format("Successfully configured budget of %.2f for %d/%d.", amount, month, year));
            result.put("budgetAmount", amount);
            result.put("month", month);
            result.put("year", year);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute setBudget tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not set budget: " + e.getMessage());
        }
    }

    // ==========================================
    // 4. CATEGORY TOOLS
    // ==========================================

    public Map<String, Object> getAllCategories(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [getAllCategories] for user: {}", email);
        try {
            List<CategoryResponse> expenseCats = categoryService.getCategories(CategoryType.EXPENSE, email);
            List<CategoryResponse> incomeCats = categoryService.getCategories(CategoryType.INCOME, email);

            List<String> expenseNames = expenseCats.stream().map(CategoryResponse::getCategoryName).toList();
            List<String> incomeNames = incomeCats.stream().map(CategoryResponse::getCategoryName).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("expenseCategories", expenseNames);
            result.put("incomeCategories", incomeNames);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute getAllCategories tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not retrieve categories: " + e.getMessage());
        }
    }

    public Map<String, Object> addCategory(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [addCategory] for user: {} with args: {}", email, args);
        try {
            String categoryName = args.get("categoryName") != null ? args.get("categoryName").toString().trim() : "";
            if (categoryName.isBlank()) {
                return Map.of("success", false, "error", "Category name cannot be empty.");
            }

            String typeStr = args.get("type") != null ? args.get("type").toString().toUpperCase() : "EXPENSE";
            CategoryType type = "INCOME".equalsIgnoreCase(typeStr) ? CategoryType.INCOME : CategoryType.EXPENSE;

            ensureCategoryExists(email, categoryName, type);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", String.format("Category '%s' of type '%s' is ready to use.", categoryName, type));
            result.put("categoryName", categoryName);
            result.put("type", type.name());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute addCategory tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not add category: " + e.getMessage());
        }
    }

    // ==========================================
    // 5. DASHBOARD & OVERVIEW TOOLS
    // ==========================================

    public Map<String, Object> getDashboardOverview(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [getDashboardOverview] for user: {} with args: {}", email, args);
        try {
            LocalDate now = LocalDate.now();
            int month = args.get("month") != null ? parseInteger(args.get("month"), now.getMonthValue()) : now.getMonthValue();
            int year = args.get("year") != null ? parseInteger(args.get("year"), now.getYear()) : now.getYear();

            DashboardResponse dashboard = dashboardService.getDashBoard(month, year, email);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("month", month);
            result.put("year", year);
            result.put("totalIncome", dashboard.getTotalIncome());
            result.put("totalExpense", dashboard.getTotalExpense());
            result.put("balance", dashboard.getBalance());
            result.put("budget", dashboard.getBudget());
            result.put("remainingBudget", dashboard.getRemainingBudget());
            result.put("expenseCategorySummary", dashboard.getExpenseCategorySummary());
            result.put("incomeSourceSummary", dashboard.getIncomeSourceSummary());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute getDashboardOverview tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not retrieve dashboard overview: " + e.getMessage());
        }
    }

    public Map<String, Object> getMonthlySummary(String email, Map<String, Object> args) {
        log.info("Executing AI Tool [getMonthlySummary] for user: {} with args: {}", email, args);
        try {
            LocalDate now = LocalDate.now();
            int month = args.get("month") != null ? parseInteger(args.get("month"), now.getMonthValue()) : now.getMonthValue();
            int year = args.get("year") != null ? parseInteger(args.get("year"), now.getYear()) : now.getYear();

            MonthlyReportResponse report = reportService.getMonthlyReport(email, month, year);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("month", month);
            result.put("year", year);
            result.put("totalIncome", report.getTotalIncome());
            result.put("totalExpense", report.getTotalExpense());
            result.put("netSavings", report.getNetSavings());
            result.put("categoryWiseExpense", report.getCategoryWiseExpense());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute getMonthlySummary tool: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Could not generate monthly summary: " + e.getMessage());
        }
    }

    // ==========================================
    // GEMINI FUNCTION DEFINITIONS SCHEMA
    // ==========================================

    public List<Map<String, Object>> getGeminiTools() {
        List<Map<String, Object>> declarations = new ArrayList<>();

        declarations.add(createFunctionDeclaration("recordExpense",
                "Records a new expense transaction in the expense tracker",
                Map.of(
                        "amount", Map.of("type", "NUMBER", "description", "The numerical expense amount spent"),
                        "categoryName", Map.of("type", "STRING", "description", "Category such as Food, Shopping, Transport, Rent, Groceries, Utilities, Entertainment, General"),
                        "title", Map.of("type", "STRING", "description", "Short title or name of expense (e.g., Chocolates, Cab, Room Rent)"),
                        "description", Map.of("type", "STRING", "description", "Additional details about the expense")
                ),
                List.of("amount")
        ));

        declarations.add(createFunctionDeclaration("getAllExpenses",
                "Retrieves all expenses recorded by the user, optionally filtered by month and year",
                Map.of(
                        "month", Map.of("type", "INTEGER", "description", "Month number (1-12) to filter by. Defaults to current month if asked for monthly expenses"),
                        "year", Map.of("type", "INTEGER", "description", "Full 4-digit year (e.g. 2026)")
                ),
                Collections.emptyList()
        ));

        declarations.add(createFunctionDeclaration("recordIncome",
                "Records an incoming income stream for the user",
                Map.of(
                        "amount", Map.of("type", "NUMBER", "description", "Numerical income amount received"),
                        "source", Map.of("type", "STRING", "description", "Income source like Salary, Freelance, Investment, Bonus"),
                        "description", Map.of("type", "STRING", "description", "Income description or notes")
                ),
                List.of("amount")
        ));

        declarations.add(createFunctionDeclaration("getAllIncomes",
                "Retrieves all recorded incomes and aggregate income summary",
                Collections.emptyMap(),
                Collections.emptyList()
        ));

        declarations.add(createFunctionDeclaration("checkBudgetStatus",
                "Checks current budget allocation, total expenses against budget, and remaining amount",
                Map.of(
                        "month", Map.of("type", "INTEGER", "description", "Month number (1-12)"),
                        "year", Map.of("type", "INTEGER", "description", "Year (e.g. 2026)")
                ),
                Collections.emptyList()
        ));

        declarations.add(createFunctionDeclaration("setBudget",
                "Sets or updates the monthly budget limit for a specific month and year",
                Map.of(
                        "amount", Map.of("type", "NUMBER", "description", "Target budget allowance amount"),
                        "month", Map.of("type", "INTEGER", "description", "Month number (1-12)"),
                        "year", Map.of("type", "INTEGER", "description", "Year (e.g. 2026)")
                ),
                List.of("amount")
        ));

        declarations.add(createFunctionDeclaration("getAllCategories",
                "Lists all categories available for expenses and incomes",
                Collections.emptyMap(),
                Collections.emptyList()
        ));

        declarations.add(createFunctionDeclaration("addCategory",
                "Creates a new custom category for expenses or incomes",
                Map.of(
                        "categoryName", Map.of("type", "STRING", "description", "Name of category to create"),
                        "type", Map.of("type", "STRING", "description", "Either EXPENSE or INCOME")
                ),
                List.of("categoryName")
        ));

        declarations.add(createFunctionDeclaration("getDashboardOverview",
                "Fetches consolidated dashboard figures including Total Income, Total Expense, Balance, and Remaining Budget",
                Map.of(
                        "month", Map.of("type", "INTEGER", "description", "Month number (1-12)"),
                        "year", Map.of("type", "INTEGER", "description", "Year (e.g. 2026)")
                ),
                Collections.emptyList()
        ));

        declarations.add(createFunctionDeclaration("getMonthlySummary",
                "Generates comprehensive monthly report including net savings and category-wise expense breakdown",
                Map.of(
                        "month", Map.of("type", "INTEGER", "description", "Month number (1-12)"),
                        "year", Map.of("type", "INTEGER", "description", "Year (e.g. 2026)")
                ),
                Collections.emptyList()
        ));

        return List.of(Map.of("functionDeclarations", declarations));
    }

    private Map<String, Object> createFunctionDeclaration(String name, String description,
                                                          Map<String, Object> properties, List<String> required) {
        Map<String, Object> decl = new LinkedHashMap<>();
        decl.put("name", name);
        decl.put("description", description);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "OBJECT");
        parameters.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            parameters.put("required", required);
        }
        decl.put("parameters", parameters);

        return decl;
    }

    private void ensureCategoryExists(String email, String categoryName, CategoryType type) {
        User user = userRepository.findByEmailId(email)
                .orElseThrow(() -> new ETMException(404, "User not found with email: " + email));

        boolean exists = categoryRepository.existsByUserAndCategoryNameAndType(user, categoryName, type);

        if (!exists) {
            Category newCategory = new Category();
            newCategory.setCategoryName(categoryName);
            newCategory.setType(type);
            newCategory.setUser(user);
            categoryRepository.save(newCategory);
            log.info("Auto-provisioned category '{}' [{}] for user: {}", categoryName, type, email);
        }
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(Object val, Integer defaultValue) {
        if (val == null) return defaultValue;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
