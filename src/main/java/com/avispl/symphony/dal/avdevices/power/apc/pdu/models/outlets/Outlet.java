/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;

/**
 * Represents outlet information parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Outlet {
	String source;
	String username;
	String outlets;
	Map<String, OutletDetail> outletDetails;

	public void setStatuses(ResponseOutlet status) {
		var rawLines = status.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.get(comp[0]);
			outletDetail.name = comp[2];
			outletDetail.powerStatus = comp[1];
		}
	}

	public void setPowerOffDelays(ResponseOutlet status) {
		var rawLines = status.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.get(comp[0]);
			outletDetail.powerOffDelay = Util.extractValue(comp[2]);
		}
	}

	public void setPowerOnDelays(ResponseOutlet status) {
		var rawLines = status.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.get(comp[0]);
			outletDetail.powerOnDelay = Util.extractValue(comp[2]);
		}
	}

	public void setRebootDurations(ResponseOutlet status) {
		var rawLines = status.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.get(comp[0]);
			outletDetail.rebootDuration = Util.extractValue(comp[2]);
		}
	}

	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class OutletDetail {
		String name;
		String powerOffDelay;
		boolean isNeverPowerOffDelay;
		String powerOnDelay;
		boolean isNeverPowerOnDelay;
		String powerStatus;
		String rebootDuration;
	}

	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class ResponseOutlet extends BaseModel {
		String value;

		@Override
		public void parse(String response) {
			if (response == null || response.isBlank()) {
				this.log.warn("The response param is null or blank; ignore parsing the value");
				return;
			}
			this.value = response;
		}
	}
}
