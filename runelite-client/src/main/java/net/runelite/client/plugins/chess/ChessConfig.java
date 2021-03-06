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
public interface ChessConfig extends Config
{
	@ConfigItem(
			keyName = "chessPlayerUsernames",
			name = "Chess Players",
			description = "Define chess player username(s) sepparated by a comma here, this is required to let the plugin know which chat commands to read",
			position = 1
	)
	default String chessPlayerUsernames() { return ""; }

	@ConfigItem(
			keyName = "chessPieceUsernames",
			name = "Chess Pieces",
			description = "Define chess piece username(s) sepparated by a comma here, this is required to let the plugin know which username is a chess piece",
			position = 1
	)
	default String chessPieceUsernames() { return ""; }

	@ConfigItem(
			keyName = "showChessBoard",
			name = "Turns chess tiles on or off",
			description = "Turns the chess tiles on or off",
			position = 2
	)
	default boolean rememberTileColors()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showBackground",
			name = "Turns background on or off",
			description = "Turns the background on or off",
			position = 3
	)
	default boolean showBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
			keyName = "markerColor",
			name = "Color of the tile",
			description = "Configures the color of marked tile",
			position = 4
	)
	default Color markerColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
			keyName = "blackTileColor",
			name = "Color of the black tiles",
			description = "Configures the color of the black tiles",
			position = 5
	)
	default Color blackTileColor()
	{
		return new Color (0,0,0,255);
	}

	@Alpha
	@ConfigItem(
			keyName = "whiteTileColor",
			name = "Color of the white tiles",
			description = "Configures the color of the white tiles",
			position = 6
	)
	default Color whiteTileColor()
	{
		return new Color (255,255,255,255);
	}

	@Alpha
	@ConfigItem(
			keyName = "backgroundColor",
			name = "Color of the background",
			description = "Configures the color of the background",
			position = 7
	)
	default Color backgroundColor()
	{
		return Color.GREEN;
	}
}
