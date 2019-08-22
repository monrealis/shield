package shield;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventsSourceTest {
	private WhiteboxInputStream input = loadSampleWhitebox();
	private EventsSource eventsSource = new EventsSource(input);

	@Test
	void parsesAllEventsFromSample() {
		List<Event> events = eventsSource.events();

		assertEquals(7, events.size());
		assertEquals("profile_create", events.get(0).type);
		assertEquals("request", events.get(1).type);
	}

	@Test
	void closesInputStreamAfterParsing() {
		eventsSource.events();

		assertTrue(input.isClosed());
	}

	@Test
	void mapsProfileFields() {
		Event event = eventsSource.iterator().next();

		assertEquals("profile_create", event.type);
		assertEquals("iPhone", event.modelName);
		assertEquals("block", event.defaultPolicy);
		assertEquals(asList("facebook.com"), event.whitelist);
		assertEquals(emptyList(), event.blacklist);
		assertEquals(1562827784L, event.timestamp);
	}

	@Test
	public void mapsRequestFields() {
		Event event = eventsSource.events().get(1);

		assertEquals("request", event.type);
		assertEquals("66d03a6947c048009f0b34260f35f3bd", event.requestId);
		assertEquals("iPhone", event.modelName);
		assertEquals("7fcbe1dff23947e5b3db8a20c1a2f8c0", event.deviceId);
		assertEquals("facebook.com", event.url);
		assertEquals(1562827794L, event.timestamp);
	}

	@Test
	void readsElementsOneByOne() {
		eventsSource.iterator().next();

		assertFalse(input.isClosed());
	}

	private WhiteboxInputStream loadSampleWhitebox() {
		return new WhiteboxInputStream(loadSample());
	}

	private InputStream loadSample() {
		return getClass().getResourceAsStream("/sample.json");
	}

	private final class WhiteboxInputStream extends FilterInputStream {
		private boolean closed;

		private WhiteboxInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
			super.close();
			closed = true;
		}

		public boolean isClosed() {
			return closed;
		}
	}
}
