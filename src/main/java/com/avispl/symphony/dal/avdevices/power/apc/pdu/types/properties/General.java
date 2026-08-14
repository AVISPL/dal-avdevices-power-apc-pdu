/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Represents general properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum General {
	AOS_VERSION("AOSVersion"),
	INPUT_TYPE("InputType"),
	MAX_LOAD_CURRENT("MaximumLoadCurrent(A)"),
	MODEL("Model"),
	OUTLET_TOTAL("OutletTotal"),
	PDU_VERSION("PDUVersion"),
	APPARENT_POWER("ApparentPower(VA)"),
	ACTIVE_POWER("ActivePower(W)"),
	CURRENT("Current(A)"),
	PHASE_1_CURRENT("Phase1Current(A)"),
	PHASE_2_CURRENT("Phase2Current(A)"),
	PHASE_3_CURRENT("Phase3Current(A)");

	String displayName;

	public static final List<General> COMMON_PROPERTIES = List.of(
			AOS_VERSION, INPUT_TYPE, MAX_LOAD_CURRENT, MODEL, OUTLET_TOTAL, PDU_VERSION, APPARENT_POWER, ACTIVE_POWER
	);

	public static final List<General> THREE_PHASE_PROPERTIES = List.of(PHASE_1_CURRENT, PHASE_2_CURRENT, PHASE_3_CURRENT);

	/** Properties backed by the {@code power} reading, which not every generation can serve. */
	public static final List<General> POWER_PROPERTIES = List.of(APPARENT_POWER, ACTIVE_POWER);
}
