/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.api.common.error.InvalidArgumentException;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.GeneralInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlets;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.Command;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.InputType;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.PduGeneration;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Configuration;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.General;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Outlet;

/**
 * Helper class for generating monitoring properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MonitoringHelper {
	/**
	 * Generates general properties from the provided {@link GeneralInformation}.
	 *
	 * @param generalInformation the source of general device information (must not be {@code null})
	 * @param pdu the source of PDU data (must not be {@code null})
	 * @return a map of property display names to their corresponding values; never {@code null}
	 * @throws InvalidArgumentException if an unexpected {@link General} value is encountered
	 */
	public static Map<String, String> generateGeneral(GeneralInformation generalInformation, Pdu pdu) {

		var properties = new HashMap<String, String>();
		var lowerCaseValues = List.of(General.AOS_VERSION, General.INPUT_TYPE, General.PDU_VERSION);
		var isPowerSupported = Command.POWER.isSupportedOn(generation);
		for (General general : General.COMMON_PROPERTIES) {
			// A generation with no `power` command can never fill these, so omit them rather than publish a permanent N/A.
			if (!isPowerSupported && General.POWER_PROPERTIES.contains(general)) {
				continue;
			}
			String propertyValue = switch (general) {
				case AOS_VERSION -> generalInformation.getAosVersion();
				case INPUT_TYPE -> generalInformation.getInputType();
				case MAX_LOAD_CURRENT -> Util.extractExactValue(generalInformation.getMaxLoad()).orElse(null);
				case MODEL -> generalInformation.getModel();
				case OUTLET_TOTAL -> generalInformation.getOutlets();
				case PDU_VERSION -> generalInformation.getPduVersion();
				case ACTIVE_POWER -> Util.extractValue(pdu.getPowerW()).orElse(null);
				case APPARENT_POWER -> Util.extractValue(pdu.getPowerVA()).orElse(null);
				default -> throw new InvalidArgumentException("Unexpected General in COMMON_PROPERTIES: " + general);
			};
			properties.put(general.getDisplayName(), Util.mapToValue(propertyValue, !lowerCaseValues.contains(general)));
		}
		return properties;
	}

	/**
	 * Generates adapter metadata properties from the provided {@link Properties}.
	 *
	 * @param versionProperties the source properties containing adapter metadata
	 * @return a map of metadata properties (never {@code null})
	 */
	public static Map<String, String> generateAdapterMetadata(Properties versionProperties) {
		var properties = new HashMap<String, String>();
		for (AdapterMetadata adapterMetadata : AdapterMetadata.values()) {
			String rawValue = versionProperties.getProperty(adapterMetadata.getProperty());
			String propertyValue = Util.mapToValue(switch (adapterMetadata) {
				case ADAPTER_UPTIME -> Util.mapToUptime(rawValue);
				case ADAPTER_UPTIME_MIN -> Util.mapToUptimeMin(rawValue);
				default -> rawValue;
			});
			properties.put(adapterMetadata.getDisplayName(), propertyValue);
		}

		return properties;
	}

	/**
	 * Generates a map of configuration properties for the given {@link Pdu}.
	 *
	 * @param is3Phases indicates whether the PDU operates in three-phase mode
	 * @param pdu the source of configuration data (must not be {@code null})
	 * @return a map of configuration display names to their corresponding values; never {@code null}
	 * @throws InvalidArgumentException if an unexpected {@link Configuration} value is encountered
	 */
	public static Map<String, String> generateConfiguration(boolean is3Phases, Pdu pdu) {
		var properties = new HashMap<String, String>();
		properties.put(Configuration.COLD_START_DELAY_SEC.getDisplayName(), Util.mapToValue(pdu.getColdStartDelay()));
		properties.put(Configuration.COLD_START_DELAY.getDisplayName(), pdu.isColdStartDelayDisabled() ? "Off" : "On");
		if (is3Phases) {
			for (Configuration configuration : Configuration.THREE_PHASE_PROPERTIES) {
				String propertyValue = switch (configuration) {
					case PHASE_1_LOW_LOAD_WARNING -> pdu.getLowLoadWarnings().get(1);
					case PHASE_2_LOW_LOAD_WARNING -> pdu.getLowLoadWarnings().get(2);
					case PHASE_3_LOW_LOAD_WARNING -> pdu.getLowLoadWarnings().get(3);
					case PHASE_1_NEAR_OVERLOAD_WARNING -> pdu.getNearOverloadWarnings().get(1);
					case PHASE_2_NEAR_OVERLOAD_WARNING -> pdu.getNearOverloadWarnings().get(2);
					case PHASE_3_NEAR_OVERLOAD_WARNING -> pdu.getNearOverloadWarnings().get(3);
					case PHASE_1_OVERLOAD_ALARM -> pdu.getOverloadAlarms().get(1);
					case PHASE_2_OVERLOAD_ALARM -> pdu.getOverloadAlarms().get(2);
					case PHASE_3_OVERLOAD_ALARM -> pdu.getOverloadAlarms().get(3);
					case PHASE_1_OVERLOAD_RESTRICTION -> pdu.getOverloadRestrictions().get(1);
					case PHASE_2_OVERLOAD_RESTRICTION -> pdu.getOverloadRestrictions().get(2);
					case PHASE_3_OVERLOAD_RESTRICTION -> pdu.getOverloadRestrictions().get(3);
					default -> throw new InvalidArgumentException("Unexpected Configuration in THREE_PHASE_PROPERTIES: " + configuration);
				};
				properties.put(configuration.getDisplayName(), Util.mapToValue(propertyValue));
			}
		} else {
			for (Configuration configuration : Configuration.ONE_PHASE_PROPERTIES) {
				String propertyValue = switch (configuration) {
					case LOW_LOAD_WARNING -> pdu.getLowLoadWarning();
					case NEAR_OVERLOAD_WARNING -> pdu.getNearOverloadWarning();
					case OVERLOAD_ALARM -> pdu.getOverloadAlarm();
					case OVERLOAD_RESTRICTION -> pdu.getOverloadRestriction();
					default -> throw new InvalidArgumentException("Unexpected Configuration in ONE_PHASE_PROPERTIES: " + configuration);
				};
				properties.put(configuration.getDisplayName(), Util.mapToValue(propertyValue));
			}
		}
		return properties;
	}

	/**
	 * Generates a map of outlets properties for the given {@link Outlets}.
	 *
	 * @param outlets the source of outlets data (must not be {@code null})
	 * @return a map of outlets display names to their corresponding values; never {@code null}
	 * @throws InvalidArgumentException if an unexpected {@link Outlets} value is encountered
	 */
	public static Map<String, String> generateOutlets(Outlets outlets) {
		var properties = new HashMap<String, String>();
		for (var outletNumber : outlets.getOrderedOutletNumbers()) {
			var outlet = outlets.getOutletDetails().get(outletNumber);
			var propertyName = Util.buildOutletPropertyPrefix(outletNumber);
			for (Outlet property : Outlet.values()) {
				var propertyValue = switch (property) {
					case NAME -> outlet.getName();
					case POWER_OFF_DELAY -> outlet.isPowerOffDelayDisabled() ? "Off" : "On";
					case POWER_OFF_DELAY_SEC -> outlet.isPowerOffDelayDisabled() ? Constant.MIN_VALUE : outlet.getPowerOffDelay();
					case POWER_ON_DELAY -> outlet.isPowerOnDelayDisabled() ? "Off" : "On";
					case POWER_ON_DELAY_SEC -> outlet.isPowerOnDelayDisabled() ? Constant.MIN_VALUE : outlet.getPowerOnDelay();
					// Lower-cased so that `rpdu` ("ON") and `rpdu2g` ("On") both render identically after title-casing.
					case POWER_STATUS -> outlet.getPowerStatus() == null ? null : outlet.getPowerStatus().toLowerCase();
					case REBOOT -> Constant.NOT_AVAILABLE;
					case REBOOT_DURATION_SEC -> outlet.getRebootDuration();
				};
				properties.put(property.getDisplayName(propertyName), Util.mapToValue(propertyValue));
			}
		}

		return properties;
	}

	/**
	 * Generates current-related properties based on the PDU input type, routing each one to either {@code statistics}
	 * or the returned dynamic map depending on whether it is listed in {@code historicalProperties}.
	 * Considers phase currents for {@link InputType#THREE_PHASE} PDUs, or total current for non–3-phase PDUs.
	 *
	 * @param is3Phases indicates whether the PDU is 3-phase
	 * @param pdu the PDU source data
	 * @param historicalProperties display names of properties to report as dynamic instead of regular; supports
	 * {@link General#CURRENT}, {@link General#PHASE_1_CURRENT}, {@link General#PHASE_2_CURRENT} and
	 * {@link General#PHASE_3_CURRENT}
	 * @param statistics the regular statistics map to add non-historical current properties to
	 * @return map of dynamic current property names to their values; empty when none of the applicable properties
	 * are listed in {@code historicalProperties}
	 * @throws InvalidArgumentException if an unexpected {@link General} value is encountered
	 */
	public static Map<String, String> generateGeneralDynamicProperties(boolean is3Phases, Pdu pdu, Set<String> historicalProperties,
			Map<String, String> statistics) {
		var dynamicStatistics = new HashMap<String, String>();
		for (General general : is3Phases ? General.THREE_PHASE_PROPERTIES : List.of(General.CURRENT)) {
			String propertyValue = switch (general) {
				case PHASE_1_CURRENT -> Util.extractValue(pdu.getCurrents().get(1)).orElse(null);
				case PHASE_2_CURRENT -> Util.extractValue(pdu.getCurrents().get(2)).orElse(null);
				case PHASE_3_CURRENT -> Util.extractValue(pdu.getCurrents().get(3)).orElse(null);
				case CURRENT -> pdu.getCurrent();
				default -> throw new InvalidArgumentException("Unexpected General for dynamic property: " + general);
			};
			var mappedValue = Util.mapToValue(propertyValue);
			if (historicalProperties.contains(general.getDisplayName())) {
				dynamicStatistics.put(general.getDisplayName(), mappedValue);
			} else {
				statistics.put(general.getDisplayName(), mappedValue);
			}
		}

		return dynamicStatistics;
	}
}
