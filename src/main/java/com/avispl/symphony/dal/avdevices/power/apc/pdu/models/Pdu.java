/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;

/**
 * Represents pdu information parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pdu {
	private static final Logger LOG = Logger.ofClass(Pdu.class);

	String coldStartDelay;
	String oldColdStartDelay;
	boolean isColdStartDelayDisabled;
	String powerVA;
	String powerW;
	//	One phase Pdu
	String current;
	String lowLoadWarning;
	String nearOverloadWarning;
	String overloadAlarm;
	String overloadRestriction;
	//	3 phases Pdu
	final Map<Integer, String> currents = new HashMap<>(Constant.MAX_PHASE);
	final Map<Integer, String> lowLoadWarnings = new HashMap<>(Constant.MAX_PHASE);
	final Map<Integer, String> nearOverloadWarnings = new HashMap<>(Constant.MAX_PHASE);
	final Map<Integer, String> overloadAlarms = new HashMap<>(Constant.MAX_PHASE);
	final Map<Integer, String> overloadRestrictions = new HashMap<>(Constant.MAX_PHASE);

	public void setPower(ResponsePdu power) {
		var powers = power.getValue().split("\n");
		this.powerVA = powers[0];
		this.powerW = powers[1];
	}

	public String getColdStartDelay() {
		return this.isColdStartDelayDisabled ? Constant.MIN_VALUE : this.coldStartDelay;
	}

	public void setColdStartDelay(ResponsePdu coldStartDelay) {
		String value = Optional.ofNullable(coldStartDelay).map(ResponsePdu::getValue).orElse(null);
		if (null == value) {
			this.coldStartDelay = null;
			this.oldColdStartDelay = null;
			this.isColdStartDelayDisabled = false;
			return;
		}
		if (Constant.NEVER.equals(value)) {
			var isNullOrNever = null == this.coldStartDelay || Constant.NEVER.equals(this.coldStartDelay);
			this.oldColdStartDelay = isNullOrNever ? Constant.MIN_VALUE : this.coldStartDelay;
			this.isColdStartDelayDisabled = true;
		} else {
			this.oldColdStartDelay = null;
			this.isColdStartDelayDisabled = false;
		}
		this.coldStartDelay = value;
	}

	public void setCurrent(ResponsePdu current) {
		this.current = Optional.ofNullable(current).map(ResponsePdu::getValue).orElse(null);
	}

	public void setLowLoadWarning(ResponsePdu lowLoadWarning) {
		this.lowLoadWarning = Optional.ofNullable(lowLoadWarning).map(ResponsePdu::getValue).orElse(null);
	}

	public void setNearOverloadWarning(ResponsePdu nearOverloadWarning) {
		this.nearOverloadWarning = Optional.ofNullable(nearOverloadWarning).map(ResponsePdu::getValue).orElse(null);
	}

	public void setOverloadRestriction(ResponsePdu overloadRestriction) {
		this.overloadRestriction = Optional.ofNullable(overloadRestriction).map(ResponsePdu::getValue).orElse(null);
	}

	/** Sets an already-normalized overload restriction state, for generations whose reading is not numeric. */
	public void setOverloadRestriction(String overloadRestriction) {
		this.overloadRestriction = overloadRestriction;
	}

	/** Sets an already-normalized per-phase overload restriction state. */
	public void setPhaseOverloadRestriction(int phase, String overloadRestriction) {
		this.overloadRestrictions.put(phase, overloadRestriction);
	}

	public void setOverloadAlarm(ResponsePdu overloadAlarm) {
		this.overloadAlarm = Optional.ofNullable(overloadAlarm).map(ResponsePdu::getValue).orElse(null);
	}

	public void set3PhasesCurrent(ResponsePdu threePhasesCurrent) {
		var phaseCurrents = threePhasesCurrent.getValue().split("\n");
		for (String phaseCurrent : phaseCurrents) {
			String[] comp = phaseCurrent.split(Constant.COLON);
			this.currents.put(Integer.parseInt(comp[0].trim()), comp[1].trim());
		}
	}

	public void setPhaseLowLoadWarning(int phase, ResponsePdu lowLoadWarning) {
		var value = Optional.ofNullable(lowLoadWarning).map(ResponsePdu::getValue).orElse(null);
		this.lowLoadWarnings.put(phase, value);
	}

	public void setPhaseNearOverloadWarning(int phase, ResponsePdu nearOverloadWarning) {
		var value = Optional.ofNullable(nearOverloadWarning).map(ResponsePdu::getValue).orElse(null);
		this.nearOverloadWarnings.put(phase, value);
	}


	public void setPhaseOverloadRestriction(int phase, ResponsePdu overloadRestriction) {
		var value = Optional.ofNullable(overloadRestriction).map(ResponsePdu::getValue).orElse(null);
		this.overloadRestrictions.put(phase, value);
	}

	public void setPhaseOverloadAlarm(int phase, ResponsePdu overloadAlarm) {
		var value = Optional.ofNullable(overloadAlarm).map(ResponsePdu::getValue).orElse(null);
		this.overloadAlarms.put(phase, value);
	}

	/**
	 * Distributes a multi-phase reading across the per-phase maps.
	 *
	 * <p>The 2nd generation phase commands accept {@code all}, which returns one {@code <phase>: <value>} line per
	 * phase actually present. Reading with {@code all} rather than looping over phases 1..3 keeps the result correct
	 * regardless of how many phases the device has - an explicit {@code phLowLoad 2} is rejected with {@code E102} on a
	 * single-phase unit, so a fixed loop would depend on the phase count having been detected correctly.
	 *
	 * @param response the raw multi-phase response
	 * @param target the per-phase map to populate
	 * @param mapper converts a reported value into the stored representation
	 */
	private static void distributePhaseValues(RawResponse response, Map<Integer, String> target, UnaryOperator<String> mapper) {
		var raw = Optional.ofNullable(response).map(RawResponse::getValue).orElse(null);
		if (raw == null || raw.isBlank()) {
			LOG.warn("Skip distributing phase values: the response is null or blank");
			return;
		}
		for (String line : raw.split("\n")) {
			// Limit the split so that values containing a colon survive intact.
			var comp = line.split(Constant.COLON, 2);
			if (comp.length < 2) {
				continue;
			}
			try {
				target.put(Integer.parseInt(comp[0].trim()), mapper.apply(comp[1].trim()));
			} catch (NumberFormatException e) {
				LOG.warn("Skipping unparseable phase in line '%s'".formatted(line));
			}
		}
	}

	public void setPhaseCurrents(RawResponse currents) {
		distributePhaseValues(currents, this.currents, value -> Util.extractValue(value).orElse(null));
	}

	public void setPhaseLowLoadWarnings(RawResponse lowLoadWarnings) {
		distributePhaseValues(lowLoadWarnings, this.lowLoadWarnings, value -> Util.extractValue(value).orElse(null));
	}

	public void setPhaseNearOverloadWarnings(RawResponse nearOverloadWarnings) {
		distributePhaseValues(nearOverloadWarnings, this.nearOverloadWarnings, value -> Util.extractValue(value).orElse(null));
	}

	public void setPhaseOverloadAlarms(RawResponse overloadAlarms) {
		distributePhaseValues(overloadAlarms, this.overloadAlarms, value -> Util.extractValue(value).orElse(null));
	}

	public void setPhaseOverloadRestrictions(RawResponse overloadRestrictions) {
		distributePhaseValues(overloadRestrictions, this.overloadRestrictions, Util::toRestrictionState);
	}

	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class ResponsePdu extends BaseModel {
		String value;

		@Override
		public void parse(String response) {
			if (response == null || response.isBlank()) {
				this.log.warn("The response param is null or blank; ignore parsing the value");
				return;
			}
			var isMultipleValues = response.split("\n").length > 1;
			this.value = isMultipleValues ? response : Util.extractValue(response).orElse(null);
			if (this.value == null) {
				log.warn("Skip parsing Pdu: unknown response '%s'".formatted(response));
			}
		}
	}
}
