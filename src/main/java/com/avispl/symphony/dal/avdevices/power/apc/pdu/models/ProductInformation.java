/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models;

import java.util.LinkedHashMap;
import java.util.Map;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;

/**
 * Represents the response of the 2nd generation {@code prodInfo} command.
 *
 * <p>{@code prodInfo} is the closest counterpart to 1st generation {@code ver}: a flat list of {@code Key: value}
 * lines covering both identity and the electrical characteristics. The 2nd generation {@code about} command reports
 * neither outlet count, maximum current nor phase count, so it cannot serve this purpose on its own.
 *
 * <pre>
 * AOS:              2.5.3.2
 * Switched Rack PDU: 2.5.2.5
 * Model:            AP7920B
 * Present Outlets:  8
 * Max Current:      10 A
 * Present Phases:   1
 * </pre>
 *
 * <p>Values are looked up by exact key rather than by line prefix, because several keys share a suffix
 * ({@code Present Outlets} / {@code Switched Outlets} / {@code Metered Outlets}).
 *
 * @author Symphony Dev Team
 * @since 1.1.0
 */
public class ProductInformation extends BaseModel {
	public static final String KEY_AOS = "AOS";
	public static final String KEY_PDU = "Switched Rack PDU";
	public static final String KEY_MODEL = "Model";
	public static final String KEY_PRESENT_OUTLETS = "Present Outlets";
	public static final String KEY_MAX_CURRENT = "Max Current";
	public static final String KEY_PRESENT_PHASES = "Present Phases";

	private final Map<String, String> values = new LinkedHashMap<>();

	/**
	 * Returns a reported value by its exact key.
	 *
	 * @param key the key as printed by the device, without the colon
	 * @return the trimmed value, or {@code null} when absent
	 */
	public String get(String key) {
		return this.values.get(key);
	}

	@Override
	public void parse(String response) {
		if (response == null || response.isBlank()) {
			this.log.warn("The response param is null or blank; ignore parsing Product information");
			return;
		}
		for (String raw : response.split("\\r?\\n")) {
			var line = raw.trim();
			var separator = line.indexOf(':');
			if (line.isEmpty() || separator <= 0) {
				continue;
			}
			var key = line.substring(0, separator).trim();
			var value = line.substring(separator + 1).trim();
			if (!value.isEmpty()) {
				this.values.put(key, value);
			}
		}
		if (this.values.isEmpty()) {
			this.log.warn("Parsed no values from the Product information response");
		}
	}
}
