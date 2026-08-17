/** Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved. */
package com.avispl.symphony.dal.avdevices.power.apc.pdu;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.common.error.InvalidArgumentException;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Constant;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.common.Util;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.AboutInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.GeneralInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.Pdu;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.ProductInformation;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.RawResponse;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.models.outlets.Outlets;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.Command;
import com.avispl.symphony.dal.avdevices.power.apc.pdu.types.PduGeneration;

/**
 * Fixture-based tests covering both CLI dialects.
 *
 * <p>These are pure parser/command tests with no device involved, unlike {@link APCPDUCommunicatorTest} which requires
 * a reachable unit. They are the regression net for 1st generation ({@code rpdu}) behaviour, which is otherwise only
 * verifiable against hardware.
 *
 * <p>Every fixture below is verbatim device output captured on 2026-08-05:
 * <ul>
 *   <li>1st generation - AP7900, AOS v2.7.0 / Switched Rack PDU v2.7.3</li>
 *   <li>2nd generation - AP7920B, application module {@code rpdu2g} v2.5.2.5 / AOS v2.5.3.2</li>
 * </ul>
 * Success markers and the trailing prompt are shown already removed, as response normalization does.
 *
 * @author Symphony Dev Team
 * @since 1.1.0
 */
@Tag("Mock")
class PduDialectTest {

	@Nested
	@DisplayName("1st generation (rpdu) identity")
	class RpduIdentity {
		private static final String VER_RESPONSE = """
				APC OS v2.7.0
				Switched Rack PDU v2.7.3
				Model: AP7900
				Outlets: 8
				Max Current: 12A
				Input Type: single-phase
				""";

		@Test
		void mapsEveryField() {
			var general = new GeneralInformation();
			general.parse(VER_RESPONSE);

			Assertions.assertEquals("v2.7.0", general.getAosVersion());
			Assertions.assertEquals("v2.7.3", general.getPduVersion());
			Assertions.assertEquals("AP7900", general.getModel());
			Assertions.assertEquals("8", general.getOutlets());
			Assertions.assertEquals("12A", general.getMaxLoad());
			Assertions.assertEquals("single-phase", general.getInputType());
		}

		@Test
		void toleratesBlankResponse() {
			var general = new GeneralInformation();
			general.parse("   ");

			Assertions.assertNull(general.getModel());
		}
	}

	@Nested
	@DisplayName("2nd generation (rpdu2g) identity")
	class Rpdu2gIdentity {
		private static final String PROD_INFO_RESPONSE = """
				AOS:              2.5.3.2
				Switched Rack PDU: 2.5.2.5
				Model:            AP7920B
				Name:             CHILAB-APC7920B-1
				Location:         Glendale Heights Lab
				Contact:          Kaeden Adams
				Present Outlets:  8
				Switched Outlets: 8
				Metered Outlets:  0
				Max Current:      10 A
				Present Phases:   1
				Metered Phases:   1
				Uptime:           1 Day 2 Hours 13 Minutes
				Network Link:     Link Active
				""";

		private ProductInformation parsed() {
			var product = new ProductInformation();
			product.parse(PROD_INFO_RESPONSE);
			return product;
		}

		@Test
		@DisplayName("keys sharing a suffix are not confused with one another")
		void readsExactKeys() {
			var product = this.parsed();

			Assertions.assertEquals("8", product.get(ProductInformation.KEY_PRESENT_OUTLETS));
			Assertions.assertEquals("0", product.get("Metered Outlets"));
			Assertions.assertEquals("8", product.get("Switched Outlets"));
		}

		@Test
		@DisplayName("produces the same field set as 1st generation `ver`")
		void mapsOntoGeneralInformation() {
			var general = GeneralInformation.ofProductInformation(this.parsed());

			Assertions.assertEquals("2.5.3.2", general.getAosVersion());
			Assertions.assertEquals("2.5.2.5", general.getPduVersion());
			Assertions.assertEquals("AP7920B", general.getModel());
			Assertions.assertEquals("8", general.getOutlets());
			Assertions.assertEquals("10 A", general.getMaxLoad());
			// `prodInfo` reports a phase count; the Input Type vocabulary is derived from it.
			Assertions.assertEquals("single-phase", general.getInputType());
		}

