package net.runelite.client.plugins.chess.twitchintegration;

import java.util.Arrays;
import java.util.Map;

import com.google.inject.Inject;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.pubsub.domain.FollowingData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.FollowingEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

import net.runelite.client.plugins.chess.ChessOverlay;
import net.runelite.client.plugins.chess.ChessPlugin;
import net.runelite.client.plugins.chess.data.ChessEmotes;
import net.runelite.client.plugins.chess.twitchintegration.events.ChessboardColorChangeEvent;
import net.runelite.client.plugins.chess.twitchintegration.events.ChessboardDisco;
import net.runelite.client.plugins.twitch4j.TwitchIntegration;

public class TwitchEventRunners {

	private TwitchIntegration twitchHandler = TwitchIntegration.INSTANCE;
	@Inject
	private ChessPlugin plugin;
	private ChessOverlay overlay;
	private OAuth2Credential credential;
	private volatile boolean shutdown;

	public TwitchEventRunners(ChessPlugin plugin, ChessOverlay overlay) {
		this.plugin = plugin;
		this.overlay = overlay;
		this.credential = new OAuth2Credential("twitch", overlay.config.OAUthCode());
	}

	public void configChanged() {
		if (credential != null) {
			shutdown = true;
			twitchHandler.shutdown(credential);
		}
		init();
	}

	public void init() {
		credential = new OAuth2Credential("twitch", overlay.config.OAUthCode());
		TwitchHelix helix = twitchHandler.createTwitchHelixEndpointsIfNotExist(overlay.config.clientID(), credential);
		Map<String, String> channelInfo = twitchHandler.getTwitchChannelIDsFromName(helix, credential, Arrays.asList(new String[] {overlay.config.channelName(), overlay.config.channelUsername()}));
		twitchHandler.createTwitchClientIfNotExist(overlay.config.clientID(), credential, Arrays.asList(channelInfo.get(overlay.config.channelUsername())), true, true);
		twitchHandler.registerShutdownListener(o -> {
			if (credential == null)
				return true;
			else if (o.equals(credential))
				return shutdown;
			else
				return true;
		});
		// this is stupid, need to refactor to allow for removal of listeners.
		TwitchIntegration eventManager = twitchHandler;
		eventManager.addPubsubListener(credential, RewardRedeemedEvent.class);
		eventManager.RegisterListener(credential, RewardRedeemedEvent.class, this::CheckChessBoardColorChange);
		twitchHandler.joinChannel(credential, plugin.getConfig().channelName());
	}

	public void CheckChessBoardColorChange(RewardRedeemedEvent event) {
		String eventType = event.getRedemption().getReward().getTitle();
		TwitchRedemption redemption = TwitchRedemption.getFromName(eventType);

		if (redemption == null) {
			return;
		}

		switch (redemption) {
		case ChangeBlackBoardColor:
		case ChangeWhiteBoardColor:
			plugin.queueTwitchRedemption(new ChessboardColorChangeEvent(plugin, redemption.params[0], event.getRedemption().getUserInput()));
			break;
		case DiscoChessboard:
			plugin.queueTwitchRedemption(new ChessboardDisco(plugin));
			break;
		default:
			break;

		}
	}

	public void shutdown() {
		this.shutdown = true;
		twitchHandler.shutdown(credential);
	}
}
