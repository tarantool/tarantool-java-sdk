/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.core.protocol.requests;

import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

public interface IProtoConstant {

  int MINIMAL_HEADER_SIZE = 5; // MP_UINT32

  int IPROTO_CHUNK = 0x80;
  int IPROTO_DATA = 0x30;
  int IPROTO_ERROR = 0x52;
  int IPROTO_ERROR_24 = 0x31;
  int IPROTO_ERROR_BASE = 0x8000;
  int IPROTO_ERR_ACCESS_DENIED = 0x2A;
  int IPROTO_ERR_CREDS_MISMATCH = 0x2F;
  int IPROTO_ERR_INVALID_MSGPACK = 0x20;
  int IPROTO_ERR_NO_SUCH_PROC = 0x21;
  int IPROTO_ERR_PROC_LUA = 0x20;
  int IPROTO_ERR_SPACE_DOES_NOT_EXIST = 0x24;
  int IPROTO_ERR_TUPLE_FOUND = 0x03;
  int IPROTO_ERR_UNKNOWN = 0x00;
  int IPROTO_EVENT = 0x4c;
  int IPROTO_EVENT_DATA = 0x58;
  int IPROTO_EVENT_KEY = 0x57;
  int IPROTO_FEATURES = 0x55;
  int IPROTO_INDEX_ID = 0x11;
  int IPROTO_INDEX_NAME = 0x5f;
  int IPROTO_KEY = 0x20;
  int IPROTO_LIMIT = 0x12;
  int IPROTO_OK = 0x00;
  int IPROTO_POSITION = 0x35;
  int IPROTO_REQUEST_TYPE = 0x00;
  int IPROTO_SCHEMA_VERSION = 0x05;
  int IPROTO_SELECT = 0x01;
  int IPROTO_SPACE_ID = 0x10;
  int IPROTO_SPACE_NAME = 0x5e;
  int IPROTO_STMT_ID = 0x43;
  int IPROTO_STREAM_ID = 0x0a;
  int IPROTO_SYNC_ID = 0x01;
  int IPROTO_TUPLE_FORMATS = 0x60;
  int IPROTO_VERSION = 0x54;

  int IPROTO_TYPE_SELECT = 0x01;
  int IPROTO_TYPE_INSERT = 0x02;
  int IPROTO_TYPE_REPLACE = 0x03;
  int IPROTO_TYPE_UPDATE = 0x04;
  int IPROTO_TYPE_DELETE = 0x05;
  int IPROTO_TYPE_AUTH = 0x07;
  int IPROTO_TYPE_EVAL = 0x08;
  int IPROTO_TYPE_UPSERT = 0x09;
  int IPROTO_TYPE_CALL = 0x0a;
  int IPROTO_TYPE_EXECUTE = 0x0b;
  int IPROTO_TYPE_PREPARE = 0x0d;
  int IPROTO_TYPE_BEGIN = 0x0e;
  int IPROTO_TYPE_COMMIT = 0x0f;
  int IPROTO_TYPE_ROLLBACK = 0x10;
  int IPROTO_TYPE_PING = 0x40;
  int IPROTO_TYPE_ID = 0x49;
  int IPROTO_TYPE_WATCH = 0x4a;
  int IPROTO_TYPE_UNWATCH = 0x4b;
  int IPROTO_TYPE_WATCH_ONCE = 0x4d;

  byte IPROTO_EXT_DECIMAL = 0x01;
  byte IPROTO_EXT_UUID = 0x02;
  byte IPROTO_EXT_DATETIME = 0x04;
  byte IPROTO_EXT_INTERVAL = 0x06;
  byte IPROTO_EXT_TUPLE = 0x07;

