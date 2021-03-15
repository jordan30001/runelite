
/*
 * Copyright (c) 2019, Lotto <https://github.com/devLotto>
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
package net.runelite.client.plugins.chess.data;


import com.google.common.collect.ImmutableMap;

import java.awt.image.BufferedImage;
import java.util.Map;

import net.runelite.client.plugins.chess.ChessPlugin;
import net.runelite.client.util.ImageUtil;

public enum ChessEmotes {
    ThreeHead("3Head"),
    FourHead("4Head"),
    FiveHead("5Head"),
    Bladeb7PogChamp("bladeb7PogChamp"),
    FlipThis("FlipThis"),
    HandsUp("HandsUp"),
    Kappa("Kappa"),
    KEKW("KEKW"),
    MiniK("MiniK"),
    PeepoHappy("PeepoHappy"),
    Pog("Pog"),
    PrimeWhatYouSay("PrimeWhatYouSay"),
    SadKek("sadKEK"),
    TableHere("TableHere"),
    WidePeepoHappy("widePeepoHappy")
    ;

    private static final Map<String, ChessEmotes> emojiMap;

    public final String trigger;

    static {
        ImmutableMap.Builder<String, ChessEmotes> builder = new ImmutableMap.Builder<>();

        for (final ChessEmotes emoji : values()) {
            builder.put(emoji.trigger, emoji);
        }

        emojiMap = builder.build();
    }

    ChessEmotes(String trigger) {
        this.trigger = trigger;
    }

    public BufferedImage loadImage() {
    	return null;
        //return ImageUtil.loadImageResource(getClass(), this.name().toLowerCase() + ".png");
    }

    public static ChessEmotes getEmoji(String trigger) {
        return emojiMap.get(trigger);
    }
    public String toHTMLString(ChessPlugin plugin){
    	return "<img=" + (plugin.modIconsStart + this.ordinal()) + ">";
    }

    public String toHTMLString(int startingOrdinal){
        return "<img=" + (startingOrdinal + this.ordinal()) + ">";
    }
}
