package net.runelite.client.plugins.twitchtoconfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.helix.TwitchHelix;
import com.google.inject.Inject;

import net.runelite.client.plugins.twitch4j.TwitchIntegration;

public class TwitchEventRunners {

	private TwitchIntegration twitchHandler = TwitchIntegration.INSTANCE;
	@Inject
	private TwitchToConfigPlugin plugin;
	private OAuth2Credential credential;
	private volatile boolean shutdown;
	private Map<String, String> shortcuts;

	public TwitchEventRunners(TwitchToConfigPlugin plugin) {
		this.plugin = plugin;
		this.credential = new OAuth2Credential("twitch", plugin.getConfig().OAUthCode());
	}

	public void configChanged() {
		if (credential != null) {
			twitchHandler.shutdown(credential);
		}
		init();
	}

	public void init() {
		shortcuts = plugin.loadShortcuts();
		if(shortcuts == null) shortcuts = new HashMap<>();
		credential = new OAuth2Credential("twitch", plugin.getConfig().OAUthCode());
		TwitchHelix helix = twitchHandler.createTwitchHelixEndpointsIfNotExist(plugin.getConfig().clientID(), credential);
		Map<String, String> channelInfo = twitchHandler.getTwitchChannelIDsFromName(helix, credential, Arrays.asList(new String[] { plugin.getConfig().channelName(), plugin.getConfig().botName() }));
		twitchHandler.createTwitchClientIfNotExist(plugin.getConfig().clientID(), credential, Arrays.asList(channelInfo.get(plugin.getConfig().botName())), true, true);
		twitchHandler.registerShutdownListener(o -> {
			if (credential == null)
				return true;
			else if (o.equals(credential))
				return shutdown;
			else
				return true;
		});
		// this is stupid, need to refactor to allow for removal of listeners.
		twitchHandler.addChatListener(credential, ChannelMessageEvent.class);
		twitchHandler.RegisterListener(credential, ChannelMessageEvent.class, this::onMessageEvent);
		twitchHandler.joinChannel(credential, plugin.getConfig().channelName());
	}

	public void onMessageEvent(ChannelMessageEvent event) {
		String messageToSend = null;
		if (event.getChannel().getName().equals(plugin.getConfig().channelName()) && event.getPermissions().contains(CommandPermission.BROADCASTER) && event.getUser().getName().equalsIgnoreCase(plugin.getConfig().botName())) {
			String[] chunks = event.getMessage().split(" ");
			if (chunks.length > 0) {
				if (chunks[0].equalsIgnoreCase("!ttc") == false) {
					return;
				}
				if (chunks.length == 1) {
					messageToSend = "commands are: set|addshortcut";
				} else {
					switch (chunks[1].toLowerCase()) {
					case "set":
						if (chunks.length == 2 || chunks.length == 3) {
							messageToSend = "Set client configurations that are listening: Fully Qualified Config Name (case sensitive) followed by the value. Syntax: chess.whiteBoardColor #696969";
						} else {
							String FQCN = chunks[2].startsWith("!") ? shortcuts.getOrDefault(chunks[2].toLowerCase(), null) : chunks[2];
							String pluginName = FQCN.substring(0, FQCN.indexOf("."));
							String key = FQCN.substring(FQCN.indexOf(".") + 1, FQCN.length());
							String value = chunks.length > 3 ? Arrays.stream(chunks, 3, chunks.length).collect(Collectors.joining("")): "";
							String tempMessageToSend = String.format("Set config '%s' to '%s'", FQCN, value);
							plugin.getClientThread().invokeLater(() -> {
								plugin.setConfig(pluginName, key, value);
								event.getTwitchChat().sendMessage(event.getChannel().getName(), tempMessageToSend);
							});
						}
						break;
					case "addshortcut":
						if (chunks.length == 2 || chunks.length == 3 || chunks[2].startsWith("!") == false) {
							messageToSend = "Sets a shotcut for a Fully Qualified Config Name for use in 'set' commands (shortcut must begin with '!'). Syntax: !wbc chess.whiteBoardColor";
						} else {
							String tempMessageToSend = String.format("Set shortcut '%s' to fully qualified config name '%s'", chunks[2], chunks[3]);
							plugin.getClientThread().invokeLater(() -> {
								shortcuts.put(chunks[2], chunks[3]);
								plugin.setShortcuts(shortcuts);
								event.getTwitchChat().sendMessage(event.getChannel().getName(), tempMessageToSend);
							});
						}
						break;
					}
				}
			}
		}
		if (messageToSend != null) {
			event.getTwitchChat().sendMessage(event.getChannel().getName(), messageToSend);
		}
	}

	public void shutdown() {
		this.shutdown = true;
		twitchHandler.shutdown(credential);
	}
}