  Value MP_ERROR_ERRCODE = ValueFactory.newInteger(0x05);
  Value MP_ERROR_ERRNO = ValueFactory.newInteger(0x04);
  Value MP_ERROR_FIELDS = ValueFactory.newInteger(0x06);
  Value MP_ERROR_FILE = ValueFactory.newInteger(0x01);
  Value MP_ERROR_LINE = ValueFactory.newInteger(0x02);
  Value MP_ERROR_MESSAGE = ValueFactory.newInteger(0x03);
  Value MP_ERROR_STACK = ValueFactory.newInteger(0x00);
  Value MP_ERROR_TYPE = ValueFactory.newInteger(0x00);
  Value MP_IPROTO_ERROR = ValueFactory.newInteger(IPROTO_ERROR);
  Value MP_IPROTO_ERROR_24 = ValueFactory.newInteger(IPROTO_ERROR_24);
  Value MP_IPROTO_EVENT_KEY = ValueFactory.newInteger(IPROTO_EVENT_KEY);
  Value MP_IPROTO_EXPR = ValueFactory.newInteger(0x27);
  Value MP_IPROTO_FEATURES = ValueFactory.newInteger(IPROTO_FEATURES);
  Value MP_IPROTO_FUNCTION_NAME = ValueFactory.newInteger(0x22);
  Value MP_IPROTO_INDEX_BASE = ValueFactory.newInteger(0x15);
  Value MP_IPROTO_INDEX_ID = ValueFactory.newInteger(IPROTO_INDEX_ID);
  Value MP_IPROTO_INDEX_NAME = ValueFactory.newInteger(IPROTO_INDEX_NAME);
  Value MP_IPROTO_ITERATOR = ValueFactory.newInteger(0x14);
  Value MP_IPROTO_KEY = ValueFactory.newInteger(IPROTO_KEY);
  Value MP_IPROTO_LIMIT = ValueFactory.newInteger(IPROTO_LIMIT);
  Value MP_IPROTO_OFFSET = ValueFactory.newInteger(0x13);
  Value MP_IPROTO_OPS = ValueFactory.newInteger(0x28);
  Value MP_IPROTO_OPTIONS = ValueFactory.newInteger(0x2b);
  Value MP_IPROTO_REQUEST_TYPE = ValueFactory.newInteger(IPROTO_REQUEST_TYPE);
  Value MP_IPROTO_SPACE_ID = ValueFactory.newInteger(IPROTO_SPACE_ID);
  Value MP_IPROTO_SPACE_NAME = ValueFactory.newInteger(IPROTO_SPACE_NAME);
  Value MP_IPROTO_SQL_BIND = ValueFactory.newInteger(0x41);
  Value MP_IPROTO_SQL_TEXT = ValueFactory.newInteger(0x40);
  Value MP_IPROTO_STMT_ID = ValueFactory.newInteger(IPROTO_STMT_ID);
  Value MP_IPROTO_STREAM_ID = ValueFactory.newInteger(IPROTO_STREAM_ID);
  Value MP_IPROTO_SYNC_ID = ValueFactory.newInteger(IPROTO_SYNC_ID);
  Value MP_IPROTO_TIMEOUT = ValueFactory.newInteger(0x56);
  Value MP_IPROTO_TUPLE = ValueFactory.newInteger(0x21);
  Value MP_IPROTO_TXN_ISOLATION = ValueFactory.newInteger(0x59);
  Value MP_IPROTO_USER_NAME = ValueFactory.newInteger(0x23);
  Value MP_IPROTO_VERSION = ValueFactory.newInteger(IPROTO_VERSION);

