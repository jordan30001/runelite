package net.runelite.client.plugins.chess.twitchintegration;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.loloof64.chess_lib_java.rules.Position;
import com.loloof64.chess_lib_java.rules.coords.BoardCell;
import com.loloof64.functional.monad.Either;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.chess.ChessHandler;
import net.runelite.client.plugins.chess.ChessOverlay;
import net.runelite.client.plugins.chess.ChessPlugin;
import net.runelite.client.plugins.chess.Constants.TwitchChat;
import net.runelite.client.plugins.chess.Utils;
import net.runelite.client.plugins.chess.data.ChessAscii;
import net.runelite.client.plugins.chess.data.ChessEmotes;
import net.runelite.client.plugins.chess.data.ChessMarkerPoint;
import net.runelite.client.plugins.chess.data.ColorTileMarker;
import net.runelite.client.plugins.chess.twitchintegration.events.ChessboardColorChangeEvent;
import net.runelite.client.util.Text;

public class ChatCommands {
	private static final Set<CommandPermission> MOD_PLUS_OWNER_PERMISSIONS = new HashSet<>(Arrays.asList(CommandPermission.BROADCASTER, CommandPermission.MODERATOR));

	@Getter(AccessLevel.PUBLIC)
	private ChessPlugin plugin;

	public ChatCommands(ChessPlugin plugin) {
		this.plugin = plugin;
		plugin.getTwitchHandler().RegisterListener(ChannelMessageEvent.class, this::onMessageEvent);
	}

	public void onMessageEvent(ChannelMessageEvent event) {
		onMessageEvent(null, event);
	}

	public void onMessageEvent(ChatMessage event) {
		onMessageEvent(event, null);
	}

