/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.common;

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class that defines constant values used across the application.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constant {
	public static final String PROMPT_COMMAND = "APC>";
	public static final String NOT_AVAILABLE = "N/A";
	public static final String EMPTY = "";
	public static final String HASH = "#";
	public static final int MAX_PHASE = 3;
	public static final String NEVER = "never";
	public static final String MIN_VALUE = "0";
	public static final String UNDERSCORE = "_";
	public static final String SPACE = " ";
	public static final String COLON = ":";

	//	Format
	public static final Pattern ERROR_RESPONSE_PATTERN = Pattern.compile("(?m)^\\s*E(?:10|20)");
	public static final Pattern VALUE_WITH_UNIT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(A|VA|W)");
	public static final Pattern TIME_SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*seconds");
	public static final Pattern OVERLOAD_SETTING_PATTERN = Pattern.compile("Overload restriction is (\\S+)");

	//	Group
	public static final String ADAPTER_METADATA_GROUP = "AdapterMetadata";
	public static final String CONFIGURATION_GROUP = "Configuration";
	public static final String OUTLET_GROUP = "Outlet_";
}
