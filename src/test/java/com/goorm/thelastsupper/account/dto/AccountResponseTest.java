package com.goorm.thelastsupper.account.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.goorm.thelastsupper.account.entity.Account;
import com.goorm.thelastsupper.account.entity.Role;

class AccountResponseTest {

	@Test
	void accountResponseIncludesAccountId() {
		Account account = Account.builder()
			.email("new.customer@example.com")
			.nickName("신규고객")
			.phoneNumber("010-1234-5678")
			.password("encoded-password")
			.role(Role.CUSTOMER)
			.deleted(false)
			.build();
		account.setId("account-new-customer");

		AccountResponse response = AccountResponse.toAccountResponse(account);

		assertThat(response.id()).isEqualTo("account-new-customer");
		assertThat(response.email()).isEqualTo("new.customer@example.com");
	}
}
