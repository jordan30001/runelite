package net.runelite.client.plugins.chess;

//import chesspresso.game.Game;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import lombok.Getter;

public class ChessHandler {

	private ChessPlugin plugin;
	private ChessOverlay overlay;
	@Getter
	private Board board;

	public ChessHandler(ChessPlugin plugin, ChessOverlay overlay, Board board) {
		this.plugin = plugin;
		this.overlay = overlay;
		this.board = board;
		board.loadFromFen("8/8/8/2k5/5K2/5Q2/8/8 w - - 0 1");
		System.err.println(board.toString());
//		chesspresso.move.Move.createCastle()
//		chesspresso.game.Game game = new Game();
//		game.move
		//this.board.clear();
	}

	public void initPiece(int x, int y, String pieceType) {
		//Piece piece = Piece.valueOf(pieceType.toUpperCase().replaceFirst("B ", "BLACK_").replaceFirst("W ", "WHITE_"));
		//board.setPiece(piece, Square.valueOf(Utils.getCharForNumber(x).toUpperCase() + y));
		System.err.println(board.toString());
	}

	public boolean tryMove(String from, String to) {

		System.err.println("-");
		System.err.println("-");
		Square mFrom = Square.valueOf(from.toUpperCase());
		Square mTo = Square.valueOf(to.toUpperCase());
		Move move = new Move(mFrom, mTo);

		if (board.isMoveLegal(new Move(mFrom, mTo), true)) {
			board.doMove(move, true);
			System.err.println(board.toString());
			System.err.println("-");
			System.err.println("-");
			return true;
		}
		System.err.println(board.toString());
		System.err.println("-");
		System.err.println("-");
		return false;
	}

}
