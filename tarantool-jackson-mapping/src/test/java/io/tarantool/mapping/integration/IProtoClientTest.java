/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.integration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.tarantool.TarantoolContainer;
import org.testcontainers.containers.utils.TarantoolContainerClientHelper;

import io.tarantool.core.IProtoClient;
import io.tarantool.core.IProtoClientImpl;
import io.tarantool.core.exceptions.BoxErrorStackItem;
import io.tarantool.core.exceptions.BoxErrorValue;
import io.tarantool.core.protocol.BoxIterator;
import io.tarantool.core.protocol.IProtoResponse;
import io.tarantool.core.protocol.requests.IProtoConstant;
import io.tarantool.mapping.Adjust;
import io.tarantool.mapping.Interval;
import io.tarantool.mapping.TarantoolJacksonMapping;
import io.tarantool.mapping.TarantoolResponse;
import io.tarantool.mapping.Tuple;
import io.tarantool.mapping.entities.TestEntity;

public class IProtoClientTest extends BaseTest {

  static final Logger log = LoggerFactory.getLogger(IProtoClientImpl.class);

  private static TarantoolContainer<?> tt;

  public static final String ECHO_EXPRESSION = "return ...";
  public static final String YEAR = "2022";
  public static final String MONTH = "07";
  public static final String DAY = "01";
  public static final String HOUR = "08";
  public static final String MINUTES = "30";
  public static final String SECONDS = "05";
  public static final String NANOSECONDS = "000000123";
  public static final String EUROPE_MOSCOW = "Europe/Moscow";
  public static final String MSK_OFFSET = "+03:00";
  public static final String T_2022_08_30_05 =
      String.format("%s-%s-%sT%s:%s:%s", YEAR, MONTH, DAY, HOUR, MINUTES, SECONDS);
  public static final String T_2022_08_30_05_000000123 =
      String.format("%s.%s", T_2022_08_30_05, NANOSECONDS);
  public static final LocalDateTime LOCAL_DATE_TIME =
      LocalDateTime.parse(T_2022_08_30_05_000000123);
  public static final long EPOCH_SECOND = LOCAL_DATE_TIME.toEpochSecond(ZoneOffset.UTC);
  private static final String LONG_MESSAGE = String.join("", Collections.nCopies(256, "x"));
  private static int spaceTestId;
  private static InetSocketAddress address;

  @BeforeAll
  public static void setUp() throws Exception {
    tt =
        TarantoolContainerClientHelper.createTarantoolContainer()
            .withEnv(ENV_MAP)
            .withLogConsumer(new Slf4jLogConsumer(log));
    tt.start();
    TarantoolContainerClientHelper.execInitScript(tt);

    List<Integer> result =
        TarantoolContainerClientHelper.executeCommandDecoded(tt, "return box.space.test.id");
    spaceTestId = result.get(0);
    address = new InetSocketAddress(tt.getHost(), tt.getFirstMappedPort());
  }

  @BeforeEach
  public void truncateSpaces() throws Exception {
    TarantoolContainerClientHelper.executeCommand(tt, "return box.space.test:truncate()");
  }

  @AfterAll
  static void tearDown() {
    tt.stop();
  }

  public static Stream<Arguments> dataForTestInsertAndSelect() {
    return Stream.of(
        Arguments.of(
            ValueFactory.newArray(ValueFactory.newString("testkey")),
            new ArrayList<>(Collections.emptyList()),
            spaceTestId,
            0,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            ValueFactory.newArray(ValueFactory.newString("key_a")),
            new ArrayList<>(
                Collections.singletonList(new TestEntity("key_a".toCharArray(), "value_a"))),
            spaceTestId,
            0,
            1,
            0,
            BoxIterator.EQ),
        Arguments.of(
            ValueFactory.newArray(ValueFactory.newString("key_a")),
            new ArrayList<>(
                Arrays.asList(
                    new TestEntity("key_a".toCharArray(), "value_a"),
                    new TestEntity("key_b".toCharArray(), "value_b"))),
            spaceTestId,
            0,
            2,
            0,
            BoxIterator.GE));
  }

  @ParameterizedTest
  @MethodSource("dataForTestInsertAndSelect")
  public void testInsertAndSelect(
      ArrayValue key,
      List<TestEntity> input,
      Integer space,
      Integer index,
      Integer limit,
      Integer offset,
      BoxIterator iterator)
      throws Exception {
    IProtoClient client = getClientAndConnect();
    List<Tuple<TestEntity>> tuples;
    for (TestEntity entity : input) {
      byte[] tuple = TarantoolJacksonMapping.toValue(entity);
      IProtoResponse insertResponse = client.insert(spaceTestId, tuple).get();

      tuples = TarantoolJacksonMapping.readSpaceData(insertResponse, TestEntity.class).get();
      assertEquals(Collections.singletonList(entity), getTupleData(tuples));
    }

    IProtoResponse selectResponse = client.select(space, index, key, limit, offset, iterator).get();
    tuples = TarantoolJacksonMapping.readSpaceData(selectResponse, TestEntity.class).get();
    assertEquals(input, getTupleData(tuples));
  }

