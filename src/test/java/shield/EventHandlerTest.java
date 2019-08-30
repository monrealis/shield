package shield;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import shield.EventHandler.Action;
import shield.EventHandler.Decision;

public class EventHandlerTest {
	private StringWriter output = new StringWriter();
	private EventHandler handler = new EventHandler(new PrintWriter(output)) {
		protected void handleDecision(Decision decision, String deviceId) {
			super.handleDecision(decision, deviceId);
			decisions.add(decision);
		};
	};
	private List<Decision> decisions = new ArrayList<>();

	@Test
	void allowsOnlyWhitelistedUrls() {
		Event event = allowOnlyFacebook();
		handler.handle(event);

		assertTrue(handler.inspectRequest("M1", "facebook.com"));
		assertFalse(handler.inspectRequest("M1", "other.com"));
	}

	@Test
	void handlesProfileUpdates() {
		handler.handle(allowOnlyFacebook());
		handler.handle(updateWhiteList(emptyList()));

		assertFalse(handler.inspectRequest("M1", "facebook.com"));
		assertFalse(handler.inspectRequest("M1", "other.com"));
	}

	@Test
	void profileUpdateWhitoutExistingProfileIsNotAllowed() {
		assertThrows(RuntimeException.class, () -> handler.handle(updateWhiteList(emptyList())));
	}

	@Test
	void ignoresEventTypeNull() {
		handler.handle(new Event());

		assertEquals(1, handler.errorCount());
	}

	@Test
	void ignoreUnknownEvent() {
		Event event = new Event();
		event.type = "unknown";
		handler.handle(event);

		assertEquals(1, handler.errorCount());
	}

	private Event allowOnlyFacebook() {
		return allowOnly(asList("facebook.com"));
	}

	private Event allowOnly(List<String> whitelist) {
		Event event = new Event();
		event.type = "profile_create";
		event.modelName = "M1";
		event.defaultPolicy = "block";
		event.whitelist = whitelist;
		event.blacklist = emptyList();
		return event;
	}

	private Event updateWhiteList(List<String> whitelist) {
		Event event = new Event();
		event.type = "profile_update";
		event.modelName = "M1";
		event.whitelist = whitelist;
		event.blacklist = emptyList();
		return event;
	}

	@Test
	void logsRequestToOutput() {
		Event event = allowOnlyFacebook();
		handler.handle(event);

		handler.handle(createRequest("r1", "d1", "M1", "facebook.com"));

		// We don't care about spacing since there's no easy way to configure Jackson
		assertEquals("{'request_id':'r1','action':'allow'}\n".replace('\'', '"'), output.toString());
	}

	@Test
	void allowsUrlFromWhitelist() {
		Event event = allowOnlyFacebook();
		handler.handle(event);

		handler.handle(createRequest("r1", "d1", "M1", "facebook.com"));

		assertEquals(1, decisions.size());
		assertEquals(Action.ALLOW, decisions.get(0).action);
		assertEquals("r1", decisions.get(0).requestId);
		assertEquals(0, handler.errorCount());
	}

	@Test
	void ifUrlIsNotFromWhitelistQuarantineActionIsTaken() {
		Event event = allowOnlyFacebook();
		handler.handle(event);

		handler.handle(createRequest("r1", "d1", "M1", "other.com"));

		assertEquals("Q", actions());
	}

	@Test
	void cannotMoveOutOfQuarantine() {
		Event event = allowOnlyFacebook();
		handler.handle(event);

		handler.handle(createRequest("r1", "d1", "M1", "other.com"));
		handler.handle(createRequest("r1", "d1", "M1", "facebook.com"));

		assertEquals("QQ", actions());
	}

	@Test
	void quarantineIsForDeviceOnlyNotForModel() {
		Event event = allowOnlyFacebook();
		handler.handle(event);

		handler.handle(createRequest("r1", "d1", "M1", "other.com"));
		handler.handle(createRequest("r2", "d2", "M1", "facebook.com"));

		assertEquals("QA", actions());
	}

	private Event createRequest(String requestId, String deviceId, String modelName, String url) {
		Event e = new Event();
		e.type = "request";
		e.deviceId = deviceId;
		e.requestId = requestId;
		e.modelName = modelName;
		e.url = url;
		return e;
	}

	private String actions() {
		return decisions.stream().map(d -> d.action.name().substring(0, 1)).collect(joining());
	}
}
