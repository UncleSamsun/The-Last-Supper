package com.goorm.thelastsupper.account.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.thelastsupper.account.dto.AccountResponse;
import com.goorm.thelastsupper.account.dto.AccountSummaryRequest;
import com.goorm.thelastsupper.account.dto.AccountSummaryResponse;
import com.goorm.thelastsupper.account.dto.AccountUpdateRequest;
import com.goorm.thelastsupper.account.dto.PasswordRequest;
import com.goorm.thelastsupper.account.service.AccountService;
import com.goorm.thelastsupper.common.security.CustomPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class AccountController {

	private final AccountService accountService;

	@GetMapping("/customers")
	public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal CustomPrincipal customPrincipal) {
		AccountResponse response = accountService.getAccount(customPrincipal.getId());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/customers/summaries")
	public ResponseEntity<List<AccountSummaryResponse>> getAccountSummaries(
		@RequestBody AccountSummaryRequest request
	) {
		List<AccountSummaryResponse> response = accountService.getAccountSummaries(request.accountIds());
		return ResponseEntity.ok(response);
	}

	@PutMapping("/customers")
	public ResponseEntity<AccountResponse> updateAccount(@AuthenticationPrincipal CustomPrincipal customPrincipal,
		@RequestBody AccountUpdateRequest accountUpdateRequest) {
		AccountResponse response = accountService.updateAccount(customPrincipal.getId(), accountUpdateRequest);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/customers")
	public ResponseEntity<AccountResponse> updatePassword(@AuthenticationPrincipal CustomPrincipal customPrincipal,
		@RequestBody PasswordRequest passwordRequest) {
		AccountResponse response = accountService.updatePassword(customPrincipal.getId(), passwordRequest);
		return ResponseEntity.ok(response);
	}

}