		@Test
		@DisplayName("`Max Current: 10 A` extracts despite the space before the unit")
		void extractsMaxCurrent() {
			Assertions.assertEquals("10", Util.extractValue("10 A").orElse(null));
			Assertions.assertEquals("12", Util.extractValue("12A").orElse(null));
		}

		@Test
		void derivesThreePhaseFromPhaseCount() {
			var product = new ProductInformation();
			product.parse("Model: AP8959\nPresent Phases:   3\n");

			Assertions.assertEquals("3-phase", GeneralInformation.ofProductInformation(product).getInputType());
		}
	}

	@Nested
	@DisplayName("2nd generation (rpdu2g) `about` parsing")
	class Rpdu2gAboutParsing {
		private static final String ABOUT_RESPONSE = """
				Hardware Factory
				---------------
				Model Number:           AP7920B
				Serial Number:          2A2548L01451
				Hardware Revision:      B3
				Manufacture Date:       11/25/2025

				Network Management Card
				---------------
				Model Number:           0N-1570-07K
				Serial Number:          2A2547L16570

				Application Module
				---------------
				Name:                   rpdu2g
				Version:                v2.5.2.5
				Time:                   05:52:41

				APC OS(AOS)
				---------------
				Name:                   aos
				Version:                v2.5.3.2
				""";

		private AboutInformation parsed() {
			var about = new AboutInformation();
			about.parse(ABOUT_RESPONSE);
			return about;
		}

		@Test
		@DisplayName("`Model Number` resolves per section, not to the last occurrence")
		void scopesRepeatedKeysToTheirSection() {
			var about = this.parsed();

			Assertions.assertEquals("AP7920B", about.getModel());
			Assertions.assertEquals("0N-1570-07K", about.getNetworkManagementCardModel());
			Assertions.assertEquals("2A2548L01451", about.getSerialNumber());
		}

		@Test
		@DisplayName("`Version` resolves per firmware module")
		void distinguishesVersionKeys() {
			var about = this.parsed();

			Assertions.assertEquals("v2.5.2.5", about.getApplicationVersion());
			Assertions.assertEquals("v2.5.3.2", about.getAosVersion());
		}

		@Test
		void readsTheGenerationDiscriminator() {
			Assertions.assertEquals("rpdu2g", this.parsed().getApplicationModule());
		}

		@Test
		@DisplayName("a value containing colons is not truncated")
		void splitsOnTheFirstColonOnly() {
			Assertions.assertEquals("05:52:41", this.parsed().get(Constant.SECTION_APPLICATION_MODULE, "Time"));
		}
	}

	@Nested
	@DisplayName("Outlet parsing")
	class OutletParsing {
		private static Outlets.ResponseOutlets response(String raw) {
			var response = new Outlets.ResponseOutlets();
			response.parse(raw);
			return response;
		}

		@Test
		@DisplayName("1st generation status is number:STATUS:name")
		void parsesRpduStatus() {
			var outlets = new Outlets();
			outlets.setStatuses(response("1:ON:Outlet 1\n2:OFF:Rack Fan"), PduGeneration.RPDU);

			Assertions.assertEquals("ON", outlets.getOutletDetails().get("1").getPowerStatus());
			Assertions.assertEquals("Outlet 1", outlets.getOutletDetails().get("1").getName());
			Assertions.assertEquals("OFF", outlets.getOutletDetails().get("2").getPowerStatus());
			Assertions.assertEquals("Rack Fan", outlets.getOutletDetails().get("2").getName());
		}

