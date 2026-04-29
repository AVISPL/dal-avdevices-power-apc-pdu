/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;

/**
 * Represents outlet properties on Symp.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Outlets {
	NAME("Name"),
	POWER_OFF_DELAY("PowerOffDelay"),
	POWER_OFF_DELAY_SEC("PowerOffDelay(sec)"),
	POWER_ON_DELAY("PowerOnDelay"),
	POWER_ON_DELAY_SEC("PowerOnDelay(sec)"),
	POWER_STATUS("PowerStatus"),
	REBOOT("Reboot"),
	REBOOT_DURATION_SEC("RebootDuration(sec)");

	String displayName;

	public String getDisplayName(String groupName) {
		return groupName + Constant.HASH + this.displayName;
	}
}
