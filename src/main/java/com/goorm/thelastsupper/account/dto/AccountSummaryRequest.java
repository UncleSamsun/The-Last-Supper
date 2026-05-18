package com.goorm.thelastsupper.account.dto;

import java.util.List;

public record AccountSummaryRequest(
	List<String> accountIds
) {
}
