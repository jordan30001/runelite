package net.runelite.client.plugins.chess.twitchintegration.events;

import net.runelite.client.plugins.chess.twitchintegration.TwitchRedemptionInfo;

public interface ITwitchRedemptionEvent {

	public boolean execute(long deltaTime);
	public TwitchRedemptionInfo getTwitchRedemptionInfo();
	public long getRepeatedDelayTime();
	public long getEndingDelayTime();
}
