/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;

/**
 * Helper class for generating monitoring properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MonitoringHelper {
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
