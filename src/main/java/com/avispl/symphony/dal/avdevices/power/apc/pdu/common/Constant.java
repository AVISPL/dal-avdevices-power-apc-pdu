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

	//	Format
	public static final Pattern ERROR_RESPONSE_PATTERN = Pattern.compile("(?m)^\\s*E10\\s*$");

	//	Group
	public static final String ADAPTER_METADATA_GROUP = "AdapterMetadata";
}
