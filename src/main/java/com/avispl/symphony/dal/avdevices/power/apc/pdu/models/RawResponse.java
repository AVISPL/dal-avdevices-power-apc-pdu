/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;

/**
 * Carries a normalized response verbatim, for commands whose value cannot be recovered by the shared numeric
 * extraction - such as the 2nd generation {@code phRestrictn}, which reports text rather than a number or token.
 *
 * @author Symphony Dev Team
 * @since 1.1.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RawResponse extends BaseModel {
	String value;

	@Override
	public void parse(String response) {
		if (response == null || response.isBlank()) {
			this.log.warn("The response param is null or blank; ignore parsing the value");
			return;
		}
		this.value = response.trim();
	}
}