  byte[] RAW_IPROTO_AFTER_POSITION = new byte[]{46};
  byte[] RAW_IPROTO_AFTER_TUPLE = new byte[]{47};
  byte[] RAW_IPROTO_EVENT_KEY = new byte[]{87};
  byte[] RAW_IPROTO_EXPR = new byte[]{39};
  byte[] RAW_IPROTO_FEATURES = new byte[]{85};
  byte[] RAW_IPROTO_FETCH_POSITION = new byte[]{31};
  byte[] RAW_IPROTO_FUNCTION_NAME = new byte[]{34};
  byte[] RAW_IPROTO_SPACE_ID = new byte[]{16};
  byte[] RAW_IPROTO_SPACE_NAME = new byte[]{94};
  byte[] RAW_IPROTO_INDEX_BASE = new byte[]{21};
  byte[] RAW_IPROTO_INDEX_ID = new byte[]{17};
  byte[] RAW_IPROTO_INDEX_NAME = new byte[]{95};
  byte[] RAW_IPROTO_ITERATOR = new byte[]{20};
  byte[] RAW_IPROTO_KEY = new byte[]{32};
  byte[] RAW_IPROTO_LIMIT = new byte[]{18};
  byte[] RAW_IPROTO_OFFSET = new byte[]{19};
  byte[] RAW_IPROTO_OPS = new byte[]{40};
  byte[] RAW_IPROTO_OPTIONS = new byte[]{43};
  byte[] RAW_IPROTO_REQUEST_TYPE = new byte[]{0};
  byte[] RAW_IPROTO_SQL_BIND = new byte[]{65};
  byte[] RAW_IPROTO_SQL_TEXT = new byte[]{64};
  byte[] RAW_IPROTO_STMT_ID = new byte[]{67};
  byte[] RAW_IPROTO_SYNC_ID = new byte[]{1};
  byte[] RAW_IPROTO_TIMEOUT = new byte[]{86};
  byte[] RAW_IPROTO_TUPLE = new byte[]{33};
  byte[] RAW_IPROTO_TXN_ISOLATION = new byte[]{89};
  byte[] RAW_IPROTO_USER_NAME = new byte[]{35};
  byte[] RAW_IPROTO_VERSION = new byte[]{84};
  byte[] RAW_MAP_HEADER_WITH_EIGHT_ITEMS = new byte[]{-120};
  byte[] RAW_MAP_HEADER_WITH_FOUR_ITEMS = new byte[]{-124};
  byte[] RAW_MAP_HEADER_WITH_ONE_ITEM = new byte[]{-127};
  byte[] RAW_MAP_HEADER_WITH_SEVEN_ITEMS = new byte[]{-121};
  byte[] RAW_MAP_HEADER_WITH_SIX_ITEMS = new byte[]{-122};
  byte[] RAW_MAP_HEADER_WITH_THREE_ITEMS = new byte[]{-125};
  byte[] RAW_MAP_HEADER_WITH_THREE_ITEMS_PLUS_IPROTO_STREAM_ID = new byte[]{-125, 10};
  byte[] RAW_MAP_HEADER_WITH_TWO_ITEMS = new byte[]{-126};
  byte[] RAW_UINT32_RESERVED_FOR_SIZE = new byte[]{-50, 0, 0, 0, 0};

  byte[] RAW_IPROTO_TYPE_SELECT = new byte[]{1};
  byte[] RAW_IPROTO_TYPE_INSERT = new byte[]{2};
  byte[] RAW_IPROTO_TYPE_REPLACE = new byte[]{3};
  byte[] RAW_IPROTO_TYPE_UPDATE = new byte[]{4};
  byte[] RAW_IPROTO_TYPE_DELETE = new byte[]{5};
  byte[] RAW_IPROTO_TYPE_AUTH = new byte[]{7};
  byte[] RAW_IPROTO_TYPE_EVAL = new byte[]{8};
  byte[] RAW_IPROTO_TYPE_UPSERT = new byte[]{9};
  byte[] RAW_IPROTO_TYPE_CALL = new byte[]{10};
  byte[] RAW_IPROTO_TYPE_EXECUTE = new byte[]{11};
  byte[] RAW_IPROTO_TYPE_PREPARE = new byte[]{13};
  byte[] RAW_IPROTO_TYPE_BEGIN = new byte[]{14};
  byte[] RAW_IPROTO_TYPE_COMMIT = new byte[]{15};
  byte[] RAW_IPROTO_TYPE_ROLLBACK = new byte[]{16};
  byte[] RAW_IPROTO_TYPE_PING = new byte[]{64};
  byte[] RAW_IPROTO_TYPE_ID = new byte[]{73};
  byte[] RAW_IPROTO_TYPE_WATCH = new byte[]{74};
  byte[] RAW_IPROTO_TYPE_UNWATCH = new byte[]{75};
  byte[] RAW_IPROTO_TYPE_WATCH_ONCE = new byte[]{77};
}
