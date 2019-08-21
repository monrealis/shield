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
	ObjectMapper mapper = new ObjectMapper();

	@Test
	void parsesAllEventsFromSample() {
		List<Event> events = parseEvents();

		assertEquals(7, events.size());
		assertEquals("profile_create", events.get(0).type);
		assertEquals("request", events.get(1).type);
	}

	// https://stackoverflow.com/questions/24835431/use-jackson-to-stream-parse-an-array-of-json-objects
	private List<Event> parseEvents() {
		try (JsonParser parser = createParser()) {
			List<Event> events = new ArrayList<>();
			while (parser.nextToken() == JsonToken.START_OBJECT)
				toEvent(events, parser);
			return events;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void toEvent(List<Event> events, JsonParser parser) throws IOException {
		TreeNode node = mapper.readTree(parser);
		events.add(mapper.convertValue(node, Event.class));
	}

	private JsonParser createParser() throws IOException, JsonParseException {
		return mapper.getFactory().createParser(loadSample());
	}

	private InputStream loadSample() {
		return getClass().getResourceAsStream("/sample.json");
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class Event {
		public String type;
	}
}
