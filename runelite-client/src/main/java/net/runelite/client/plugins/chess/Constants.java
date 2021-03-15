package net.runelite.client.plugins.chess;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class Constants {
	public static final String INVALID_COLOR = "Beep Boop '%s' is an invalid color %s";
	public static final Color FULL_ALPHA = new Color(0, 0, 0, 0);

	public static final class TwitchChat {

		public static final String BASE_COMMAND1 = "!c";
		public static final String BASE_COMMAND2 = "!chess";
		public static final String RESPONSE_BASE_COMMAND = "@%s -> Chess Commands: %s";
		public static final String RESPONSE_MOD_BASE_COMMAND = "@%s -> Chess Commands: %s - Mods: %s";
		
		public static final String USER_MOVE_PIECE1 = "move";
		public static final String USER_MOVE_PIECE2 = "m";
		public static final String RESPONSE_MOVE_PIECE_HELP = "@%s -? Valid input is 'move|m a1a2|a1 a2'";
		public static final String RESPONSE_MOVE_PIECE_VALID = null;

		
		public static final String MOD_RESTART_BOARD = "restart";
		public static final String RESPONSE_MOD_RESTART_BOARD_START = "@%s -> Please wait - restarting board";
		public static final String RESPONSE_MOD_RESTART_BOARD_END = "@%s -> The board was restarted";
		
		public static final String MOD_SET_CHESS_BOARD_COLOR = "boardcolor";
		public static final String RESPONSE_SET_CHESS_BOARD_COLOR_HELP = "@%s accepts hex color code in the form '#696969' or a color code from 'blade needs to choose a url for color codes'";
		public static final String RESPONSE_SET_CHESS_BOARD_COLOR = "@%s '%s' is not a valid color code, accepts hex color code in the form '#696969' or a color code from 'blade needs to choose a url for color codes'";

		public static final String ALL_USER_COMMANDS = initUserCommands();
		public static final String ALL_MOD_COMMANDS = initModCommands();

		private static final String initUserCommands() {
			return Arrays.stream(TwitchChat.class.getDeclaredFields())
					.filter(field -> field.getName().startsWith("USER_") == false && Modifier.isStatic(field.getModifiers()))
					.map(Utils.ThrowingFunctionWrapper(field -> field.get(null).toString())).collect(Collectors.joining("|"));
		}
		
		private static final String initModCommands() {
			return Arrays.stream(TwitchChat.class.getDeclaredFields())
					.filter(field -> field.getName().startsWith("MOD_") == false && Modifier.isStatic(field.getModifiers()))
					.map(Utils.ThrowingFunctionWrapper(field -> field.get(null).toString())).collect(Collectors.joining("|"));
		}
	}
}