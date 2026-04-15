/** Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.infrastructure.management.apcpdu.common.Constant;

/**
 * Represents adapter metadata properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AdapterMetadata {
	ADAPTER_BUILD_DATE(Constant.ADAPTER_METADATA_GROUP + "#AdapterBuildDate", "adapter.build.date"),
	ADAPTER_UPTIME(Constant.ADAPTER_METADATA_GROUP + "#AdapterUptime", "adapter.uptime"),
	ADAPTER_UPTIME_MIN(Constant.ADAPTER_METADATA_GROUP + "#AdapterUptime(min)", "adapter.uptime"),
	ADAPTER_VERSION(Constant.ADAPTER_METADATA_GROUP + "#AdapterVersion", "adapter.version");

	String displayName;
	String property;
}
