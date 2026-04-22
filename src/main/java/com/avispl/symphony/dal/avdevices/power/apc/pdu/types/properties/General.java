/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties;

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
	PDU_VERSION("PDUVersion");

	String displayName;
}
