/* Copyright 2016 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.collector.cron;

import org.torproject.collector.conf.Configuration;
import org.torproject.collector.conf.ConfigurationException;
import org.torproject.collector.conf.Key;
import org.torproject.collector.cron.CollecTorMain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler that starts the modules configured in collector.properties.
 */
public final class Scheduler implements ThreadFactory {

  public static final String ACTIVATED = "Activated";
  public static final String PERIODMIN = "PeriodMinutes";
  public static final String OFFSETMIN = "OffsetMinutes";

  private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

  private final ThreadFactory threads = Executors.defaultThreadFactory();

  private int currentThreadNo = 0;

  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(10, this);

  private static Scheduler instance = new Scheduler();

  private Scheduler(){}

  public static Scheduler getInstance() {
    return instance;
  }

  /**
   * Schedule all classes given according to the parameters in the
   * the configuration.
   */
  public void scheduleModuleRuns(Map<Key,
      Class<? extends CollecTorMain>> collecTorMains, Configuration conf) {
    for (Map.Entry<Key, Class<? extends CollecTorMain>> ctmEntry
        : collecTorMains.entrySet()) {
      try {
        if (conf.getBool(ctmEntry.getKey())) {
          String prefix = ctmEntry.getKey().name().replace(ACTIVATED, "");
          CollecTorMain ctm = ctmEntry.getValue()
              .getConstructor(Configuration.class).newInstance(conf);
          scheduleExecutions(conf.getBool(Key.RunOnce), ctm,
              conf.getInt(Key.valueOf(prefix + OFFSETMIN)),
              conf.getInt(Key.valueOf(prefix + PERIODMIN)));
        }
      } catch (ConfigurationException | IllegalAccessException
          | InstantiationException | InvocationTargetException
          | NoSuchMethodException | RejectedExecutionException
          | NullPointerException ex) {
        log.error("Cannot schedule " + ctmEntry.getValue().getName()
            + ". Reason: " + ex.getMessage(), ex);
      }
    }
  }

  private static final long MILLIS_IN_A_MINUTE = 60_000L;

  private void scheduleExecutions(boolean runOnce, CollecTorMain ctm,
      int offset, int period) {
    if (runOnce) {
      this.log.info("Single run for " + ctm.getClass().getName() + ".");
      this.scheduler.execute(ctm);
    } else {
      this.log.info("Periodic updater started for " + ctm.getClass().getName()
          + "; offset=" + offset + ", period=" + period + ".");
      long periodMillis = period * MILLIS_IN_A_MINUTE;
      long initialDelayMillis = computeInitialDelayMillis(
          System.currentTimeMillis(), offset * MILLIS_IN_A_MINUTE, periodMillis);

      /* Run after initialDelay delay and then every period min. */
      log.info("Periodic updater will first run in {} and then every {} "
          + "minutes.", initialDelayMillis < MILLIS_IN_A_MINUTE
          ? "under 1 minute"
          : (initialDelayMillis / MILLIS_IN_A_MINUTE) + " minute(s)", period);
      this.scheduler.scheduleAtFixedRate(ctm, initialDelayMillis, periodMillis,
          TimeUnit.MILLISECONDS);
    }
  }

  protected static long computeInitialDelayMillis(long currentMillis,
      long offsetMillis, long periodMillis) {
    return (periodMillis - (currentMillis % periodMillis) + offsetMillis)
        % periodMillis;
  }

  /**
   * Try to shutdown smoothly, i.e., wait for running tasks to terminate.
   */
  public void shutdownScheduler() {
    try {
      scheduler.shutdown();
      scheduler.awaitTermination(20L, java.util.concurrent.TimeUnit.MINUTES);
      log.info("Shutdown of all scheduled tasks completed successfully.");
    } catch (InterruptedException ie) {
      List<Runnable> notTerminated = scheduler.shutdownNow();
      log.error("Regular shutdown failed for: " + notTerminated);
      if (!notTerminated.isEmpty()) {
        log.error("Forced shutdown failed for: " + notTerminated);
      }
    }
  }

  /**
   * Provide a nice name for debugging and log thread creation.
   */
  @Override
  public Thread newThread(Runnable runner) {
    Thread newThread = threads.newThread(runner);
    newThread.setName("CollecTor-Scheduled-Thread-" + ++currentThreadNo);
    log.info("New Thread created: " + newThread.getName());
    return newThread;
  }
}
