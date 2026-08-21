/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.models;

import java.util.LinkedHashMap;
import java.util.Map;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseModel;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;

/**
 * Represents the response of the 2nd generation {@code about} command.
 *
 * <p>The response is grouped into sections, each introduced by a bare title line followed by a dashed rule:
 *
 * <pre>
 * Hardware Factory
 * ---------------
 * Model Number:           AP7920B
 * Serial Number:          2A2548L01451
 *
 * Network Management Card
 * ---------------
 * Model Number:           0N-1570-07K
 *
 * Application Module
 * ---------------
 * Name:                   rpdu2g
 * Version:                v2.5.2.5
 *
 * APC OS(AOS)
 * ---------------
 * Name:                   aos
 * Version:                v2.5.3.2
 * </pre>
 *
 * <p>Keys repeat across sections - {@code Model Number} identifies the PDU chassis in one section and the management
 * card in another, and {@code Version} appears once per firmware module. Parsing therefore has to be section-scoped;
 * flat line-prefix matching silently returns whichever occurrence happens to come last.
 *
 * @author Symphony Dev Team
 * @since 1.1.0
 */
public class AboutInformation extends BaseModel {
	/** Section title to its key/value pairs, in encounter order. */
	private final Map<String, Map<String, String>> sections = new LinkedHashMap<>();

	/**
	 * Returns a value scoped to the section it belongs to.
	 *
	 * @param section the section title, e.g. {@link Constant#SECTION_APPLICATION_MODULE}
	 * @param key the key within that section, e.g. {@link Constant#KEY_VERSION}
	 * @return the trimmed value, or {@code null} when either the section or the key is absent
	 */
	public String get(String section, String key) {
		return this.sections.getOrDefault(section, Map.of()).get(key);
	}

	/** @return the PDU chassis model, e.g. {@code AP7920B} */
	public String getModel() {
		return this.get(Constant.SECTION_HARDWARE_FACTORY, Constant.KEY_MODEL_NUMBER);
	}

	/** @return the PDU chassis serial number */
	public String getSerialNumber() {
		return this.get(Constant.SECTION_HARDWARE_FACTORY, Constant.KEY_SERIAL_NUMBER);
	}

	/** @return the management card model, e.g. {@code 0N-1570-07K} */
	public String getNetworkManagementCardModel() {
		return this.get(Constant.SECTION_NETWORK_MANAGEMENT_CARD, Constant.KEY_MODEL_NUMBER);
	}

	/** @return the application module name - the firmware generation discriminator, e.g. {@code rpdu2g} */
	public String getApplicationModule() {
		return this.get(Constant.SECTION_APPLICATION_MODULE, Constant.KEY_NAME);
	}

	/** @return the application module version, e.g. {@code v2.5.2.5} */
	public String getApplicationVersion() {
		return this.get(Constant.SECTION_APPLICATION_MODULE, Constant.KEY_VERSION);
	}

	/** @return the AOS version, e.g. {@code v2.5.3.2} */
	public String getAosVersion() {
		return this.get(Constant.SECTION_AOS, Constant.KEY_VERSION);
	}

	@Override
	public void parse(String response) {
		if (response == null || response.isBlank()) {
			this.log.warn("The response param is null or blank; ignore parsing About information");
			return;
		}
		String section = null;
		for (String raw : response.split("\\r?\\n")) {
			var line = raw.trim();
			if (line.isEmpty() || line.chars().allMatch(c -> c == '-')) {
				continue;
			}
			var separator = line.indexOf(':');
			// A line without a colon - or whose colon belongs to the title itself, as in `APC OS(AOS)` - starts a
			// new section. Key lines always carry a colon before any other structure.
			if (separator < 0) {
				section = line;
				continue;
			}
			var key = line.substring(0, separator).trim();
			var value = line.substring(separator + 1).trim();
			if (value.isEmpty()) {
				section = key;
				continue;
			}
			if (section == null) {
				this.log.warn("Skipping '%s'; no section header seen yet".formatted(line));
				continue;
			}
			this.sections.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, value);
		}
		if (this.sections.isEmpty()) {
			this.log.warn("Parsed no sections from the About response");
		}
	}
}
