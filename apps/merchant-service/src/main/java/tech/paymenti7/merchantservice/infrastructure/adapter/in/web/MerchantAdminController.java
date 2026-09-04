package tech.paymenti7.merchantservice.infrastructure.adapter.in.web;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusCommand;
import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusUseCase;

import java.util.UUID;

@Tag(name = "Merchants", description = "Operações administrativas de merchants.")
@RestController
@RequestMapping("/v1/admin/merchants")
public class MerchantAdminController {

	private final UpdateMerchantStatusUseCase updateMerchantStatusUseCase;

	public MerchantAdminController(UpdateMerchantStatusUseCase updateMerchantStatusUseCase) {
		this.updateMerchantStatusUseCase = updateMerchantStatusUseCase;
	}

	@Operation(summary = "Atualiza o status de um merchant",
			description = "Solicita a atualização do status e publica o evento MerchantUpdated pelo padrão Outbox.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "202", description = "Atualização aceita para processamento."),
			@ApiResponse(responseCode = "400", description = "Corpo inválido ou status ausente.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "InvalidRequest", value = """
									{"title":"Invalid request","status":400,"detail":"status is required"}
									"""))),
			@ApiResponse(responseCode = "404", description = "Merchant não encontrado.",
					content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class),
							examples = @ExampleObject(name = "MerchantNotFound", value = """
									{"title":"Merchant not found","status":404,"detail":"Merchant not found: 11111111-1111-1111-1111-111111111111"}
									""")))
	})
	@PutMapping("/{merchantId}")
	public ResponseEntity<Void> updateStatus(
			@Parameter(description = "Identificador do merchant", required = true,
					example = "11111111-1111-1111-1111-111111111111") @PathVariable UUID merchantId,
			@Valid @RequestBody MerchantStatusUpdateRequest request) {
		updateMerchantStatusUseCase.updateStatus(new UpdateMerchantStatusCommand(merchantId, request.status()));
		return ResponseEntity.accepted().build();
	}
}
