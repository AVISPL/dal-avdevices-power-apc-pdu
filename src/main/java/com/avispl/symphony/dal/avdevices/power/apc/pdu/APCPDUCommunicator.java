/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.avdevices.power.apc.pdu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.CollectionUtils;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseCommunicator;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers.ControllerHelper;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers.MonitoringHelper;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.GeneralInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu.ResponsePdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.OutletList;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.OutletList.ResponseOutlets;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.Command;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.InputType;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;
import com.avispl.symphony.dal.util.StringUtils;

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

	/** Stores general information from {@link Command} */
	private GeneralInformation generalInformation;
	/** Stores pdu information from {@link Command} */
	private Pdu pdu;
	/** Indicates whether the PDU has a {@link InputType#THREE_PHASE}, used to determine statistic properties */
	private boolean is3PhasesPdu;
	/** Stores outlets from {@link Command} */
	private OutletList outletList;

	public APCPDUCommunicator() {
		this.localExtendedStatistics = new ExtendedStatistics();
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		this.versionProperties = new Properties();
		this.adapterInitializationTimestamp = System.currentTimeMillis();
		this.generalInformation = new GeneralInformation();
		this.pdu = new Pdu();
		this.outletList = new OutletList();
	}

	@Override
	protected void internalInit() throws Exception {
		this.loadVersionProperties();
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		this.outletList = null;
		this.pdu = null;
		this.generalInformation = null;
		this.localExtendedStatistics.getStatistics().clear();
		this.localExtendedStatistics.getControllableProperties().clear();
		this.versionProperties.clear();
		super.internalDestroy();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		this.reentrantLock.lock();
		try {
			this.populateData();
			var statistics = new HashMap<>(MonitoringHelper.generateGeneral(this.is3PhasesPdu, this.generalInformation, this.pdu));
			statistics.putAll(MonitoringHelper.generateAdapterMetadata(this.versionProperties));
			statistics.putAll(MonitoringHelper.generateConfiguration(this.is3PhasesPdu, this.pdu));
			statistics.putAll(MonitoringHelper.generateOutlets(this.outletList));

			var controllableProperties = ControllerHelper.generateConfigurationControllers(this.is3PhasesPdu, this.pdu);
			controllableProperties.addAll(ControllerHelper.generateOutletsControllers(this.outletList));

			this.localExtendedStatistics.setStatistics(statistics);
			this.localExtendedStatistics.setControllableProperties(controllableProperties);
			this.localExtendedStatistics.setDynamicStatistics(MonitoringHelper.generateGeneralDynamicProperties(is3PhasesPdu, this.pdu));
		} finally {
			this.reentrantLock.unlock();
		}
		return Collections.singletonList(this.localExtendedStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		this.reentrantLock.lock();
		try {
			var property = controllableProperty.getProperty();
			var value = controllableProperty.getValue();
			if (property.startsWith(Constant.CONFIGURATION_GROUP)) {
				this.sendControl(ControllerHelper.generateConfigurationRequest(property, value, this.pdu));
			} else if (property.contains(Constant.OUTLET_GROUP)) {
				this.sendControl(ControllerHelper.generateOutletRequest(property, value));
			} else {
				this.log.warn("Unsupported property to control: '%s'".formatted(property));
			}
		} finally {
			this.reentrantLock.unlock();
		}
	}

	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		if (CollectionUtils.isEmpty(controllableProperties)) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("ControllableProperties list is null or empty, skipping control operation");
			}
			return;
		}
		for (ControllableProperty controllableProperty : controllableProperties) {
			this.controlProperty(controllableProperty);
		}
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

	/**
	 * Executes predefined commands to retrieve data from the device
	 * and populates corresponding model objects.
	 *
	 * @throws Exception if command execution or parsing fails
	 */
	private void populateData() throws Exception {
		this.generalInformation = this.send(Command.VER.getRequest(), GeneralInformation.class);
		this.is3PhasesPdu = InputType.is3Phases(this.generalInformation.getInputType());
		this.pdu.setPower(this.send(Command.POWER.getRequest(), ResponsePdu.class));
		this.pdu.setColdStartDelay(this.send(Command.COLD_START_DELAY.getRequest(), ResponsePdu.class));
		if (InputType.is3Phases(this.generalInformation.getInputType())) {
			this.pdu.set3PhasesCurrent(this.send(Command.CURRENT.getRequest(), ResponsePdu.class));
			for (int i = 1; i <= Constant.MAX_PHASE; i++) {
				this.pdu.setPhaseLowLoadWarning(i, this.send(Command.LOW_LOAD_WARNING.getRequest(i), ResponsePdu.class));
				this.pdu.setPhaseNearOverloadWarning(i, this.send(Command.NEAR_OVERLOAD_WARNING.getRequest(i), ResponsePdu.class));
				this.pdu.setPhaseOverloadAlarm(i, this.send(Command.OVERLOAD_ALARM.getRequest(i), ResponsePdu.class));
				this.pdu.setPhaseOverloadRestriction(i, this.send(Command.OVERLOAD_RESTRICTION.getRequest(i), ResponsePdu.class));
			}
		} else {
			this.pdu.setCurrent(this.send(Command.CURRENT.getRequest(), ResponsePdu.class));
			this.pdu.setLowLoadWarning(this.send(Command.LOW_LOAD_WARNING.getRequest(), ResponsePdu.class));
			this.pdu.setNearOverloadWarning(this.send(Command.NEAR_OVERLOAD_WARNING.getRequest(), ResponsePdu.class));
			this.pdu.setOverloadAlarm(this.send(Command.OVERLOAD_ALARM.getRequest(), ResponsePdu.class));
			this.pdu.setOverloadRestriction(this.send(Command.OVERLOAD_RESTRICTION.getRequest(), ResponsePdu.class));
		}
		this.outletList = this.send(Command.LIST.getRequest(), OutletList.class);
		if (StringUtils.isNotNullOrEmpty(this.outletList.getOutletNumbers())) {
			this.outletList.setStatuses(this.send(Command.STATUS.getRequest(this.outletList.getOutletNumbers()), ResponseOutlets.class));
			this.outletList.setPowerOffDelays(this.send(Command.POWER_OFF_DELAY.getRequest(this.outletList.getOutletNumbers()), ResponseOutlets.class));
			this.outletList.setPowerOnDelays(this.send(Command.POWER_ON_DELAY.getRequest(this.outletList.getOutletNumbers()), ResponseOutlets.class));
			this.outletList.setRebootDurations(this.send(Command.REBOOT_DURATION.getRequest(this.outletList.getOutletNumbers()), ResponseOutlets.class));
		}
	}
}
