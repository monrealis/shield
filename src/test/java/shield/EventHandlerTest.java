package shield;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class EventHandlerTest {
	private StringWriter output = new StringWriter();
	private EventHandler handler = new EventHandler(new PrintWriter(output));

	@Test
	void allowsOnlyWhitelistedUrls() {
		Event event = new Event();
		event.type = "profile_create";
		event.modelName = "M1";
		event.defaultPolicy = "block";
		event.whitelist = asList("facebook.com");
		event.blacklist = emptyList();
		handler.handle(event);

		assertTrue(handler.inspectRequest("M1", "facebook.com"));
		assertFalse(handler.inspectRequest("M1", "other.com"));
	}

	@Test
	@Disabled
	void logsRequestToOutput() {
		Event event = new Event();
		event.type = "profile_create";
		event.modelName = "M1";
		event.defaultPolicy = "block";
		event.whitelist = asList("facebook.com");
		event.blacklist = emptyList();
		handler.handle(event);

		handler.handle(createRequest("r1", "M1", "facebook.com"));

		assertEquals("{'request_id': 'r1', 'action': 'allow'}\n".replace('\'', '"'), output.toString());
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
