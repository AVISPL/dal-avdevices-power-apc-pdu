/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.power.apc.pdu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseCommunicator;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers.MonitoringHelper;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;

/**
 * APCPDUCommunicator class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public class APCPDUCommunicator extends BaseCommunicator implements Monitorable, Controller {
	/** Stores extended statistics to be sent to the adapter. */
	private final ExtendedStatistics localExtendedStatistics;
	/** Application configuration loaded from {@code version.properties}. */
	private final Properties versionProperties;
	/** Device adapter instantiation timestamp. */
	private final long adapterInitializationTimestamp;

	public APCPDUCommunicator() {
		this.localExtendedStatistics = new ExtendedStatistics();
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		this.versionProperties = new Properties();
		this.adapterInitializationTimestamp = System.currentTimeMillis();
	}

	@Override
	protected void internalInit() throws Exception {
		this.loadVersionProperties();
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		this.localExtendedStatistics.getStatistics().clear();
		this.localExtendedStatistics.getControllableProperties().clear();
		this.versionProperties.clear();
		super.internalDestroy();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		this.reentrantLock.lock();
		try {
			var statistics = new HashMap<String, String>();
			var controllableProperties = new ArrayList<AdvancedControllableProperty>();

			statistics.putAll(MonitoringHelper.generateAdapterMetadata(this.versionProperties));

			this.localExtendedStatistics.setStatistics(statistics);
			this.localExtendedStatistics.setControllableProperties(controllableProperties);
		} finally {
			this.reentrantLock.unlock();
		}
		return Collections.singletonList(this.localExtendedStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {

	}

	@Override
	public void controlProperties(List<ControllableProperty> list) throws Exception {

	}

	/**
	 * Loads version-related properties from the {@code version.properties} file
	 * located in the classpath and updates runtime-specific values.
	 */
	private void loadVersionProperties() {
		try {
			this.versionProperties.load(this.getClass().getResourceAsStream("/version.properties"));
		} catch (Exception e) {
			this.log.error("Failed to load the version.properties file", e);
		}
		this.versionProperties.setProperty(AdapterMetadata.ADAPTER_UPTIME.getProperty(), String.valueOf(this.adapterInitializationTimestamp));
	}
}
