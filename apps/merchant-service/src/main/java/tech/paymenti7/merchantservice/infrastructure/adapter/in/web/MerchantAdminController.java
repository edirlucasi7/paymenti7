package tech.paymenti7.merchantservice.infrastructure.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusCommand;
import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/merchants")
public class MerchantAdminController {

	private final UpdateMerchantStatusUseCase updateMerchantStatusUseCase;

	public MerchantAdminController(UpdateMerchantStatusUseCase updateMerchantStatusUseCase) {
		this.updateMerchantStatusUseCase = updateMerchantStatusUseCase;
	}

	@PutMapping("/{merchantId}")
	public ResponseEntity<Void> updateStatus(@PathVariable UUID merchantId,
			@Valid @RequestBody MerchantStatusUpdateRequest request) {
		updateMerchantStatusUseCase.updateStatus(new UpdateMerchantStatusCommand(merchantId, request.status()));
		return ResponseEntity.accepted().build();
	}
}
