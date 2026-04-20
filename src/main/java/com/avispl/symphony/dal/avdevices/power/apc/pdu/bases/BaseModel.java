/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.bases;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;

/**
 * Base class for all response models parsed from device commands.
 *
 * <p>Subclasses are responsible for implementing the parsing logic
 * to map raw response strings into structured fields.</p>
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public abstract class BaseModel {
	protected final Logger log = Logger.ofClass(getClass());

	/**
	 * Parses the raw response string and maps it to the model fields.
	 * <p>Implementations should handle response normalization and validation
	 * if required.</p>
	 *
	 * @param response raw response string from device command (non-null, may be normalized)
	 */
	public abstract void parse(String response);
}
