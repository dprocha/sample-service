package com.mongodb.sample.aspect;

public class ElapsedTime {

    private static ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    // Get the start time
    public static long getStartTime() {
        return startTimeThreadLocal.get();
    }

    // Set the start time
    public static void setStartTime(long startTime) {
        startTimeThreadLocal.set(startTime);
    }

    // Optionally, you can clean up after use
    public static void clear() {
        startTimeThreadLocal.remove();
    }
}
