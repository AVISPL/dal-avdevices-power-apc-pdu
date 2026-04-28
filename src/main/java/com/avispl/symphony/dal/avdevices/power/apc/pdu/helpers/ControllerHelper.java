/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.api.common.error.InvalidArgumentException;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.Command;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Configuration;
import com.avispl.symphony.dal.util.ControllablePropertyFactory;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * Helper class for generating controllable properties and perform control operations.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ControllerHelper {
	private static final Logger LOG = Logger.ofClass(ControllerHelper.class);

	/**
	 * Generates a list of controllable configuration properties for the given {@link Pdu}.
	 *
	 * @param is3Phases indicates whether the PDU has a 3-phase input
	 * @param pdu the source of configuration data (must not be {@code null})
	 * @return a list of {@link AdvancedControllableProperty} representing configurable controls; never {@code null}
	 */
	public static List<AdvancedControllableProperty> generateConfigurationControllers(boolean is3Phases, Pdu pdu) {
		var controllableProperties = new ArrayList<>(List.of(
				ControllablePropertyFactory.createSwitch(Configuration.COLD_START_DELAY.getDisplayName(), pdu.isNeverColdStartDelay() ? 0 : 1)
		));
		if (!pdu.isNeverColdStartDelay()) {
			controllableProperties.add(
					ControllablePropertyFactory.createSlider(Configuration.COLD_START_DELAY_SEC.getDisplayName(), 0f, 300f, Float.valueOf(pdu.getColdStartDelay()))
			);
		}
		if (is3Phases) {
			controllableProperties.addAll(List.of(
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_1_LOW_LOAD_WARNING.getDisplayName(), pdu.getLowLoadWarnings().get(1)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_2_LOW_LOAD_WARNING.getDisplayName(), pdu.getLowLoadWarnings().get(2)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_3_LOW_LOAD_WARNING.getDisplayName(), pdu.getLowLoadWarnings().get(3)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_1_NEAR_OVERLOAD_WARNING.getDisplayName(), pdu.getNearOverloadWarnings().get(1)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_2_NEAR_OVERLOAD_WARNING.getDisplayName(), pdu.getNearOverloadWarnings().get(2)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_3_NEAR_OVERLOAD_WARNING.getDisplayName(), pdu.getNearOverloadWarnings().get(3))
			));
			controllableProperties.addAll(List.of(
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_1_OVERLOAD_ALARM.getDisplayName(), pdu.getOverloadAlarms().get(1)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_2_OVERLOAD_ALARM.getDisplayName(), pdu.getOverloadAlarms().get(2)),
					ControllablePropertyFactory.createNumeric(Configuration.PHASE_3_OVERLOAD_ALARM.getDisplayName(), pdu.getOverloadAlarms().get(3)),
					ControllablePropertyFactory.createSwitch(Configuration.PHASE_1_OVERLOAD_RESTRICTION.getDisplayName(), mapToSwitchValue(pdu.getOverloadRestrictions().get(1))),
					ControllablePropertyFactory.createSwitch(Configuration.PHASE_2_OVERLOAD_RESTRICTION.getDisplayName(), mapToSwitchValue(pdu.getOverloadRestrictions().get(2))),
					ControllablePropertyFactory.createSwitch(Configuration.PHASE_3_OVERLOAD_RESTRICTION.getDisplayName(), mapToSwitchValue(pdu.getOverloadRestrictions().get(3)))
			));
		} else {
			controllableProperties.addAll(List.of(
					ControllablePropertyFactory.createNumeric(Configuration.LOW_LOAD_WARNING.getDisplayName(), pdu.getLowLoadWarning()),
					ControllablePropertyFactory.createNumeric(Configuration.NEAR_OVERLOAD_WARNING.getDisplayName(), pdu.getNearOverloadWarning()),
					ControllablePropertyFactory.createNumeric(Configuration.OVERLOAD_ALARM.getDisplayName(), pdu.getOverloadAlarm()),
					ControllablePropertyFactory.createSwitch(Configuration.OVERLOAD_RESTRICTION.getDisplayName(), mapToSwitchValue(pdu.getOverloadRestriction()))
			));
		}

		return controllableProperties;
	}

	/**
	 * Generates a device configuration request string based on property name and value.
	 *
	 * @param property the configuration property name (may include prefix with separator)
	 * @param value the value associated with the property
	 * @return the formatted request string to be sent to the device
	 * @throws InvalidArgumentException if the property is not recognized
	 */
	public static String generateConfigurationRequest(String property, Object value, Pdu pdu) {
		if (Configuration.COLD_START_DELAY.getDisplayName().equals(property)) {
			var param = "1".equals(value.toString()) ? pdu.getOldColdStartDelay() : Constant.NEVER;
			return Command.COLD_START_DELAY.getRequest(param);
		}
		if (Configuration.COLD_START_DELAY_SEC.getDisplayName().equals(property)) {
			return Command.COLD_START_DELAY.getRequest((int) Double.parseDouble(value.toString()));
		}

		var configProperty = property.split(Constant.HASH)[1];
		var phase = extractPhase(configProperty);
		var param = buildParam(phase, configProperty, String.valueOf(value));
		if (configProperty.contains("LowLoadWarning")) {
			return Command.LOW_LOAD_WARNING.getRequest(param);
		}
		if (configProperty.contains("NearOverloadWarning")) {
			return Command.NEAR_OVERLOAD_WARNING.getRequest(param);
		}
		if (configProperty.contains("OverloadAlarm")) {
			return Command.OVERLOAD_ALARM.getRequest(param);
		}
		if (configProperty.contains("OverloadRestriction")) {
			return Command.OVERLOAD_RESTRICTION.getRequest(param);
		}

		throw new InvalidArgumentException("Unknown configuration property: '%s'".formatted(property));
	}

	/**
	 * Extracts phase number from property name.
	 * <p>Defaults to phase 1 if no explicit phase is found.</p>
	 *
	 * @param property the configuration property name
	 * @return phase number (1, 2, or 3)
	 */
	private static Integer extractPhase(String property) {
		if (property.startsWith("Phase2")) {
			return 2;
		}
		if (property.startsWith("Phase3")) {
			return 3;
		}
		return 1;
	}

	/**
	 * Builds parameter string for command request based on phase, property, and value.
	 * <p>Applies special mapping for overload restriction values.</p>
	 *
	 * @param phase the phase number (can be null)
	 * @param property the configuration property name
	 * @param value the value as string
	 * @return formatted parameter string
	 */
	private static String buildParam(Integer phase, String property, String value) {
		if (phase == null) {
			return property.contains("OverloadRestriction") ? mapToStatusValue(value) : value;
		}
		if (property.contains("OverloadRestriction")) {
			return phase + " " + mapToStatusValue(value);
		}
		return phase + " " + value;
	}

	private static int mapToSwitchValue(String input) {
		if (StringUtils.isNullOrEmpty(input, true)) {
			LOG.warn("Skip mapping to switch value; the input is null or empty");
			return 0;
		}
		try {
			return "on".equalsIgnoreCase(input.trim()) ? 1 : 0;
		} catch (Exception e) {
			LOG.error("Failed to map to switch value from input '%s'".formatted(input), e);
			return 0;
		}
	}

	private static String mapToStatusValue(String input) {
		try {
			return "1".equals(input) ? "on" : "off";
		} catch (Exception e) {
			LOG.error("Failed to map to status value from input '%s'".formatted(input), e);
			return "off";
		}
	}
}
