package com.solab.alarms;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is used in unit tests to receive a notification when an alarm is actually sent.
 * 
 * @author Enrique Zamudio
 */
public class UnitTestChannel implements AlarmChannel {

	private static volatile int instanceCount;
	private Logger log = LoggerFactory.getLogger(String.format("Test-%d", ++instanceCount));
	long stamp;
	long lastSent;
	int resend = 5000;
	private ChanDelegate delegate;
	private AtomicBoolean sent = new AtomicBoolean();

	/** Clear the sent flag. */
	public void clearSent() {
		sent.set(false);
	}
	/** Returns true if the alarm has been (supposedly) sent. */
	public boolean isSent() {
		return sent.get();
	}
	/** Sets the delegate that will be notified when the alarm is supposed to
	 * be sent. */
	public void setDelegate(ChanDelegate value) {
		delegate = value;
	}

	@Override
	public void send(String msg, String source) {
		long now = System.currentTimeMillis();
		if (source == null) {
			log.info("Supposed to send alarm '{}', time diff is {}", msg, now-stamp);
		} else {
			log.info("Supposed to send alarm {}:'{}', time diff is {}", new Object[]{ source, msg, now-stamp });
		}
		delegate.alarmReceived(msg, now);
		lastSent = now;
		sent.set(true);
	}

	/** Clears the sent flag and records the current timestamp. */
	public void prepare() {
		stamp = System.currentTimeMillis();
		sent.set(false);
	}

	/** Waits inside a loop, blocking the calling thread until the channel
	 * is supposed to send the alarm and has notified the delegate. */
	public void waitForSend() {
		try {
			int count = 0;
			while (!sent.get()) {
				count++;
				Thread.sleep(1000);
				if (!sent.get()) {
					if (count % 10 == 0) {
						log.info("Still waiting... {}", String.format("%TT", new Date()));
					}
					if (count > 300) {
						log.info("5 minutes? I think this test has failed");
						assert false;
					}
				}
			}
		} catch (InterruptedException ex) {
			//nothing
		}
	}

	@Override
	public int getMinResendInterval() {
		return resend;
	}

	@Override
	public void shutdown() {
	}

	/** Implement this interface if you want to get notified when the
	 * channel received the send message. */
	public static interface ChanDelegate {
		/** Notifies the delegate that the alarm message has been sent, at
		 * the specified time. */
		public void alarmReceived(String msg, long when);
	}

}
