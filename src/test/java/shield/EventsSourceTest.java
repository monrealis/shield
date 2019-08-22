package shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventsSourceTest {
	@Test
	void parsesAllEventsFromSample() {
		InputStream input = loadSample();

		List<Event> events = new EventsSource(input).events();

		assertEquals(7, events.size());
		assertEquals("profile_create", events.get(0).type);
		assertEquals("request", events.get(1).type);
	}

	@Test
	void closesInputStreamAfterParsing() {
		WhiteboxFilterStream input = loadSampleWhitebox();

		new EventsSource(input).events();

		assertTrue(input.isClosed());
	}

	@Test
	void readsElementsOneByOne() {
		WhiteboxFilterStream input = loadSampleWhitebox();

		new EventsSource(input).iterator().next();

		assertFalse(input.isClosed());

	}

	private WhiteboxFilterStream loadSampleWhitebox() {
		return new WhiteboxFilterStream(loadSample());
	}

	private InputStream loadSample() {
		return getClass().getResourceAsStream("/sample.json");
	}

	private final class WhiteboxFilterStream extends FilterInputStream {
		private boolean closed;

		private WhiteboxFilterStream(InputStream in) {
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
