/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Represents supported CLI commands for the APC PDU communicator.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@FieldDefaults(makeFinal = true)
@RequiredArgsConstructor
@Getter
public enum Command {
	VER("ver"),
	COLD_START_DELAY("pducoldstartdelay"),
	POWER("power"),
	CURRENT("current"),
	LOW_LOAD_WARNING("lowloadwarning"),
	NEAR_OVERLOAD_WARNING("nearoverloadwarning"),
	OVERLOAD_RESTRICTION("overloadrestriction"),
	OVERLOAD_ALARM("overloadalarm");

	String request;

	public String getRequest(Object param) {
		return this.request + " " + param;
	}
}
