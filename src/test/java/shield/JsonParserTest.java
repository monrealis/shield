package shield;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonParserTest {
	ObjectMapper mapper = new ObjectMapper();

	@Test
	void parsesAllEventsFromSample() throws IOException {
		assertEquals(7, parseEvents().size());
	}

	// https://stackoverflow.com/questions/24835431/use-jackson-to-stream-parse-an-array-of-json-objects
	private List<?> parseEvents() throws IOException {
		List<Object> events = new ArrayList<>();
		try (JsonParser parser = createParser()) {
			while (parser.nextToken() == JsonToken.START_OBJECT)
				events.add(mapper.readTree(parser));
		}
		return events;
	}

	private JsonParser createParser() throws IOException, JsonParseException {
		return mapper.getFactory().createParser(loadSample());
	}

	private InputStream loadSample() {
		return getClass().getResourceAsStream("/sample.json");
	}
}
