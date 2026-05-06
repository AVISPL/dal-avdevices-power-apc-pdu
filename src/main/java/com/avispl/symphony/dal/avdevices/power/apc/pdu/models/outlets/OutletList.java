/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;

/**
 * Represents outlets parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutletList extends BaseModel {
	final List<OutletUser> outletUsers = new ArrayList<>();
	String outletNumbers;
	final Map<String, Outlet> outletDetails = new HashMap<>();

	public void setStatuses(ResponseOutlets status) {
		var rawLines = status.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new Outlet());
			outletDetail.setName(comp[2]);
			outletDetail.setPowerStatus(comp[1]);
		}
	}

	public void setPowerOffDelays(ResponseOutlets powerOffDelays) {
		var rawLines = powerOffDelays.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new Outlet());
			var value = Util.extractValue(comp[2]).orElse(null);
			outletDetail.setPowerOffDelay(value);
			outletDetail.setNeverPowerOffDelay(Constant.NEVER.equals(value));
		}
	}

	public void setPowerOnDelays(ResponseOutlets powerOnDelays) {
		var rawLines = powerOnDelays.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.get(comp[0]);
			var value = Util.extractValue(comp[2]).orElse(null);
			outletDetail.setPowerOnDelay(value);
			outletDetail.setNeverPowerOnDelay(Constant.NEVER.equals(value));
		}
	}

	public void setRebootDurations(ResponseOutlets rebootDurations) {
		var rawLines = rebootDurations.value.split("\n");
		for (String rawLine : rawLines) {
			var comp = rawLine.split(":");
			var outletDetail = this.outletDetails.get(comp[0]);
			outletDetail.setRebootDuration(Util.extractValue(comp[2]).orElse(null));
		}
	}

	@Override
	public void parse(String response) {
		if (response == null || response.isBlank()) {
			this.log.warn("The response param is null or blank; ignore parsing the value");
			return;
		}
		var rawLines = response.split("\n");
		for (String rawLine : rawLines) {
			var comp = Arrays.stream(rawLine.split(":")).map(String::trim).toArray(String[]::new);
			if (comp.length < 3) {
				this.log.warn("Unknown response from list command: '%s'".formatted(response));
			}
			var outletNumbersOfUser = Arrays.stream(comp[2].split(",")).map(String::trim).collect(Collectors.toSet());
			this.outletUsers.add(new OutletUser(comp[0], comp[1], outletNumbersOfUser));
		}
		this.outletNumbers = this.outletUsers.stream().flatMap(user -> user.getOutletNumbers().stream())
				.filter(number -> !number.isBlank()).distinct()
				.collect(Collectors.joining(","));
	}

	@Getter
	@FieldDefaults(level = AccessLevel.PRIVATE)
	public static class ResponseOutlets extends BaseModel {
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
