/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.dal.util.StringUtils;

/**
 * Utility class for this adapter. This class includes helper methods to extract and convert properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Util {
	private static final Logger LOG = Logger.ofClass(Util.class);

	/**
	 * Maps the given value to a formatted string using title case for normal text.
	 * Delegates to {@link #mapToValue(Object, boolean)} with {@code isTitleCase = true}.
	 *
	 * @param value the input value to map
	 * @return the mapped string, or {@code Constant.NOT_AVAILABLE} if unavailable
	 */
	public static String mapToValue(Object value) {
		return mapToValue(value, true);
	}

	/**
	 * Maps an input value to its normalized string representation.
	 * <p>
	 * Handles {@link String} and {@link Boolean} values with optional title casing.
	 * Returns {@code Constant.NOT_AVAILABLE} for null, empty, or unsupported types.
	 * </p>
	 *
	 * @param value the input value to map
	 * @param isTitleCase whether to convert string to title case
	 * @return normalized string representation or {@link Constant#NOT_AVAILABLE}
	 */
	public static String mapToValue(Object value, boolean isTitleCase) {
		if (value == null) {
			LOG.warn("Skip value mapping: value is null");
			return Constant.NOT_AVAILABLE;
		}
		if (value instanceof String str) {
			if (StringUtils.isNullOrEmpty(str, true)) {
				LOG.warn("Skip value mapping: string is null/empty");
				return Constant.NOT_AVAILABLE;
			}
			if (isBoolean(str)) {
				return str.toLowerCase();
			}
			if (isInt(str)) {
				return String.valueOf(Integer.parseInt(str));
			}

			return isTitleCase ? toTitleCase(str) : str;
		}
		if (value instanceof Boolean) {
			return value.toString();
		}

		LOG.warn("Skip value mapping: unsupported type '%s'".formatted(value.getClass().getName()));
		return Constant.NOT_AVAILABLE;
	}

	/**
	 * Capitalizes the first character of the input string.
	 * <p>
	 * If the input is {@code null}, empty, or the literal string {@code "null"}, this method returns {@code null}.
	 * If the input is {@code "true"} or {@code "false"}, the method returns the input unchanged.
	 * Otherwise, it returns the input string with its first character converted to uppercase.
	 * </p>
	 *
	 * @param value the input string to convert
	 * @return a string with the first character capitalized, or {@code null} if the input is invalid
	 */
	private static String toTitleCase(String value) {
		if (StringUtils.isNullOrEmpty(value) || value.equals("null")) {
			LOG.warn("The value is invalid(%s), returning null.".formatted(value));
			return null;
		}
		if (isBoolean(value)) {
			return value;
		}

		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static boolean isBoolean(String value) {
		return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
	}

	private static boolean isInt(String value) {
		if (StringUtils.isNullOrEmpty(value, true)) {
			return false;
		}
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Returns the elapsed uptime between the current system time and the given timestamp in milliseconds.
	 * <p>
	 * The input timestamp represents the start time in milliseconds (typically from {@link System#currentTimeMillis()}).
	 * The returned string represents the absolute duration in the format:
	 * "X d Y hr Z min W sec", omitting any zero-value units except seconds.
	 *
	 * @param uptime the start time in milliseconds as a string (e.g., "1717581000000")
	 * @return a formatted duration string like "2 d 3 hr 15 min 42 sec",
	 * or {@link Constant#NOT_AVAILABLE} if parsing fails
	 */
	public static String mapToUptime(String uptime) {
		try {
			if (StringUtils.isNullOrEmpty(uptime)) {
				LOG.warn("Skip uptime mapping, the value is null or empty");
				return null;
			}

			var uptimeSecond = (System.currentTimeMillis() - Long.parseLong(uptime)) / 1000;
			var seconds = uptimeSecond % 60;
			var minutes = uptimeSecond % 3600 / 60;
			var hours = uptimeSecond % 86400 / 3600;
			var days = uptimeSecond / 86400;
			var rs = new StringBuilder();
			if (days > 0) {
				rs.append(days).append(" d ");
			}
			if (hours > 0) {
				rs.append(hours).append(" hr ");
			}
			if (minutes > 0) {
				rs.append(minutes).append(" min ");
			}
			if (seconds > 0 || rs.isEmpty()) {
				rs.append(seconds).append(" sec");
			}

			return rs.toString().trim();
		} catch (Exception e) {
			LOG.error("Failed to mapToUptime with uptime: " + uptime, e);
			return null;
		}
	}

	/**
	 * Returns the elapsed uptime in **whole minutes** between the current system time and the given timestamp in milliseconds.
	 * <p>
	 * The input timestamp represents the start time in milliseconds (typically from {@link System#currentTimeMillis()}).
	 * The returned string is the total number of minutes that have elapsed, excluding seconds.
	 *
	 * @param uptime the start time in milliseconds as a string (e.g., "1717581000000")
	 * @return a string representing the total number of elapsed minutes (e.g., "125"),
	 * or {@link Constant#NOT_AVAILABLE} if parsing fails
	 */
	public static String mapToUptimeMin(String uptime) {
		try {
			if (StringUtils.isNullOrEmpty(uptime)) {
				LOG.warn("Skip uptime min mapping, the value is null or empty");
				return null;
			}

			var uptimeSecond = (System.currentTimeMillis() - Long.parseLong(uptime)) / 1000;
			var minutes = uptimeSecond / 60;

			return String.valueOf(minutes);
		} catch (Exception e) {
			LOG.error("Failed to mapToUptimeMin with uptime: " + uptime, e);
			return null;
		}
	}

	/**
	 * Extracts a version string from the given input.
	 *
	 * <p>The method scans the input from left to right and returns the first
	 * continuous sequence that starts with a digit and may contain digits and dots ('.').
	 * Parsing stops when a non-digit and non-dot character is encountered after the version starts.	 *
	 * <p>This implementation avoids regular expressions to ensure predictable performance
	 * and to eliminate the risk of excessive backtracking on large inputs.
	 *
	 * @param input the input string that may contain a version
	 * @return the extracted version string, or {@code null} if no version is found
	 */
	public static String extractVersion(String input) {
		if (input == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		boolean started = false;
		for (char c : input.toCharArray()) {
			if (Character.isDigit(c)) {
				sb.append(c);
				started = true;
			} else if (c == '.' && started) {
				sb.append(c);
			} else if (started) {
				break;
			}
		}

		return !sb.isEmpty() ? sb.toString() : null;
	}

	/**
	 * Extracts the numeric portion from the given input string by removing all
	 * non-numeric characters except digits, decimal point ('.'), and minus sign ('-').
	 *
	 * <p>This method is useful for quickly normalizing values that include units,
	 * such as "12A", "5.5kW", or "-3.3V".
	 *
	 * @param input the input string containing a numeric value with optional unit
	 * @return a string containing only numeric characters, decimal point, and minus sign;
	 * or an empty string if no numeric characters are found
	 */
	public static String extractUnit(String input) {
		return input.replaceAll("[^\\d.-]", Constant.EMPTY);
	}
}
