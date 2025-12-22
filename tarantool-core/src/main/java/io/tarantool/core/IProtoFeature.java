/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.core;

/**
 * @author Artyom Dubinin
 * @author Ivan Dneprov
 */
public enum IProtoFeature {

  /** Streams were introduced in protocol version 1 (Tarantool 2.10.0). */
  STREAMS,
  /** Transactions were introduced in protocol version 1 (Tarantool 2.10.0). */
  TRANSACTIONS,
  /** Error extension type was introduced in protocol version 2 (Tarantool 2.10.0). */
  ERROR_EXTENSION,
  /** Watchers were introduced in protocol version 3 (Tarantool 2.10.0). */
  WATCHERS,
  /** Pagination were introduced in protocol version 4 (Tarantool 2.11.0). */
  PAGINATION,
  /** Space and index names were introduced in protocol version 5 (Tarantool 3.0.0). */
  SPACE_AND_INDEX_NAMES,

  /** IPROTO_WATCH_ONCE request support. Were introduced in protocol version 6 (Tarantool 3.0.0). */
  WATCH_ONCE,
  /**
   * Tuple format in DML request responses support: Tuples in IPROTO_DATA response field are encoded
   * as MP_TUPLE and tuple format is sent in IPROTO_TUPLE_FORMATS field. Were introduced in protocol
   * version 7 (Tarantool 3.0.0).
   */
  DML_TUPLE_EXTENSION,

  /**
   * Tuple format in call and eval request responses support: Tuples in IPROTO_DATA response field
   * are encoded as MP_TUPLE and tuple formats are sent in IPROTO_TUPLE_FORMATS field. Were
   * introduced in protocol version 7 (Tarantool 3.0.0).
   */
  CALL_RET_TUPLE_EXTENSION,

  /**
   * Tuple format in call and eval request arguments support: Tuples in IPROTO_TUPLE request field
   * are encoded as MP_TUPLE and tuple formats are received in IPROTO_TUPLE_FORMATS field. Were
   * introduced in protocol version 7 (Tarantool 3.0.0).
   */
  CALL_ARG_TUPLE_EXTENSION;

  public static final int PROTOCOL_VERSION = 7;

  public static IProtoFeature valueOf(int ordinal) {
    for (IProtoFeature feature : values()) {
      if (feature.ordinal() == ordinal) {
        return feature;
      }
    }
    throw new IllegalArgumentException("No IProtoFeature with ordinal:" + ordinal);
  }
}
