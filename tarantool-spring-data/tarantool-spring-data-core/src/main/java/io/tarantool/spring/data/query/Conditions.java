/*
 * Copyright (c) 2025 VK Company Limited.
 * All Rights Reserved.
 */

package io.tarantool.spring.data.query;

import java.util.ArrayList;
import java.util.List;

import io.tarantool.client.crud.Condition;

/**
 * @author Artyom Dubinin
 */
public class Conditions extends ArrayList<Condition> implements List<Condition> {}
