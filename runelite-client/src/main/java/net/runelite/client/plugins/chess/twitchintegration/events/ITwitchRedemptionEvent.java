package net.runelite.client.plugins.chess.twitchintegration.events;

import net.runelite.client.plugins.chess.twitchintegration.TwitchRedemptionInfo;

public interface ITwitchRedemptionEvent {

	public boolean execute(int callingCount);
	public TwitchRedemptionInfo getTwitchRedemptionInfo();
	public long getRepeatedDelayTime();
	public long getEndingDelayTime();
}
