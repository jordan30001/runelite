package net.runelite.client.plugins.chess;

import javax.inject.Inject;

import com.loloof64.chess_lib_java.history.ChessHistoryNode;
import com.loloof64.chess_lib_java.rules.GameInfo;
import com.loloof64.chess_lib_java.rules.Move;
import com.loloof64.chess_lib_java.rules.Position;
import com.loloof64.chess_lib_java.rules.coords.BoardCell;
import com.loloof64.chess_lib_java.rules.pieces.Piece;
import com.loloof64.functional.monad.Either;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ClientUI;

public class ChessHandler {

	public static final String RANK = "ABCDEFGH";

	private ChessPlugin plugin;
	private ChessOverlay overlay;
	private Position position;
	private String[][] pieceUsernames;
	@Getter(AccessLevel.PUBLIC)
	private ChessHistoryNode history;
	@Inject
	private ClientUI clientUI;
	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PUBLIC)
	private boolean isThisAccountMoving;

	public ChessHandler(ChessPlugin plugin, ChessOverlay overlay) {
		this.plugin = plugin;
		this.overlay = overlay;

		this.pieceUsernames = new String[8][8];
	}

	public void reset() {
		history = null;
		position = Position.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").right();
	}

	public void initBaseBoard(char[][] pieces) {
		Position testPosition = position = Position.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").right();
		StringBuilder sb = new StringBuilder();
		int emptyCount = 0;
		int rowCount = 0;
		for (int y = 7; y >=0; y--) {
			for (int x = 0; x < 8; x++) {
				if (pieces[y][x] == '\0') {
					emptyCount++;
					if (emptyCount == 8 || rowCount == 8) {
						sb.append(emptyCount);
						sb.append("/");
						emptyCount = 0;
						continue;
					}
				}
				else {
					if(emptyCount > 0) {
						sb.append(emptyCount);
						emptyCount = 0;
						rowCount++;
						if(rowCount == 8) {
							sb.append("/");
							rowCount = 0;
						}
					}
					sb.append(pieces[y][x]);
					rowCount++;
					if(rowCount == 8) {
						sb.append("/");
						rowCount = 0;
					}
				}
			}
		}
		sb.setLength(sb.length() - 1);

		Either<Exception, Position> e = Position.fromFEN(sb.toString() + " w KQkq - 0 1");
		if (e.isLeft()) {
			e.left().printStackTrace();
		}
		position = Position.fromFEN(sb.toString() + " w KQkq - 0 1").right();
		history = ChessHistoryNode.rootNode(position, "start", "").right();
	}

	public Either<Exception, Position> tryMove(int[] iMove) {
		if (history == null)
			history = ChessHistoryNode.rootNode(position, "start", "").right();
		BoardCell from = new BoardCell(iMove[0], iMove[1]);
		BoardCell to = new BoardCell(iMove[2], iMove[3]);
		Move move = new Move(from, to);
		Either<Exception, Position> positionMove = position.move(move);
		if (positionMove.isRight()) {
			position = positionMove.right();
			if (pieceUsernames[from.rank][from.file].equals(plugin.getClient().getLocalPlayer().getName())) {
				clientUI.forceFocus();
				setThisAccountMoving(true);
			}
			pieceUsernames[to.rank][to.file] = pieceUsernames[from.rank][from.file];
			pieceUsernames[from.rank][from.file] = null;
			history = ChessHistoryNode.nonRootNode(history, move, "", "").right();
		}
		return positionMove;
	}

	public static final int[] getXYOffset(String sFrom, String sTo) {
		sFrom = sFrom.toUpperCase();
		sTo = sTo.toUpperCase();
		return new int[] { Integer.parseInt(sFrom.substring(1)) - 1, RANK.indexOf(sFrom.charAt(0)), Integer.parseInt(sTo.substring(1)) - 1, RANK.indexOf(sTo.charAt(0)) };
	}
}
