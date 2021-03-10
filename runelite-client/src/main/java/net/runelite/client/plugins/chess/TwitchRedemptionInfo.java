package net.runelite.client.plugins.chess;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class TwitchRedemptionInfo {

	private static final ReentrantLock lock = new ReentrantLock();
	private static final Timer timer = new Timer(true);

	@Getter(AccessLevel.PUBLIC)
	private Function<Integer, Boolean> callback;

	@Getter(AccessLevel.PUBLIC)
	private int currentCallCount;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean started;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean currentExecutionFinished;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean finished;

	private TimerTask currentTask;

	public TwitchRedemptionInfo(Function<Integer, Boolean> callback) {
		this.callback = callback;
		this.currentCallCount = 0;
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
