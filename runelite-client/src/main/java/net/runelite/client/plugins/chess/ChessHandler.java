package net.runelite.client.plugins.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class ChessHandler {

	private ChessPlugin plugin;
	private ChessOverlay overlay;
	private Board board;

	public ChessHandler(ChessPlugin plugin, ChessOverlay overlay, Board board) {
		this.plugin = plugin;
		this.overlay = overlay;
		this.board = board;
	}

	public void initPiece(int x, int y, String pieceType) {
		Piece piece = Piece.valueOf(pieceType.toUpperCase().replaceFirst("B ", "BLACK_").replaceFirst("W ", "WHITE_"));
		board.setPiece(piece, Square.valueOf(Utils.getCharForNumber(x).toUpperCase() + y));
	}
	
	public boolean tryMove(String from, String to) {
		Square mFrom = Square.valueOf(from.toUpperCase());
		Square mTo = Square.valueOf(to.toUpperCase());
		Move move = new Move(mFrom, mTo);
		
		if(board.isMoveLegal(new Move(mFrom, mTo), true)) {
			board.doMove(move, false);
		}
		return false;
	}

}
