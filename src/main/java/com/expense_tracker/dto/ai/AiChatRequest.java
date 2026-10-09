package com.expense_tracker.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User conversational prompt for the AI Financial Assistant")
public class AiChatRequest {

    @NotBlank(message = "Prompt message cannot be blank")
    @Schema(description = "Natural language message or query (e.g., 'I spent $25 on lunch today' or 'How much did I spend this month?')",
            example = "I spent $35 on groceries today at Trader Joe's")
    private String message;
}
