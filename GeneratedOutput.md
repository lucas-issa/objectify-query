# Entity class #

```
package com.example;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.query.annotation.List;

@Entity
class Car {
  private @Id String vin; 
  private Colour colour;
  private int doors;
  private Key<Manufacturer> madeBy;

  @List(singularName = "MadeBy", pluralName = "Manufacturers")
  public Key<Manufacturer> getManufacturer() {
    return this.madeBy;
  }
  
  // ...
}
```


# Query class #

```
package com.example;

import java.util.ArrayList;
import java.util.Map;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.helper.QueryWrapper;
import com.googlecode.objectify.query.shared.ListPage;
import com.example.Car;

public class CarQuery extends QueryWrapper<Car> { 

  private final Query<Car> query;
  private Objectify lazyOfy;

  public CarQuery(Query<Car> query) {
    super(query);
    this.query = query;
  }

  public CarQuery filterByVin(java.lang.String vin) {
    this.query.filter("vin", vin);
    return this;
  }

  public CarQuery filterByVin(String operation, Object value) {
    this.query.filter("vin " + operation, value);
    return this;
  }

  public CarQuery filterByColour(com.example.Colour colour) {
    this.query.filter("colour", colour);
    return this;
  }

  public CarQuery filterByColour(String operation, Object value) {
    this.query.filter("colour " + operation, value);
    return this;
  }

  public CarQuery filterByDoors(int doors) {
    this.query.filter("doors", doors);
    return this;
  }

  public CarQuery filterByDoors(String operation, Object value) {
    this.query.filter("doors " + operation, value);
    return this;
  }

  public CarQuery filterByMadeBy(com.googlecode.objectify.Key<com.example.Manufacturer> madeBy) {
    this.query.filter("madeBy", madeBy);
    return this;
  }

  public CarQuery filterByMadeBy(String operation, Object value) {
    this.query.filter("madeBy " + operation, value);
    return this;
  }

  public ListPage<Car> list(String cursor, int pageSize) {
    if (cursor != null) {
      this.query.startCursor(Cursor.fromWebSafeString(cursor));
    }
    QueryResultIterator<Car> iterator = this.query.iterator();
    boolean more = false;
    ArrayList<Car> list = new ArrayList<Car>();
    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {
      list.add(iterator.next());
    }
    return new ListPage<Car>(list, iterator.getCursor().toWebSafeString(), more);
  }

  public ListPage<Key<Car>> listKeys(String cursor, int pageSize) {
    if (cursor != null) {
      this.query.startCursor(Cursor.fromWebSafeString(cursor));
    }
    QueryResultIterator<Key<Car>> iterator = this.query.fetchKeys().iterator();
    boolean more = false;
    ArrayList<Key<Car>> list = new ArrayList<Key<Car>>();
    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {
      list.add(iterator.next());
    }
    return new ListPage<Key<Car>>(list, iterator.getCursor().toWebSafeString(), more);
  }
  public ListPage<Key<com.example.Manufacturer>> listMadeByKeys(String cursor, int pageSize) {

    if (cursor != null) {
      this.query.startCursor(Cursor.fromWebSafeString(cursor));
    }
    QueryResultIterator<Car> iterator = query.iterator();

    boolean more = false;
    ArrayList<Key<com.example.Manufacturer>> idList = new ArrayList<Key<com.example.Manufacturer>>();
    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {
      idList.add(iterator.next().getManufacturer());
    }

    return new ListPage<Key<com.example.Manufacturer>>(idList, iterator.getCursor()
        .toWebSafeString(), more);
  }

  public ListPage<com.example.Manufacturer> listManufacturers(String cursor, int pageSize) {
    if (cursor != null) {
      this.query.startCursor(Cursor.fromWebSafeString(cursor));
    }
    QueryResultIterator<Car> iterator = query.iterator();

    boolean more = false;
    ArrayList<Long> idList = new ArrayList<Long>();
    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {
      idList.add(iterator.next().getManufacturer().getId());
    }

    Map<Long, com.example.Manufacturer> list =
      ofy().get(com.example.Manufacturer.class, idList);
    return new ListPage<com.example.Manufacturer>(new ArrayList<com.example.Manufacturer>(
      list.values()), iterator.getCursor().toWebSafeString(), more);
  }

  protected Objectify ofy() {
    if (this.lazyOfy == null) {
      ObjectifyOpts opts = new ObjectifyOpts().setSessionCache(true);
      this.lazyOfy = ObjectifyService.factory().begin(opts);
    }
    return this.lazyOfy;
  }

}

```