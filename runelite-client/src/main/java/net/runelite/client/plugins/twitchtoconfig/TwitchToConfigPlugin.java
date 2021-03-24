package net.runelite.client.plugins.twitchtoconfig;

import java.util.Map;

import javax.annotation.Nullable;
import com.google.inject.Inject;

import com.google.gson.Gson;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chess.ChessConfig;

@Slf4j
@PluginDescriptor(name = "TwitchToConfig", description = "Twitch to Config Config", tags = { "Twitch", "Config" })
public class TwitchToConfigPlugin extends Plugin {

	@Inject
	@Getter
	private TwitchToConfigConfig config;

	@Inject
	public ConfigManager configManager;
	@Inject
	private Gson gson;
	@Inject
	@Getter(AccessLevel.PUBLIC)
	private ClientThread clientThread;

	private TwitchEventRunners twitchListeners;

	@Override
	protected void startUp() throws Exception {
		try {
			twitchListeners = new TwitchEventRunners(this);
			twitchListeners.init();
		} catch (Exception e) {
			log.error("Error loading TwitchToConfigPlugin", e);
		}
	}

	@Provides
	TwitchToConfigConfig getConfig(ConfigManager configManager) {
		return configManager.getConfig(TwitchToConfigConfig.class);
	}

	@Override
	protected void shutDown() throws Exception {
		twitchListeners.shutdown();
		twitchListeners = null;
	}

	@Subscribe
	public void onConfigChanged(@Nullable ConfigChanged event) {
		if(event != null && (event.getKey().equals("shortcuts") || event.getGroup().equals("chess"))) return;
		twitchListeners.configChanged();
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> loadShortcuts() {
		return gson.fromJson(configManager.getConfiguration("twitchtoconfig", null, "shortcuts"), Map.class);
	}

	public void setShortcuts(Map<String, String> shortcuts) {
		configManager.setConfiguration("twitchtoconfig", "shortcuts", gson.toJson(shortcuts));
	}
	
	public void setConfig(String groupName, String key, String value) {
		configManager.setConfiguration(groupName, key, value);
	}
}