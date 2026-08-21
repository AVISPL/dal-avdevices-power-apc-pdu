/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Represents the types of pdu input.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum InputType {
	SINGLE_PHASE("single-phase"),
	BANKED_PHASE("banked"),
	THREE_PHASE("3-phase");

	String value;

	public static boolean is3Phases(String inputType) {
		return THREE_PHASE.value.equalsIgnoreCase(inputType);
	}
}
