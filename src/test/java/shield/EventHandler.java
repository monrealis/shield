package shield;

import java.util.HashMap;
import java.util.Map;

public class EventHandler {
	private Map<String, Event> currentPolicy = new HashMap<>();

	public void handle(Event event) {
		currentPolicy.put(event.modelName, event);
	}

	public boolean inspectRequest(String modelName, String url) {
		return currentPolicy.get(modelName).whitelist.contains(url);
	}
}
