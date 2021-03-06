/*
 * Copyright (c) 2018, Joris K <kjorisje@gmail.com>
 * Copyright (c) 2018, Lasse <cronick@zytex.dk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.chess;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("chess")
public interface ChessConfig extends Config {
    @ConfigItem(
            keyName = "twitchPlayers",
            name = "twitchPlayers",
            description = "Define chess player username(s) sepparated by a comma here, this is required to let the plugin know which chat commands to read",
            position = 1
    )
    default String twitchPlayers() {
        return "";
    }

    @ConfigItem(
            keyName = "osrsPlayers",
            name = "osrsPlayers",
            description = "Define chess player username(s) sepparated by a comma here, this is required to let the plugin know which chat commands to read",
            position = 2
    )
    default String osrsPlayers() {
        return "";
    }

    @ConfigItem(
            keyName = "chessPieceUsernames",
            name = "chessPieceUsernames",
            description = "Define chess piece username(s) sepparated by a comma here, this is required to let the plugin know which username is a chess piece",
            position = 3
    )
    default String chessPieceUsernames() {
        return "B Rook 1,B Knight 1,B Bishop 1,B Queen 1, B King 1,B Bishop 2,B Knight 2,B Rook 2,B Pawn 1,B Pawn 2,B Pawn 3,B Pawn 4,B Pawn 5,B Pawn 6,B Pawn 7,B Pawn 8,W Pawn 1,W Pawn 2,W Pawn 3,W Pawn 4,W Pawn 5,W Pawn 6,W Pawn 7,W Pawn 8,W Rook 1,W Knight 1,W Bishop 1,W Queen 1,W King1,W Bishop 2,W Knight1,W Bishop 2,W Knight 2,W Rook 2";
    }

    @ConfigItem(
            keyName = "chessPieceTypes",
            name = "chessPieceTypes",
            description = "Define chess piece username(s) sepparated by a comma here, this is required to let the plugin know which username is a chess piece",
            position = 4
    )
    default String chessPieceTypes1() {
        return "B Rook,B Knight,B Bishop,B Queen, B King,B Bishop,B Knight,B Rook";
    }

    @ConfigItem(
            keyName = "chessPieceTypes2",
            name = "chessPieceTypes2",
            description = "Define chess piece username(s) sepparated by a comma here, this is required to let the plugin know which username is a chess piece",
            position = 5
    )
    default String chessPieceTypes2() {
        return "B Pawn,B Pawn,B Pawn,B Pawn,B Pawn,B Pawn,B Pawn,B Pawn";
    }

    @ConfigItem(
            keyName = "chessPieceTypes3",
            name = "chessPieceTypes3",
            description = "Define chess piece username(s) sepparated by a comma here, this is required to let the plugin know which username is a chess piece",
            position = 6
    )
    default String chessPieceTypes3() {
        return "W Pawn,W Pawn,W Pawn,W Pawn,W Pawn,W Pawn,W Pawn,W Pawn";
    }

    @ConfigItem(
            keyName = "chessPieceTypes4",
            name = "chessPieceTypes4",
            description = "Define chess piece username(s) sepparated by a comma here, this is required to let the plugin know which username is a chess piece",
            position = 7
    )
    default String chessPieceTypes4() {
        return "W Rook,W Knight,W Bishop,W Queen,W King,W Bishop,W Knight,W Bishop,W Knight,W Rook";
    }

    @ConfigItem(
            keyName = "showChessBoard",
            name = "showChessBoard",
            description = "Turns the chess tiles on or off",
            position = 8
    )
    default boolean showChessBoard() {
        return false;
    }

    @ConfigItem(
            keyName = "showBackground",
            name = "Turns background on or off",
            description = "Turns the background on or off",
            position = 9
    )
    default boolean showBackground() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "markerColor",
            name = "Color of the tile",
            description = "Configures the color of marked tile",
            position = 10
    )
    default Color markerColor() {
        return Color.YELLOW;
    }

    @Alpha
    @ConfigItem(
            keyName = "blackTileColor",
            name = "Color of the black tiles",
            description = "Configures the color of the black tiles",
            position = 11
    )
    default Color blackTileColor() {
        return new Color(0, 0, 0, 255);
    }

    @Alpha
    @ConfigItem(
            keyName = "whiteTileColor",
            name = "Color of the white tiles",
            description = "Configures the color of the white tiles",
            position = 12
    )
    default Color whiteTileColor() {
        return new Color(255, 255, 255, 255);
    }

    @Alpha
    @ConfigItem(
            keyName = "backgroundColor",
            name = "Color of the background",
            description = "Configures the color of the background",
            position = 13
    )
    default Color backgroundColor() {
        return Color.GREEN;
    }
}
