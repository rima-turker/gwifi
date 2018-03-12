package org.fiz.ise.gwifi.util;

import java.util.concurrent.TimeUnit;

public class TimeUtil {
	public static long getStart() {
		return System.currentTimeMillis();
	}
	
	public static long getEnd(TimeUnit u,long now) {
		switch (u) {
		case SECONDS:
			return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-now);
		case NANOSECONDS:
			return TimeUnit.NANOSECONDS.toNanos(System.currentTimeMillis()-now);
		default:
			return 0;
		}
	}
}