	private void onMessageEvent(ChatMessage chatMessage, ChannelMessageEvent channelMessage) {

		boolean isChatMessage = chatMessage != null;
		boolean isChannelMessage = channelMessage != null;

		String sanitisedInput = isChatMessage ? Text.removeFormattingTags(chatMessage.getMessage().trim()) : channelMessage.getMessage();
		String[] splitInput = sanitisedInput.split(" ");

		String messageToSend = null;
		if (splitInput[0].toLowerCase().equals(TwitchChat.BASE_COMMAND1) || splitInput[0].toLowerCase().equals(TwitchChat.BASE_COMMAND2)) {
			if (isChannelMessage) {
				if (splitInput.length == 1) {
					if (Collections.disjoint(MOD_PLUS_OWNER_PERMISSIONS, channelMessage.getPermissions()) == false) {
						messageToSend = String.format(TwitchChat.RESPONSE_MOD_BASE_COMMAND, channelMessage.getUser().getName(), TwitchChat.ALL_USER_COMMANDS, TwitchChat.ALL_MOD_COMMANDS);
					} else {
						messageToSend = String.format(TwitchChat.RESPONSE_BASE_COMMAND, channelMessage.getUser().getName(), TwitchChat.ALL_USER_COMMANDS);
					}
				} else {
					switch (splitInput[1].toLowerCase()) {
					case TwitchChat.MOD_RESTART_BOARD:
						channelMessage.getTwitchChat().sendMessage(channelMessage.getChannel().getName(), String.format(TwitchChat.RESPONSE_MOD_RESTART_BOARD_START, channelMessage.getUser().getName()));
						getPlugin().getClientThread().invokeLater(() -> {
							plugin.restartBoard();
							channelMessage.getTwitchChat().sendMessage(channelMessage.getChannel().getName(), String.format(TwitchChat.RESPONSE_MOD_RESTART_BOARD_END, channelMessage.getUser().getName()));
						});
						break;
					case TwitchChat.MOD_SET_CHESS_BOARD_COLOR:
						if (splitInput.length == 2 || splitInput.length == 3) {
							messageToSend = String.format(TwitchChat.RESPONSE_SET_CHESS_BOARD_COLOR_HELP, channelMessage.getUser().getName());
						} else if (splitInput.length == 4) {
							String strColor = Arrays.stream(splitInput, 3, splitInput.length).collect(Collectors.joining(" "));
							Color color = Utils.ColorFromString(strColor);
							if (color == null) {
								messageToSend = String.format(TwitchChat.RESPONSE_SET_CHESS_BOARD_COLOR, channelMessage.getUser().getName(), strColor);
								break;
							}
							getPlugin().queueTwitchRedemption(new ChessboardColorChangeEvent(getPlugin(), splitInput[2] + "TileColor", strColor));
						}
						break;
					case TwitchChat.USER_MOVE_PIECE1:
					case TwitchChat.USER_MOVE_PIECE2:
						if (ChessOverlay.chessPieceUsername.contains(channelMessage.getUser().getName()) == false && Collections.disjoint(MOD_PLUS_OWNER_PERMISSIONS, channelMessage.getPermissions())) {
							break;
						}
						if (splitInput.length == 1) {
							messageToSend = String.format(TwitchChat.RESPONSE_MOVE_PIECE_HELP, channelMessage.getUser().getName());
						}
						Either<Exception, Position> result;
						String sFrom, sTo;
						int[] move = null;
						if (splitInput.length == 3) {
							sFrom = splitInput[2].substring(0, 2);
							sTo = splitInput[2].substring(2, 4);
							move = ChessHandler.getXYOffset(sFrom, sTo);
							result = plugin.getChessHandler().tryMove(move);
						} else if (splitInput.length == 4) {
							sFrom = splitInput[2];
							sTo = splitInput[3];
							move = ChessHandler.getXYOffset(sFrom, sTo);
							result = plugin.getChessHandler().tryMove(move);
						} else {
							messageToSend = String.format(TwitchChat.RESPONSE_MOVE_PIECE_HELP, channelMessage.getUser().getName());
							break;
						}
						if (result.isRight()) {
							List<ColorTileMarker> points = plugin.getPoints();

							for (int i = points.size() - 1; i >= 0; i--) {
								if (points.get(i).isTemporary()) {
									points.remove(i);
								}
							}
							
							if (plugin.getChessHandler().isThisAccountMoving()) {
								ChessMarkerPoint cmp = new ChessMarkerPoint(plugin.getWorldPoint().getRegionID(), plugin.getWorldPoint().getRegionX() + move[1] + 1, plugin.getWorldPoint().getRegionY() + move[0] + 1,
										plugin.getClient().getPlane(), null, Color.RED, null);
								ChessMarkerPoint cmp2 = new ChessMarkerPoint(plugin.getWorldPoint().getRegionID(), plugin.getWorldPoint().getRegionX() + move[3] + 1, plugin.getWorldPoint().getRegionY() + move[2] + 1,
										plugin.getClient().getPlane(), null, Color.GREEN, null);

								Stream.of(cmp, cmp2)
										.map(point -> new ColorTileMarker(WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()), point.getType(), point.getColor(), point.getLabel(), true))
										.flatMap(colorTile -> {
											final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(plugin.getClient(), colorTile.getWorldPoint());
											return localWorldPoints.stream().map(wp -> new ColorTileMarker(wp, colorTile.getType(), colorTile.getColor(), colorTile.getLabel(), colorTile.isTemporary()));
										}).forEach(ctm -> plugin.getPoints().add(ctm));

								Position position = plugin.getChessHandler().getHistory().parent.relatedPosition;
								char a = position.getPieceAt(new BoardCell(move[0], move[1])).toFEN();
								messageToSend = String.format(TwitchChat.RESPONSE_MOVE_PIECE_VALID, channelMessage.getUser().getName(), ChessAscii.fromFEN(a).ascii, sFrom, sTo);
							}
						} else {
							messageToSend = String.format(TwitchChat.RESPONSE_MOVE_PIECE_HELP, channelMessage.getUser().getName());
						}
						break;
					}
				}
			}
		}
		if (isChannelMessage && messageToSend != null) {
			channelMessage.getTwitchChat().sendMessage(channelMessage.getChannel().getName(), messageToSend);
		}
	}
}
