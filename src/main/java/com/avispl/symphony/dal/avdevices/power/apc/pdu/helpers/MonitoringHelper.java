/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.GeneralInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.General;

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
	 * @param generalInformation the general information object
	 * @return a map of general properties (never {@code null})
	 */
	public static Map<String, String> generateGeneral(GeneralInformation generalInformation) {
		var properties = new HashMap<String, String>();
		for (General general : General.values()) {
			String propertyValue = switch (general) {
				case AOS_VERSION -> generalInformation.getAosVersion();
				case INPUT_TYPE -> generalInformation.getInputType();
				case MAX_LOAD_CURRENT -> Util.extractUnit(generalInformation.getMaxLoad());
				case MODEL -> generalInformation.getModel();
				case OUTLET_TOTAL -> generalInformation.getOutlets();
				case PDU_VERSION -> generalInformation.getPduVersion();
			};
			properties.put(general.getDisplayName(), Util.mapToValue(propertyValue, !General.INPUT_TYPE.equals(general)));
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
}
