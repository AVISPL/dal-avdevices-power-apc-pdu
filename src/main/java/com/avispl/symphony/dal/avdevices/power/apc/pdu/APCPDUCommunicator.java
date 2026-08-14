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
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.bases.BaseCommunicator;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers.ControllerHelper;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.helpers.MonitoringHelper;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.AboutInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.GeneralInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu.ResponsePdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.ProductInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.RawResponse;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlets;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlets.ResponseOutlets;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.Command;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.InputType;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.PduGeneration;
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
	private Outlets outlets;
	/** CLI dialect the connected device speaks; resolved once per connection on the first poll. */
	private PduGeneration generation;

	public APCPDUCommunicator() {
		this.localExtendedStatistics = new ExtendedStatistics();
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		this.versionProperties = new Properties();
		this.adapterInitializationTimestamp = System.currentTimeMillis();
		this.generalInformation = new GeneralInformation();
		this.pdu = new Pdu();
		this.outlets = new Outlets();
	}

	@Override
	protected void internalInit() throws Exception {
		this.loadVersionProperties();
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		this.outlets = null;
		this.pdu = null;
		this.generalInformation = null;
		this.generation = null;
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
			var statistics = new HashMap<>(MonitoringHelper.generateGeneral(this.is3PhasesPdu, this.generation, this.generalInformation, this.pdu));
			statistics.putAll(MonitoringHelper.generateAdapterMetadata(this.versionProperties));
			statistics.putAll(MonitoringHelper.generateConfiguration(this.is3PhasesPdu, this.pdu));
			statistics.putAll(MonitoringHelper.generateOutlets(this.outlets));

			var controllableProperties = new ArrayList<AdvancedControllableProperty>(
					ControllerHelper.generateConfigurationControllers(this.is3PhasesPdu, this.generation, this.pdu));
			controllableProperties.addAll(ControllerHelper.generateOutletsControllers(this.outlets));

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
			// A control can arrive before the first poll, in which case the dialect is not known yet.
			this.resolveGeneration();
			if (property.startsWith(Constant.CONFIGURATION_GROUP)) {
				this.sendControl(ControllerHelper.generateConfigurationRequest(property, value, this.pdu, this.generation));
			} else if (property.contains(Constant.OUTLET_GROUP)) {
				this.sendControl(ControllerHelper.generateOutletRequest(property, value, this.generation));
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
		this.resolveGeneration();
		if (this.generation.is2G()) {
			this.populateRpdu2gData();
			return;
		}
		this.populateRpduData();
	}

	/**
	 * Determines which CLI dialect the connected device speaks, probing only once per connection.
	 *
	 * <p>There is no command common to both generations that reports the firmware identity: 1st generation has
	 * {@code ver} and 2nd generation has {@code about}, and neither is documented on the other. Detection therefore
	 * probes {@code about} and falls back to 1st generation when the device rejects it. The probe is not wasted work -
	 * on a 2nd generation device its response carries the identity fields the General group needs.
	 *
	 * <p>A device that answers {@code about} but reports an application module other than {@code rpdu2g} is treated as
	 * 1st generation, so an unrecognised module degrades to the long-standing behaviour rather than to a dialect the
	 * device may not speak.
	 *
	 * @throws Exception if the probe cannot be delivered
	 */
	private void resolveGeneration() throws Exception {
		if (this.generation != null) {
			// Resolved for this connection already; the firmware cannot change underneath us.
			return;
		}
		var response = this.trySend(Command.VER.requestFor(PduGeneration.RPDU_2G));
		if (response.isEmpty()) {
			this.generation = PduGeneration.RPDU;
			this.log.info("Detected APC PDU firmware generation '%s'".formatted(this.generation.getApplicationModule()));
			return;
		}
		var about = new AboutInformation();
		about.parse(response.get());
		this.generation = PduGeneration.fromApplicationModule(about.getApplicationModule());
		this.log.info("Detected APC PDU firmware generation '%s' (model '%s', application module '%s' %s, AOS %s)"
				.formatted(this.generation.getApplicationModule(), about.getModel(), about.getApplicationModule(),
						about.getApplicationVersion(), about.getAosVersion()));
	}

	/**
	 * Retrieves and parses monitoring data using the 2nd generation ({@code rpdu2g}) command set.
	 *
	 * <p>Populates exactly the same models as {@link #populateRpduData()} so that the resulting property set is
	 * identical across generations. Differences handled here are the dialect's own: {@code prodInfo} replaces
	 * {@code ver}, the phase commands take an explicit phase argument where 1st generation reads a single-phase value
	 * with no argument, the outlet commands take {@code all} instead of a comma-separated outlet list, and the overload
	 * restriction is reported as prose rather than a token.
	 *
	 * @throws Exception if command execution or parsing fails
	 */
	private void populateRpdu2gData() throws Exception {
		var product = this.send(Command.PROD_INFO.requestFor(this.generation), ProductInformation.class);
		this.generalInformation = GeneralInformation.ofProductInformation(product);
		this.is3PhasesPdu = InputType.is3Phases(this.generalInformation.getInputType());
		this.pdu.setColdStartDelay(this.send(Command.COLD_START_DELAY.requestFor(this.generation), ResponsePdu.class));
		// Apparent/active power has no equivalent on this generation; the properties stay present but unpopulated.
		if (this.is3PhasesPdu) {
			// Read with `all` rather than looping phases 1..3: the device returns one line per phase actually present,
			// whereas an explicit out-of-range phase is rejected with E102. That keeps this correct even if the phase
			// count derived from `Present Phases` is wrong, and costs 4 round trips instead of 12.
			this.pdu.setPhaseCurrents(this.readAllPhases(Command.CURRENT, Constant.READING_CURRENT_2G));
			this.pdu.setPhaseLowLoadWarnings(this.readAllPhases(Command.LOW_LOAD_WARNING, Constant.EMPTY));
			this.pdu.setPhaseNearOverloadWarnings(this.readAllPhases(Command.NEAR_OVERLOAD_WARNING, Constant.EMPTY));
			this.pdu.setPhaseOverloadAlarms(this.readAllPhases(Command.OVERLOAD_ALARM, Constant.EMPTY));
			this.pdu.setPhaseOverloadRestrictions(this.readAllPhases(Command.OVERLOAD_RESTRICTION, Constant.EMPTY));
		} else {
			this.pdu.setCurrent(this.readCurrent("1"));
			this.pdu.setLowLoadWarning(this.send(Command.LOW_LOAD_WARNING.requestFor(this.generation, 1), ResponsePdu.class));
			this.pdu.setNearOverloadWarning(this.send(Command.NEAR_OVERLOAD_WARNING.requestFor(this.generation, 1), ResponsePdu.class));
			this.pdu.setOverloadAlarm(this.send(Command.OVERLOAD_ALARM.requestFor(this.generation, 1), ResponsePdu.class));
			this.pdu.setOverloadRestriction(this.readRestriction(1));
		}
		this.outlets = new Outlets();
		this.outlets.initialize(this.generalInformation.getOutlets());
		if (StringUtils.isNotNullOrEmpty(this.outlets.getOutletNumbers())) {
			this.outlets.setStatuses(this.readAllOutlets(Command.STATUS), this.generation);
			this.outlets.setPowerOffDelays(this.readAllOutlets(Command.POWER_OFF_DELAY));
			this.outlets.setPowerOnDelays(this.readAllOutlets(Command.POWER_ON_DELAY));
			this.outlets.setRebootDurations(this.readAllOutlets(Command.REBOOT_DURATION));
		}
	}

	/** Reads every outlet in one round trip, which the 2nd generation {@code all} parameter allows. */
	private ResponseOutlets readAllOutlets(Command command) throws Exception {
		return this.send(command.requestFor(this.generation, Constant.ALL_2G), ResponseOutlets.class);
	}

	/**
	 * Reads a phase current. Unlike 1st generation {@code current}, {@code phReading} requires both a phase selector
	 * and an explicit measurement argument.
	 *
	 * @param phaseSelector a phase number, or {@link Constant#ALL_2G} to read every phase in one round trip
	 */
	private ResponsePdu readCurrent(String phaseSelector) throws Exception {
		var param = phaseSelector + Constant.SPACE + Constant.READING_CURRENT_2G;
		return this.send(Command.CURRENT.requestFor(this.generation, param), ResponsePdu.class);
	}

	/** Reads and normalizes the overload restriction, which this generation reports as prose. */
	private String readRestriction(int phase) throws Exception {
		var response = this.send(Command.OVERLOAD_RESTRICTION.requestFor(this.generation, phase), RawResponse.class);
		return Util.toRestrictionState2G(response.getValue());
	}

	/**
	 * Reads a phase command for every phase present, using the {@code all} selector.
	 *
	 * @param command the phase command to issue
	 * @param trailingArgument an extra argument the command requires ({@code phReading} needs a measurement), or
	 * {@link Constant#EMPTY} when it takes none
	 */
	private RawResponse readAllPhases(Command command, String trailingArgument) throws Exception {
		var param = trailingArgument.isEmpty() ? Constant.ALL_2G : Constant.ALL_2G + Constant.SPACE + trailingArgument;
		return this.send(command.requestFor(this.generation, param), RawResponse.class);
	}

	/**
	 * Retrieves and parses monitoring data using the 1st generation ({@code rpdu}) command set.
	 *
	 * @throws Exception if command execution or parsing fails
	 */
	private void populateRpduData() throws Exception {
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
		this.outlets = new Outlets();
		this.outlets.initialize(this.generalInformation.getOutlets());
		if (StringUtils.isNotNullOrEmpty(this.outlets.getOutletNumbers())) {
			this.outlets.setStatuses(this.send(Command.STATUS.getRequest(this.outlets.getOutletNumbers()), ResponseOutlets.class), this.generation);
			this.outlets.setPowerOffDelays(this.send(Command.POWER_OFF_DELAY.getRequest(this.outlets.getOutletNumbers()), ResponseOutlets.class));
			this.outlets.setPowerOnDelays(this.send(Command.POWER_ON_DELAY.getRequest(this.outlets.getOutletNumbers()), ResponseOutlets.class));
			this.outlets.setRebootDurations(this.send(Command.REBOOT_DURATION.getRequest(this.outlets.getOutletNumbers()), ResponseOutlets.class));
		}
	}
}
