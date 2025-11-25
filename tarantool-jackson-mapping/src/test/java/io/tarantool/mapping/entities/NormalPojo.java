/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.mapping.entities;

import java.math.BigInteger;

// TODO: need to have complex POJO rather than TestEntity

/**
 * @author Artyom Dubinin
 */
public class NormalPojo {

  public enum Suit {
    SPADE,
    HEART,
    DIAMOND,
    CLUB;
  }

  public boolean bool;
  public int i;
  public long l;
  public Float f;
  public Double d;
  public byte[] b;
  public BigInteger bi;
  public Suit suit;
  String s;

  public String getS() {
    return s;
  }

  public void setS(String s) {
    this.s = s;
  }
}
