/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Identifies the Rack PDU firmware generation, which determines the CLI dialect the device speaks.
 *
 * <p>The two generations share almost no command names: 1st generation uses lowercase unprefixed verbs
 * ({@code ver}, {@code status}, {@code on}), while 2nd generation uses camelCase domain-prefixed verbs
 * ({@code about}, {@code olStatus}, {@code olOn}). The discriminator is the {@code Application Module Name}
 * reported by {@code about}.
 *
 * <p><strong>Do not infer the generation from firmware version numbers.</strong> The two lineages are numbered
 * independently and are not ordered relative to each other - the 1st generation CLI requires AOS 2.7.0 /
 * application module 2.7.3, whereas an observed {@code rpdu2g} unit reports AOS 2.5.3.2 / application module
 * 2.5.2.5. A version comparison yields the wrong answer.
 *
 * @author Symphony Dev Team
 * @since 1.1.0
 */
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PduGeneration {
	/** 1st generation Switched Rack PDU firmware. */
	RPDU("rpdu"),
	/** 2nd generation Rack PDU firmware. */
	RPDU_2G("rpdu2g");

	/** Value reported as {@code Application Module Name}. */
	String applicationModule;

	/**
	 * Resolves the generation from the {@code Application Module Name} reported by {@code about}.
	 *
	 * <p>Matched most-specific-first, because {@code rpdu} is a prefix of {@code rpdu2g}.
	 *
	 * @param applicationModule the reported application module name; may be {@code null}
	 * @return the matching generation, or {@link #RPDU} when the name is absent or unrecognised
	 */
	public static PduGeneration fromApplicationModule(String applicationModule) {
		if (applicationModule == null || applicationModule.isBlank()) {
			return RPDU;
		}
		var normalized = applicationModule.trim();
		return RPDU_2G.applicationModule.equalsIgnoreCase(normalized) ? RPDU_2G : RPDU;
	}

	public boolean is2G() {
		return this == RPDU_2G;
	}
}
