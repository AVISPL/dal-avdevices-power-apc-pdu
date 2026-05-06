/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Represents information about outlets assigned to a user, parsed from the command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutletUser {
	String source;
	String username;
	Set<String> outletNumbers;
}
