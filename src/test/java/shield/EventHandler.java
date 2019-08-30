package shield;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventHandler {
	private static ObjectMapper mapper = new ObjectMapper();
	private final Map<String, Event> currentPolicies = new HashMap<>();
	private final Set<String> quarantinedDevices = new HashSet<>();
	private final List<String> unsupportedTypeErrors = new ArrayList<>();
	private final PrintWriter output;

	public EventHandler(PrintWriter output) {
		this.output = output;
	}

	public void handle(Event event) {
		if ("profile_create".equals(event.type))
			handleProfileCreate(event);
		else if ("profile_update".equals(event.type))
			handleProfileUpdate(event);
		else if ("request".equals(event.type))
			handleRequest(event);
		else
			unsupportedTypeErrors.add(event.type);
	}

	private void handleProfileCreate(Event event) {
		checkArgument(event.defaultPolicy != null, "default policy cannot be given in a profile update event");
		checkProfileEvent(event);
		currentPolicies.put(event.modelName, event);
	}

	private void handleProfileUpdate(Event event) {
		checkArgument(event.defaultPolicy == null, "default policy cannot be given in a profile update event");
		checkProfileEvent(event);
		Event current = currentPolicy(event);
		checkState(current != null, "cannot update not existing policy");
		current.blacklist = new ArrayList<>(event.blacklist);
		current.whitelist = new ArrayList<>(event.whitelist);
	}

	private void checkProfileEvent(Event event) {
		checkArgument(event.whitelist != null, "whitelist is mandatory");
		checkArgument(event.blacklist != null, "blacklist is mandatory");
		checkArgument(event.modelName != null, "modelName is mandatory");
	}

	private void handleRequest(Event event) {
		handleDecision(decide(event), event.deviceId);
	}

	private Decision decide(Event event) {
		Event policy = currentPolicy(event);
		if (quarantinedDevices.contains(event.deviceId))
			return Decision.quarantine(event.requestId);
		if (policy.whitelist.contains(event.url))
			return Decision.allow(event.requestId);
		return Decision.quarantine(event.requestId);
	}

	private Event currentPolicy(Event event) {
		return currentPolicies.get(event.modelName);
	}

	private void handleDecision(Decision decision, String deviceId) {
		if (decision.action == Action.BLOCK)
			quarantine(deviceId);
		handleDecision(decision);
	}

	protected void quarantine(String deviceId) {
		quarantinedDevices.add(deviceId);
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
		return currentPolicies.get(modelName).whitelist.contains(url);
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
			return action(requestId, Action.ALLOW);
		}

		public static Decision quarantine(String requestId) {
			return action(requestId, Action.BLOCK);
		}

		private static Decision action(String requestId, Action action) {
			Decision d = new Decision();
			d.requestId = requestId;
			d.action = action;
			return d;
		}
	}

	static enum Action {
		@JsonProperty("allow")
		ALLOW, //
		@JsonProperty("block")
		BLOCK
	}
}
