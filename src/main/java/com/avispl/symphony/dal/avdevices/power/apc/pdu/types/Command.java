/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.api.common.error.InvalidArgumentException;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;

/**
 * Represents supported CLI commands for the APC PDU communicator.
 *
 * <p>Each constant carries the verb for both firmware generations, since the two dialects share no command names.
 * A {@code null} second column means no verified 2nd generation equivalent is known yet; requesting one throws
 * rather than sending a verb the device would reject.
 *
 * <p>2nd generation verbs below were read from the {@code help} output of an AP7920B running application module
 * {@code rpdu2g} v2.5.2.5. Note that {@link #REBOOT} maps to {@code olReboot} and <strong>not</strong> to the
 * {@code reboot} system command, which restarts the management card rather than an outlet.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@FieldDefaults(makeFinal = true)
@RequiredArgsConstructor
@Getter
public enum Command {
	VER("ver", "about"),
	COLD_START_DELAY("pducoldstartdelay", "devStartDly"),
	/**
	 * Apparent/active power. No {@code rpdu2g} equivalent on switched-only hardware: {@code phReading <n> power} and
	 * {@code phReading <n> appower} are listed in the usage text but rejected with {@code E102} on an AP7920B, which
	 * reports {@code Metered Outlets: 0}.
	 */
	POWER("power", null),
	CURRENT("current", "phReading"),
	LOW_LOAD_WARNING("lowloadwarning", "phLowLoad"),
	NEAR_OVERLOAD_WARNING("nearoverloadwarning", "phNearOver"),
	OVERLOAD_RESTRICTION("overloadrestriction", "phRestrictn"),
	OVERLOAD_ALARM("overloadalarm", "phOverLoad"),
	LIST("list", "userList"),
	STATUS("status", "olStatus"),
	POWER_OFF_DELAY("poweroffdelay", "olOffDelay"),
	POWER_ON_DELAY("powerondelay", "olOnDelay"),
	REBOOT("reboot", "olReboot"),
	REBOOT_DURATION("rebootduration", "olRbootTime"),
	ON("on", "olOn"),
	OFF("off", "olOff"),
	/** 2nd generation only; the equivalent 1st generation data is part of {@code ver}. */
	PROD_INFO(null, "prodInfo");

	String request;
	String request2g;

	public String getRequest(Object param) {
		return this.request + Constant.SPACE + param;
	}

	/**
	 * Returns the bare verb for the given generation.
	 *
	 * @param generation the firmware generation to target
	 * @return the verb this command is spelled as on that generation
	 * @throws InvalidArgumentException if no verified verb is known for that generation
	 */
	public String requestFor(PduGeneration generation) {
		var verb = generation.is2G() ? this.request2g : this.request;
		if (verb == null) {
			throw new InvalidArgumentException(
					"No known %s command for '%s'".formatted(generation.getApplicationModule(), this.name()));
		}
		return verb;
	}

	/**
	 * Returns the verb for the given generation with a single space-delimited parameter appended.
	 *
	 * @param generation the firmware generation to target
	 * @param param the parameter to append
	 * @return the formatted request
	 * @throws InvalidArgumentException if no verified verb is known for that generation
	 */
	public String requestFor(PduGeneration generation, Object param) {
		return this.requestFor(generation) + Constant.SPACE + param;
	}

	/**
	 * Indicates whether a verified verb exists for the given generation.
	 *
	 * @param generation the firmware generation to target
	 * @return {@code true} if this command can be issued on that generation
	 */
	public boolean isSupportedOn(PduGeneration generation) {
		return (generation.is2G() ? this.request2g : this.request) != null;
	}
}
