package com.expense_tracker.service.dashboard;

import com.expense_tracker.dto.dashboard.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashBoard(Integer month, Integer year, String email);
}
