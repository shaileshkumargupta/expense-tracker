package com.expense_tracker.controller.ai;

import com.expense_tracker.dto.ai.AiChatRequest;
import com.expense_tracker.dto.ai.AiChatResponse;
import com.expense_tracker.dto.ai.ChatMessageDto;
import com.expense_tracker.dto.common.CommonResponse;
import com.expense_tracker.service.ai.AiAssistantService;
import com.expense_tracker.service.ai.ChatMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Financial Assistant", description = "Endpoints for conversational financial assistant powered by Google Gemini and Redis memory")
@SecurityRequirement(name = "BearerAuth")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final ChatMemoryService chatMemoryService;

    @PostMapping("/chat")
    @Operation(
            summary = "Chat with AI Financial Assistant",
            description = "Processes user prompts, maintains a sliding-window session history in Redis, and autonomously triggers backend tools (recording expenses, fetching monthly reports, budget validation)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assistant response successfully generated",
                    content = @Content(schema = @Schema(implementation = AiChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT"),
            @ApiResponse(responseCode = "429", description = "Too Many Requests - Rate limit quota exceeded")
    })
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Incoming AI chat request from authenticated user: {}", email);

        AiChatResponse response = aiAssistantService.chat(email, request.getMessage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get conversational memory history",
            description = "Retrieves recent conversation turns for the authenticated user from the Redis sliding window cache."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved conversation history"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    })
    public ResponseEntity<List<ChatMessageDto>> getHistory(Authentication authentication) {
        String email = authentication.getName();
        List<ChatMessageDto> history = chatMemoryService.getHistory(email);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history")
    @Operation(
            summary = "Clear conversational memory",
            description = "Resets the Redis sliding-window conversation memory for the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    })
    public ResponseEntity<CommonResponse> clearHistory(Authentication authentication) {
        String email = authentication.getName();
        chatMemoryService.clearHistory(email);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK.value(), "Chat memory successfully cleared for user: " + email));
    }
}
