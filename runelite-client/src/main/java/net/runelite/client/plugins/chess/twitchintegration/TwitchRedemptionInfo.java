package net.runelite.client.plugins.chess.twitchintegration;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.chess.Utils;

public class TwitchRedemptionInfo {

	private static final ReentrantLock lock = new ReentrantLock();
	private static final Timer timer = new Timer(true);

	@Getter(AccessLevel.PUBLIC)
	private int currentCallCount;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean started = true;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean currentExecutionFinished = true;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean finished;

	private TimerTask currentTask;
	
	@Getter(AccessLevel.PUBLIC)
	private final Map<String, Object> vars = new HashMap<>();

	public <F extends CustomFunction> TwitchRedemptionInfo() {
	}

	public void startCountdown(long displayTime) {
		try {
			lock.lock();
			if (isStarted()) {
				if (isCurrentExecutionFinished() == false) {
					setStarted(true);
				} else if (isFinished()) {
					return;
				}
			} else {
				setStarted(true);
			}
			timer.schedule(currentTask = Utils.WrapTimerTask(() -> {
				try {
					lock.lock();
					if (isCurrentExecutionFinished()) {
						setFinished(true);
					} else {
						setCurrentExecutionFinished(true);
					}
					currentCallCount++;
				} finally {
					lock.unlock();
				}
			}), displayTime);
		} finally {
			lock.unlock();
		}
	}

	public void resetForNextExecution() {
		try {
			lock.lock();
			if (isStarted() && isCurrentExecutionFinished()) {
				setStarted(false);
				setCurrentExecutionFinished(false);
			}
		} finally {
			lock.unlock();
		}
	}

	public void fullReset() {
		if (currentTask != null) {
			currentTask.cancel();
			currentTask = null;
			setStarted(false);
			setCurrentExecutionFinished(false);
		}
	}
}
