package com.avispl.symphony.dal.avdevices.power.apc.pdu;

import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.InputType;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.Configuration;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.General;

class APCPDUCommunicatorTest {
	private APCPDUCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		communicator = new APCPDUCommunicator();
		communicator.setHost("");
		communicator.setPort(2323);
		communicator.setLogin("");
		communicator.setPassword("");
		communicator.init();
	}

	@AfterEach
	void destroy() throws Exception {
		communicator.disconnect();
		communicator.destroy();
	}

	@Test
	void testLogin_withInvalidCredential() throws Exception {
		this.communicator.destroy();
		this.communicator.setLogin("");
		this.communicator.setPassword("");
		this.communicator.init();

		Assertions.assertThrows(FailedLoginException.class, () -> this.communicator.getMultipleStatistics());
	}

	@Test
	void testGetMultipleStatistics_withGeneral() throws Exception {
		var extendedStatistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var verifiedStatistics = this.filterGroupStatistics(extendedStatistics.getStatistics(), null);
		var expectedSize = InputType.is3Phases(verifiedStatistics.get(General.INPUT_TYPE.getDisplayName()))
				? General.COMMON_PROPERTIES.size() + General.THREE_PHASE_PROPERTIES.size()
				: General.COMMON_PROPERTIES.size() + 1;

		Assertions.assertEquals(expectedSize, verifiedStatistics.size(), "General properties doesn't match size");
		verifiedStatistics.forEach((key, value) -> {
			Assertions.assertFalse(key.contains("#"), "Invalid field: " + key);
			Assertions.assertTrue(this.isValidValue(value), "Key '%s'. Invalid value: %s".formatted(key, value));
		});
	}

	@Test
	void testGetMultipleStatistics_withAdapterMetadataGroup() throws Exception {
		var extendedStatistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var verifiedStatistics = this.filterGroupStatistics(extendedStatistics.getStatistics(), Constant.ADAPTER_METADATA_GROUP);
		var expectedSize = AdapterMetadata.values().length;

		Assertions.assertEquals(expectedSize, verifiedStatistics.size(), "Adapter Metadata properties doesn't match size");
		verifiedStatistics.forEach((key, value) -> {
			Assertions.assertTrue(key.startsWith("AdapterMetadata#"), "Invalid field: " + key);
			Assertions.assertTrue(this.isValidValue(value), "Key '%s'. Invalid value: %s".formatted(key, value));
		});
	}

	@Test
	void testGetMultipleStatistics_withConfiguration() throws Exception {
		var extendedStatistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var verifiedStatistics = this.filterGroupStatistics(extendedStatistics.getStatistics(), Constant.CONFIGURATION_GROUP);
		var expectedSize = InputType.is3Phases(extendedStatistics.getStatistics().get(General.INPUT_TYPE.getDisplayName()))
				? 2 + Configuration.THREE_PHASE_PROPERTIES.size()
				: 2 + Configuration.ONE_PHASE_PROPERTIES.size();

		Assertions.assertEquals(expectedSize, verifiedStatistics.size(), Constant.CONFIGURATION_GROUP + " properties doesn't match size");
		verifiedStatistics.forEach((key, value) -> {
			Assertions.assertTrue(key.startsWith(Constant.CONFIGURATION_GROUP + "#"), "Invalid field: " + key);
			Assertions.assertTrue(this.isValidValue(value), "Key '%s'. Invalid value: %s".formatted(key, value));
		});
	}

	@Test
	void testControlProperty_withConfiguration() throws Exception {
		var verifiedProperty = Configuration.COLD_START_DELAY_SEC;
		var verifiedValue = "20";
		this.communicator.getMultipleStatistics();
		this.communicator.controlProperty(new ControllableProperty(verifiedProperty.getDisplayName(), verifiedValue, null));
		var extendedStatistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var statistics = this.filterGroupStatistics(extendedStatistics.getStatistics(), Constant.CONFIGURATION_GROUP);

		Assertions.assertEquals(verifiedValue, statistics.get(verifiedProperty.getDisplayName()));
	}

	private Map<String, String> filterGroupStatistics(Map<String, String> statistics, String groupName) {
		return statistics.entrySet().stream()
				.filter(e -> groupName == null ? !e.getKey().contains("#") : e.getKey().startsWith(groupName))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty() && !value.isBlank();
	}
}
