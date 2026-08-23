package tech.paymenti7.merchantservice.merchant.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.paymenti7.merchantservice.merchant.application.MerchantStatusUpdateService;

@RestController
@RequestMapping("/v1/admin/merchants")
public class MerchantAdminController {

	private final MerchantStatusUpdateService merchantStatusUpdateService;

	public MerchantAdminController(MerchantStatusUpdateService merchantStatusUpdateService) {
		this.merchantStatusUpdateService = merchantStatusUpdateService;
	}

	@PutMapping("/{merchantId}")
	public ResponseEntity<Void> updateStatus(@PathVariable UUID merchantId,
			@RequestBody MerchantStatusUpdateRequest request) {
		merchantStatusUpdateService.updateStatus(merchantId, request.status());
		return ResponseEntity.accepted().build();
	}
}
