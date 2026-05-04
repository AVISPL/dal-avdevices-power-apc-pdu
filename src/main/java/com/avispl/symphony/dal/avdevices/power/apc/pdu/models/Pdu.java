/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
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
	String coldStartDelay;
	String oldColdStartDelay;
	boolean isNeverColdStartDelay;
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
		return this.isNeverColdStartDelay ? Constant.MIN_VALUE : this.coldStartDelay;
	}

	public void setColdStartDelay(ResponsePdu coldStartDelay) {
		String value = Optional.ofNullable(coldStartDelay).map(ResponsePdu::getValue).orElse(null);
		if (null == value) {
			this.coldStartDelay = null;
			this.oldColdStartDelay = null;
			this.isNeverColdStartDelay = false;
			return;
		}
		if (Constant.NEVER.equals(value)) {
			var isNullOrNever = null == this.coldStartDelay || Constant.NEVER.equals(this.coldStartDelay);
			this.oldColdStartDelay = isNullOrNever ? Constant.MIN_VALUE : this.coldStartDelay;
			this.isNeverColdStartDelay = true;
		} else {
			this.oldColdStartDelay = null;
			this.isNeverColdStartDelay = false;
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

	public void setOverloadAlarm(ResponsePdu overloadAlarm) {
		this.overloadAlarm = Optional.ofNullable(overloadAlarm).map(ResponsePdu::getValue).orElse(null);
	}

	public void set3PhasesCurrent(ResponsePdu threePhasesCurrent) {
		var phaseCurrents = threePhasesCurrent.getValue().split("\n");
		for (String phaseCurrent : phaseCurrents) {
			String[] comp = phaseCurrent.split(":");
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
