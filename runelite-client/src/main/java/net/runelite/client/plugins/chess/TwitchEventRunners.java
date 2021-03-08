package net.runelite.client.plugins.chess;

import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

import com.github.twitch4j.chat.events.roomstate.FollowersOnlyEvent;
import com.github.twitch4j.pubsub.domain.FollowingData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelCommerceEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.CheerbombEvent;
import com.github.twitch4j.pubsub.events.FollowingEvent;
import com.github.twitch4j.pubsub.events.HypeTrainApproachingEvent;
import com.github.twitch4j.pubsub.events.HypeTrainConductorUpdateEvent;
import com.github.twitch4j.pubsub.events.HypeTrainEndEvent;
import com.github.twitch4j.pubsub.events.HypeTrainLevelUpEvent;
import com.github.twitch4j.pubsub.events.HypeTrainStartEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

import net.runelite.api.Player;
import net.runelite.client.plugins.chess.twitchintegration.TwitchIntegration;

public class TwitchEventRunners {

	private ChessPlugin plugin;
	private ChessOverlay overlay;

	public TwitchEventRunners(ChessPlugin plugin, ChessOverlay overlay) {
		this.plugin = plugin;
		this.overlay = overlay;
	}

	public void init() {
		TwitchIntegration eventManager = plugin.getTwitchEventManager();
		eventManager.RegisterPubSubListener(RewardRedeemedEvent.class);
		eventManager.RegisterPubSubListener(ChannelBitsEvent.class);
		eventManager.RegisterPubSubListener(ChannelCommerceEvent.class);
		eventManager.RegisterPubSubListener(ChannelSubGiftEvent.class);
		eventManager.RegisterPubSubListener(ChannelSubscribeEvent.class);
		eventManager.RegisterPubSubListener(CheerbombEvent.class);
		eventManager.RegisterPubSubListener(FollowingEvent.class);
		eventManager.RegisterPubSubListener(HypeTrainApproachingEvent.class);
		eventManager.RegisterPubSubListener(HypeTrainConductorUpdateEvent.class);
		eventManager.RegisterPubSubListener(HypeTrainLevelUpEvent.class);
		eventManager.RegisterPubSubListener(HypeTrainStartEvent.class);
		eventManager.RegisterPubSubListener(HypeTrainEndEvent.class);

		eventManager.RegisterListener(RewardRedeemedEvent.class, this::CheckChessBoardColorChange);
		eventManager.RegisterListener(ChannelSubscribeEvent.class, this::onTwitchSub);
		eventManager.RegisterListener(FollowingEvent.class, this::onFollower);

		/**
		 * from twitch integration switch statemetn
		 *
		 * } break; case "chesskill": {
		 * 
		 * } break; case "chessdance": {
		 * 
		 * } break;
		 * 
		 * case "chesssubtime": case "chessaddtime": {
		 * 
		 * } break; }
		 */
	}

	public void CheckChessBoardColorChange(RewardRedeemedEvent event) {
		String eventType = event.getRedemption().getReward().getTitle();
		Color color = Utils.ColorFromString(event.getRedemption().getUserInput());

		if (color == null) {
			plugin.queueOverheadText(String.format("Beep Boop Invalid Color: %s", ChessEmotes.SadKek.toHTMLString(plugin.modIconsStart),
					ChessEmotes.SadKek.toHTMLString(plugin.modIconsStart)), 6000, false);
			return;
		}
		if ("Change Black Chessboard Tiles".equals(eventType)) {
			plugin.configManager.setConfiguration("chess", "blackTileColor", color.getRGB());
		}
		if ("Change White Chessboard Tiles".equals(eventType)) {
			plugin.configManager.setConfiguration("chess", "whiteTileColor", color.getRGB());
		}
	}

	public void onTwitchSub(ChannelSubscribeEvent event) {
		SubscriptionData data = event.getData();
		switch (data.getSubPlan()) {
		case TIER1:
			plugin.queueOverheadText(String.format("%s just subscribed %s", data.getDisplayName(),
					ChessEmotes.Bladeb7PogChamp.toHTMLString(plugin.modIconsStart)), 5000, false);
			break;
		case TIER2:
			plugin.queueOverheadText(String.format("%s just subscribed %s", data.getDisplayName(),
					ChessEmotes.HandsUp.toHTMLString(plugin.modIconsStart)), 5000, false);
			break;
		case TIER3:
			plugin.queueOverheadText(String.format("%s just subscribed %s", data.getDisplayName(),
					ChessEmotes.WidePeepoHappy.toHTMLString(plugin.modIconsStart)), 5000, false);
			break;
		case TWITCH_PRIME:
			plugin.queueOverheadText(String.format("%s just subscribed %s", data.getDisplayName(),
					ChessEmotes.PrimeWhatYouSay.toHTMLString(plugin.modIconsStart)), 5000, false);
			break;
		case NONE:
		default:
			break;

		}
	}

	public void onFollower(FollowingEvent event) {
		FollowingData data = event.getData();
		plugin.queueOverheadText(String.format("Thanks for following %s%s",
				data.getDisplayName(), ChessEmotes.PeepoHappy.toHTMLString(plugin.modIconsStart)), 5000, false);
	}
}
