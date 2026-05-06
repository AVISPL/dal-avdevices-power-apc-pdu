/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;

/**
 * Represents configuration properties on Symp.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Configuration {
	UNDEFINED(Constant.NOT_AVAILABLE),
	COLD_START_DELAY(Constant.CONFIGURATION_GROUP + "#ColdStartDelay"),
	COLD_START_DELAY_SEC(Constant.CONFIGURATION_GROUP + "#ColdStartDelay(sec)"),
	LOW_LOAD_WARNING(Constant.CONFIGURATION_GROUP + "#LowLoadWarning(A)"),
	PHASE_1_LOW_LOAD_WARNING(Constant.CONFIGURATION_GROUP + "#Phase1LowLoadWarning(A)"),
	PHASE_2_LOW_LOAD_WARNING(Constant.CONFIGURATION_GROUP + "#Phase2LowLoadWarning(A)"),
	PHASE_3_LOW_LOAD_WARNING(Constant.CONFIGURATION_GROUP + "#Phase3LowLoadWarning(A)"),
	NEAR_OVERLOAD_WARNING(Constant.CONFIGURATION_GROUP + "#NearOverloadWarning(A)"),
	PHASE_1_NEAR_OVERLOAD_WARNING(Constant.CONFIGURATION_GROUP + "#Phase1NearOverloadWarning(A)"),
	PHASE_2_NEAR_OVERLOAD_WARNING(Constant.CONFIGURATION_GROUP + "#Phase2NearOverloadWarning(A)"),
	PHASE_3_NEAR_OVERLOAD_WARNING(Constant.CONFIGURATION_GROUP + "#Phase3NearOverloadWarning(A)"),
	OVERLOAD_ALARM(Constant.CONFIGURATION_GROUP + "#OverloadAlarm(A)"),
	PHASE_1_OVERLOAD_ALARM(Constant.CONFIGURATION_GROUP + "#Phase1OverloadAlarm(A)"),
	PHASE_2_OVERLOAD_ALARM(Constant.CONFIGURATION_GROUP + "#Phase2OverloadAlarm(A)"),
	PHASE_3_OVERLOAD_ALARM(Constant.CONFIGURATION_GROUP + "#Phase3OverloadAlarm(A)"),
	OVERLOAD_RESTRICTION(Constant.CONFIGURATION_GROUP + "#OverloadRestriction"),
	PHASE_1_OVERLOAD_RESTRICTION(Constant.CONFIGURATION_GROUP + "#Phase1OverloadRestriction"),
	PHASE_2_OVERLOAD_RESTRICTION(Constant.CONFIGURATION_GROUP + "#Phase2OverloadRestriction"),
	PHASE_3_OVERLOAD_RESTRICTION(Constant.CONFIGURATION_GROUP + "#Phase3OverloadRestriction");

	String displayName;

	public static final List<Configuration> ONE_PHASE_PROPERTIES = List.of(
			LOW_LOAD_WARNING, NEAR_OVERLOAD_WARNING, OVERLOAD_ALARM, OVERLOAD_RESTRICTION
	);

	public static final List<Configuration> THREE_PHASE_PROPERTIES = List.of(
			PHASE_1_LOW_LOAD_WARNING, PHASE_2_LOW_LOAD_WARNING, PHASE_3_LOW_LOAD_WARNING,
			PHASE_1_NEAR_OVERLOAD_WARNING, PHASE_2_NEAR_OVERLOAD_WARNING, PHASE_3_NEAR_OVERLOAD_WARNING,
			PHASE_1_OVERLOAD_ALARM, PHASE_2_OVERLOAD_ALARM, PHASE_3_OVERLOAD_ALARM,
			PHASE_1_OVERLOAD_RESTRICTION, PHASE_2_OVERLOAD_RESTRICTION, PHASE_3_OVERLOAD_RESTRICTION
	);
}
