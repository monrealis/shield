package shield;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class EventHandler {
	private static ObjectMapper mapper = new ObjectMapper();
	private final Map<String, Event> currentPolicy = new HashMap<>();
	private final PrintWriter output;

	public EventHandler(PrintWriter output) {
		this.output = output;
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public void handle(Event event) {
		if (event.type.equals("profile_create") || event.type.equals("profile_update"))
			currentPolicy.put(event.modelName, event);
		else
			handleRequest(event);
	}

	private void handleRequest(Event event) {
		Decision d = new Decision();
		d.action = "allow";
		d.requestId = event.requestId;
		String s = marshall(d);
		output.println(s);
	}

	private String marshall(Decision d) {
		try {
			String s = mapper.writeValueAsString(d);
			return s;
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean inspectRequest(String modelName, String url) {
		return currentPolicy.get(modelName).whitelist.contains(url);
	}

	private static class Decision {
		@JsonProperty("request_id")
		public String requestId;
		@JsonProperty("action")
		public String action;
	}
}
