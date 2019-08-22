package shield;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class EventsSource {
	private final ObjectMapper mapper = new ObjectMapper();
	private final InputStream inputStream;
	private Iterator<Event> iterator;

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
		if (iterator == null)
			iterator = new EventIterator();
		return iterator;
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