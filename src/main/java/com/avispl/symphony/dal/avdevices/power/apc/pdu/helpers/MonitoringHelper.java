/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.api.common.error.InvalidArgumentException;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.GeneralInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlet.OutletDetail;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.OutletList;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.InputType;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Configuration;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.General;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Outlets;

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
	 * @param is3Phases indicates whether the PDU is 3-phase
	 * @param generalInformation the source of general device information (must not be {@code null})
	 * @param pdu the source of PDU data (must not be {@code null})
	 * @return a map of property display names to their corresponding values; never {@code null}
	 * @throws InvalidArgumentException if an unexpected {@link General} value is encountered
	 */
	public static Map<String, String> generateGeneral(boolean is3Phases, GeneralInformation generalInformation, Pdu pdu) {
		var properties = new HashMap<String, String>();
		var lowerCaseValues = List.of(General.AOS_VERSION, General.INPUT_TYPE, General.PDU_VERSION);
		for (General general : General.COMMON_PROPERTIES) {
			String propertyValue = switch (general) {
				case AOS_VERSION -> generalInformation.getAosVersion();
				case INPUT_TYPE -> generalInformation.getInputType();
				case MAX_LOAD_CURRENT -> Util.extractValue(generalInformation.getMaxLoad());
				case MODEL -> generalInformation.getModel();
				case OUTLET_TOTAL -> generalInformation.getOutlets();
				case PDU_VERSION -> generalInformation.getPduVersion();
				case ACTIVE_POWER -> Util.extractValue(pdu.getPowerW());
				case APPARENT_POWER -> Util.extractValue(pdu.getPowerVA());
				default -> throw new InvalidArgumentException("Unexpected General in COMMON_PROPERTIES: " + general);
			};
			properties.put(general.getDisplayName(), Util.mapToValue(propertyValue, !lowerCaseValues.contains(general)));
		}
		if (is3Phases) {
			for (General general : General.THREE_PHASE_PROPERTIES) {
				String propertyValue = switch (general) {
					case PHASE_1_CURRENT -> Util.extractValue(pdu.getCurrents().get(1));
					case PHASE_2_CURRENT -> Util.extractValue(pdu.getCurrents().get(2));
					case PHASE_3_CURRENT -> Util.extractValue(pdu.getCurrents().get(3));
					default -> throw new InvalidArgumentException("Unexpected General in THREE_PHASE_PROPERTIES: " + general);
				};
				properties.put(general.getDisplayName(), Util.mapToValue(propertyValue));
			}
		} else {
			properties.put(General.CURRENT.getDisplayName(), Util.mapToValue(pdu.getCurrent()));
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
		properties.put(Configuration.COLD_START_DELAY.getDisplayName(), pdu.isNeverColdStartDelay() ? "Off" : "On");
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
	 * Generates a map of outlets properties for the given {@link OutletList}.
	 *
	 * @param outletList the source of outlets data (must not be {@code null})
	 * @return a map of outlets display names to their corresponding values; never {@code null}
	 * @throws InvalidArgumentException if an unexpected {@link OutletList} value is encountered
	 */
	public static Map<String, String> generateOutlets(OutletList outletList) {
		var properties = new HashMap<String, String>();
		outletList.getOutlets().forEach(outlet -> {
			var prefixName = new StringBuilder();
			prefixName.append(Util.toTitleCase(outlet.getSource())).append("_");
			prefixName.append(Util.toTitleCase(outlet.getUsername())).append("_");
			prefixName.append("Outlet_");
			for (Entry<String, OutletDetail> entry : outlet.getOutletDetails().entrySet()) {
				var outletDetail = entry.getValue();
				for (Outlets property : Outlets.values()) {
					var propertyName = prefixName + "%02d".formatted(Integer.parseInt(entry.getKey()));
					var propertyValue = switch (property) {
						case NAME -> outletDetail.getName();
						case POWER_OFF_DELAY -> outletDetail.isNeverPowerOffDelay() ? "Off" : "On";
						case POWER_OFF_DELAY_SEC -> outletDetail.getPowerOffDelay();
						case POWER_ON_DELAY -> outletDetail.isNeverPowerOnDelay() ? "Off" : "On";
						case POWER_ON_DELAY_SEC -> outletDetail.getPowerOnDelay();
						case POWER_STATUS -> outletDetail.getPowerStatus().toLowerCase();
						case REBOOT -> Constant.NOT_AVAILABLE;
						case REBOOT_DURATION_SEC -> outletDetail.getRebootDuration();
					};
					properties.put(property.getDisplayName(propertyName), Util.mapToValue(propertyValue));
				}
			}
		});
		return properties;
	}

	/**
	 * Generates dynamic current-related properties based on the PDU input type.
	 * Returns phase currents for {@link InputType#THREE_PHASE} PDUs, or total current for non–3-phase PDUs.
	 *
	 * @param is3Phases indicates whether the PDU is 3-phase
	 * @param pdu the PDU source data
	 * @return map of current property names to their values
	 */
	public static Map<String, String> generateGeneralDynamicProperties(boolean is3Phases, Pdu pdu) {
		var dynamicStatistics = new HashMap<String, String>();
		if (is3Phases) {
			dynamicStatistics.put(General.PHASE_1_CURRENT.getDisplayName(), pdu.getCurrents().get(0));
			dynamicStatistics.put(General.PHASE_2_CURRENT.getDisplayName(), pdu.getCurrents().get(1));
			dynamicStatistics.put(General.PHASE_3_CURRENT.getDisplayName(), pdu.getCurrents().get(2));
		} else {
			dynamicStatistics.put(General.CURRENT.getDisplayName(), pdu.getCurrent());
		}

		return dynamicStatistics;
	}
}
