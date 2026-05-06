/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Represents outlet information parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutletDetail {
	String name;
	String powerOffDelay;
	boolean isPowerOffDelayDisabled;
	String powerOnDelay;
	boolean isPowerOnDelayDisabled;
	String powerStatus;
	String rebootDuration;
}
