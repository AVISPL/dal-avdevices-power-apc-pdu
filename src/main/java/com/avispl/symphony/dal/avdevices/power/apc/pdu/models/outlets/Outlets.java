/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.PduGeneration;

/**
 * Represents the PDU's outlets, keyed by outlet number.
 *
 * <p>Outlets are enumerated from the outlet count the device reports about itself ({@code Outlets:} on {@code rpdu},
 * {@code Present Outlets:} on {@code rpdu2g}) and then filled in from the individual outlet commands. They are
 * deliberately not enumerated from the user-to-outlet assignment commands ({@code list} / {@code userList}): those
 * report access assignments rather than outlets, so an outlet reachable by three accounts was reported three times
 * over - 24 property groups for the 8 outlets an AP7920B actually has, and its own web UI shows.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Outlets {
	private static final Logger LOG = Logger.ofClass(Outlets.class);

	String outletNumbers;
	final Map<String, OutletDetail> outletDetails = new HashMap<>();

	/**
	 * Seeds the outlet set from the outlet count the device reports.
	 *
	 * @param outletTotal the reported outlet count; when {@code null} or non-numeric no outlets are seeded
	 */
	public void initialize(String outletTotal) {
		this.outletDetails.clear();
		this.outletNumbers = Constant.EMPTY;
		if (outletTotal == null || outletTotal.isBlank()) {
			LOG.warn("Cannot enumerate outlets: the device reported no outlet count");
			return;
		}
		int total;
		try {
			total = Integer.parseInt(outletTotal.trim());
		} catch (NumberFormatException e) {
			LOG.warn("Cannot enumerate outlets: unparseable outlet count '%s'".formatted(outletTotal));
			return;
		}
		var numbers = new LinkedHashSet<String>();
		for (int outlet = 1; outlet <= total; outlet++) {
			var key = String.valueOf(outlet);
			numbers.add(key);
			this.outletDetails.put(key, new OutletDetail());
		}
		this.outletNumbers = String.join(",", numbers);
	}

	/**
	 * Returns the outlet numbers in ascending numeric order, so generated properties are stably ordered.
	 *
	 * @return the ordered outlet numbers; empty when none were enumerated
	 */
	public List<String> getOrderedOutletNumbers() {
		return this.outletDetails.keySet().stream()
				.sorted(Comparator.comparingInt(Integer::parseInt))
				.toList();
	}

	/**
	 * Applies outlet power statuses.
	 *
	 * <p>The two generations permute the fields: {@code rpdu} reports {@code number:STATUS:name} whereas
	 * {@code rpdu2g} reports {@code number: name: status}. Reading the wrong index fails silently - the status column
	 * would be populated with the outlet name - so the order is selected explicitly per generation.
	 *
	 * @param status the raw response
	 * @param generation the firmware generation that produced it
	 */
	public void setStatuses(ResponseOutlets status, PduGeneration generation) {
		var nameIndex = generation.is2G() ? 1 : 2;
		var statusIndex = generation.is2G() ? 2 : 1;
		for (String rawLine : split(status)) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				LOG.warn("Unknown response from status command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			outletDetail.setName(comp[nameIndex]);
			outletDetail.setPowerStatus(comp[statusIndex]);
		}
	}

	public void setPowerOffDelays(ResponseOutlets powerOffDelays) {
		for (String rawLine : split(powerOffDelays)) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				LOG.warn("Unknown response from power off delay command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			// Normalize the -1 sentinel onto the word, so the stored value is the same whichever spelling was reported.
			var disabled = Util.isDelayDisabled(comp[2]);
			outletDetail.setPowerOffDelay(disabled ? Constant.NEVER : Util.extractValue(comp[2]).orElse(null));
			outletDetail.setPowerOffDelayDisabled(disabled);
		}
	}

	public void setPowerOnDelays(ResponseOutlets powerOnDelays) {
		for (String rawLine : split(powerOnDelays)) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				LOG.warn("Unknown response from power on delay command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			var disabled = Util.isDelayDisabled(comp[2]);
			outletDetail.setPowerOnDelay(disabled ? Constant.NEVER : Util.extractValue(comp[2]).orElse(null));
			outletDetail.setPowerOnDelayDisabled(disabled);
		}
	}

	public void setRebootDurations(ResponseOutlets rebootDurations) {
		for (String rawLine : split(rebootDurations)) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				LOG.warn("Unknown response from reboot duration command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			outletDetail.setRebootDuration(Util.extractValue(comp[2]).orElse(null));
		}
	}

	private static String[] split(ResponseOutlets response) {
		return response == null || response.value == null ? new String[0] : response.value.split("\n");
	}

	/**
	 * Splits a colon-delimited outlet line into trimmed components.
	 *
	 * <p>Trimming matters because {@code rpdu2g} pads the outlet number with a leading space and the status with a
	 * trailing one; without it the outlet number would not match the enumerated keys.
	 */
	private static String[] components(String rawLine) {
		return Arrays.stream(rawLine.split(Constant.COLON)).map(String::trim).toArray(String[]::new);
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
