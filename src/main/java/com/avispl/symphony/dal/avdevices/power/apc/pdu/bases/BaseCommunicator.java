/** Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.bases;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

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
		// Terminators must cover both firmware generations: they are fixed before any I/O happens, so the
		// generation is not yet known here. ShellCommunicator#doneReading matches with case-sensitive
		// String#endsWith, hence both prompt spellings have to be listed explicitly.
		super.setCommandSuccessList(List.of(Constant.PROMPT_COMMAND, Constant.PROMPT_COMMAND_2G, "Bye."));
		// Only 1st generation message text is enumerated here. This list is an early-exit optimisation for the read
		// loop, not the error classifier - the prompt above terminates the read on either generation and
		// ERROR_RESPONSE_PATTERN does the actual classification. 2nd generation messages are deliberately omitted
		// because endsWith cannot match on a code prefix and its message text differs per command.
		super.setCommandErrorList(List.of(
				"E100: Command does not exist.",
				"E101: Invalid command arguments.",
				"E102: User already exists.",
				"E103: User does not exist.",
				"E104: User does not have access to this command.",
				"E200: Input error."
		));
		super.setLoginSuccessList(List.of(Constant.PROMPT_COMMAND, Constant.PROMPT_COMMAND_2G));
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
			var normalizeResponse = this.normalizeResponse(response, request);
			if (Constant.ERROR_RESPONSE_PATTERN.matcher(response).find()) {
				throw new CommandFailureException(this.host, request, response, 400);
			}
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
	 * Sends a control command to the target device and validates the response.
	 * <p>Propagates connection-related exceptions such as {@link SocketTimeoutException}
	 * and {@link FailedLoginException} without wrapping.</p>
	 *
	 * @param request the command request to be sent
	 * @throws SocketTimeoutException if the request times out
	 * @throws FailedLoginException if authentication fails
	 * @throws CommandFailureException if the response is invalid or indicates an error
	 * @throws ResourceNotReachableException if an unexpected error occurs while sending the request
	 */
	protected void sendControl(String request) throws Exception {
		try {
			var response = super.send(request);
			if (response == null || response.trim().isEmpty()) {
				throw new CommandFailureException(this.host, request, response, 502);
			}
			var normalizeResponse = this.normalizeResponse(response, request);
			if (Constant.ERROR_RESPONSE_PATTERN.matcher(normalizeResponse).find()) {
				throw new CommandFailureException(this.host, request, normalizeResponse, 400);
			}
		} catch (SocketTimeoutException | FailedLoginException e) {
			throw e;
		} catch (CommandFailureException e) {
			// Surface the device's own status line. A control can be rejected because the value is not in the
			// command's grammar on this firmware generation - for example disabling an outlet delay writes `never`,
			// which `rpdu` documents but `rpdu2g` does not list for olOnDelay/olOffDelay. Failing with the device's
			// message is deliberate: silently substituting a numeric delay would report the control as disabled
			// while the device kept it enabled.
			throw new IllegalStateException("Device rejected control command '%s': %s"
					.formatted(request, describeFailure(e.getResponse())), e);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to send control command '%s'".formatted(request), e);
		}
	}

	/** Reduces a rejection response to its status line, discarding any usage block the device appended. */
	private static String describeFailure(String response) {
		if (response == null || response.isBlank()) {
			return "no response";
		}
		return response.lines()
				.map(String::trim)
				.filter(line -> !line.isEmpty())
				.findFirst()
				.orElse("no response");
	}

	/**
	 * Sends a command and returns its normalized response only if the device accepted it.
	 *
	 * <p>Unlike {@link #send(String, Class)} this does not throw when the device rejects the command, which makes it
	 * usable for probing whether a verb exists on the connected firmware generation.
	 *
	 * @param request command string to send
	 * @return the normalized response, or {@link Optional#empty()} if the response was blank or carried an error code
	 * @throws SocketTimeoutException if the request times out
	 * @throws FailedLoginException if authentication fails
	 */
	protected Optional<String> trySend(String request) throws Exception {
		var response = super.send(request);
		if (response == null || response.trim().isEmpty()) {
			return Optional.empty();
		}
		var normalizeResponse = this.normalizeResponse(response, request);
		if (Constant.ERROR_RESPONSE_PATTERN.matcher(normalizeResponse).find()) {
			this.log.debug("Device rejected probe command '%s'".formatted(request));
			return Optional.empty();
		}
		return Optional.of(normalizeResponse);
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
		// normalize line break + remove prompt (either generation's spelling)
		response = response.replace("\r\n", "\n")
				.replace("\r", "\n");
		response = Constant.PROMPT_PATTERN.matcher(response).replaceFirst(Constant.EMPTY);
		// remove response status - `OK` on 1st generation, `E000: Success` on 2nd. Anchored to a whole line so that
		// an outlet named e.g. "OK-rack" is not mangled.
		response = Constant.SUCCESS_MARKER_PATTERN.matcher(response).replaceFirst(Constant.EMPTY).trim();

		return response;
	}
}
