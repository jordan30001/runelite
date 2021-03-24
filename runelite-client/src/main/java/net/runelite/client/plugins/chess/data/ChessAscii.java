package net.runelite.client.plugins.chess.data;

import com.loloof64.chess_lib_java.rules.pieces.Bishop;
import com.loloof64.chess_lib_java.rules.pieces.King;
import com.loloof64.chess_lib_java.rules.pieces.Knight;
import com.loloof64.chess_lib_java.rules.pieces.Pawn;
import com.loloof64.chess_lib_java.rules.pieces.Piece;
import com.loloof64.chess_lib_java.rules.pieces.Queen;
import com.loloof64.chess_lib_java.rules.pieces.Rook;

public enum ChessAscii {
	WHITE_PAWN("P", "♙"), WHITE_KNIGHT("N", "♘"), WHITE_BISHOP("B", "♗"), WHITE_ROOK("R", "♖"), WHITE_QUEEN("Q", "♕"), WHITE_KING("K", "♔"),
	BLACK_PAWN("p", "♟︎"), BLACK_KNIGHT("n", "♞"), BLACK_BISHOP("b", "♝"), BLACK_ROOK("r", "♜"), BLACK_QUEEN("q", "♛"), BLACK_KING("k", "♚");
	
	public String FEN;
	public String ascii;

	private ChessAscii(String fen, String ascii) {
		this.FEN = fen;
		this.ascii = ascii;
	}
	
    public static ChessAscii fromFEN(char fenChar){
        switch (fenChar){
        case 'P': return WHITE_PAWN;
        case 'N': return WHITE_KNIGHT;
        case 'B': return WHITE_BISHOP;
        case 'R': return WHITE_ROOK;
        case 'Q': return WHITE_QUEEN;
        case 'K': return WHITE_KING;
        case 'p': return BLACK_PAWN;
        case 'n': return BLACK_KNIGHT;
        case 'b': return BLACK_BISHOP;
        case 'r': return BLACK_ROOK;
        case 'q': return BLACK_QUEEN;
        case 'k': return BLACK_KING;
            default: return null;
        }
    }
}
