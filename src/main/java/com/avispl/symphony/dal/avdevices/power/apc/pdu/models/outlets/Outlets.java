/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.PduGeneration;

/**
 * Represents outlets parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Outlets extends BaseModel {
	private static final Logger LOG = Logger.ofClass(Outlets.class);
	/** First column header of the 2nd generation {@code userList} table. */
	private static final String COLUMN_HEADER_NAME = "Name";
	/** {@code userList} columns: Name, User Type, Status, Outlets. */
	private static final int USER_LIST_COLUMNS = 4;
	/** Source recorded for 2nd generation users, matching what 1st generation reports for local users. */
	private static final String SOURCE_LOCAL = "Local";

	final List<OutletUser> outletUsers = new ArrayList<>();
	String outletNumbers;
	final Map<String, OutletDetail> outletDetails = new HashMap<>();

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
				this.log.warn("Unknown response from status command: '%s'".formatted(rawLine));
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
				this.log.warn("Unknown response from power off delay command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			var value = Util.extractValue(comp[2]).orElse(null);
			outletDetail.setPowerOffDelay(value);
			outletDetail.setPowerOffDelayDisabled(Constant.NEVER.equals(value));
		}
	}

	public void setPowerOnDelays(ResponseOutlets powerOnDelays) {
		for (String rawLine : split(powerOnDelays)) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				this.log.warn("Unknown response from power on delay command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			var value = Util.extractValue(comp[2]).orElse(null);
			outletDetail.setPowerOnDelay(value);
			outletDetail.setPowerOnDelayDisabled(Constant.NEVER.equals(value));
		}
	}

	public void setRebootDurations(ResponseOutlets rebootDurations) {
		for (String rawLine : split(rebootDurations)) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				this.log.warn("Unknown response from reboot duration command: '%s'".formatted(rawLine));
				continue;
			}
			var outletDetail = this.outletDetails.computeIfAbsent(comp[0], key -> new OutletDetail());
			outletDetail.setRebootDuration(Util.extractValue(comp[2]).orElse(null));
		}
	}

	private static String[] split(ResponseOutlets response) {
		return response.value == null ? new String[0] : response.value.split("\n");
	}

	/**
	 * Splits a colon-delimited outlet line into trimmed components.
	 *
	 * <p>Trimming matters because {@code rpdu2g} pads the outlet number with a leading space and the status with a
	 * trailing one; without it the outlet number would not match the keys derived from the user list.
	 */
	private static String[] components(String rawLine) {
		return Arrays.stream(rawLine.split(Constant.COLON)).map(String::trim).toArray(String[]::new);
	}

	@Override
	public void parse(String response) {
		if (response == null || response.isBlank()) {
			this.log.warn("The response param is null or blank; ignore parsing the value");
			return;
		}
		var rawLines = response.split("\n");
		for (String rawLine : rawLines) {
			var comp = components(rawLine);
			if (comp.length < 3) {
				this.log.warn("Unknown response from list command: '%s'".formatted(rawLine));
				continue;
			}
			var outletNumbersOfUser = Arrays.stream(comp[2].split(",")).map(String::trim).collect(Collectors.toSet());
			this.outletUsers.add(new OutletUser(comp[0], comp[1], outletNumbersOfUser));
		}
		this.recomputeOutletNumbers();
	}

	/**
	 * Applies the outlet-to-user assignments reported by the 2nd generation {@code userList} command.
	 *
	 * <p>The response is a fixed-width table rather than the colon-delimited lines of 1st generation {@code list}, and
	 * it expresses outlet assignments as ranges:
	 *
	 * <pre>
	 * Name                 User Type            Status    Outlets
	 * ----                 ---------            ------    -------
	 * apc                  Super                ******    1-8
	 * </pre>
	 *
	 * <p>There is no source column, so the source is recorded as {@code Local} - which is what 1st generation reports
	 * for the same locally defined users. That keeps the generated property names ({@code Local_Apc_Outlet_01}, ...)
	 * identical across generations.
	 *
	 * @param userList the raw {@code userList} response
	 */
	public void setUsers(ResponseOutlets userList) {
		for (String rawLine : split(userList)) {
			var line = rawLine.strip();
			if (line.isEmpty() || line.startsWith(COLUMN_HEADER_NAME) || isRule(line)) {
				continue;
			}
			var comp = line.split("\\s{2,}");
			if (comp.length < USER_LIST_COLUMNS) {
				this.log.warn("Unknown response from user list command: '%s'".formatted(rawLine));
				continue;
			}
			var outletNumbersOfUser = expandOutletNumbers(comp[USER_LIST_COLUMNS - 1]);
			if (outletNumbersOfUser.isEmpty()) {
				this.log.warn("No outlets assigned to user '%s'; skipping".formatted(comp[0]));
				continue;
			}
			this.outletUsers.add(new OutletUser(SOURCE_LOCAL, comp[0], outletNumbersOfUser));
		}
		this.recomputeOutletNumbers();
	}

	private void recomputeOutletNumbers() {
		this.outletNumbers = this.outletUsers.stream().flatMap(user -> user.getOutletNumbers().stream())
				.filter(number -> !number.isBlank()).distinct()
				.collect(Collectors.joining(","));
	}

	/** Matches the dashed rule that separates the {@code userList} header from its rows. */
	private static boolean isRule(String line) {
		return line.chars().allMatch(c -> c == '-' || c == ' ');
	}

	/**
	 * Expands an outlet assignment expression into individual outlet numbers.
	 *
	 * <p>Accepts the range notation used by {@code userList} ({@code 1-8}) as well as comma-separated numbers, in any
	 * combination ({@code 1-4,6}).
	 *
	 * @param expression the assignment expression
	 * @return the individual outlet numbers, in ascending encounter order; empty when nothing could be parsed
	 */
	private static Set<String> expandOutletNumbers(String expression) {
		var numbers = new LinkedHashSet<String>();
		if (expression == null || expression.isBlank()) {
			return numbers;
		}
		for (String part : expression.split(",")) {
			var token = part.trim();
			if (token.isEmpty()) {
				continue;
			}
			var range = token.split("-");
			try {
				if (range.length == 2) {
					var from = Integer.parseInt(range[0].trim());
					var to = Integer.parseInt(range[1].trim());
					for (int outlet = from; outlet <= to; outlet++) {
						numbers.add(String.valueOf(outlet));
					}
				} else {
					numbers.add(String.valueOf(Integer.parseInt(token)));
				}
			} catch (NumberFormatException e) {
				LOG.warn("Skipping unparseable outlet assignment '%s'".formatted(token));
			}
		}

		return numbers;
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
