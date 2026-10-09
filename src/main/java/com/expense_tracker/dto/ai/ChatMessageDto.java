package com.expense_tracker.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Role in conversation: "user", "model", or "function"
     */
    private String role;

    /**
     * Textual message body or summary
     */
    private String content;

    /**
     * Name of the tool invoked (if function response)
     */
    private String functionName;

    /**
     * Structured JSON/String output of tool execution
     */
    private String functionResult;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
