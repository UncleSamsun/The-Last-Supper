package com.goorm.thelastsupper.account.dto;

import com.goorm.thelastsupper.account.entity.Account;

public record AccountSummaryResponse(
	String id,
	String nickName
) {
	public static AccountSummaryResponse from(Account account) {
		return new AccountSummaryResponse(account.getId(), account.getNickName());
	}
}
