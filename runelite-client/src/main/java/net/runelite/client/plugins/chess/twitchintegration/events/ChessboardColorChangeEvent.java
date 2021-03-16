package net.runelite.client.plugins.chess.twitchintegration.events;

import static net.runelite.client.plugins.chess.Constants.INVALID_COLOR;
import static net.runelite.client.plugins.chess.data.ChessEmotes.SadKek;

import java.awt.Color;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.plugins.chess.ChessPlugin;
import net.runelite.client.plugins.chess.Utils;
import net.runelite.client.plugins.chess.twitchintegration.TwitchRedemptionInfo;

public class ChessboardColorChangeEvent extends TwitchRedemptionEvent {

	private static final long REPEATED_DELAY_TIME = 0;
	private static final long ENDING_DELAY_TIME = 6000;

	@Getter(AccessLevel.PUBLIC)
	private final ChessPlugin plugin;
	@Getter(AccessLevel.PUBLIC)
	private final String side;
	@Getter(AccessLevel.PUBLIC)
	private final String userInput;

	public ChessboardColorChangeEvent(ChessPlugin plugin, String side, String userInput) {
		super(REPEATED_DELAY_TIME, ENDING_DELAY_TIME, new TwitchRedemptionInfo());
		this.plugin = plugin;
		this.side = side;
		this.userInput = userInput;
	}

	public boolean execute(int callingCount) {
		Color color = Utils.ColorFromString(userInput);
		if (color == null)
			plugin.queueOverheadText(String.format(INVALID_COLOR, userInput, SadKek.toHTMLString(plugin)), 6000, false);
		else
			plugin.configManager.setConfiguration("chess", side, color.getRGB());
		return true;
	}

}
