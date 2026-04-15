package com.avispl.symphony.dal.avdevices.power.apc.pdu;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

class APCPDUCommunicatorTest {
	private APCPDUCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		communicator = new APCPDUCommunicator();
		communicator.setHost("localhost");
		communicator.setPort(2323);
		communicator.setLogin("apc");
		communicator.setPassword("apcz");
		communicator.init();
		communicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		communicator.disconnect();
		communicator.destroy();
	}

	@Test
	void testGetMultipleStatistics() throws Exception {
		var extendedStatistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
	}
}
