/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.mapping.datetime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.tarantool.mapping.Adjust;
import io.tarantool.mapping.Interval;

/**
 * @author Artyom Dubinin, Belonogov Nikolay
 */
public class IntervalTest {

  private static final int ITERATION = 10_000;
  private static final int THREADS = 100;
  private static final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
  private static final Interval interval = new Interval();
  StringBuilder sb = new StringBuilder();
  List<Future<Interval>> futures = new ArrayList<>(ITERATION);

  public static Stream<Arguments> dataForTestIntervalLimits() {
    return Stream.of(Arguments.of((Executable) () -> new Interval().setYear(Interval.MAX_YEAR_RANGE + 1),
            "Value 11759222 of year is out of allowed range [-11759221, 11759221]"),
        Arguments.of((Executable) () -> new Interval().setMonth(Interval.MAX_MONTH_RANGE + 1),
            "Value 141110653 of month is out of allowed range [-141110652, 141110652]"),
        Arguments.of((Executable) () -> new Interval().setWeek(Interval.MAX_WEEK_RANGE + 1),
            "Value 611479493 of week is out of allowed range [-611479492, 611479492]"),
        Arguments.of((Executable) () -> new Interval().setDay(Interval.MAX_DAY_RANGE + 1),
            "Value 4292115666 of day is out of allowed range [-4292115665, 4292115665]"),
        Arguments.of((Executable) () -> new Interval().setHour(Interval.MAX_HOUR_RANGE + 1),
            "Value 103010775961 of hour is out of allowed range [-103010775960, " +
                "103010775960]"),
        Arguments.of((Executable) () -> new Interval().setMin(Interval.MAX_MIN_RANGE + 1),
            "Value 6180646557601 of min is out of allowed range [-6180646557600, " +
                "6180646557600]"),
        Arguments.of((Executable) () -> new Interval().setSec(Interval.MAX_SEC_RANGE + 1),
            "Value 370838793456001 of sec is out of allowed range [-370838793456000, " +
                "370838793456000]"),
        Arguments.of((Executable) () -> new Interval().setNsec(Interval.MAX_NSEC_RANGE + 1),
            "Value 2147483648 of nsec is out of allowed range [-2147483647, 2147483647]"));
  }

  @ParameterizedTest
  @MethodSource("dataForTestIntervalLimits")
  void testIntervalLimits(Executable supplier, String exceptionMessage) throws Exception {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, supplier);
    assertEquals(exceptionMessage, ex.getMessage());
  }

  @Test
  void testIntervalSetYearWithThreads() {
    for (long i = Interval.MAX_YEAR_RANGE + 1; i <= Interval.MAX_YEAR_RANGE + 1_000; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setYear(finalI)));
    }
    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_YEAR_RANGE + 1 + i)
          .append(" of year is out of allowed range [-11759221, 11759221]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetMonthWithThreads() {
    for (long i = Interval.MAX_MONTH_RANGE + 1; i <= Interval.MAX_MONTH_RANGE + 1_000; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setMonth(finalI)));
    }

    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_MONTH_RANGE + 1 + i)
          .append(" of month is out of allowed range [-141110652, 141110652]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetWeekWithThreads() {
    List<Future<Interval>> futures = new ArrayList<>(ITERATION);
    for (long i = Interval.MAX_WEEK_RANGE + 1; i <= Interval.MAX_WEEK_RANGE + ITERATION; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setWeek(finalI)));
    }

    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_WEEK_RANGE + 1 + i)
          .append(" of week is out of allowed range [-611479492, 611479492]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetDayWithThreads() {
    for (long i = Interval.MAX_DAY_RANGE + 1; i <= Interval.MAX_DAY_RANGE + ITERATION; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setDay(finalI)));
    }

    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_DAY_RANGE + 1 + i)
          .append(" of day is out of allowed range [-4292115665, 4292115665]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetHourWithThreads() {
    for (long i = Interval.MAX_HOUR_RANGE + 1; i <= Interval.MAX_HOUR_RANGE + ITERATION; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setHour(finalI)));
    }

    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_HOUR_RANGE + 1 + i)
          .append(" of hour is out of allowed range [-103010775960, 103010775960]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetMinuteWithThreads() {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    Interval interval = new Interval();
    List<Future<Interval>> futures = new ArrayList<>(ITERATION);
    for (long i = Interval.MAX_MIN_RANGE + 1; i <= Interval.MAX_MIN_RANGE + ITERATION; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setMin(finalI)));
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_MIN_RANGE + 1 + i)
          .append(" of min is out of allowed range [-6180646557600, 6180646557600]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetSecondWithThreads() {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    Interval interval = new Interval();
    List<Future<Interval>> futures = new ArrayList<>(ITERATION);
    for (long i = Interval.MAX_SEC_RANGE + 1; i <= Interval.MAX_SEC_RANGE + ITERATION; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setSec(finalI)));
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_SEC_RANGE + 1 + i)
          .append(" of sec is out of allowed range [-370838793456000, 370838793456000]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testIntervalSetNanoSecondWithThreads() {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    Interval interval = new Interval();
    List<Future<Interval>> futures = new ArrayList<>(ITERATION);
    for (long i = Interval.MAX_NSEC_RANGE + 1; i <= Interval.MAX_NSEC_RANGE + ITERATION; i++) {
      long finalI = i;
      futures.add(pool.submit(() -> interval.setNsec(finalI)));
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < futures.size(); i++) {
      int finalI = i;
      Throwable thr = assertThrows(ExecutionException.class, () -> futures.get(finalI).get());
      assertEquals(IllegalArgumentException.class, thr.getCause().getClass());
      assertEquals(sb.delete(0, sb.length())
          .append("Value ")
          .append(Interval.MAX_NSEC_RANGE + 1 + i)
          .append(" of nsec is out of allowed range [-2147483647, 2147483647]")
          .toString(), thr.getCause().getMessage());
    }
  }

  @Test
  void testToStringWithThreads() throws ExecutionException, InterruptedException {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    List<Interval> intervals = new ArrayList<>(ITERATION);
    List<Future<String>> futures = new ArrayList<>(ITERATION);
    List<String> toStrings = new ArrayList<>(ITERATION);
    StringBuilder sb = new StringBuilder("Interval{");

    for (long i = 1; i <= ITERATION; i++) {
      sb.delete(0, sb.length());
      toStrings.add(sb.append("Interval{")
          .append("year=")
          .append(i)
          .append(", month=")
          .append(i + 1)
          .append(", week=")
          .append(i + 2)
          .append(", day=")
          .append(i + 3)
          .append(", hour=")
          .append(i + 4)
          .append(", min=")
          .append(i + 5)
          .append(", sec=")
          .append(i + 6)
          .append(", nsec=")
          .append(i + 7)
          .append(", adjust=")
          .append(Adjust.LastAdjust)
          .append('}')
          .toString());
      Interval interval = new Interval().setYear(i)
          .setMonth(i + 1)
          .setWeek(i + 2)
          .setDay(i + 3)
          .setHour(i + 4)
          .setMin(i + 5)
          .setSec(i + 6)
          .setNsec(i + 7)
          .setAdjust(Adjust.LastAdjust);
      intervals.add(interval);
    }
    for (int i = 0; i < ITERATION; i++) {
      int finalI = i;
      futures.add(pool.submit(() -> intervals.get(finalI).toString()));
    }

    for (int i = 0; i < ITERATION; i++) {
      assertEquals(toStrings.get(i), futures.get(i).get());
    }
  }

  @AfterEach
  public void cleanUp() {
    futures.clear();
  }
}
