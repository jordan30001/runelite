package net.runelite.client.plugins.chess;

import java.util.Timer;
import java.util.concurrent.locks.ReentrantLock;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class OverheadTextInfo {

	private static final ReentrantLock lock = new ReentrantLock();
	private static final Timer timer = new Timer(true);

	@Getter(AccessLevel.PUBLIC)
	private String overheadText;
	@Getter(AccessLevel.PUBLIC)
	private long displayTime;
	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean finished;
	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PRIVATE)
	private volatile boolean started;

	public OverheadTextInfo(String overheadText, long displayTime) {
		this.overheadText = overheadText;
		this.displayTime = displayTime;
	}

	public void startCountdown() {
		if (isStarted())
			return;
		setStarted(true);
		try {
			lock.lock();
			timer.schedule(Utils.WrapTimerTask(() -> setFinished(true)), displayTime);
		} finally {
			lock.unlock();
		}
	}
	
	public void reset() {
		finished = false;
		started = false;
	}
}
