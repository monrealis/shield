package shield;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventHandler {
	private static ObjectMapper mapper = new ObjectMapper();
	private final Map<String, Event> currentPolicy = new HashMap<>();
	private final List<String> unsupportedTypeErrors = new ArrayList<>();
	private final PrintWriter output;

	public EventHandler(PrintWriter output) {
		this.output = output;
	}

	public void handle(Event event) {
		if ("profile_create".equals(event.type))
			handleProfileEvent(event);
		else if ("profile_update".equals(event.type))
			handleProfileEvent(event);
		else if ("request".equals(event.type))
			handleRequest(event);
		else
			unsupportedTypeErrors.add(event.type);
	}

	private void handleProfileEvent(Event event) {
		currentPolicy.put(event.modelName, event);
	}

	private void handleRequest(Event event) {
		handleDecision(Decision.allow(event.requestId));
	}

	protected void handleDecision(Decision decision) {
		String s = marshall(decision);
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

	public int errorCount() {
		return unsupportedTypeErrors.size();
	}

	static class Decision {
		@JsonProperty("request_id")
		public String requestId;
		@JsonProperty("action")
		public Action action;

		public static Decision allow(String requestId) {
			Decision d = new Decision();
			d.requestId = requestId;
			d.action = Action.ALLOW;
			return d;
		}
	}

	static enum Action {
		@JsonProperty("allow")
		ALLOW
	}
}
