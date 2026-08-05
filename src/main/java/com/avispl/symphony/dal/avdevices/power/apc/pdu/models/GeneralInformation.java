/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models;

import java.util.Map;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.InputType;

/**
 * Represents general device information parsed from command response.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneralInformation extends BaseModel {
	String aosVersion;
	String pduVersion;
	String model;
	String inputType;
	String maxLoad;
	String outlets;

	/**
	 * Mapping between line prefixes in the raw response and field setters.
	 *
	 * <p>Each entry defines how a line starting with a specific prefix should be
	 * parsed and mapped to a field of {@code GeneralInformation}. When a line
	 * matches a prefix, the prefix is removed and the remaining value is passed
	 * to the corresponding {@link Consumer}.</p>
	 */
	private final Map<String, Consumer<String>> mappers = Map.of(
			"APC OS", v -> this.aosVersion = v,
			"Switched Rack PDU", v -> this.pduVersion = v,
			"Model:", v -> this.model = v,
			"Outlets:", v -> this.outlets = v,
			"Max Current:", v -> this.maxLoad = v,
			"Input Type:", v -> this.inputType = v
	);

	/**
	 * Builds general information from a 2nd generation {@code prodInfo} response.
	 *
	 * <p>Populates exactly the same fields 1st generation {@code ver} does, so that the resulting Symphony property
	 * set is identical across generations. {@code Input Type} has no direct counterpart and is derived from
	 * {@code Present Phases}, which is the value {@link InputType#is3Phases(String)} needs.
	 *
	 * @param product the parsed {@code prodInfo} response (must not be {@code null})
	 * @return general information carrying the same fields as the 1st generation equivalent
	 */
	public static GeneralInformation ofProductInformation(ProductInformation product) {
		var general = new GeneralInformation();
		general.aosVersion = product.get(ProductInformation.KEY_AOS);
		general.pduVersion = product.get(ProductInformation.KEY_PDU);
		general.model = product.get(ProductInformation.KEY_MODEL);
		general.outlets = product.get(ProductInformation.KEY_PRESENT_OUTLETS);
		general.maxLoad = product.get(ProductInformation.KEY_MAX_CURRENT);
		general.inputType = toInputType(product.get(ProductInformation.KEY_PRESENT_PHASES));

		return general;
	}

	/**
	 * Maps a 2nd generation phase count onto the 1st generation {@code Input Type} vocabulary.
	 *
	 * <p>{@code prodInfo} reports a count, whereas {@code ver} reports {@code single-phase} / {@code banked} /
	 * {@code 3-phase}. A count of 3 is the only value that changes adapter behaviour; anything else is reported as
	 * single-phase, since the count alone cannot distinguish single from banked.
	 *
	 * @param presentPhases the reported phase count; may be {@code null}
	 * @return the matching {@code Input Type} value, or {@code null} when the count is absent
	 */
	private static String toInputType(String presentPhases) {
		if (presentPhases == null || presentPhases.isBlank()) {
			return null;
		}
		return String.valueOf(Constant.MAX_PHASE).equals(presentPhases.trim())
				? InputType.THREE_PHASE.getValue()
				: InputType.SINGLE_PHASE.getValue();
	}

	/**
	 * Parses raw CLI response text and maps extracted values to this {@code GeneralInformation} object.
	 *
	 * <p>The method processes the response line by line. Each non-empty line is
	 * matched against a set of predefined prefixes (configured in {@code mappers}).
	 * When a line starts with a known prefix, the prefix is removed and the
	 * remaining value is trimmed and assigned to the corresponding field via
	 * a {@link Consumer}.</p>
	 *
	 * @param response raw response string from the device; if {@code null} or blank,
	 * the method logs a warning and performs no parsing
	 */
	@Override
	public void parse(String response) {
		if (response == null || response.isBlank()) {
			this.log.warn("The response param is null or blank; ignore parsing General information");
			return;
		}
		for (String raw : response.split("\\r?\\n")) {
			String line = raw.trim();
			if (line.isEmpty()) {
				continue;
			}
			for (var entry : mappers.entrySet()) {
				String prefix = entry.getKey();
				if (line.startsWith(prefix)) {
					String value = line.substring(prefix.length()).trim();
					entry.getValue().accept(value);
					break;
				}
			}
		}
	}
}
