
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
package net.runelite.client.plugins.chess;


import com.google.common.collect.ImmutableMap;

import java.awt.image.BufferedImage;
import java.util.Map;

import net.runelite.client.util.ImageUtil;

public enum ChessEmotes {

    KAPPA("kappa"),
    MINIK("minik"),
    MINIMINIK("miniminik"),
    SADKEK("sadkek"),
    BLADE_POG("bladepog"), 
    FFZ_HANDS_UP("handsup"), 
    FFZ_WIDE_PEEPO_HAPPY("widepeepohappy"), 
    PRIME_YOU_DONT_SAY("youdontsay"), 
    FFZ_PEEPO_HAPPY("peepohappy")
    ;

    private static final Map<String, ChessEmotes> emojiMap;

    private final String trigger;

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

    BufferedImage loadImage() {
        return ImageUtil.loadImageResource(getClass(), this.name().toLowerCase() + ".png");
    }

    static ChessEmotes getEmoji(String trigger) {
        return emojiMap.get(trigger);
    }
    String toHTMLString(int startingOrdinal){
    	return "<img=" + (startingOrdinal + this.ordinal()) + ">";
    }
}

