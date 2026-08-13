/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.common;

import java.util.Optional;
import java.util.regex.Matcher;

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
	public static String toTitleCase(String value) {
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
	 * Extracts a value from the given input string based on predefined patterns.
	 *
	 * @param input the raw input string
	 * @return an {@link Optional} containing the extracted value if matched;
	 *         otherwise {@link Optional#empty()}
	 */
	public static Optional<String> extractValue(String input) {
		return extract(input, true);
	}

	/**
	 * Extracts a value the way {@link #extractValue(String)} does, but renders a unit-bearing reading as a decimal
	 * instead of rounding it to a whole number, so that {@code 10 A} yields {@code 10.0} and {@code 10.5 A} survives
	 * intact.
	 *
	 * <p>Used for the load thresholds, which are reported to Symphony as doubles.
	 *
	 * @param input the raw input string
	 * @return an {@link Optional} containing the extracted value if matched; otherwise {@link Optional#empty()}
	 */
	public static Optional<String> extractDecimalValue(String input) {
		return extract(input, false);
	}

	/**
	 * @param roundUnitValue whether a value carrying an {@code A}/{@code VA}/{@code W} unit is rounded to a whole
	 * number rather than kept as a decimal
	 */
	private static Optional<String> extract(String input, boolean roundUnitValue) {
		if (input == null || input.isBlank()) {
			LOG.warn("Input is null or blank; returning empty optional");
			return Optional.empty();
		}
		try {
			Matcher matcher;

			matcher = Constant.OVERLOAD_SETTING_PATTERN.matcher(input);
			if (matcher.find()) {
				return Optional.ofNullable(matcher.group(1));
			}
			matcher = Constant.TIME_SECONDS_PATTERN.matcher(input);
			if (matcher.find()) {
				return Optional.ofNullable(matcher.group(1));
			}
			matcher = Constant.VALUE_WITH_UNIT_PATTERN.matcher(input);
			if (matcher.find()) {
				var value = matcher.group(1);
				return Optional.ofNullable(roundUnitValue ? roundValue(value) : toDecimal(value));
			}
            matcher = Constant.NEVER_PATTERN.matcher(input);
			if (matcher.find()) {
				return Optional.of(Constant.NEVER);
			}
			LOG.warn("Input '%s' does not match any known pattern; returning empty optional".formatted(input));
			return Optional.empty();
		} catch (Exception e) {
			LOG.error("Failed to extract value from input '%s'; returning empty optional".formatted(input), e);
			return Optional.empty();
		}
	}

	/**
	 * Renders a numeric string as a decimal, so that a whole number gains an explicit fractional part.
	 *
	 * @param input the numeric string
	 * @return the value as a decimal string, or {@code null} if parsing fails
	 */
	private static String toDecimal(String input) {
		try {
			return String.valueOf(Double.parseDouble(input));
		} catch (Exception e) {
			LOG.error("Failed to convert value '%s' to decimal".formatted(input), e);
			return null;
		}
	}

	/**
	 * Rounds a numeric string value to the nearest integer.
	 *
	 * @param input the numeric string
	 * @return rounded value as string, or {@code null} if parsing fails
	 */
	private static String roundValue(String input) {
		try {
			return String.valueOf(Math.round(Double.parseDouble(input)));
		} catch (Exception e) {
			LOG.error("Failed to round value from input '%s'".formatted(input), e);
			return null;
		}
	}

	/**
	 * Builds the property-group prefix for a physical outlet, e.g. {@code Outlet_01}.
	 *
	 * <p>Outlets are identified by their number alone. They were previously prefixed with the account they are
	 * assigned to, which produced one duplicate group per account with access - the same outlet reported repeatedly
	 * with identical values, and controllable regardless of whether that account was even enabled.
	 *
	 * @param outletNumber the outlet number as reported by the device
	 * @return the property-group prefix, zero-padded to two digits
	 */
	public static String buildOutletPropertyPrefix(String outletNumber) {
		return Constant.OUTLET_GROUP + "%02d".formatted(Integer.parseInt(outletNumber));
	}

	/**
	 * Normalizes a 2nd generation {@code phRestrictn} reading onto the on/off token 1st generation reports, so that the
	 * resulting Symphony property carries the same value on both generations.
	 *
	 * <p>{@code rpdu} answers {@code Overload restriction is off for 1.} whereas {@code rpdu2g} answers prose such as
	 * {@code 1: Always Allow Turn On}. Only the "always allow" wording means unrestricted; every other wording denotes
	 * some restriction being in effect, which avoids having to enumerate the {@code near} and {@code over} phrasings.
	 *
	 * @param input the raw reading
	 * @return {@code "off"} when unrestricted, {@code "on"} when restricted, or {@code null} when the input is unusable
	 */
	public static String toRestrictionState(String input) {
		if (StringUtils.isNullOrEmpty(input, true)) {
			LOG.warn("Skip restriction mapping: the input is null or empty");
			return null;
		}
		return input.toLowerCase().contains(Constant.RESTRICTION_DISABLED_2G) ? "off" : Constant.ON;
	}

	public static String mapToStatusValue(String input) {
		try {
			return "1".equals(input) ? "on" : "off";
		} catch (Exception e) {
			LOG.error("Failed to map to status value from input '%s'".formatted(input), e);
			return "off";
		}
	}
}
