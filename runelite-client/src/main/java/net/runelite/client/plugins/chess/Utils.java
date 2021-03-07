package net.runelite.client.plugins.chess;

import java.awt.Color;
import java.util.TimerTask;

public class Utils {

	public static Color ColorFromString(String str) {
		if (str.indexOf("#") >= 0) {
			return new Color(Integer.valueOf(str.substring(1, 3), 16), Integer.valueOf(str.substring(3, 5), 16),
					Integer.valueOf(str.substring(5, 7), 16));
		} else {
			switch (str.toLowerCase()) {
			case "black":
				return Color.BLACK;
			case "blue":
				return Color.BLUE;
			case "cyan":
				return Color.CYAN;
			case "darkgray":
				return Color.DARK_GRAY;
			case "gray":
				return Color.GRAY;
			case "green":
				return Color.GREEN;
			case "yellow":
				return Color.YELLOW;
			case "lightgray":
				return Color.LIGHT_GRAY;
			case "magneta":
				return Color.MAGENTA;
			case "orange":
				return Color.ORANGE;
			case "pink":
				return Color.PINK;
			case "red":
				return Color.RED;
			case "white":
				return Color.WHITE;
			default:
				return null;
			}
		}
	}

	public static final TimerTask WrapTimerTask(Runnable r) {
		return new TimerTask() {
			@Override
			public void run() {
				r.run();
			}
		};
	}
}
