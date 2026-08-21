/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.api.common.error.InvalidArgumentException;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;

/**
 * Represents outlet properties on Symp.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public enum Outlet {
	NAME("Name"),
	POWER_OFF_DELAY("PowerOffDelay"),
	POWER_OFF_DELAY_SEC("PowerOffDelay(sec)"),
	POWER_ON_DELAY("PowerOnDelay"),
	POWER_ON_DELAY_SEC("PowerOnDelay(sec)"),
	POWER_STATUS("PowerStatus"),
	REBOOT("Reboot"),
	REBOOT_DURATION_SEC("RebootDuration(sec)");

	String property;

	public String getDisplayName(String groupName) {
		return groupName + Constant.HASH + this.property;
	}

	public static Outlet fromProperty(String property) {
		return Arrays.stream(values()).filter(p -> p.property.equals(property)).findFirst()
				.orElseThrow(() -> new InvalidArgumentException("Unknown outlet property: '%s'".formatted(property)));
	}
}
