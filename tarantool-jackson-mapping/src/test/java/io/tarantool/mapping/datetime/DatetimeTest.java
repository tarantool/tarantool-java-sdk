/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.tarantool.mapping.Adjust;
import io.tarantool.mapping.Interval;

/**
 * @author Artyom Dubinin
 */
public class DatetimeTest {

  @Test
  public void testDatetimeAdd() {
    LocalDateTime dt = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    LocalDateTime updatedDt =
        dt.plus(
            new Interval()
                .setYear(1)
                .setMonth(-3)
                .setWeek(3)
                .setDay(4)
                .setHour(-5)
                .setMin(5)
                .setSec(6)
                .setNsec(-3));
    assertEquals(LocalDateTime.parse("1970-10-25T19:05:05.999999997"), updatedDt);
  }

  public static Stream<Arguments> dataForTestDatetimeAddAdjust() {
    return Stream.of(
        Arguments.of(
            LocalDate.of(2020, 1, 31), new Interval().setMonth(1), LocalDate.of(2020, 2, 29)),
        Arguments.of(
            LocalDate.of(2020, 1, 31),
            new Interval().setMonth(1).setAdjust(Adjust.NoneAdjust),
            LocalDate.of(2020, 2, 29)),
        Arguments.of(
            LocalDate.of(2020, 1, 31),
            new Interval().setMonth(1).setAdjust(Adjust.ExcessAdjust),
            LocalDate.of(2020, 3, 2)),
        Arguments.of(
            LocalDate.of(2020, 1, 31),
            new Interval().setMonth(1).setAdjust(Adjust.LastAdjust),
            LocalDate.of(2020, 2, 29)),
        Arguments.of(
            LocalDateTime.parse("2013-02-28T00:00:00"),
            new Interval().setMonth(1).setAdjust(Adjust.NoneAdjust),
            LocalDateTime.parse("2013-03-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-02-28T00:00:00"),
            new Interval().setYear(0).setMonth(1).setAdjust(Adjust.LastAdjust),
            LocalDateTime.parse("2013-03-31T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-02-28T00:00:00"),
            new Interval().setYear(0).setMonth(1).setAdjust(Adjust.ExcessAdjust),
            LocalDateTime.parse("2013-03-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-01-31T00:00:00"),
            new Interval().setYear(0).setMonth(1).setAdjust(Adjust.NoneAdjust),
            LocalDateTime.parse("2013-02-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-01-31T00:00:00"),
            new Interval().setYear(0).setMonth(1).setAdjust(Adjust.LastAdjust),
            LocalDateTime.parse("2013-02-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-01-31T00:00:00"),
            new Interval().setYear(0).setMonth(1).setAdjust(Adjust.ExcessAdjust),
            LocalDateTime.parse("2013-03-03T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2011-12-31T00:00:00"),
            new Interval().setYear(2).setMonth(2).setAdjust(Adjust.NoneAdjust),
            LocalDateTime.parse("2014-02-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2011-12-31T00:00:00"),
            new Interval().setYear(2).setMonth(2).setAdjust(Adjust.LastAdjust),
            LocalDateTime.parse("2014-02-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2011-12-31T00:00:00"),
            new Interval().setYear(2).setMonth(2).setAdjust(Adjust.ExcessAdjust),
            LocalDateTime.parse("2014-03-03T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-02-28T00:00:00"),
            new Interval().setYear(0).setMonth(-1).setAdjust(Adjust.NoneAdjust),
            LocalDateTime.parse("2013-01-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-02-28T00:00:00"),
            new Interval().setYear(0).setMonth(-1).setAdjust(Adjust.LastAdjust),
            LocalDateTime.parse("2013-01-31T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2013-02-28T00:00:00"),
            new Interval().setYear(0).setMonth(-1).setAdjust(Adjust.ExcessAdjust),
            LocalDateTime.parse("2013-01-28T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2012-12-31T00:00:00"),
            new Interval().setYear(0).setMonth(-1).setAdjust(Adjust.NoneAdjust),
            LocalDateTime.parse("2012-11-30T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2012-12-31T00:00:00"),
            new Interval().setYear(0).setMonth(-1).setAdjust(Adjust.LastAdjust),
            LocalDateTime.parse("2012-11-30T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2012-12-31T00:00:00"),
            new Interval().setYear(0).setMonth(-1).setAdjust(Adjust.ExcessAdjust),
            LocalDateTime.parse("2012-12-01T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2011-01-31T00:00:00"),
            new Interval().setYear(-2).setMonth(-2).setAdjust(Adjust.NoneAdjust),
            LocalDateTime.parse("2008-11-30T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2011-12-31T00:00:00"),
            new Interval().setYear(-2).setMonth(-2).setAdjust(Adjust.LastAdjust),
            LocalDateTime.parse("2009-10-31T00:00:00")),
        Arguments.of(
            LocalDateTime.parse("2011-12-31T00:00:00"),
            new Interval().setYear(-2).setMonth(-2).setAdjust(Adjust.ExcessAdjust),
            LocalDateTime.parse("2009-10-31T00:00:00")));
  }

  @ParameterizedTest
  @MethodSource("dataForTestDatetimeAddAdjust")
  public void testDatetimeAddAdjust(Temporal input, Interval interval, Temporal expected) {
    assertEquals(input.plus(interval), expected);
  }

  @Test
  public void testDatetimeAddSubSymmetric() {
    LocalDateTime dt = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    LocalDateTime updatedDtPlus =
        dt.plus(
            new Interval()
                .setYear(1)
                .setMonth(-3)
                .setWeek(3)
                .setDay(4)
                .setHour(-5)
                .setMin(5)
                .setSec(6)
                .setNsec(-3));
    LocalDateTime updatedDtMinus =
        dt.minus(
            new Interval()
                .setYear(-1)
                .setMonth(3)
                .setWeek(-3)
                .setDay(-4)
                .setHour(5)
                .setMin(-5)
                .setSec(-6)
                .setNsec(3));
    assertEquals(LocalDateTime.parse("1970-10-25T19:05:05.999999997"), updatedDtPlus);
    assertEquals(LocalDateTime.parse("1970-10-25T19:05:05.999999997"), updatedDtMinus);
  }
}
