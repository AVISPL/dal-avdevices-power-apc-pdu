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
	/** CLI prompt as emitted by 1st generation ({@code rpdu}) firmware. */
	public static final String PROMPT_COMMAND = "APC>";
	/** CLI prompt as emitted by 2nd generation ({@code rpdu2g}) firmware. */
	public static final String PROMPT_COMMAND_2G = "apc>";
	public static final String NOT_AVAILABLE = "N/A";
	public static final String EMPTY = "";
	public static final String HASH = "#";
	public static final int MAX_PHASE = 3;
	public static final String NEVER = "never";
	/**
	 * Value the outlet delay commands take to disable a delay. The device reports the resulting state back as
	 * {@link #NEVER} but rejects that word as an argument, so reads and writes do not share a representation.
	 *
	 * <p>Undocumented: the CLI reference lists only a {@code 1}-{@code 7200} time for {@code olOffDelay}/{@code olOnDelay}
	 * and documents {@code never} solely for {@code devStartDly}. Verified against the device.
	 */
	public static final String NEVER_DELAY = "-1";
	public static final String MIN_VALUE = "0";
	public static final String UNDERSCORE = "_";
	public static final String SPACE = " ";
	public static final String COLON = ":";

	//	Format
	/**
	 * Matches any CLI status code that denotes a failure.
	 * <p>1st generation firmware reports {@code E100}-{@code E104} / {@code E200} and signals success with the
	 * literal {@code OK}. 2nd generation firmware reports success as {@code E000: Success} and reuses the
	 * {@code E1xx}/{@code E2xx} range for failures - with different meanings for the same code (for example
	 * {@code E102} is "User already exists." on {@code rpdu} but "Parameter Error" on {@code rpdu2g}). Matching any
	 * {@code E<digits>} other than {@code E000} therefore covers both generations without depending on message text.
	 */
	public static final Pattern ERROR_RESPONSE_PATTERN = Pattern.compile("(?m)^\\s*E(?!000)\\d{3}");
	/** Trailing CLI prompt, matched case-insensitively so that both {@code APC>} and {@code apc>} are stripped. */
	public static final Pattern PROMPT_PATTERN = Pattern.compile("(?i)apc>\\s*$");
	/** Leading success marker - {@code OK} on 1st generation, {@code E000: Success} on 2nd generation. */
	public static final Pattern SUCCESS_MARKER_PATTERN = Pattern.compile("(?m)^[ \\t]*(?:OK|E000:[ \\t]*Success)[ \\t]*\\R?");
	public static final Pattern VALUE_WITH_UNIT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(A|VA|W)");
    public static final Pattern NEVER_PATTERN = Pattern.compile("(?i)never");
	/** Matches the {@link #NEVER_DELAY} sentinel an outlet delay reading uses for the disabled state. */
	public static final Pattern NEVER_DELAY_PATTERN = Pattern.compile("(?<![\\d.-])-1(?![\\d.])");
	/** {@code rpdu} spells the unit out ("is 5 seconds."), {@code rpdu2g} abbreviates it ("5 sec"). */
	public static final Pattern TIME_SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*sec(?:onds?)?\\b");
	public static final Pattern OVERLOAD_SETTING_PATTERN = Pattern.compile("Overload restriction is (\\S+)");
	/**
	 * {@code rpdu2g} reports the overload restriction as text ("Always Allow Turn On") rather than the on/off token
	 * {@code rpdu} uses. Only the disabled wording is matched; anything else means some restriction is in effect, which
	 * keeps the mapping correct without having to enumerate the {@code near} and {@code over} wordings.
	 */
	public static final String RESTRICTION_DISABLED_2G = "always allow";
	/** Prose marker for the {@code near} state, which the web interface labels "On Warning". */
	public static final String RESTRICTION_NEAR_PROSE_2G = "warning";
	/** Values the 2nd generation {@code phRestrictn} setter accepts. */
	public static final String RESTRICTION_NONE_2G = "none";
	public static final String RESTRICTION_NEAR_2G = "near";
	public static final String RESTRICTION_OVER_2G = "over";
	/** Measurement argument required by the 2nd generation `phReading` command. */
	public static final String READING_CURRENT_2G = "current";
	/** Parameter that makes the 2nd generation outlet commands report every outlet in one round trip. */
	public static final String ALL_2G = "all";
	/** Status token as reported by 2nd generation `olStatus`. */
	public static final String ON = "on";

	//	Group
	public static final String ADAPTER_METADATA_GROUP = "AdapterMetadata";
	public static final String CONFIGURATION_GROUP = "Configuration";
	public static final String OUTLET_GROUP = "Outlet_";

	//	Sections of the 2nd generation `about` response
	public static final String SECTION_HARDWARE_FACTORY = "Hardware Factory";
	public static final String SECTION_NETWORK_MANAGEMENT_CARD = "Network Management Card";
	public static final String SECTION_APPLICATION_MODULE = "Application Module";
	public static final String SECTION_AOS = "APC OS(AOS)";

	//	Keys of the 2nd generation `about` response; repeated across sections, so always read section-scoped
	public static final String KEY_MODEL_NUMBER = "Model Number";
	public static final String KEY_SERIAL_NUMBER = "Serial Number";
	public static final String KEY_NAME = "Name";
	public static final String KEY_VERSION = "Version";
}
