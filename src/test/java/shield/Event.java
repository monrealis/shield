package shield;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
	public String type;
	@JsonProperty("model_name")
	public String modelName;
	@JsonProperty("default_policy")
	public String defaultPolicy;
	public List<String> whitelist;
	public List<String> blacklist;
	public Long timestamp;
	@JsonProperty("request_id")
	public String requestId;
	@JsonProperty("device_id")
	public String deviceId;
	@JsonProperty("url")
	public String url;
}