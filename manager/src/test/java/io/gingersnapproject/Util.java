package io.gingersnapproject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class Util {
    public static void eventually(Supplier<Boolean> condition) {
        eventually(condition, 10, TimeUnit.SECONDS);
    }

    public static void eventually(Supplier<Boolean> condition, long timeout, TimeUnit unit) {
        eventually(() -> "Condition is still false after " + timeout + " " + unit, condition, timeout, unit);
    }

    public static void eventually(Supplier<String> messageSupplier, Supplier<Boolean> condition, long timeout,
                                  TimeUnit timeUnit) {
        try {
            long timeoutNanos = timeUnit.toNanos(timeout);
            // We want the sleep time to increase in arithmetic progression
            // 30 loops with the default timeout of 30 seconds means the initial wait is ~ 65 millis
            int loops = 30;
            int progressionSum = loops * (loops + 1) / 2;
            long initialSleepNanos = timeoutNanos / progressionSum;
            long sleepNanos = initialSleepNanos;
            long expectedEndTime = System.nanoTime() + timeoutNanos;
            while (expectedEndTime - System.nanoTime() > 0) {
                if (condition.get())
                    return;
                LockSupport.parkNanos(sleepNanos);
                sleepNanos += initialSleepNanos;
            }
            if (!condition.get()) {
                fail(messageSupplier.get());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected!", e);
        }
    }
}
