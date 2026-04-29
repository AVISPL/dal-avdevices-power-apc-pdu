/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlet.OutletDetail;

/**
 * Represents outlets parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutletList extends BaseModel {
	final List<Outlet> outlets = new ArrayList<>();

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
			var outletDetails = Arrays.stream(comp[2].split(",")).collect(Collectors.toMap(
					String::trim, value -> new OutletDetail()
			));
			var detailOutlet = new Outlet(comp[0], comp[1], comp[2], outletDetails);
			this.outlets.add(detailOutlet);
		}
	}
}
