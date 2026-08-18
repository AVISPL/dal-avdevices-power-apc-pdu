/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types;

import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;

/**
 * The overload outlet restriction states of 2nd generation firmware.
 *
 * <p>Each state has two spellings that must not be confused: the token {@code phRestrictn} accepts as an argument, and
 * the label APC's own web interface shows for it. Only the token may go on the wire; only the label should be shown to
 * an operator. They are paired here so the two lists cannot drift apart.
 *
 * <p>Note the device's CLI uses a third vocabulary again when reporting the current state - an AP7920B answers
 * {@code Restrict On Near Overload} for {@link #NEAR} - which is why reading is handled separately by
 * {@link com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util#toRestrictionState2G}.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum OverloadRestriction {
	NONE(Constant.RESTRICTION_NONE_2G, "None"),
	NEAR(Constant.RESTRICTION_NEAR_2G, "On Warning"),
	OVER(Constant.RESTRICTION_OVER_2G, "On Critical");

	/** The token {@code phRestrictn} accepts. */
	String state;
	/** The label APC's web interface uses for this state. */
	String displayName;

	/** The tokens, in the order the device escalates; the dropdown's values. */
	public static final List<String> STATES = Arrays.stream(values()).map(OverloadRestriction::getState).toList();

	/** The labels, aligned index-for-index with {@link #STATES}; the dropdown's labels. */
	public static final List<String> LABELS = Arrays.stream(values()).map(OverloadRestriction::getDisplayName).toList();
}
