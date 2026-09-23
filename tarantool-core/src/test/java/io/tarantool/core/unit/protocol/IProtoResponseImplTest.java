/*
 * Copyright (c) 2026 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core.unit.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.msgpack.value.ValueFactory;

import static io.tarantool.core.protocol.requests.IProtoConstant.MP_IPROTO_REQUEST_TYPE;
import io.tarantool.core.protocol.IProtoResponseImpl;

public class IProtoResponseImplTest {

  @Test
  public void testToStringDoesNotDuplicateOnRepeatedCalls() {
    IProtoResponseImpl response =
        new IProtoResponseImpl(
            ValueFactory.newMap(MP_IPROTO_REQUEST_TYPE, ValueFactory.newInteger(0)),
            ValueFactory.emptyMap());

    String first = response.toString();
    String second = response.toString();

    assertTrue(first.startsWith("IProtoResponseImpl(header = "));
    assertEquals(first, second);
  }
}
