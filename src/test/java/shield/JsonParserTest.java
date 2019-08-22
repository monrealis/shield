package shield;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonParserTest {
	@Test
	void parsesAllEventsFromSample() {
		List<Event> events = new EventsSource(loadSample()).events();

		assertEquals(7, events.size());
		assertEquals("profile_create", events.get(0).type);
		assertEquals("request", events.get(1).type);
	}

	private InputStream loadSample() {
		return getClass().getResourceAsStream("/sample.json");
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
			try (JsonParser parser = createParser()) {
				List<Event> events = new ArrayList<>();
				while (parser.nextToken() == JsonToken.START_OBJECT)
					events.add(toEvent(parser));
				return events;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private Event toEvent(JsonParser parser) throws IOException {
			TreeNode node = mapper.readTree(parser);
			return mapper.convertValue(node, Event.class);
		}

		private JsonParser createParser() throws IOException, JsonParseException {
			return mapper.getFactory().createParser(inputStream);
		}
	}
}
