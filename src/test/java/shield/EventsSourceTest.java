package shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Event {
		public String type;
	}

	private static class EventsSource {
		private final ObjectMapper mapper = new ObjectMapper();
		private final InputStream inputStream;

		public EventsSource(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		// https://stackoverflow.com/questions/24835431/use-jackson-to-stream-parse-an-array-of-json-objects
		public List<Event> events() {
			List<Event> r = new ArrayList<>();
			Iterator<Event> it = iterator();
			while (it.hasNext())
				r.add(it.next());
			return r;
		}

		public Iterator<Event> iterator() {
			return new EventIterator();
		}

		private final class EventIterator implements Iterator<Event> {
			private final JsonParser parser = createParser();
			private boolean hasNext;

			public EventIterator() {
				updateHasNext();
			}

			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public Event next() {
				Event event = toEvent();
				updateHasNext();
				return event;
			}

			private void updateHasNext() {
				hasNext = findHasNext();
				if (!hasNext)
					closeParser();
			}

			private void closeParser() {
				try {
					parser.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			private boolean findHasNext() {
				try {
					return parser.nextToken() == JsonToken.START_OBJECT;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			private Event toEvent() {
				TreeNode node = readTree();
				return mapper.convertValue(node, Event.class);
			}

			private TreeNode readTree() {
				try {
					return mapper.readTree(parser);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			private JsonParser createParser() {
				try {
					return mapper.getFactory().createParser(inputStream);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
