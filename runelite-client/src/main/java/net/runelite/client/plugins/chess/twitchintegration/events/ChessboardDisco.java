package net.runelite.client.plugins.chess.twitchintegration.events;

import java.awt.Color;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.AnimationID;
import net.runelite.client.plugins.chess.ChessPlugin;
import net.runelite.client.plugins.chess.Utils;
import net.runelite.client.plugins.chess.data.DanceAnimations;
import net.runelite.client.plugins.chess.twitchintegration.TwitchRedemptionInfo;

public class ChessboardDisco extends TwitchRedemptionEvent {

	private static final long REPEATED_DELAY_TIME = 20;
	private static final long ENDING_DELAY_TIME = 0;

	@Getter(AccessLevel.PROTECTED)
	private final ChessPlugin plugin;

	public ChessboardDisco(ChessPlugin plugin) {
		super(REPEATED_DELAY_TIME, ENDING_DELAY_TIME, new TwitchRedemptionInfo());
		this.plugin = plugin;
	}

	@SuppressWarnings("unchecked")
	public boolean execute(int callingCount) {
		if (callingCount == 1) {
			getTwitchRedemptionInfo().getVars().put("black", Utils.LerpRandomColors(getTwitchRedemptionInfo().getVars(), 5, 192));
			getTwitchRedemptionInfo().getVars().put("white", Utils.LerpRandomColors(getTwitchRedemptionInfo().getVars(), 5, 192));
		}
		getPlugin().configManager.setConfiguration("chess", "whiteTileColor", ((Queue<Color>) getTwitchRedemptionInfo().getVars().get("black")).poll());
		getPlugin().configManager.setConfiguration("chess", "blackTileColor", ((Queue<Color>) getTwitchRedemptionInfo().getVars().get("white")).poll());
		if (getPlugin().client.getLocalPlayer().getAnimation() == AnimationID.IDLE) {
			getPlugin().client.getLocalPlayer().setAnimation(DanceAnimations.values()[ThreadLocalRandom.current().nextInt(0, DanceAnimations.values().length)].id);
			getPlugin().client.getLocalPlayer().setActionFrame(0);
		}
		if (callingCount >= 960) {
			return true;
		}
		return false;
	}

}
