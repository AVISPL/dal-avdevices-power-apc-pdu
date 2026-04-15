/** Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu.bases;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Logger;
import com.avispl.symphony.dal.communicator.TelnetCommunicator;

/**
 * Configures the communicator and provides helper methods for managing adapter properties.
 * <p>This class centralizes all communicator-related configuration and exposes utility methods to access adapter properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public abstract class BaseCommunicator extends TelnetCommunicator {
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
		super.setLoginPrompt("User Name: ");
		super.setPasswordPrompt("Password: ");
		super.setCommandSuccessList(List.of("APC>"));
		super.setCommandErrorList(List.of("Error"));
		super.setLoginSuccessList(List.of("APC>"));
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		super.internalDestroy();
	}
}
