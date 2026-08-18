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
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlets;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.Command;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.OverloadRestriction;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.PduGeneration;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Configuration;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Outlet;
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

	private static final String PHASE = "Phase";
	private static final String LOW_LOAD_WARNING = "LowLoadWarning";
	private static final String NEAR_OVERLOAD_WARNING = "NearOverloadWarning";
	private static final String OVERLOAD_ALARM = "OverloadAlarm";
	private static final String OVERLOAD_RESTRICTION = "OverloadRestriction";

	/** Accepted range, in seconds, for an outlet's power on/off delay. */
	private static final int DELAY_MIN_SECONDS = 0;
	private static final int DELAY_MAX_SECONDS = 7200;
	/** Accepted range, in seconds, for an outlet's reboot duration. */
	private static final int REBOOT_DURATION_MIN_SECONDS = 5;
	private static final int REBOOT_DURATION_MAX_SECONDS = 60;

	/**
	 * Generates a list of controllable configuration properties for the given {@link Pdu}.
	 *
	 * @param is3Phases indicates whether the PDU has a 3-phase input
	 * @param generation the firmware generation, which decides the shape of the overload restriction control
	 * @param pdu the source of configuration data (must not be {@code null})
	 * @return a list of {@link AdvancedControllableProperty} representing configurable controls; never {@code null}
	 */
	public static List<AdvancedControllableProperty> generateConfigurationControllers(boolean is3Phases, PduGeneration generation, Pdu pdu) {
		var controllableProperties = new ArrayList<>(List.of(
				ControllablePropertyFactory.createSwitch(Configuration.COLD_START_DELAY.getDisplayName(), pdu.isColdStartDelayDisabled() ? 0 : 1)
		));
		if (!pdu.isColdStartDelayDisabled()) {
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
					createRestrictionControl(Configuration.PHASE_1_OVERLOAD_RESTRICTION, generation, pdu.getOverloadRestrictions().get(1)),
					createRestrictionControl(Configuration.PHASE_2_OVERLOAD_RESTRICTION, generation, pdu.getOverloadRestrictions().get(2)),
					createRestrictionControl(Configuration.PHASE_3_OVERLOAD_RESTRICTION, generation, pdu.getOverloadRestrictions().get(3))
			));
		} else {
			controllableProperties.addAll(List.of(
					ControllablePropertyFactory.createNumeric(Configuration.LOW_LOAD_WARNING.getDisplayName(), pdu.getLowLoadWarning()),
					ControllablePropertyFactory.createNumeric(Configuration.NEAR_OVERLOAD_WARNING.getDisplayName(), pdu.getNearOverloadWarning()),
					ControllablePropertyFactory.createNumeric(Configuration.OVERLOAD_ALARM.getDisplayName(), pdu.getOverloadAlarm()),
					createRestrictionControl(Configuration.OVERLOAD_RESTRICTION, generation, pdu.getOverloadRestriction())
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
	public static String generateConfigurationRequest(String property, Object value, Pdu pdu, PduGeneration generation) {
		if (Configuration.COLD_START_DELAY.getDisplayName().equals(property)) {
			var param = "1".equals(value.toString()) ? pdu.getOldColdStartDelay() : Constant.NEVER;
			return Command.COLD_START_DELAY.requestFor(generation, param);
		}
		if (Configuration.COLD_START_DELAY_SEC.getDisplayName().equals(property)) {
			return Command.COLD_START_DELAY.requestFor(generation, (int) Double.parseDouble(value.toString()));
		}

		var configProperty = property.split(Constant.HASH)[1];
		var phase = extractPhase(configProperty);
		validateThresholdOrder(configProperty, phase, String.valueOf(value), pdu);
		var param = buildParam(phase, configProperty, String.valueOf(value), generation);
		if (configProperty.contains(LOW_LOAD_WARNING)) {
			return Command.LOW_LOAD_WARNING.requestFor(generation, param);
		}
		if (configProperty.contains(NEAR_OVERLOAD_WARNING)) {
			return Command.NEAR_OVERLOAD_WARNING.requestFor(generation, param);
		}
		if (configProperty.contains(OVERLOAD_ALARM)) {
			return Command.OVERLOAD_ALARM.requestFor(generation, param);
		}
		if (configProperty.contains(OVERLOAD_RESTRICTION)) {
			return Command.OVERLOAD_RESTRICTION.requestFor(generation, param);
		}

		throw new InvalidArgumentException("Unknown configuration property: '%s'".formatted(property));
	}

	/**
	 * Builds the overload restriction control, whose shape depends on how many states the generation exposes.
	 *
	 * <p>2nd generation firmware has three states, so it gets a dropdown carrying the device's own tokens. 1st generation
	 * is modelled as the on/off pair it reports and keeps its switch.
	 *
	 * @param property the restriction property to build a control for
	 * @param generation the firmware generation
	 * @param state the current restriction state
	 * @return a dropdown on 2nd generation, a switch otherwise
	 */
	private static AdvancedControllableProperty createRestrictionControl(Configuration property, PduGeneration generation, String state) {
		if (!generation.is2G()) {
			return ControllablePropertyFactory.createSwitch(property.getDisplayName(), mapToSwitchValue(state));
		}
		// The factory labels a dropdown with its own values; the operator-facing labels differ from the wire tokens here.
		var control = ControllablePropertyFactory.createDropdown(property.getDisplayName(), OverloadRestriction.STATES, state);
		if (control.getType() instanceof AdvancedControllableProperty.DropDown dropdown) {
			dropdown.setLabels(OverloadRestriction.LABELS.toArray(String[]::new));
		}

		return control;
	}

	/**
	 * Generates a list of controllable outlets properties for the given {@link Outlets}.
	 *
	 * @param outlets the source of outlet data (must not be {@code null})
	 * @return a list of {@link AdvancedControllableProperty} representing outlets controls; never {@code null}
	 */
	public static List<AdvancedControllableProperty> generateOutletsControllers(Outlets outlets) {
		var controllableProperties = new ArrayList<AdvancedControllableProperty>();
		for (var outletNumber : outlets.getOrderedOutletNumbers()) {
			var propertyName = Util.buildOutletPropertyPrefix(outletNumber);
			var outlet = outlets.getOutletDetails().get(outletNumber);
			controllableProperties.addAll(List.of(
					ControllablePropertyFactory.createSwitch(Outlet.POWER_OFF_DELAY.getDisplayName(propertyName), outlet.isPowerOffDelayDisabled() ? 0 : 1),
					ControllablePropertyFactory.createSwitch(Outlet.POWER_ON_DELAY.getDisplayName(propertyName), outlet.isPowerOnDelayDisabled() ? 0 : 1),
					ControllablePropertyFactory.createSwitch(Outlet.POWER_STATUS.getDisplayName(propertyName), mapToSwitchValue(outlet.getPowerStatus())),
					ControllablePropertyFactory.createButton(Outlet.REBOOT.getDisplayName(propertyName), "Reboot", "Rebooting", 0L),
					ControllablePropertyFactory.createNumeric(Outlet.REBOOT_DURATION_SEC.getDisplayName(propertyName), outlet.getRebootDuration())
			));
			if (!outlet.isPowerOffDelayDisabled()) {
				controllableProperties.add(ControllablePropertyFactory.createNumeric(Outlet.POWER_OFF_DELAY_SEC.getDisplayName(propertyName), outlet.getPowerOffDelay()));
			}
			if (!outlet.isPowerOnDelayDisabled()) {
				controllableProperties.add(ControllablePropertyFactory.createNumeric(Outlet.POWER_ON_DELAY_SEC.getDisplayName(propertyName), outlet.getPowerOnDelay()));
			}
		}

		return controllableProperties;
	}

	/**
	 * Generates a request command for an outlet property.
	 *
	 * @param property the outlet property identifier
	 * @param value the value associated with the property
	 * @return the generated request command
	 * @throws InvalidArgumentException if the outlet property is unsupported
	 * @throws IllegalArgumentException if a numeric value falls outside the range the device accepts
	 */
	public static String generateOutletRequest(String property, Object value, PduGeneration generation) {
		var propertyComponent = property.split(Constant.HASH);
		var nameComponent = propertyComponent[0].split(Constant.UNDERSCORE);
		var outletNumber = Integer.parseInt(nameComponent[nameComponent.length - 1]);
		var outletProperty = Outlet.fromProperty(propertyComponent[1]);
		validateOutletRange(property, outletProperty, String.valueOf(value));
		var param = buildOutletParam(outletNumber, outletProperty, value.toString(), generation);

		return switch (outletProperty) {
			case POWER_OFF_DELAY, POWER_OFF_DELAY_SEC -> Command.POWER_OFF_DELAY.requestFor(generation, param);
			case POWER_ON_DELAY, POWER_ON_DELAY_SEC -> Command.POWER_ON_DELAY.requestFor(generation, param);
			case POWER_STATUS -> "1".equals(value.toString())
					? Command.ON.requestFor(generation, outletNumber)
					: Command.OFF.requestFor(generation, outletNumber);
			case REBOOT -> Command.REBOOT.requestFor(generation, outletNumber);
			case REBOOT_DURATION_SEC -> Command.REBOOT_DURATION.requestFor(generation, param);
			default -> throw new InvalidArgumentException("Unknown outlet property: '%s'".formatted(property));
		};
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
	 * Enforces the ordering the firmware requires between the three load thresholds, so a violating control fails here
	 * rather than after a round trip the operator never sees the result of.
	 *
	 * <p>The rules are {@code lowLoad < nearOverload <= overloadAlarm}. Note they are not symmetric: near-overload must
	 * be strictly above low-load but may equal the overload alarm. An AP7920B rejects a violation with
	 * {@code Near Overload Warning Phase 1 must be greater than Low Load Warning and less than or equal to Overload
	 * Alarm, set failed.}, and the messages below are transcribed from those responses.
	 *
	 * <p>Neither the {@code phLowLoad}/{@code phNearOver}/{@code phOverLoad} CLI reference nor the web interface's
	 * "Configure Load Thresholds" documentation states this dependency; both describe the value only as amps. The device
	 * is therefore the sole source for these rules, which is why they are asserted rather than cited.
	 *
	 * <p>A threshold that is missing or unparseable is skipped instead of failing the control: the value simply is not
	 * known locally, and the device stays the final authority. Zero is left legal on purpose - it is the documented
	 * default for the low-load warning and disables it.
	 *
	 * @param configProperty the configuration property name, without its group prefix
	 * @param phase the phase the control targets
	 * @param value the requested value
	 * @param pdu the currently known thresholds to compare against
	 * @throws IllegalArgumentException if the requested value would break the required ordering
	 */
	private static void validateThresholdOrder(String configProperty, Integer phase, String value, Pdu pdu) {
		// The restriction control carries a switch state rather than an amperage, so it has no ordering to satisfy.
		if (configProperty.contains(OVERLOAD_RESTRICTION)) {
			return;
		}
		var requested = toNumber(value);
		if (requested == null) {
			return;
		}
		var is3Phases = configProperty.startsWith(PHASE);
		var lowLoad = toNumber(is3Phases ? pdu.getLowLoadWarnings().get(phase) : pdu.getLowLoadWarning());
		var nearOverload = toNumber(is3Phases ? pdu.getNearOverloadWarnings().get(phase) : pdu.getNearOverloadWarning());
		var overloadAlarm = toNumber(is3Phases ? pdu.getOverloadAlarms().get(phase) : pdu.getOverloadAlarm());

		if (configProperty.contains(LOW_LOAD_WARNING) && nearOverload != null && requested >= nearOverload) {
			throw thresholdViolation("Low Load Warning", phase, "must be less than Near Overload Warning");
		}
		if (configProperty.contains(NEAR_OVERLOAD_WARNING)
				&& ((lowLoad != null && requested <= lowLoad) || (overloadAlarm != null && requested > overloadAlarm))) {
			throw thresholdViolation("Near Overload Warning", phase,
					"must be greater than Low Load Warning and less than or equal to Overload Alarm");
		}
		if (configProperty.contains(OVERLOAD_ALARM) && nearOverload != null && requested < nearOverload) {
			throw thresholdViolation("Overload Alarm", phase, "must be greater than or equal to Near Overload Warning");
		}
	}

	/**
	 * Enforces the ranges the device accepts for the numeric outlet controls, so an out-of-range value fails here rather
	 * than being rejected after a round trip.
	 *
	 * <p>Only the {@code (sec)} controls carry a range. The bare {@code PowerOffDelay} and {@code PowerOnDelay}
	 * properties are switches that resolve to {@code min} or {@code never}, and the remaining outlet controls are
	 * switches or a button.
	 *
	 * <p>A value that is not numeric is left to the device, matching how {@link #validateThresholdOrder} treats a value
	 * it cannot compare.
	 *
	 * @param property the full property identifier, used in the error message
	 * @param outletProperty the resolved outlet property
	 * @param value the requested value
	 * @throws IllegalArgumentException if the value falls outside the accepted range
	 */
	private static void validateOutletRange(String property, Outlet outletProperty, String value) {
		switch (outletProperty) {
			case POWER_OFF_DELAY_SEC, POWER_ON_DELAY_SEC -> requireInRange(property, value, DELAY_MIN_SECONDS, DELAY_MAX_SECONDS);
			case REBOOT_DURATION_SEC -> requireInRange(property, value, REBOOT_DURATION_MIN_SECONDS, REBOOT_DURATION_MAX_SECONDS);
			default -> {
				// Nothing to bound; the remaining outlet controls do not carry a numeric value.
			}
		}
	}

	private static void requireInRange(String property, String value, int min, int max) {
		var requested = toNumber(value);
		if (requested == null) {
			return;
		}
		if (requested < min || requested > max) {
			throw new IllegalArgumentException(
					"%s must be between %d and %d, set failed.".formatted(property, min, max));
		}
	}

	/** Builds a violation error worded the way the device words its own rejection. */
	private static IllegalArgumentException thresholdViolation(String label, Integer phase, String rule) {
		return new IllegalArgumentException("%s Phase %d %s, set failed.".formatted(label, phase, rule));
	}

	/**
	 * Parses a reading or a requested value into a comparable number.
	 *
	 * @param value the value, which may be {@code null} or a placeholder such as {@link Constant#NOT_AVAILABLE}
	 * @return the parsed value, or {@code null} when it cannot be compared
	 */
	private static Double toNumber(String value) {
		if (StringUtils.isNullOrEmpty(value, true)) {
			return null;
		}
		try {
			return Double.valueOf(value.trim());
		} catch (NumberFormatException e) {
			LOG.warn("Skip numeric comparison; '%s' is not numeric".formatted(value));
			return null;
		}
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
	private static String buildParam(Integer phase, String property, String value, PduGeneration generation) {
		var paramValue = value;
		if (property.contains(OVERLOAD_RESTRICTION)) {
			// `rpdu2g` exposes all three states through a dropdown, so its selection is already the token to send.
			// `rpdu` is modelled as a switch and takes on/off.
			paramValue = generation.is2G() ? requireRestrictionState(property, value) : Util.mapToStatusValue(value);
		}
		return phase == null ? paramValue : phase + Constant.SPACE + paramValue;
	}

	/**
	 * Checks a dropdown selection against the states {@code phRestrictn} accepts.
	 *
	 * @param property the property being controlled, used in the error message
	 * @param value the selected state
	 * @return the state, unchanged
	 * @throws IllegalArgumentException if the selection is not one of {@link OverloadRestriction#STATES}
	 */
	private static String requireRestrictionState(String property, String value) {
		var state = value == null ? null : value.trim().toLowerCase();
		if (!OverloadRestriction.STATES.contains(state)) {
			throw new IllegalArgumentException(
					"%s must be one of %s, set failed.".formatted(property, OverloadRestriction.STATES));
		}
		return state;
	}

	/**
	 * Builds the request parameter for outlet commands.
	 *
	 * @param outletNumber the outlet number
	 * @param property the outlet property
	 * @param value the property value
	 * @return the formatted outlet request parameter
	 */
	private static String buildOutletParam(int outletNumber, Outlet property, String value, PduGeneration generation) {
		var paramBuilder = new StringBuilder().append(outletNumber);
		if (Outlet.REBOOT_DURATION_SEC.equals(property)) {
			// `rebootduration` separates outlet from duration with a colon; `olRbootTime` uses a space.
			paramBuilder.append(generation.is2G() ? Constant.SPACE : Constant.COLON).append(value);
		} else if (Outlet.POWER_OFF_DELAY.equals(property) || Outlet.POWER_ON_DELAY.equals(property)) {
			// `olOffDelay`/`olOnDelay` reject the word the device reports the disabled state as, and take -1 instead.
			// `poweroffdelay`/`powerondelay` keep the word; the 1st generation dialect could not be verified against hardware.
			var disabled = generation.is2G() ? Constant.NEVER_DELAY : Constant.NEVER;
			paramBuilder.append(Constant.SPACE).append("1".equals(value) ? Constant.MIN_VALUE : disabled);
		} else {
			paramBuilder.append(Constant.SPACE).append(value);
		}

		return paramBuilder.toString();
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
}