		@Test
		@DisplayName("2nd generation status is number:name:status - the reverse, and it must not be read positionally")
		void parsesRpdu2gStatus() {
			var outlets = new Outlets();
			outlets.setStatuses(response(" 1: Outlet 1: On \n 2: Rack Fan: Off "), PduGeneration.RPDU_2G);

			// Reading the 1st generation indices here would silently report the name as the status.
			Assertions.assertEquals("On", outlets.getOutletDetails().get("1").getPowerStatus());
			Assertions.assertEquals("Outlet 1", outlets.getOutletDetails().get("1").getName());
			Assertions.assertEquals("Off", outlets.getOutletDetails().get("2").getPowerStatus());
			Assertions.assertEquals("Rack Fan", outlets.getOutletDetails().get("2").getName());
		}

		@Test
		@DisplayName("delay values extract from either generation's wording")
		void parsesDelaysOfBothGenerations() {
			var rpdu = new Outlets();
			rpdu.setRebootDurations(response("1: Outlet 1: Reboot duration is 5 seconds."));
			Assertions.assertEquals("5", rpdu.getOutletDetails().get("1").getRebootDuration());

			var rpdu2g = new Outlets();
			rpdu2g.setRebootDurations(response(" 1: Outlet 1: 5 sec"));
			Assertions.assertEquals("5", rpdu2g.getOutletDetails().get("1").getRebootDuration());
		}

		@Test
		@DisplayName("outlets are enumerated from the device's outlet count, one group per physical outlet")
		void enumeratesFromOutletCount() {
			var outlets = new Outlets();
			outlets.initialize("8");

			// 8 outlets, not one group per account with access to them.
			Assertions.assertEquals(8, outlets.getOutletDetails().size());
			Assertions.assertEquals("1,2,3,4,5,6,7,8", outlets.getOutletNumbers());
			Assertions.assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"), outlets.getOrderedOutletNumbers());
		}

		@Test
		@DisplayName("outlet numbers are ordered numerically, not lexicographically")
		void ordersOutletsNumerically() {
			var outlets = new Outlets();
			outlets.initialize("12");

			Assertions.assertEquals("9", outlets.getOrderedOutletNumbers().get(8));
			Assertions.assertEquals("12", outlets.getOrderedOutletNumbers().get(11));
		}

		@Test
		void toleratesAnAbsentOrUnparseableCount() {
			var absent = new Outlets();
			absent.initialize(null);
			Assertions.assertTrue(absent.getOutletDetails().isEmpty());
			Assertions.assertEquals("", absent.getOutletNumbers());

			var garbage = new Outlets();
			garbage.initialize("N/A");
			Assertions.assertTrue(garbage.getOutletDetails().isEmpty());
		}

		@Test
		@DisplayName("property names are per outlet, with no account prefix")
		void namesPropertiesByOutletAlone() {
			Assertions.assertEquals("Outlet_01", Util.buildOutletPropertyPrefix("1"));
			Assertions.assertEquals("Outlet_08", Util.buildOutletPropertyPrefix("8"));
			Assertions.assertEquals("Outlet_12", Util.buildOutletPropertyPrefix("12"));
		}
	}

	@Nested
	@DisplayName("Multi-phase reads via the `all` selector")
	class PhaseReads {
		private static RawResponse response(String raw) {
			var response = new RawResponse();
			response.parse(raw);
			return response;
		}

		@Test
		@DisplayName("three phases are distributed across the per-phase maps")
		void distributesThreePhases() {
			var pdu = new Pdu();
			// Line format taken from a real single-phase reply; a 3-phase unit repeats it once per phase.
			pdu.setPhaseCurrents(response("1: 0.4 A\n2: 1.6 A\n3: 2.5 A"));
			pdu.setPhaseOverloadAlarms(response("1: 10.0 A\n2: 10.0 A\n3: 12.0 A"));

			Assertions.assertEquals("0", pdu.getCurrents().get(1));
			Assertions.assertEquals("2", pdu.getCurrents().get(2));
			Assertions.assertEquals("3", pdu.getCurrents().get(3));
			Assertions.assertEquals("12", pdu.getOverloadAlarms().get(3));
		}

