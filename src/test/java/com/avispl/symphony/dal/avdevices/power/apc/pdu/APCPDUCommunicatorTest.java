package com.avispl.symphony.dal.avdevices.power.apc.pdu;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.properties.AdapterMetadata;

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

	private Map<String, String> filterGroupStatistics(Map<String, String> statistics, String groupName) {
		return statistics.entrySet().stream()
				.filter(e -> groupName == null ? !e.getKey().contains("#") : e.getKey().startsWith(groupName))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private boolean isValidValue(String value) {
		return value != null && !Constant.NOT_AVAILABLE.equals(value);
	}
}
