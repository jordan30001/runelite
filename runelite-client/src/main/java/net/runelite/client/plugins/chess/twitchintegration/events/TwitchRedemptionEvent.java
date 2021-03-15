package net.runelite.client.plugins.chess.twitchintegration.events;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.plugins.chess.twitchintegration.TwitchRedemptionInfo;

@AllArgsConstructor
public abstract class TwitchRedemptionEvent implements ITwitchRedemptionEvent {
	@Getter(AccessLevel.PUBLIC)
	private final long repeatedDelayTime;
	@Getter(AccessLevel.PUBLIC)
	private final long endingDelayTime;
	@Getter(AccessLevel.PUBLIC)
	private final TwitchRedemptionInfo twitchRedemptionInfo;
}