  public static Stream<Arguments> dataForTestAllBasicTypes() {
    return Stream.of(
        /* string like types */
        Arguments.of("s", String.class),
        Arguments.of("string", String.class),
        Arguments.of('s', Character.class),
        /* long */
        Arguments.of(0L, Long.class),
        Arguments.of(Long.MAX_VALUE, Long.class),
        Arguments.of(Long.MIN_VALUE, Long.class),
        /* int */
        Arguments.of(0, Integer.class),
        Arguments.of(Integer.MAX_VALUE, Integer.class),
        Arguments.of(Integer.MIN_VALUE, Integer.class),
        /* short */
        Arguments.of((short) 0, Short.class),
        Arguments.of(Short.MAX_VALUE, Short.class),
        Arguments.of(Short.MIN_VALUE, Short.class),
        /* double */
        Arguments.of(0.0, Double.class),
        Arguments.of(Double.MAX_VALUE, Double.class),
        Arguments.of(Double.MIN_VALUE, Double.class),
        /* float */
        Arguments.of(0.0f, Float.class),
        Arguments.of(Float.MAX_VALUE, Float.class),
        Arguments.of(Float.MIN_VALUE, Float.class),
        /* boolean */
        Arguments.of(true, Boolean.class),
        Arguments.of(false, Boolean.class),
        /* nil */
        Arguments.of(null, Integer.class),
        /* containers */
        Arguments.of(new ArrayList<>(), List.class),
        Arguments.of(
            new ArrayList<>(
                Arrays.asList(
                    1,
                    "a",
                    null,
                    true,
                    new HashMap<Object, Object>() {
                      {
                        put("nestedArray", new ArrayList<>(Arrays.asList(1, "a", null, true)));
                      }
                    })),
            List.class),
        Arguments.of(new HashMap<>(), Map.class),
        Arguments.of(
            new HashMap<Object, Object>() {
              {
                put("a", 1);
                put("b", "3");
                put("99", true);
                put(
                    "nestedArray",
                    new ArrayList<>(
                        Arrays.asList(
                            1,
                            "a",
                            null,
                            true,
                            new HashMap<Object, Object>() {
                              {
                                put("hello", "world");
                              }
                            })));
              }
            },
            Map.class),
        /* decimal */
        Arguments.of(BigDecimal.ZERO, BigDecimal.class),
        Arguments.of(BigDecimal.ONE, BigDecimal.class),
        // 2^31 - 1
        Arguments.of(new BigDecimal(Integer.MAX_VALUE), BigDecimal.class),
        // 2^31
        Arguments.of(new BigDecimal("2147483648"), BigDecimal.class),
        // 2^32 - 1
        Arguments.of(new BigDecimal("4294967295"), BigDecimal.class),
        // 2^32
        Arguments.of(new BigDecimal("4294967296"), BigDecimal.class),
        // 2^63 - 1
        Arguments.of(new BigDecimal(Long.MAX_VALUE), BigDecimal.class),
        // 2^63
        Arguments.of(new BigDecimal("9223372036854775808"), BigDecimal.class),
        // 2^64 - 1
        Arguments.of(new BigDecimal("18446744073709551615"), BigDecimal.class),
        // 2^64
        Arguments.of(new BigDecimal("18446744073709551616"), BigDecimal.class),
        // Tarantool Decimal MAX
        Arguments.of(new BigDecimal("-" + StringUtils.repeat("9", 38)), BigDecimal.class),
        // Tarantool Decimal MIN
        Arguments.of(new BigDecimal(StringUtils.repeat("9", 38)), BigDecimal.class),
        Arguments.of(randomBigDecimal(StringUtils.repeat("0", 5)), BigDecimal.class),
        Arguments.of(randomBigDecimal(StringUtils.repeat("0", 10)), BigDecimal.class),
        Arguments.of(randomBigDecimal(StringUtils.repeat("0", 15)), BigDecimal.class),
        Arguments.of(randomBigDecimal(StringUtils.repeat("0", 20)), BigDecimal.class),
        Arguments.of(randomBigDecimal(StringUtils.repeat("9", 36)), BigDecimal.class),
        Arguments.of(randomBigDecimal("-" + StringUtils.repeat("9", 36)), BigDecimal.class),
        /* uuid */
        Arguments.of(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.class),
        Arguments.of(UUID.randomUUID(), UUID.class),
        /* datetime */
        Arguments.of(Instant.ofEpochSecond(EPOCH_SECOND), Instant.class),
        Arguments.of(
            Instant.ofEpochSecond(EPOCH_SECOND, Integer.parseInt(NANOSECONDS)), Instant.class),
        Arguments.of(LOCAL_DATE_TIME.atZone(ZoneId.of(EUROPE_MOSCOW)).toInstant(), Instant.class),
        Arguments.of(
            LocalDateTime.parse(T_2022_08_30_05).atZone(ZoneOffset.UTC), ZonedDateTime.class),
        Arguments.of(LOCAL_DATE_TIME.atZone(ZoneOffset.UTC), ZonedDateTime.class),
        Arguments.of(LOCAL_DATE_TIME.atZone(ZoneId.of(EUROPE_MOSCOW)), ZonedDateTime.class),
        Arguments.of(LOCAL_DATE_TIME.atZone(ZoneOffset.of(MSK_OFFSET)), ZonedDateTime.class),
        Arguments.of(LOCAL_DATE_TIME.atOffset(ZoneOffset.of(MSK_OFFSET)), OffsetDateTime.class),
        Arguments.of(LOCAL_DATE_TIME, LocalDateTime.class),
        Arguments.of(
            LocalDate.of(Integer.parseInt(YEAR), Integer.parseInt(MONTH), Integer.parseInt(DAY)),
            LocalDate.class),
        Arguments.of(
            LocalTime.of(
                Integer.parseInt(HOUR),
                Integer.parseInt(MINUTES),
                Integer.parseInt(SECONDS),
                Integer.parseInt(NANOSECONDS)),
            LocalTime.class),
        Arguments.of(new Interval().setYear(1).setMonth(200).setDay(-77), Interval.class),
        // blocked by https://github.com/tarantool/tarantool/issues/8887
        // Arguments.of(
        //     new Interval()
        //         .setYear(Interval.MAX_YEAR_RANGE)
        //         .setMonth(Interval.MAX_MONTH_RANGE)
        //         .setWeek(Interval.MAX_WEEK_RANGE)
        //         .setDay(Interval.MAX_DAY_RANGE)
        //         .setHour(Interval.MAX_HOUR_RANGE)
        //         .setMin(Interval.MAX_MIN_RANGE)
        //         .setSec(Interval.MAX_SEC_RANGE)
        //         .setNsec(Interval.MAX_NSEC_RANGE),
        //     Interval.class),
        // Arguments.of(
        //     new Interval()
        //         .setYear(-Interval.MAX_YEAR_RANGE)
        //         .setMonth(-Interval.MAX_MONTH_RANGE)
        //         .setWeek(-Interval.MAX_WEEK_RANGE)
        //         .setDay(-Interval.MAX_DAY_RANGE)
        //         .setHour(-Interval.MAX_HOUR_RANGE)
        //         .setMin(-Interval.MAX_MIN_RANGE)
        //         .setSec(-Interval.MAX_SEC_RANGE)
        //         .setNsec(-Interval.MAX_NSEC_RANGE),
        //     Interval.class),
        Arguments.of(new Interval().setAdjust(Adjust.NoneAdjust), Interval.class),
        Arguments.of(new Interval().setAdjust(Adjust.ExcessAdjust), Interval.class),
        Arguments.of(new Interval().setAdjust(Adjust.LastAdjust), Interval.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestAllBasicTypes")
  public <T> void testAllBasicTypes(T object, Class<T> expectedClass) throws Exception {
    IProtoClient client = getClientAndConnect();
    IProtoResponse evalResponse =
        client
            .eval(
                ECHO_EXPRESSION, TarantoolJacksonMapping.toValue(Collections.singletonList(object)))
            .get();

    T parsedResponse =
        TarantoolJacksonMapping.readResponse(evalResponse, expectedClass).get().get(0);
    assertEquals(object, parsedResponse);
  }

  public static BigDecimal randomBigDecimal(String range) {
    BigDecimal max = new BigDecimal(range);
    BigDecimal randFromDouble = BigDecimal.valueOf(Math.random());
    BigDecimal actualRandomDec = randFromDouble.multiply(max);
    actualRandomDec = actualRandomDec.setScale(2, RoundingMode.DOWN);
    return actualRandomDec;
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestNilWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of("return nil", null, Integer.class),
        Arguments.of("return box.NULL", null, Integer.class),
        Arguments.of("return {1, box.NULL, 3}", Arrays.asList(1, null, 3), List.class),
        Arguments.of("return {1, [3] = 3}", Arrays.asList(1, null, 3), List.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestNilWithDifferentTargetTypes")
  public <T> void testNilWithDifferentTargetTypes(String evalString, T object, Class<T> targetType)
      throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestBooleanWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of("return true", true, Boolean.class),
        Arguments.of("return require('ffi').new('bool', true)", true, Boolean.class),
        Arguments.of("return false", false, Boolean.class),
        Arguments.of("return require('ffi').new('bool', false)", false, Boolean.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestBooleanWithDifferentTargetTypes")
  public <T> void testBooleanWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestUnsignedWithDifferentTargetTypes() {
    return Stream.of(
        /* number */
        Arguments.of("return 0", new BigInteger("0"), BigInteger.class),
        Arguments.of("return 0", 0L, Long.class),
        Arguments.of("return 0", 0, Integer.class),
        Arguments.of("return 0", (short) 0, Short.class),
        Arguments.of("return 0", (byte) 0, Byte.class),
        Arguments.of("return 0", 0.0, Double.class),
        Arguments.of("return 0", 0.0f, Float.class),
        /* long long */
        Arguments.of("return 0LL", new BigInteger("0"), BigInteger.class),
        Arguments.of("return 0LL", 0L, Long.class),
        Arguments.of("return 0LL", 0, Integer.class),
        Arguments.of("return 0LL", (short) 0, Short.class),
        Arguments.of("return 0LL", (byte) 0, Byte.class),
        Arguments.of("return 0LL", 0.0, Double.class),
        Arguments.of("return 0LL", 0.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 0ULL", new BigInteger("0"), BigInteger.class),
        Arguments.of("return 0ULL", 0L, Long.class),
        Arguments.of("return 0ULL", 0, Integer.class),
        Arguments.of("return 0ULL", (short) 0, Short.class),
        Arguments.of("return 0ULL", (byte) 0, Byte.class),
        Arguments.of("return 0ULL", 0.0, Double.class),
        Arguments.of("return 0ULL", 0.0f, Float.class),
        /* number */
        Arguments.of("return 1", new BigInteger("1"), BigInteger.class),
        Arguments.of("return 1", 1L, Long.class),
        Arguments.of("return 1", 1, Integer.class),
        Arguments.of("return 1", (short) 1, Short.class),
        Arguments.of("return 1", (byte) 1, Byte.class),
        Arguments.of("return 1", 1.0, Double.class),
        Arguments.of("return 1", 1.0f, Float.class),
        /* long long */
        Arguments.of("return 1LL", new BigInteger("1"), BigInteger.class),
        Arguments.of("return 1LL", 1L, Long.class),
        Arguments.of("return 1LL", 1, Integer.class),
        Arguments.of("return 1LL", (short) 1, Short.class),
        Arguments.of("return 1LL", (byte) 1, Byte.class),
        Arguments.of("return 1LL", 1.0, Double.class),
        Arguments.of("return 1LL", 1.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 1ULL", new BigInteger("1"), BigInteger.class),
        Arguments.of("return 1ULL", 1L, Long.class),
        Arguments.of("return 1ULL", 1, Integer.class),
        Arguments.of("return 1ULL", (short) 1, Short.class),
        Arguments.of("return 1ULL", (byte) 1, Byte.class),
        Arguments.of("return 1ULL", 1.0, Double.class),
        Arguments.of("return 1ULL", 1.0f, Float.class),
        /* number */
        Arguments.of("return 127", new BigInteger("127"), BigInteger.class),
        Arguments.of("return 127", 127L, Long.class),
        Arguments.of("return 127", 127, Integer.class),
        Arguments.of("return 127", (short) 127, Short.class),
        Arguments.of("return 127", (byte) 127, Byte.class),
        Arguments.of("return 127", 127.0, Double.class),
        Arguments.of("return 127", 127.0f, Float.class),
        /* long long */
        Arguments.of("return 127LL", new BigInteger("127"), BigInteger.class),
        Arguments.of("return 127LL", 127L, Long.class),
        Arguments.of("return 127LL", 127, Integer.class),
        Arguments.of("return 127LL", (short) 127, Short.class),
        Arguments.of("return 127LL", (byte) 127, Byte.class),
        Arguments.of("return 127LL", 127.0, Double.class),
        Arguments.of("return 127LL", 127.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 127ULL", new BigInteger("127"), BigInteger.class),
        Arguments.of("return 127ULL", 127L, Long.class),
        Arguments.of("return 127ULL", 127, Integer.class),
        Arguments.of("return 127ULL", (short) 127, Short.class),
        Arguments.of("return 127ULL", (byte) 127, Byte.class),
        Arguments.of("return 127ULL", 127.0, Double.class),
        Arguments.of("return 127ULL", 127.0f, Float.class),
        /* number */
        Arguments.of("return 128", new BigInteger("128"), BigInteger.class),
        Arguments.of("return 128", 128L, Long.class),
        Arguments.of("return 128", 128, Integer.class),
        Arguments.of("return 128", (short) 128, Short.class),
        Arguments.of("return 128", 128.0, Double.class),
        Arguments.of("return 128", 128.0f, Float.class),
        /* long long */
        Arguments.of("return 128LL", new BigInteger("128"), BigInteger.class),
        Arguments.of("return 128LL", 128L, Long.class),
        Arguments.of("return 128LL", 128, Integer.class),
        Arguments.of("return 128LL", (short) 128, Short.class),
        Arguments.of("return 128LL", 128.0, Double.class),
        Arguments.of("return 128LL", 128.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 128ULL", new BigInteger("128"), BigInteger.class),
        Arguments.of("return 128ULL", 128L, Long.class),
        Arguments.of("return 128ULL", 128, Integer.class),
        Arguments.of("return 128ULL", (short) 128, Short.class),
        Arguments.of("return 128ULL", 128.0, Double.class),
        Arguments.of("return 128ULL", 128.0f, Float.class),
        /* number */
        Arguments.of("return 32767", new BigInteger("32767"), BigInteger.class),
        Arguments.of("return 32767", 32767L, Long.class),
        Arguments.of("return 32767", 32767, Integer.class),
        Arguments.of("return 32767", (short) 32767, Short.class),
        Arguments.of("return 32767", 32767.0, Double.class),
        Arguments.of("return 32767", 32767.0f, Float.class),
        /* long long */
        Arguments.of("return 32767LL", new BigInteger("32767"), BigInteger.class),
        Arguments.of("return 32767LL", 32767L, Long.class),
        Arguments.of("return 32767LL", 32767, Integer.class),
        Arguments.of("return 32767LL", (short) 32767, Short.class),
        Arguments.of("return 32767LL", 32767.0, Double.class),
        Arguments.of("return 32767LL", 32767.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 32767ULL", new BigInteger("32767"), BigInteger.class),
        Arguments.of("return 32767ULL", 32767L, Long.class),
        Arguments.of("return 32767ULL", 32767, Integer.class),
        Arguments.of("return 32767ULL", (short) 32767, Short.class),
        Arguments.of("return 32767ULL", 32767.0, Double.class),
        Arguments.of("return 32767ULL", 32767.0f, Float.class),
        /* number */
        Arguments.of("return 32768", new BigInteger("32768"), BigInteger.class),
        Arguments.of("return 32768", 32768L, Long.class),
        Arguments.of("return 32768", 32768, Integer.class),
        Arguments.of("return 32768", 32768.0, Double.class),
        Arguments.of("return 32768", 32768.0f, Float.class),
        /* long long */
        Arguments.of("return 32768LL", new BigInteger("32768"), BigInteger.class),
        Arguments.of("return 32768LL", 32768L, Long.class),
        Arguments.of("return 32768LL", 32768, Integer.class),
        Arguments.of("return 32768LL", 32768.0, Double.class),
        Arguments.of("return 32768LL", 32768.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 32768ULL", new BigInteger("32768"), BigInteger.class),
        Arguments.of("return 32768ULL", 32768L, Long.class),
        Arguments.of("return 32768ULL", 32768, Integer.class),
        Arguments.of("return 32768ULL", 32768.0, Double.class),
        Arguments.of("return 32768ULL", 32768.0f, Float.class),
        /* number */
        Arguments.of("return 2147483647", new BigInteger("2147483647"), BigInteger.class),
        Arguments.of("return 2147483647", 2147483647L, Long.class),
        Arguments.of("return 2147483647", 2147483647, Integer.class),
        Arguments.of("return 2147483647", 2147483647.0, Double.class),
        Arguments.of("return 2147483647", 2147483647.0f, Float.class),
        /* long long */
        Arguments.of("return 2147483647LL", new BigInteger("2147483647"), BigInteger.class),
        Arguments.of("return 2147483647LL", 2147483647L, Long.class),
        Arguments.of("return 2147483647LL", 2147483647, Integer.class),
        Arguments.of("return 2147483647LL", 2147483647.0, Double.class),
        Arguments.of("return 2147483647LL", 2147483647.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 2147483647ULL", new BigInteger("2147483647"), BigInteger.class),
        Arguments.of("return 2147483647ULL", 2147483647L, Long.class),
        Arguments.of("return 2147483647ULL", 2147483647, Integer.class),
        Arguments.of("return 2147483647ULL", 2147483647.0, Double.class),
        Arguments.of("return 2147483647ULL", 2147483647.0f, Float.class),
        /* number */
        Arguments.of("return 2147483648", new BigInteger("2147483648"), BigInteger.class),
        Arguments.of("return 2147483648", 2147483648L, Long.class),
        Arguments.of("return 2147483648", 2147483648.0, Double.class),
        Arguments.of("return 2147483648", 2147483648.0f, Float.class),
        /* long long */
        Arguments.of("return 2147483648LL", new BigInteger("2147483648"), BigInteger.class),
        Arguments.of("return 2147483648LL", 2147483648L, Long.class),
        Arguments.of("return 2147483648LL", 2147483648.0, Double.class),
        Arguments.of("return 2147483648LL", 2147483648.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 2147483648ULL", new BigInteger("2147483648"), BigInteger.class),
        Arguments.of("return 2147483648ULL", 2147483648L, Long.class),
        Arguments.of("return 2147483648ULL", 2147483648.0, Double.class),
        Arguments.of("return 2147483648ULL", 2147483648.0f, Float.class),
        /* number */
        Arguments.of("return 99999999999999", new BigInteger("99999999999999"), BigInteger.class),
        Arguments.of("return 99999999999999", 99999999999999L, Long.class),
        Arguments.of("return 99999999999999", 99999999999999.0, Double.class),
        Arguments.of("return 99999999999999", 99999999999999.0f, Float.class),
        /* long long */
        Arguments.of("return 99999999999999LL", new BigInteger("99999999999999"), BigInteger.class),
        Arguments.of("return 99999999999999LL", 99999999999999L, Long.class),
        Arguments.of("return 99999999999999LL", 99999999999999.0, Double.class),
        Arguments.of("return 99999999999999LL", 99999999999999.0f, Float.class),
        /* unsigned long long */
        Arguments.of(
            "return 99999999999999ULL", new BigInteger("99999999999999"), BigInteger.class),
        Arguments.of("return 99999999999999ULL", 99999999999999L, Long.class),
        Arguments.of("return 99999999999999ULL", 99999999999999.0, Double.class),
        Arguments.of("return 99999999999999ULL", 99999999999999.0f, Float.class),
        /* number */
        Arguments.of("return 100000000000000", 100000000000000L, null),
        Arguments.of("return 100000000000000", new BigInteger("100000000000000"), BigInteger.class),
        Arguments.of("return 100000000000000", 100000000000000L, Long.class),
        Arguments.of("return 100000000000000", 100000000000000.0, Double.class),
        Arguments.of("return 100000000000000", 100000000000000.0f, Float.class),
        /* long long */
        Arguments.of("return 100000000000000LL", 100000000000000L, null),
        Arguments.of(
            "return 100000000000000LL", new BigInteger("100000000000000"), BigInteger.class),
        Arguments.of("return 100000000000000LL", 100000000000000L, Long.class),
        Arguments.of("return 100000000000000LL", 100000000000000.0, Double.class),
        Arguments.of("return 100000000000000LL", 100000000000000.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 100000000000000ULL", 100000000000000L, null),
        Arguments.of(
            "return 100000000000000ULL", new BigInteger("100000000000000"), BigInteger.class),
        Arguments.of("return 100000000000000ULL", 100000000000000L, Long.class),
        Arguments.of("return 100000000000000ULL", 100000000000000.0, Double.class),
        Arguments.of("return 100000000000000ULL", 100000000000000.0f, Float.class),
        /* long long */
        Arguments.of("return 9223372036854775807LL", 9223372036854775807L, null),
        Arguments.of(
            "return 9223372036854775807LL",
            new BigInteger("9223372036854775807"),
            BigInteger.class),
        Arguments.of("return 9223372036854775807LL", 9223372036854775807L, Long.class),
        Arguments.of("return 9223372036854775807LL", 9223372036854775807.0, Double.class),
        Arguments.of("return 9223372036854775807LL", 9223372036854775807.0f, Float.class),
        /* unsigned long long */
        Arguments.of("return 9223372036854775807ULL", 9223372036854775807L, null),
        Arguments.of(
            "return 9223372036854775807ULL",
            new BigInteger("9223372036854775807"),
            BigInteger.class),
        Arguments.of("return 9223372036854775807ULL", 9223372036854775807L, Long.class),
        Arguments.of("return 9223372036854775807ULL", 9223372036854775807.0, Double.class),
        Arguments.of("return 9223372036854775807ULL", 9223372036854775807.0f, Float.class),
        Arguments.of(
            "return 9223372036854775808ULL",
            new BigInteger("9223372036854775808"),
            BigInteger.class),
        Arguments.of("return 9223372036854775808ULL", 9223372036854775808.0, Double.class),
        Arguments.of("return 9223372036854775808ULL", 9223372036854775808.0f, Float.class),
        Arguments.of(
            "return 9223372036854775809ULL",
            new BigInteger("9223372036854775809"),
            BigInteger.class),
        Arguments.of("return 9223372036854775809ULL", 9223372036854775809.0, Double.class),
        Arguments.of("return 9223372036854775809ULL", 9223372036854775809.0f, Float.class),
        Arguments.of(
            "return 18446744073709551614ULL",
            new BigInteger("18446744073709551614"),
            BigInteger.class),
        Arguments.of("return 18446744073709551614ULL", 18446744073709551614.0, Double.class),
        Arguments.of("return 18446744073709551614ULL", 18446744073709551614.0f, Float.class),
        Arguments.of(
            "return 18446744073709551615ULL",
            new BigInteger("18446744073709551615"),
            BigInteger.class),
        Arguments.of("return 18446744073709551615ULL", 18446744073709551615.0, Double.class),
        Arguments.of("return 18446744073709551615ULL", 18446744073709551615.0f, Float.class),
        Arguments.of("return 18446744073709551615ULL", 18446744073709551615.0f, Float.class),
        Arguments.of("return -1ULL", new BigInteger("18446744073709551615"), BigInteger.class),
        Arguments.of("return -1ULL", 18446744073709551615.0, Double.class),
        Arguments.of("return -1ULL", 18446744073709551615.0f, Float.class),
        Arguments.of("return -1ULL", 18446744073709551615.0f, Float.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestUnsignedWithDifferentTargetTypes")
  public <T> void testUnsignedWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestSignedWithDifferentTargetTypes() {
    return Stream.of(
        /* number */
        Arguments.of("return -1", -1, null),
        Arguments.of("return -1", new BigInteger("-1"), BigInteger.class),
        Arguments.of("return -1", -1L, Long.class),
        Arguments.of("return -1", -1, Integer.class),
        Arguments.of("return -1", (short) -1, Short.class),
        Arguments.of("return -1", (byte) -1, Byte.class),
        Arguments.of("return -1", -1.0, Double.class),
        Arguments.of("return -1", -1.0f, Float.class),
        /* long long */
        Arguments.of("return -1LL", -1, null),
        Arguments.of("return -1LL", new BigInteger("-1"), BigInteger.class),
        Arguments.of("return -1LL", -1L, Long.class),
        Arguments.of("return -1LL", -1, Integer.class),
        Arguments.of("return -1LL", (short) -1, Short.class),
        Arguments.of("return -1LL", (byte) -1, Byte.class),
        Arguments.of("return -1LL", -1.0, Double.class),
        Arguments.of("return -1LL", -1.0f, Float.class),
        /* number */
        Arguments.of("return -128", -128, null),
        Arguments.of("return -128", new BigInteger("-128"), BigInteger.class),
        Arguments.of("return -128", -128L, Long.class),
        Arguments.of("return -128", -128, Integer.class),
        Arguments.of("return -128", (short) -128, Short.class),
        Arguments.of("return -128", (byte) -128, Byte.class),
        Arguments.of("return -128", -128.0, Double.class),
        Arguments.of("return -128", -128.0f, Float.class),
        /* long long */
        Arguments.of("return -128LL", -128, null),
        Arguments.of("return -128LL", new BigInteger("-128"), BigInteger.class),
        Arguments.of("return -128LL", -128L, Long.class),
        Arguments.of("return -128LL", -128, Integer.class),
        Arguments.of("return -128LL", (short) -128, Short.class),
        Arguments.of("return -128LL", (byte) -128, Byte.class),
        Arguments.of("return -128LL", -128.0, Double.class),
        Arguments.of("return -128LL", -128.0f, Float.class),
        /* number */
        Arguments.of("return -32768", -32768, null),
        Arguments.of("return -32768", new BigInteger("-32768"), BigInteger.class),
        Arguments.of("return -32768", -32768L, Long.class),
        Arguments.of("return -32768", -32768, Integer.class),
        Arguments.of("return -32768", (short) -32768, Short.class),
        Arguments.of("return -32768", -32768.0, Double.class),
        Arguments.of("return -32768", -32768.0f, Float.class),
        /* long long */
        Arguments.of("return -32768LL", -32768, null),
        Arguments.of("return -32768LL", new BigInteger("-32768"), BigInteger.class),
        Arguments.of("return -32768LL", -32768L, Long.class),
        Arguments.of("return -32768LL", -32768, Integer.class),
        Arguments.of("return -32768LL", (short) -32768, Short.class),
        Arguments.of("return -32768LL", -32768.0, Double.class),
        Arguments.of("return -32768LL", -32768.0f, Float.class),
        /* number */
        Arguments.of("return -2147483648", -2147483648, null),
        Arguments.of("return -2147483648", new BigInteger("-2147483648"), BigInteger.class),
        Arguments.of("return -2147483648", -2147483648L, Long.class),
        Arguments.of("return -2147483648", -2147483648, Integer.class),
        Arguments.of("return -2147483648", -2147483648.0, Double.class),
        Arguments.of("return -2147483648", -2147483648.0f, Float.class),
        /* long long */
        Arguments.of("return -2147483648LL", -2147483648, null),
        Arguments.of("return -2147483648LL", new BigInteger("-2147483648"), BigInteger.class),
        Arguments.of("return -2147483648LL", -2147483648L, Long.class),
        Arguments.of("return -2147483648LL", -2147483648, Integer.class),
        Arguments.of("return -2147483648LL", -2147483648.0, Double.class),
        Arguments.of("return -2147483648LL", -2147483648.0f, Float.class),
        /* number */
        Arguments.of("return -99999999999999", -99999999999999L, null),
        Arguments.of("return -99999999999999", new BigInteger("-99999999999999"), BigInteger.class),
        Arguments.of("return -99999999999999", -99999999999999L, Long.class),
        Arguments.of("return -99999999999999", -99999999999999.0, Double.class),
        Arguments.of("return -99999999999999", -99999999999999.0f, Float.class),
        /* long long */
        Arguments.of("return -99999999999999LL", -99999999999999L, null),
        Arguments.of(
            "return -99999999999999LL", new BigInteger("-99999999999999"), BigInteger.class),
        Arguments.of("return -99999999999999LL", -99999999999999L, Long.class),
        Arguments.of("return -99999999999999LL", -99999999999999.0, Double.class),
        Arguments.of("return -99999999999999LL", -99999999999999.0f, Float.class),
        /* number */
        Arguments.of("return -100000000000000", -100000000000000L, null),
        Arguments.of(
            "return -100000000000000", new BigInteger("-100000000000000"), BigInteger.class),
        Arguments.of("return -100000000000000", -100000000000000L, Long.class),
        Arguments.of("return -100000000000000", -100000000000000.0, Double.class),
        Arguments.of("return -100000000000000", -100000000000000.0f, Float.class),
        /* long long */
        Arguments.of("return -100000000000000LL", -100000000000000L, null),
        Arguments.of(
            "return -100000000000000LL", new BigInteger("-100000000000000"), BigInteger.class),
        Arguments.of("return -100000000000000LL", -100000000000000L, Long.class),
        Arguments.of("return -100000000000000LL", -100000000000000.0, Double.class),
        Arguments.of("return -100000000000000LL", -100000000000000.0f, Float.class),
        /* number */
        Arguments.of("return -9223372036854775808", -9223372036854775808L, null),
        Arguments.of(
            "return -9223372036854775808",
            new BigInteger("-9223372036854775808"),
            BigInteger.class),
        Arguments.of("return -9223372036854775808", -9223372036854775808L, Long.class),
        Arguments.of("return -9223372036854775808", -9223372036854775808.0, Double.class),
        Arguments.of("return -9223372036854775808", -9223372036854775808.0f, Float.class),
        /* long long */
        Arguments.of("return -9223372036854775808LL", -9223372036854775808L, null),
        Arguments.of(
            "return -9223372036854775808LL",
            new BigInteger("-9223372036854775808"),
            BigInteger.class),
        Arguments.of("return -9223372036854775808LL", -9223372036854775808L, Long.class),
        Arguments.of("return -9223372036854775808LL", -9223372036854775808.0, Double.class),
        Arguments.of("return -9223372036854775808LL", -9223372036854775808.0f, Float.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestSignedWithDifferentTargetTypes")
  public <T> void testSignedWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestDoubleWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of("return -1.1", -1.1, null),
        Arguments.of("return -1.1", new BigInteger("-1"), BigInteger.class),
        Arguments.of("return -1.1", -1L, Long.class),
        Arguments.of("return -1.1", -1, Integer.class),
        Arguments.of("return -1.1", (short) -1, Short.class),
        Arguments.of("return -1.1", (byte) -1, Byte.class),
        Arguments.of("return -1.1", -1.1, Double.class),
        Arguments.of("return -1.1", -1.1f, Float.class),
        Arguments.of("return require('ffi').new('float', 123456)", 123456.0, null),
        Arguments.of(
            "return require('ffi').new('float', 123456)",
            new BigInteger("123456"),
            BigInteger.class),
        Arguments.of("return require('ffi').new('float', 123456)", 123456L, Long.class),
        Arguments.of("return require('ffi').new('float', 123456)", 123456, Integer.class),
        Arguments.of("return require('ffi').new('float', 123456)", 123456.0, Double.class),
        Arguments.of("return require('ffi').new('float', 123456)", 123456f, Float.class),
        Arguments.of("return require('ffi').new('double', 123456)", 123456.0, null),
        Arguments.of(
            "return require('ffi').new('double', 123456)",
            new BigInteger("123456"),
            BigInteger.class),
        Arguments.of("return require('ffi').new('double', 123456)", 123456L, Long.class),
        Arguments.of("return require('ffi').new('double', 123456)", 123456, Integer.class),
        Arguments.of("return require('ffi').new('double', 123456)", 123456.0, Double.class),
        Arguments.of("return require('ffi').new('double', 123456)", 123456f, Float.class),
        Arguments.of("return require('ffi').new('float', 12.121)", 12.121000289916992, null),
        Arguments.of(
            "return require('ffi').new('float', 12.121)", 12.121000289916992, Double.class),
        Arguments.of("return require('ffi').new('float', 12.121)", 12.121f, Float.class),
        Arguments.of("return require('ffi').new('double', 12.121)", 12.121, null),
        Arguments.of("return require('ffi').new('double', 12.121)", 12.121, Double.class),
        Arguments.of("return require('ffi').new('double', 12.121)", 12.121f, Float.class),
        Arguments.of("return 3.1415926535898", 3.1415926535898, null),
        Arguments.of("return 3.1415926535898", new BigInteger("3"), BigInteger.class),
        Arguments.of("return 3.1415926535898", 3L, Long.class),
        Arguments.of("return 3.1415926535898", 3, Integer.class),
        Arguments.of("return 3.1415926535898", (short) 3, Short.class),
        Arguments.of("return 3.1415926535898", (byte) 3, Byte.class),
        Arguments.of("return 3.1415926535898", 3.1415926535898, Double.class),
        Arguments.of("return 3.1415926535898", 3.1415926535898f, Float.class),
        Arguments.of("return -3.1415926535898", -3.1415926535898, null),
        Arguments.of("return -3.1415926535898", new BigInteger("-3"), BigInteger.class),
        Arguments.of("return -3.1415926535898", -3L, Long.class),
        Arguments.of("return -3.1415926535898", -3, Integer.class),
        Arguments.of("return -3.1415926535898", (short) -3, Short.class),
        Arguments.of("return -3.1415926535898", (byte) -3, Byte.class),
        Arguments.of("return -3.1415926535898", -3.1415926535898, Double.class),
        Arguments.of("return -3.1415926535898", -3.1415926535898f, Float.class),
        Arguments.of("return 1e100", 1e100, null),
        Arguments.of(
            "return 1e100", new BigInteger("1" + StringUtils.repeat("0", 100)), BigInteger.class),
        Arguments.of("return 1e100", 1e100, Double.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestDoubleWithDifferentTargetTypes")
  public <T> void testDoubleWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestDecimalWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of("return require('decimal').new(1)", new BigDecimal(1), null),
        Arguments.of("return require('decimal').new(1)", new BigDecimal(1), BigDecimal.class),
        Arguments.of(
            "return require('decimal').new('1234567891234567890.0987654321987654321')",
            new BigDecimal("1234567891234567890.0987654321987654321"),
            BigDecimal.class),
        Arguments.of(
            "return require('decimal').new('1234567891234567890.0987654321987654321')",
            new BigDecimal("1234567891234567890.0987654321987654321"),
            BigDecimal.class),
        Arguments.of(
            "return require('decimal').new('1' .. string.rep('0', 37))",
            new BigDecimal("1" + StringUtils.repeat("0", 37)),
            BigDecimal.class),
        Arguments.of(
            "return require('decimal').new('0.' .. string.rep('0', 37) .. '1')",
            new BigDecimal("0." + StringUtils.repeat("0", 37) + "1"),
            BigDecimal.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestDecimalWithDifferentTargetTypes")
  public <T> void testDecimalWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  public static Stream<Arguments> dataForTestDecimalWithDifferentENotation() {
    List<Arguments> arguments = new ArrayList<>(37);
    StringBuilder sb = new StringBuilder();
    StringBuilder sb1 = new StringBuilder();
    for (int i = -38; i < 37; i++) {
      sb.delete(0, sb.length()).append(i).append("e").append(i);
      sb1.delete(0, sb1.length())
          .append("return require('decimal').new('")
          .append(i)
          .append("e")
          .append(i)
          .append("')");
      arguments.add(Arguments.of(sb1.toString(), new BigDecimal(sb.toString()), BigDecimal.class));
    }
    return arguments.stream();
  }

  @ParameterizedTest
  @MethodSource("dataForTestDecimalWithDifferentENotation")
  public <T> void testDecimalWithDifferentENotation(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestUUIDWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of(
            "return require('uuid').fromstr('E5EF0FEF-FB4F-4098-A7C4-2358CD896BD4')",
            UUID.fromString("E5EF0FEF-FB4F-4098-A7C4-2358CD896BD4"),
            null),
        Arguments.of(
            "return require('uuid').fromstr('E5EF0FEF-FB4F-4098-A7C4-2358CD896BD4')",
            UUID.fromString("E5EF0FEF-FB4F-4098-A7C4-2358CD896BD4"),
            UUID.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestUUIDWithDifferentTargetTypes")
  public <T> void testUUIDWithDifferentTargetTypes(String evalString, T object, Class<T> targetType)
      throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestDatetimeWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0).atZone(ZoneOffset.UTC),
            null),
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            Instant.class),
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0),
            LocalDateTime.class),
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0).toLocalDate(),
            LocalDate.class),
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0).toLocalTime(),
            LocalTime.class),
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0).atZone(ZoneOffset.UTC),
            ZonedDateTime.class),
        Arguments.of(
            "return require('datetime').new({year = -5879609})",
            LocalDateTime.of(-5879609, 1, 1, 0, 0).atOffset(ZoneOffset.UTC),
            OffsetDateTime.class),
        Arguments.of(
            "return require('datetime').new({year = 5879611})",
            LocalDateTime.of(5879611, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
            Instant.class),
        Arguments.of(
            "return require('datetime').new({year = 5879611})",
            LocalDateTime.of(5879611, 1, 1, 0, 0),
            LocalDateTime.class),
        Arguments.of(
            "return require('datetime').new({year = 5879611})",
            LocalDateTime.of(5879611, 1, 1, 0, 0).toLocalDate(),
            LocalDate.class),
        Arguments.of(
            "return require('datetime').new({year = 5879611})",
            LocalDateTime.of(5879611, 1, 1, 0, 0).toLocalTime(),
            LocalTime.class),
        Arguments.of(
            "return require('datetime').new({year = 5879611})",
            LocalDateTime.of(5879611, 1, 1, 0, 0).atZone(ZoneOffset.UTC),
            ZonedDateTime.class),
        Arguments.of(
            "return require('datetime').new({year = 5879611})",
            LocalDateTime.of(5879611, 1, 1, 0, 0).atOffset(ZoneOffset.UTC),
            OffsetDateTime.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestDatetimeWithDifferentTargetTypes")
  public <T> void testDatetimeWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  public static Stream<Arguments> dataForTestIntervalWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of(
            "return require('datetime').interval.new({"
                + "year=1, month=2, week=3, day=4, hour=5, min=6, sec=7, nsec=8, adjust = 'last'})",
            new Interval()
                .setYear(1)
                .setMonth(2)
                .setWeek(3)
                .setDay(4)
                .setHour(5)
                .setMin(6)
                .setSec(7)
                .setNsec(8)
                .setAdjust(Adjust.LastAdjust),
            null));
  }

  @ParameterizedTest
  @MethodSource("dataForTestIntervalWithDifferentTargetTypes")
  public <T> void testIntervalWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  /**
   * refs: <a
   * href="https://www.tarantool.io/en/doc/latest/concepts/data_model/value_store/#field-type-details">
   * type-details </a> <a
   * href="https://github.com/tarantool/tarantool/blob/master/test/app-tap/lua/serializer_test.lua">
   * serializer_test.lua </a>
   */
  public static Stream<Arguments> dataForTestStringWithDifferentTargetTypes() {
    return Stream.of(
        Arguments.of("return ''", "", null),
        Arguments.of("return ''", "", String.class),
        Arguments.of("return 'Кудыкины горы'", "Кудыкины горы", null),
        Arguments.of("return 'Кудыкины горы'", "Кудыкины горы", String.class),
        Arguments.of("return '$a\t $'", "$a\t $", null),
        Arguments.of("return '$a\t $'", "$a\t $", String.class),
        Arguments.of("return string.rep('x', 33)", StringUtils.repeat("x", 33), null),
        Arguments.of("return string.rep('x', 33)", StringUtils.repeat("x", 33), String.class));
  }

  @ParameterizedTest
  @MethodSource("dataForTestStringWithDifferentTargetTypes")
  public <T> void testStringWithDifferentTargetTypes(
      String evalString, T object, Class<T> targetType) throws Exception {
    testResultFromTarantool(evalString, object, targetType);
  }

  public static Stream<Arguments> dataForTestStringToByteArray() {
    return Stream.of(
        Arguments.of("string", byte[].class, (Function<byte[], String>) String::new),
        Arguments.of("string", char[].class, (Function<char[], String>) String::new));
  }

  @ParameterizedTest
  @MethodSource("dataForTestStringToByteArray")
  public <T> void testStringToByteArray(T object, Class<T> expectedClass, Function caster)
      throws Exception {
    IProtoClient client = getClientAndConnect();
    IProtoResponse evalResponse =
        client
            .eval(
                ECHO_EXPRESSION, TarantoolJacksonMapping.toValue(Collections.singletonList(object)))
            .get();

    T parsedResponse =
        TarantoolJacksonMapping.readResponse(evalResponse, expectedClass).get().get(0);
    assertEquals(object, caster.apply(parsedResponse));
  }

  public static Stream<Arguments> dataForTestMapWithNonStringKeys() {
    return Stream.of(
        Arguments.of(
            new HashMap<Object, Object>() {
              {
                put(99, true);
              }
            },
            new TypeReference<List<Map<Integer, Boolean>>>() {}));
  }

  @ParameterizedTest
  @MethodSource("dataForTestMapWithNonStringKeys")
  public <T> void testMapWithNonStringKeys(T object, TypeReference<List<T>> expectedClass)
      throws Exception {
    IProtoClient client = getClientAndConnect();
    IProtoResponse evalResponse =
        client
            .eval(
                ECHO_EXPRESSION, TarantoolJacksonMapping.toValue(Collections.singletonList(object)))
            .get();

    T parsedResponse =
        TarantoolJacksonMapping.readResponse(evalResponse, expectedClass).get().get(0);
    assertEquals(object, parsedResponse);
  }

  private IProtoClient getClientAndConnect() throws Exception {
    IProtoClient client = new IProtoClientImpl(factory, factory.getTimerService());
    client.connect(address, 3_000).get();
    client.authorize(API_USER, CREDS.get(API_USER)).join();
    return client;
  }

  private <T> void testResultFromTarantool(String evalString, T object, Class<T> targetType)
      throws Exception {
    IProtoClient client = getClientAndConnect();
    IProtoResponse evalResponse = client.eval(evalString, ValueFactory.emptyArray()).get();

    Object parsedResponse;
    if (targetType == null) {
      parsedResponse = TarantoolJacksonMapping.readResponse(evalResponse).get().get(0);
    } else {
      parsedResponse = TarantoolJacksonMapping.readResponse(evalResponse, targetType).get().get(0);
    }
    assertEquals(
        object, parsedResponse, String.format("object=%s targetType=%s", object, targetType));
  }

  private List<TestEntity> getTupleData(List<Tuple<TestEntity>> tuples) {
    return tuples.stream().map(Tuple::get).collect(Collectors.toList());
  }

  @DisabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "2.*")
  @Test
  public void testVarbinary() throws Exception {
    IProtoClient client = getClientAndConnect();
    byte[] object = "string".getBytes();
    IProtoResponse evalResponse =
        client
            .eval(
                ECHO_EXPRESSION, TarantoolJacksonMapping.toValue(Collections.singletonList(object)))
            .get();

    assertTrue(
        Arrays.equals(
            object, (byte[]) TarantoolJacksonMapping.readResponse(evalResponse).get().get(0)));
    assertTrue(
        Arrays.equals(
            object, TarantoolJacksonMapping.readResponse(evalResponse, byte[].class).get().get(0)));
  }

  @EnabledIfEnvironmentVariable(named = "TARANTOOL_VERSION", matches = "2.*")
  @Test
  public void testVarbinaryInAndStringFrom() throws Exception {
    IProtoClient client = getClientAndConnect();
    String object = "string";
    IProtoResponse evalResponse =
        client
            .eval(
                ECHO_EXPRESSION,
                TarantoolJacksonMapping.toValue(Collections.singletonList(object.getBytes())))
            .get();

    String parsedResponse =
        TarantoolJacksonMapping.readResponse(evalResponse, String.class).get().get(0);
    assertEquals(object, parsedResponse);
  }

  public static Stream<Arguments> dataForTestErrorDeserialization() {
    return Stream.of(
        // The error with the code and the argument.
        Arguments.of(
            "The error with the code and the argument",
            "return box.error.new(box.error.NO_SUCH_SPACE, 'my_space')",
            1,
            "ClientError",
            "Space 'my_space' does not exist",
            36L),
        // The error without the code: the unknown code is used by default.
        Arguments.of(
            "The error without the code: the unknown code is used by default",
            "return box.error.new({reason = 'The error without a code'})",
            1,
            "ClientError",
            "The error without a code",
            0L),
        // The error with the custom payload.
        Arguments.of(
            "The error with the custom payload",
            "return box.error.new({code = 42, reason = 'The custom error'})",
            1,
            "ClientError",
            "The custom error",
            42L),
        // The plain Lua error is a string, not a box.error object: it must be wrapped
        // into the box.error object to be returned as the MP_ERROR value.
        Arguments.of(
            "The Lua error, caught by pcall",
            "local ok, err = pcall(fail) return box.error.new({reason = tostring(err)})",
            1,
            "ClientError",
            "Fail!",
            0L),
        // The error with the chain of two errors.
        Arguments.of(
            "The error with the chain of two errors",
            "local ok, err = pcall(wrapped_fail_by_box_error) return err",
            2,
            "ClientError",
            "wrapped failure",
            0L),
        // The error in the eval script itself. The plain Lua error is a string, not a
        // box.error object: it must be wrapped into the box.error object to be returned
        // as the MP_ERROR value.
        Arguments.of(
            "The error in the eval script itself",
            "local x = nil local ok, err = pcall(function() return x.field end) "
                + "return box.error.new({reason = tostring(err)})",
            1,
            "ClientError",
            "attempt to index",
            0L));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dataForTestErrorDeserialization")
  void testErrorDeserialization(
      String label,
      String expression,
      int expectedStackSize,
      String expectedType,
      String expectedMessage,
      long expectedCode)
      throws Exception {
    final IProtoClient client = getClientAndConnect();
    final IProtoResponse response = client.eval(expression, ValueFactory.emptyArray()).join();

    final TarantoolResponse<List<BoxErrorValue>> result =
        TarantoolJacksonMapping.readResponse(response, BoxErrorValue.class);
    final BoxErrorValue errorValue = result.get().get(0);

    Assertions.assertEquals(expectedStackSize, errorValue.stack().size());

    final BoxErrorStackItem item = errorValue.stack().get(0);
    Assertions.assertEquals(expectedType, item.type());
    // The Lua error message contains the position prefix, so it is compared partially.
    Assertions.assertTrue(
        item.message().contains(expectedMessage),
        "Expected the message to contain '" + expectedMessage + "', but got: " + item.message());
    Assertions.assertEquals(expectedCode, item.code());
    Assertions.assertEquals(0L, item.errNo());

    client.close();
  }

  // The second element of the chain must not be lost.
  @Test
  @DisplayName("Check wrapped errors")
  void testErrorDeserializationStack() throws Exception {
    final IProtoClient client = getClientAndConnect();
    try {
      final IProtoResponse response =
          client
              .eval(
                  "local ok, err = pcall(wrapped_fail_by_box_error) return err",
                  ValueFactory.emptyArray())
              .join();

      final BoxErrorValue errorValue =
          TarantoolJacksonMapping.readResponse(response, BoxErrorValue.class).get().get(0);
      Assertions.assertEquals(2, errorValue.stack().size());
      Assertions.assertEquals("wrapped failure", errorValue.stack().get(0).message());
      Assertions.assertEquals("fail", errorValue.stack().get(1).message());
    } finally {
      client.close();
    }
  }

  // The error inside an array and inside a map.
  @Test
  void testErrorDeserializationInContainer() throws Exception {
    final IProtoClient client = getClientAndConnect();
    try {
      // The error is the first element of the returned array.
      final IProtoResponse arrayResponse =
          client
              .eval(
                  "return {box.error.new(box.error.NO_SUCH_SPACE, 'my_space'), 42}",
                  ValueFactory.emptyArray())
              .join();

      final List<?> arrayResult = TarantoolJacksonMapping.readResponse(arrayResponse).get();
      final List<?> tuple = (List<?>) arrayResult.get(0);
      final BoxErrorValue errorValue = assertInstanceOf(BoxErrorValue.class, tuple.get(0));
      Assertions.assertEquals(
          "Space 'my_space' does not exist", errorValue.stack().get(0).message());

      // The error is the value of the 'err' key of the returned map.
      final IProtoResponse mapResponse =
          client
              .eval(
                  "return {err = box.error.new(box.error.NO_SUCH_SPACE, 'my_space')}",
                  ValueFactory.emptyArray())
              .join();

      final List<?> mapResult = TarantoolJacksonMapping.readResponse(mapResponse).get();
      Map<?, ?> map = assertInstanceOf(Map.class, mapResult.get(0));
      final BoxErrorValue err = assertInstanceOf(BoxErrorValue.class, map.get("err"));
      Assertions.assertEquals("Space 'my_space' does not exist", err.stack().get(0).message());
    } finally {
      client.close();
    }
  }

  @Test
  void testNilErrorDeserialization() throws Exception {
    final IProtoClient client = getClientAndConnect();
    try {
      final IProtoResponse response = client.eval("return nil", ValueFactory.emptyArray()).join();
      final List<BoxErrorValue> errors =
          TarantoolJacksonMapping.readResponse(response, BoxErrorValue.class).get();
      Assertions.assertEquals(1, errors.size());
      Assertions.assertNull(errors.get(0));
    } finally {
      client.close();
    }
  }

  public static Stream<Arguments> dataForTestErrorSerialization() {
    return Stream.of(
        // All the fields have their default values.
        Arguments.of(
            new BoxErrorValue(Collections.singletonList(BoxErrorStackItem.builder().build())),
            "all the fields are default"),
        // All the fields are set.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder()
                        .withType("ClientError")
                        .withLine(42L)
                        .withFile("test.lua")
                        .withMessage("Something went wrong")
                        .withErrNo(7L)
                        .withCode(32L)
                        .build())),
            "all the fields are set"),
        // Only the type and the message are set, the other fields have their default values.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder()
                        .withType("ClientError")
                        .withMessage("Only the type and the message are set")
                        .build())),
            "only the type and the message are set"),
        // Only the message is set, the other fields have their default values.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder().withMessage("Only the message is set").build())),
            "only the message is set"),
        // The additional fields are set to an empty map.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder()
                        .withType("ClientError")
                        .withMessage("The additional fields are empty")
                        .withFields(ValueFactory.emptyMap())
                        .build())),
            "the additional fields are an empty map"),
        // The additional fields are set to a non-empty map.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder()
                        .withType("AccessDeniedError")
                        .withMessage("The additional fields are not empty")
                        .withFields(
                            ValueFactory.newMap(
                                ValueFactory.newString("object_type"),
                                ValueFactory.newString("space"),
                                ValueFactory.newString("object_name"),
                                ValueFactory.newString("my_space"),
                                ValueFactory.newString("access_type"),
                                ValueFactory.newString("write")))
                        .build())),
            "the additional fields are a non-empty map"),
        // The stack consists of two errors.
        Arguments.of(
            new BoxErrorValue(
                Arrays.asList(
                    BoxErrorStackItem.builder()
                        .withType("ClientError")
                        .withMessage("The first error of the stack")
                        .build(),
                    BoxErrorStackItem.builder()
                        .withType("LuajitError")
                        .withMessage("The second error of the stack")
                        .build())),
            "the stack consists of two errors"),
        // The message contains special characters.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder()
                        .withMessage("\"quotes\", \n newline, \\ backslash, \t tab")
                        .build())),
            "the message contains special characters"),
        // The numeric fields have the maximum values.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder()
                        .withLine(Integer.MAX_VALUE)
                        .withErrNo(Integer.MAX_VALUE)
                        .withCode(Integer.MAX_VALUE)
                        .withMessage("The maximum numeric values")
                        .build())),
            "the numeric fields have the maximum values"),
        // The message is long.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.builder().withMessage(LONG_MESSAGE).build())),
            "the message is long"),
        // The error object is built by parsing the MP_ERROR structure.
        Arguments.of(
            new BoxErrorValue(
                Collections.singletonList(
                    BoxErrorStackItem.fromStruct(
                        ValueFactory.newMap(
                            IProtoConstant.MP_ERROR_TYPE,
                            ValueFactory.newString("ClientError"),
                            IProtoConstant.MP_ERROR_FILE,
                            ValueFactory.newString("test.lua"),
                            IProtoConstant.MP_ERROR_LINE,
                            ValueFactory.newInteger(42L),
                            IProtoConstant.MP_ERROR_MESSAGE,
                            ValueFactory.newString("The parsed error"),
                            IProtoConstant.MP_ERROR_ERRNO,
                            ValueFactory.newInteger(7L),
                            IProtoConstant.MP_ERROR_ERRCODE,
                            ValueFactory.newInteger(32L))))),
            "the error object is parsed from the MP_ERROR structure"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("dataForTestErrorSerialization")
  void testErrorSerialization(BoxErrorValue errorValue, String caseName) throws Exception {
    final IProtoClient client = getClientAndConnect();
    try {
      final byte[] value = TarantoolJacksonMapping.toValue(Collections.singletonList(errorValue));
      final CompletableFuture<IProtoResponse> call = client.call("error_verify", value);
      final IProtoResponse response = call.join();

      final List<?> result = TarantoolJacksonMapping.readResponse(response).get();
      final Object check = result.get(0);
      final Map<?, ?> checkMap =
          Assertions.assertInstanceOf(
              Map.class, check, String.format("Unexpected result: %s", check));

      Assertions.assertEquals(
          Boolean.TRUE,
          checkMap.get("ok"),
          String.format(
              "The case '%s' has failed: %s (%s)",
              caseName, checkMap.get("reason"), checkMap.get("raw_value")));
    } finally {
      client.close();
    }
  }
}
