package net.runelite.client.plugins.chess;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import com.github.twitch4j.pubsub.domain.HypeTrainApproaching;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelCommerceEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.CheerbombEvent;
import com.github.twitch4j.pubsub.events.FollowingEvent;
import com.github.twitch4j.pubsub.events.HypeTrainApproachingEvent;
import com.github.twitch4j.pubsub.events.HypeTrainEndEvent;
import com.github.twitch4j.pubsub.events.HypeTrainStartEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.github.twitch4j.pubsub.events.HypeTrainConductorUpdateEvent;
import com.github.twitch4j.pubsub.events.HypeTrainLevelUpEvent;

import net.runelite.api.Player;
import net.runelite.client.plugins.chess.twitchintegration.TwitchIntegration;

public class TwitchEventRunners {

	private ChessPlugin plugin;
	private ChessOverlay overlay;

	private Timer timer;

	public TwitchEventRunners(ChessPlugin plugin, ChessOverlay overlay) {
		this.plugin = plugin;
		this.overlay = overlay;
		this.timer = new Timer(true);
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
		
		/**
		 * from twitch integration switch statemetn
		 * 
		}
			break;
		case "chesskill": {

		}
			break;
		case "chessdance": {

		}
			break;

		case "chesssubtime":
		case "chessaddtime": {

		}
			break;
		}
		 */
	}

	public void CheckChessBoardColorChange(RewardRedeemedEvent event) {
		String eventType = event.getRedemption().getReward().getTitle();
		Color color = Utils.ColorFromString(event.getRedemption().getUserInput());

		if (color == null) {
			Player localPlayer = plugin.client.getLocalPlayer();
			// TODO: blade needs to fix this in his git ignore
			String curOverhead = "<bold>Beep Boop Invalid Color: " + event.getRedemption().getUserInput() + "<img="
					+ (1/* plugin.modIconsStart + ChessEmotes.SADKEK.ordinal() */) + ">";
			localPlayer.setOverheadText(curOverhead);
			TimerTask task = Utils.WrapTimerTask(() -> {
				if (localPlayer.getOverheadText().equals(curOverhead))
					localPlayer.setOverheadText("");
			});

			timer.schedule(task, 6000);
		}
		if ("Change Black Chessboard Tiles".equals(eventType)) {
			plugin.configManager.setConfiguration("chess", "blackTileColor", color.getRGB());
		}
		if ("Change White Chessboard Tiles".equals(eventType)) {
			plugin.configManager.setConfiguration("chess", "whiteTileColor", color.getRGB());
		}

	}

}
