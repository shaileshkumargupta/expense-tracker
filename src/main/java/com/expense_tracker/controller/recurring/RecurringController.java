package com.expense_tracker.controller.recurring;

import com.expense_tracker.dto.common.Response;
import com.expense_tracker.dto.recurringTxn.RecurringRequest;
import com.expense_tracker.service.recurringTxn.RecurringTxnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recurring")
@Slf4j
@RequiredArgsConstructor
public class RecurringController {

    private final RecurringTxnService recurringTxnService;

    @PostMapping
    public Response createRecurring(@Valid @RequestBody RecurringRequest request, Authentication authentication){
        String email = authentication.getName();

        log.info("Create recurring request: {}",request);

        recurringTxnService.createRecurring(request,email);
        Response response = new Response();
        response.setSuccessResponse();
        return response;
    }
}
