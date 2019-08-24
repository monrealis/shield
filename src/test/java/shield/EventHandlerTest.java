package shield;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EventHandlerTest {
	private EventHandler handler = new EventHandler();

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
}
