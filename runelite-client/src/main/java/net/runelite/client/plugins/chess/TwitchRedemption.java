package net.runelite.client.plugins.chess;

import static net.runelite.client.plugins.chess.ChessEmotes.SadKek;
import static net.runelite.client.plugins.chess.Constants.*;

import java.awt.Color;
import java.util.Arrays;

import net.runelite.api.AnimationID;
import net.runelite.client.config.ConfigManager;

public enum TwitchRedemption {
	ChangeBlackBoardColor("Change Black Chessboard Tiles", 0, 6000, (Function1<String>) (i, cp, ui) -> {
		Color color = Utils.ColorFromString(ui);

		if (color == null)
			cp.queueOverheadText(String.format(INVALID_COLOR, ui, SadKek.toHTMLString(cp)), 6000, false);
		else
			cp.configManager.setConfiguration("chess", "blackTileColor", color.getRGB());

		return true;
	}), ChangeWhiteBoardColor("Change White Chessboard Tiles", 0, 6000, (Function1<String>) (i, cp, ui) -> {
		Color color = Utils.ColorFromString(ui);

		if (color == null)
			cp.queueOverheadText(String.format(INVALID_COLOR, ui, SadKek.toHTMLString(cp)), 6000, false);
		else
			cp.configManager.setConfiguration("chess", "whiteTileColor", color.getRGB());
		return true;
	}), DiscoChessboard("Everybody to the dancefloor", 500, 0, (Function) (i, cp) -> {
		cp.configManager.setConfiguration("chess", "whiteTileColor", Utils.getRandomColor());
		cp.configManager.setConfiguration("chess", "blackTileColor", Utils.getRandomColor());
		// TODO: change animation to dance, doesn't seem like it is undocumented though
		// :(
		if (cp.client.getLocalPlayer().getAnimation() == AnimationID.IDLE) {
			cp.client.getLocalPlayer().setAnimation(AnimationID.DIG);
		}
		if (i >= 50)
			return true;
		return false;
	});

	public String name;
	public CustomFunction function;
	public long repeatedExecutionDelay;
	public long endingDelay;

	<F extends CustomFunction> TwitchRedemption(String name, long repeatedExecutionDelay, long endingDelay,
			F function) {
		this.name = name;
		this.function = function;
		this.repeatedExecutionDelay = repeatedExecutionDelay;
		this.endingDelay = endingDelay;
	}

	public static TwitchRedemption getFromName(String name) {
		return Arrays.stream(values()).filter(e -> e.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	public static interface CustomFunction {
	}

	@FunctionalInterface
	public static interface Function extends CustomFunction {
		boolean accept(int callCount, ChessPlugin plugin);
	}

	@FunctionalInterface
	public static interface Function1<A> extends CustomFunction {
		boolean accept(int callCount, ChessPlugin plugin, A a);
	}

	@FunctionalInterface
	public static interface Function2<A, B> extends CustomFunction {
		boolean accept(int callCount, ChessPlugin plugin, A a, B b);
	}

	@FunctionalInterface
	public static interface Function3<A, B, C> extends CustomFunction {
		boolean accept(int callCount, ChessPlugin plugin, A a, B b, C c);
	}

	@FunctionalInterface
	public static interface Function4<A, B, C, D> extends CustomFunction {
		boolean accept(int callCount, ChessPlugin plugin, A a, B b, C c, D d);
	}

	@FunctionalInterface
	public static interface Function5<A, B, C, D, E> extends CustomFunction {
		boolean accept(int callCount, ChessPlugin plugin, A a, B b, C c, D d, E e);
	}

}
