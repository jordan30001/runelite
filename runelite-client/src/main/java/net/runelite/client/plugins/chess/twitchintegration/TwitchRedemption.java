package net.runelite.client.plugins.chess.twitchintegration;

import java.util.Arrays;

import javax.annotation.Nullable;

public enum TwitchRedemption {
	// @formatter:off
	ChangeBlackBoardColor("Change Black Chessboard Tiles", "blackTileColor"), 
	ChangeWhiteBoardColor("Change White Chessboard Tiles", "whiteTileColor"), 
	DiscoChessboard("Everybody to the dancefloor");
	// @formatter:on

	public String name;
	public CustomFunction function;
	public long repeatedExecutionDelay;
	public long endingDelay;
	@Nullable
	public String[] params;

	<F extends CustomFunction> TwitchRedemption(String name, String... params) {
		this.name = name;
		this.params = params;
	}

	public static TwitchRedemption getFromName(String name) {
		return Arrays.stream(values()).filter(e -> e.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}

}