		@Test
		@DisplayName("a single-phase reply through the same path populates only phase 1")
		void toleratesFewerPhasesThanExpected() {
			var pdu = new Pdu();
			pdu.setPhaseCurrents(response("1: 0.0 A"));

			Assertions.assertEquals("0", pdu.getCurrents().get(1));
			Assertions.assertNull(pdu.getCurrents().get(2));
		}

		@Test
		@DisplayName("per-phase restriction text maps to the shared on/off value")
		void distributesRestrictions() {
			var pdu = new Pdu();
			pdu.setPhaseOverloadRestrictions(response("1: Always Allow Turn On \n2: Restrict on Overload\n3: Restrict on Near Overload"));

			Assertions.assertEquals("off", pdu.getOverloadRestrictions().get(1));
			Assertions.assertEquals("on", pdu.getOverloadRestrictions().get(2));
			Assertions.assertEquals("on", pdu.getOverloadRestrictions().get(3));
		}

		@Test
		void ignoresUnparseableLines() {
			var pdu = new Pdu();
			pdu.setPhaseCurrents(response("E000: Success\n1: 0.4 A\nnonsense"));

			Assertions.assertEquals("0", pdu.getCurrents().get(1));
			Assertions.assertEquals(1, pdu.getCurrents().size());
		}
	}

	@Nested
	@DisplayName("Overload restriction")
	class OverloadRestriction {
		@Test
		@DisplayName("2nd generation text maps onto the same on/off value 1st generation reports")
		void normalizesRpdu2gWording() {
			Assertions.assertEquals("off", Util.toRestrictionState("1: Always Allow Turn On"));
			Assertions.assertEquals("on", Util.toRestrictionState("1: Restrict on Overload"));
			Assertions.assertEquals("on", Util.toRestrictionState("1: Restrict on Near Overload"));
			Assertions.assertNull(Util.toRestrictionState("  "));
		}

		@Test
		void keepsRpduExtractionIntact() {
			Assertions.assertEquals("off", Util.extractValue("Overload restriction is off for 1.").orElse(null));
		}
	}

	@Nested
	@DisplayName("Generation detection")
	class GenerationDetection {
		@Test
		void resolvesRpdu2gFromItsApplicationModule() {
			Assertions.assertEquals(PduGeneration.RPDU_2G, PduGeneration.fromApplicationModule("rpdu2g"));
			Assertions.assertEquals(PduGeneration.RPDU_2G, PduGeneration.fromApplicationModule("  RPDU2G "));
		}

		@Test
		@DisplayName("anything unrecognised degrades to 1st generation rather than guessing")
		void defaultsToRpdu() {
			Assertions.assertEquals(PduGeneration.RPDU, PduGeneration.fromApplicationModule("rpdu"));
			Assertions.assertEquals(PduGeneration.RPDU, PduGeneration.fromApplicationModule("something-else"));
			Assertions.assertEquals(PduGeneration.RPDU, PduGeneration.fromApplicationModule(null));
			Assertions.assertEquals(PduGeneration.RPDU, PduGeneration.fromApplicationModule(""));
		}
	}

	@Nested
	@DisplayName("Command dialects")
	class CommandDialects {
		@Test
		void spellsVerbsPerGeneration() {
			Assertions.assertEquals("status", Command.STATUS.requestFor(PduGeneration.RPDU));
			Assertions.assertEquals("olStatus", Command.STATUS.requestFor(PduGeneration.RPDU_2G));
			Assertions.assertEquals("ver", Command.VER.requestFor(PduGeneration.RPDU));
			Assertions.assertEquals("about", Command.VER.requestFor(PduGeneration.RPDU_2G));
			Assertions.assertEquals("list", Command.LIST.requestFor(PduGeneration.RPDU));
			Assertions.assertEquals("userList", Command.LIST.requestFor(PduGeneration.RPDU_2G));
		}

