/** Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.bases;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.error.CommandFailureException;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;
import com.avispl.symphony.dal.communicator.SshCommunicator;

/**
 * Configures the communicator and provides helper methods for managing adapter properties.
 * <p>This class centralizes all communicator-related configuration and exposes utility methods to access adapter properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public abstract class BaseCommunicator extends SshCommunicator {
	/** Lock for thread-safe operations. */
	protected final ReentrantLock reentrantLock;
	/** Logger used for recording diagnostic and runtime information. */
	protected final Logger log;

	protected BaseCommunicator() {
		this.reentrantLock = new ReentrantLock();
		this.log = new Logger(super.logger);
	}

	@Override
	protected void internalInit() throws Exception {
		super.setCommandSuccessList(List.of(Constant.PROMPT_COMMAND, "Bye."));
		super.setCommandErrorList(List.of("Error"));
		super.setLoginSuccessList(List.of(Constant.PROMPT_COMMAND));
		super.setLoginErrorList(List.of("Login failed."));
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		super.internalDestroy();
	}

	/**
	 * Sends a command to the target host, validates and normalizes the raw response,
	 * then parses it into a concrete {@link BaseModel} instance.
	 *
	 * @param <T> concrete type extending {@link BaseModel}
	 * @param request command string to send
	 * @param clazz target model class used to parse the response
	 * @return parsed instance of {@code T}
	 * @throws CommandFailureException if response is invalid or indicates command failure
	 * @throws SocketTimeoutException if the request times out
	 * @throws FailedLoginException if authentication fails
	 * @throws ResourceNotReachableException for any unexpected error during execution or parsing
	 */
	protected <T extends BaseModel> T send(String request, Class<T> clazz) throws Exception {
		try {
			var response = super.send(request);
			if (response == null || response.trim().isEmpty()) {
				throw new CommandFailureException(this.host, request, response, 502);
			}
			if (Constant.ERROR_RESPONSE_PATTERN.matcher(response).find()) {
				throw new CommandFailureException(this.host, request, response, 400);
			}
			var normalizeResponse = this.normalizeResponse(response, request);
			var instance = clazz.getDeclaredConstructor().newInstance();
			instance.parse(normalizeResponse);

			return instance;
		} catch (SocketTimeoutException | FailedLoginException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceNotReachableException("Failed to send command %s".formatted(request), e);
		}
	}

	/**
	 * Normalizes raw device response into a clean, parable format.
	 * <p>This method prepares the response for consistent parsing across different
	 * devices or terminal formats.</p>
	 *
	 * @param response raw response returned from the device
	 * @param request original command sent (used to strip echoed input)
	 * @return normalized response string ready for parsing
	 */
	private String normalizeResponse(String response, String request) {
		// remove request prefix
		if (response.startsWith(request)) {
			response = response.substring(request.length());
		}
		// normalize line break + remove prompt
		response = response.replace("\r\n", "\n")
				.replace("\r", "\n")
				.replaceFirst(Pattern.quote(Constant.PROMPT_COMMAND) + "\\s*$", Constant.EMPTY)
				.trim();
		// remove response status
		response = response.replaceFirst("OK", Constant.EMPTY);

		return response;
	}
}
