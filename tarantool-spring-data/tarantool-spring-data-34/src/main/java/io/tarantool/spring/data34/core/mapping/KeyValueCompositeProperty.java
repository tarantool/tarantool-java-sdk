/*
 * Copyright (c) 2025 VK DIGITAL TECHNOLOGIES LIMITED LIABILITY COMPANY
 * All Rights Reserved.
 */

package io.tarantool.spring.data34.core.mapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.data.keyvalue.core.mapping.KeyValuePersistentProperty;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.Assert;

public class KeyValueCompositeProperty<P extends KeyValuePersistentProperty<P>>
    extends KeyValuePersistentProperty<P> implements Identifier<P> {

  private final List<P> identifierPartsWithoutFirst;

  public KeyValueCompositeProperty(
      Property property, PersistentEntity<?, P> owner, SimpleTypeHolder simpleTypeHolder) {
    super(property, owner, simpleTypeHolder);
    this.identifierPartsWithoutFirst = new ArrayList<>();
  }

  @Override
  public void addPart(P property) {
    Assert.notNull(property, "property must be not null");

    if (equals(property) || this.identifierPartsWithoutFirst.contains(property)) {
      return;
    }
    this.identifierPartsWithoutFirst.add(property);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<P> getParts() {
    List<P> resultList = new ArrayList<>(this.identifierPartsWithoutFirst);
    resultList.add(0, (P) this);

    return resultList;
  }

  @Override
  public Field[] getFields() {
    int totalSize = this.identifierPartsWithoutFirst.size() + 1;

    final Field[] fields = new Field[totalSize];
    fields[0] = getField();

    for (int i = 1; i < totalSize; i++) {
      fields[i] = this.identifierPartsWithoutFirst.get(i - 1).getField();
    }
    return fields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KeyValueCompositeProperty<?> other = (KeyValueCompositeProperty<?>) o;

    if (this.identifierPartsWithoutFirst.isEmpty() && other.identifierPartsWithoutFirst.isEmpty()) {
      return super.equals(o);
    }

    if (this.identifierPartsWithoutFirst.size() != other.identifierPartsWithoutFirst.size()
        || !other.getProperty().equals(getProperty())) {
      return false;
    }

    for (int i = 0; i < this.identifierPartsWithoutFirst.size(); i++) {
      // take Property from both parts
      Property thisKeyPartProperty =
          ((KeyValueCompositeProperty<?>) this.identifierPartsWithoutFirst.get(i)).getProperty();
      Property otherKeyPartProperty =
          ((KeyValueCompositeProperty<?>) other.identifierPartsWithoutFirst.get(i)).getProperty();

      if (!thisKeyPartProperty.equals(otherKeyPartProperty)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = super.hashCode();

    if (this.identifierPartsWithoutFirst.isEmpty()) {
      return hashCode;
    }

    // sublist to avoid endless recursion
    return Objects.hash(hashCode, this.identifierPartsWithoutFirst);
  }
}
