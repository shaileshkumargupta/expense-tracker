package com.expense_tracker.service.recurringTxn;

import com.expense_tracker.dto.recurringTxn.RecurringRequest;
import jakarta.validation.Valid;

public interface RecurringTxnService {
    void createRecurring(@Valid RecurringRequest request, String email);
}
