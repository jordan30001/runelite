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

	private static final long REPEATED_DELAY_TIME = 1000;
	private static final long ENDING_DELAY_TIME = 0;
	private static final long TOTAL_EXECUTION_TIME = 10000;

	@Getter(AccessLevel.PROTECTED)
	private final ChessPlugin plugin;

	public ChessboardDisco(ChessPlugin plugin) {
		super(REPEATED_DELAY_TIME, ENDING_DELAY_TIME, TOTAL_EXECUTION_TIME, new TwitchRedemptionInfo());
		this.plugin = plugin;
		setBlackStart(Utils.getRandomColor());
		setWhiteStart(Utils.getRandomColor());
		setBlackEnd(Utils.getRandomColor());
		setWhiteEnd(Utils.getRandomColor());
	}

	public boolean execute(long deltaTime) {
		long totalExecutionTime = (long) get("totalexecutiontime", 0L);
		if (totalExecutionTime >= getTotalExecutionTime())
			return true;
		totalExecutionTime += deltaTime;
		set("totalexecutiontime", totalExecutionTime);

		long cycleExecutionTime = (long) get("cycleexecutiontime", 0L);
		cycleExecutionTime += deltaTime;
		if (cycleExecutionTime >= getRepeatedDelayTime()) {
			cycleExecutionTime = 0;
			setBlackStart(getBlackEnd());
			setWhiteStart(getWhiteStart());
			setBlackEnd(Utils.getRandomColor());
			setWhiteEnd(Utils.getRandomColor());
		}
		set("cycleexecutiontime", cycleExecutionTime);

		double colorPercent = (double) cycleExecutionTime / (double) getRepeatedDelayTime();
		Color white = Utils.mixColors(getWhiteStart(), getWhiteEnd(), colorPercent);
		Color black = Utils.mixColors(getBlackStart(), getBlackEnd(), colorPercent);
		getPlugin().configManager.setConfiguration("chess", "whiteTileColor", white);
		getPlugin().configManager.setConfiguration("chess", "blackTileColor", black);
		if (getPlugin().client.getLocalPlayer().getAnimation() == AnimationID.IDLE) {
			getPlugin().client.getLocalPlayer().setAnimation(DanceAnimations.values()[ThreadLocalRandom.current().nextInt(0, DanceAnimations.values().length)].id);
			getPlugin().client.getLocalPlayer().setActionFrame(0);
		}
		return false;
	}

	public void set(String key, Object val) {
		getTwitchRedemptionInfo().getVars().put(key, val);
	}

	public Object get(String key) {
		return getTwitchRedemptionInfo().getVars().get(key);
	}

	public Object get(String key, Object defaultValue) {
		return getTwitchRedemptionInfo().getVars().getOrDefault(key, defaultValue);
	}

	private final Color getBlackStart() {
		return (Color) get("blackstart");
	}

	private final Color getWhiteStart() {
		return (Color) get("whitestart");
	}

	private final Color getBlackEnd() {
		return (Color) get("blackend");
	}

	private final Color getWhiteEnd() {
		return (Color) get("whiteend");
	}

	private final void setBlackStart(Color color) {
		set("blackstart", color);
	}

	private final void setWhiteStart(Color color) {
		set("whitestart", color);
	}

	private final void setBlackEnd(Color color) {
		set("blackend", color);
	}

	private final void setWhiteEnd(Color color) {
		set("whiteend", color);
	}

}
