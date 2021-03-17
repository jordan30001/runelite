package net.runelite.client.plugins.chess;

import com.loloof64.chess_lib_java.history.ChessHistoryNode;
import com.loloof64.chess_lib_java.rules.Board;
import com.loloof64.chess_lib_java.rules.Move;
import com.loloof64.chess_lib_java.rules.Position;
import com.loloof64.chess_lib_java.rules.coords.BoardCell;
import com.loloof64.chess_lib_java.rules.pieces.Piece;
import com.loloof64.functional.monad.Either;

import lombok.AccessLevel;
import lombok.Getter;

public class ChessHandler {

	public static final String RANK = "ABCDEFGH";

	private ChessPlugin plugin;
	private ChessOverlay overlay;
	private Position position;
	private String[][] pieceUsernames;
	@Getter(AccessLevel.PUBLIC)
	private ChessHistoryNode history;

	public ChessHandler(ChessPlugin plugin, ChessOverlay overlay) {
		this.plugin = plugin;
		this.overlay = overlay;

		this.pieceUsernames = new String[8][8];
	}

	public void reset() {
		this.history = null;
		this.position = Position.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").right();
	}

	public void initBaseBoard() {
		this.position = Position.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").right();
		history = ChessHistoryNode.rootNode(position, "start", "").right();
	}

	public void initPiece(int x, int y, char piece) {
		this.position.board.values()[x][y] = Piece.fromFEN(piece);
		this.position = Position.fromFEN(this.position.board.toFEN()).right();
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
