package shield;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		protected void handleDecision(Decision decision) {
			super.handleDecision(decision);
			decisions.add(decision);
		};
	};
	private List<Decision> decisions = new ArrayList<>();

	@Test
	void allowsOnlyWhitelistedUrls() {
		Event event = allowOnlyFacebook("profile_create");
		handler.handle(event);

		assertTrue(handler.inspectRequest("M1", "facebook.com"));
		assertFalse(handler.inspectRequest("M1", "other.com"));
	}

	@Test
	void handlesProfileUpdates() {
		handler.handle(allowOnlyFacebook("profile_create"));
		handler.handle(allowOnly("profile_update", emptyList()));

		assertFalse(handler.inspectRequest("M1", "facebook.com"));
		assertFalse(handler.inspectRequest("M1", "other.com"));
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

	private Event allowOnlyFacebook(String eventType) {
		return allowOnly(eventType, asList("facebook.com"));
	}

	private Event allowOnly(String eventType, List<String> whitelist) {
		Event event = new Event();
		event.type = eventType;
		event.modelName = "M1";
		event.defaultPolicy = "block";
		event.whitelist = whitelist;
		event.blacklist = emptyList();
		return event;
	}

	@Test
	void logsRequestToOutput() {
		Event event = allowOnlyFacebook("profile_create");
		handler.handle(event);

		handler.handle(createRequest("r1", "M1", "facebook.com"));

		// We don't care about spacing since there's no easy way to configure Jackson
		assertEquals("{'request_id':'r1','action':'allow'}\n".replace('\'', '"'), output.toString());
	}

	@Test
	void allowsUrlFromWhitelist() {
		Event event = allowOnlyFacebook("profile_create");
		handler.handle(event);

		handler.handle(createRequest("r1", "M1", "facebook.com"));

		assertEquals(1, decisions.size());
		assertEquals(Action.ALLOW, decisions.get(0).action);
		assertEquals("r1", decisions.get(0).requestId);
		assertEquals(0, handler.errorCount());
	}

	private Event createRequest(String id, String device, String url) {
		Event e = new Event();
		e.type = "request";
		e.requestId = id;
		e.deviceId = device;
		e.url = url;
		return e;
	}
}
