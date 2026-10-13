package com.expense_tracker.controller.ai;

import com.expense_tracker.dto.ai.AiChatRequest;
import com.expense_tracker.dto.ai.AiChatResponse;
import com.expense_tracker.dto.ai.ChatMessageDto;
import com.expense_tracker.dto.ai.ReceiptScanResponse;
import com.expense_tracker.dto.common.CommonResponse;
import com.expense_tracker.dto.common.Response;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    @PostMapping(value = "/receipt-scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "AI Receipt & Invoice OCR Scanner (Multipart Upload)",
            description = "Analyzes receipt/invoice images using Gemini Vision to extract title, amount, category, date, and description."
    )
    public Response scanReceiptMultipart(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageBase64", required = false) String imageBase64,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Incoming receipt OCR scan request (multipart) from authenticated user: {}", email);

        ReceiptScanResponse scanResult = aiAssistantService.scanReceipt(file, imageBase64, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("scanResult", scanResult));
        log.info("Receipt OCR scan successfully completed for user: {}, merchant/title: {}", email, scanResult.getTitle());
        return response;
    }

    @PostMapping(value = "/receipt-scan", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "AI Receipt & Invoice OCR Scanner (Base64 JSON)",
            description = "Analyzes base64-encoded receipt/invoice images using Gemini Vision to extract title, amount, category, date, and description."
    )
    public Response scanReceiptJson(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        String email = authentication.getName();
        log.info("Incoming receipt OCR scan request (JSON base64) from authenticated user: {}", email);

        String base64Image = payload.containsKey("imageBase64") ? payload.get("imageBase64") : payload.get("file");
        ReceiptScanResponse scanResult = aiAssistantService.scanReceipt(null, base64Image, email);

        Response response = new Response();
        response.setSuccessResponse();
        response.setResponse(Map.of("scanResult", scanResult));
        log.info("Receipt OCR scan successfully completed for user: {}, merchant/title: {}", email, scanResult.getTitle());
        return response;
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
