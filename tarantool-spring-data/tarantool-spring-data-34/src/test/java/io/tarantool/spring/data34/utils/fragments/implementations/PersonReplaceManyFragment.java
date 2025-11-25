/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.utils.fragments.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.mapping.Tuple;
import io.tarantool.mapping.crud.CrudBatchResponse;
import io.tarantool.spring.data34.utils.entity.Person;
import io.tarantool.spring.data34.utils.fragments.core.ReplaceManyFragment;

public class PersonReplaceManyFragment implements ReplaceManyFragment<Person> {

  private final TarantoolCrudClient client;

  public PersonReplaceManyFragment(TarantoolCrudClient client) {
    this.client = client;
  }

  @Override
  public List<Person> replaceMany(List<Person> tuples) {
    return getResults(this.client.space("person").replaceMany(tuples, Person.class));
  }

  private static List<Person> getResults(
      CompletableFuture<CrudBatchResponse<List<Tuple<Person>>>> tuples) {
    return new ArrayList<>(tuples.join().getRows().stream().map(Tuple::get).toList());
  }
}