		@Test
		@DisplayName("outlet reboot maps to olReboot, never the card-restarting `reboot`")
		void doesNotReuseTheSystemRebootVerb() {
			Assertions.assertEquals("olReboot", Command.REBOOT.requestFor(PduGeneration.RPDU_2G));
		}

		@Test
		void appendsParameters() {
			Assertions.assertEquals("olStatus all", Command.STATUS.requestFor(PduGeneration.RPDU_2G, "all"));
			Assertions.assertEquals("status 1,5", Command.STATUS.requestFor(PduGeneration.RPDU, "1,5"));
			Assertions.assertEquals("phReading 1 current", Command.CURRENT.requestFor(PduGeneration.RPDU_2G, "1 current"));
		}

		@Test
		@DisplayName("an unmapped verb throws instead of sending something the device would reject")
		void refusesUnknownVerbs() {
			// Verified on an AP7920B: `phReading <n> power` and `<n> appower` are rejected with E102.
			Assertions.assertFalse(Command.POWER.isSupportedOn(PduGeneration.RPDU_2G));
			Assertions.assertFalse(Command.PROD_INFO.isSupportedOn(PduGeneration.RPDU));

			Assertions.assertThrows(InvalidArgumentException.class, () -> Command.POWER.requestFor(PduGeneration.RPDU_2G));
			Assertions.assertThrows(InvalidArgumentException.class, () -> Command.PROD_INFO.requestFor(PduGeneration.RPDU));
		}

		@Test
		@DisplayName("the legacy no-generation accessors still return 1st generation verbs")
		void preservesLegacyAccessors() {
			Assertions.assertEquals("ver", Command.VER.getRequest());
			Assertions.assertEquals("status 1", Command.STATUS.getRequest(1));
		}
	}

	@Nested
	@DisplayName("Response status classification")
	class ResponseStatus {
		@Test
		@DisplayName("E000 is 2nd generation success, not an error")
		void treatsE000AsSuccess() {
			Assertions.assertFalse(Constant.ERROR_RESPONSE_PATTERN.matcher("E000: Success").find());
			Assertions.assertFalse(Constant.ERROR_RESPONSE_PATTERN.matcher(" 1: Outlet 1: On\nE000: Success").find());
		}

		@Test
		void flagsErrorCodesOfBothGenerations() {
			Assertions.assertTrue(Constant.ERROR_RESPONSE_PATTERN.matcher("E100: Command does not exist.").find());
			Assertions.assertTrue(Constant.ERROR_RESPONSE_PATTERN.matcher("E101: Invalid command arguments.").find());
			Assertions.assertTrue(Constant.ERROR_RESPONSE_PATTERN.matcher("E200: Input error.").find());
			// Same code, different meaning on rpdu2g - still an error.
			Assertions.assertTrue(Constant.ERROR_RESPONSE_PATTERN.matcher("E102: Parameter Error").find());
		}

		@Test
		void stripsEitherGenerationSuccessMarker() {
			Assertions.assertEquals("1:ON:Outlet 1",
					Constant.SUCCESS_MARKER_PATTERN.matcher("OK\n1:ON:Outlet 1").replaceFirst("").trim());
			Assertions.assertEquals("1: Outlet 1: On",
					Constant.SUCCESS_MARKER_PATTERN.matcher(" 1: Outlet 1: On\nE000: Success").replaceFirst("").trim());
		}

		@Test
		@DisplayName("an outlet named `OK...` survives success-marker stripping")
		void doesNotMangleOutletNamesContainingOk() {
			Assertions.assertEquals("1:ON:OK-rack",
					Constant.SUCCESS_MARKER_PATTERN.matcher("OK\n1:ON:OK-rack").replaceFirst("").trim());
		}

		@Test
		void stripsEitherPromptSpelling() {
			Assertions.assertEquals("data\n", Constant.PROMPT_PATTERN.matcher("data\nAPC>").replaceFirst(""));
			Assertions.assertEquals("data\n", Constant.PROMPT_PATTERN.matcher("data\napc>").replaceFirst(""));
		}
	}
}
