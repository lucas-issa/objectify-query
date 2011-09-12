package com.googlecode.objectify.query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.example.Car;
import com.example.CarQuery;
import com.example.Colour;
import com.example.Manufacturer;
import com.example.ManufacturerQuery;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.query.shared.ListPage;

public class TestCarQuery {

  static {
    ObjectifyService.register(Car.class);
    ObjectifyService.register(Colour.class);
    ObjectifyService.register(Manufacturer.class);
  }

  private LocalServiceTestHelper helper;

  @Before
  public void prepare() {
    this.helper = new LocalServiceTestHelper(
        new LocalDatastoreServiceTestConfig());
    this.helper.setUp();
    Objectify ofy = ObjectifyService.begin();

    Colour red = new Colour();
    red.id = "red";
    Key<Colour> redKey = ofy.put(red);

    Manufacturer honda = new Manufacturer();
    honda.name = "honda";
    Key<Manufacturer> hondaKey = ofy.put(honda);

    Manufacturer toyota = new Manufacturer();
    toyota.name = "toyota";
    Key<Manufacturer> toyotaKey = ofy.put(toyota);

    Car car1 = new Car();
    car1.doors = 2;
    car1.colour = redKey;
    car1.vin = "ABC123";
    car1.madeBy = hondaKey;
    ofy.put(car1);

    Car car2 = new Car();
    car2.doors = 2;
    car2.colour = redKey;
    car2.vin = "DEF123";
    car2.madeBy = hondaKey;
    ofy.put(car2);

    Car car3 = new Car();
    car3.doors = 2;
    car3.colour = redKey;
    car3.madeBy = toyotaKey;
    car3.vin = "GHI123";
    ofy.put(car3);

    Car car4 = new Car();
    car4.doors = 2;
    car4.colour = redKey;
    car4.vin = "JKL123";
    car4.madeBy = toyotaKey;
    ofy.put(car4);

    Car car5 = new Car();
    car5.doors = 4;
    car5.colour = redKey;
    car5.vin = "MNO123";
    car5.madeBy = toyotaKey;
    ofy.put(car5);
  }

  @After
  public void releaseLocalServices() {
    this.helper.tearDown();
  }

  @Test
  public void testCarQuery() {
    System.out.println("testCarQuery begin");
    Objectify ofy = ObjectifyService.begin();

    Key<Colour> red = new Key(Colour.class, "red");

    // use "strong" filterBy methods
    CarQuery query = (new CarQuery(ofy.query(Car.class))).filterByColour(red).filterByDoors(
        2);

    // retrieve a "page" of cars
    ListPage<Car> cars = query.list(null, 3);
    assertThat(cars.size(), is(equalTo(3)));
    assertThat(cars.more(), is(true));

    for (Car car : cars) {
      assertThat(car.colour, is(red));
    }

    // is this page the last page?
    ListPage<Car> moreCars = query.list(cars.getCursor(), 20);
    assertThat(cars.size(), is(equalTo(3)));
    assertThat(cars.more(), is(true));

    // list related entities - manufacturers of red cars with 2 doors
    CarQuery query2 = (new CarQuery(ofy.query(Car.class))).filterByColour(red).filterByDoors(
        2);
    ListPage<Manufacturer> whoMakesRedCars = query2.listManufacturers(null, 20);
    assertThat(whoMakesRedCars.size(), is(equalTo(2)));
    assertThat(whoMakesRedCars.more(), is(false));

    System.out.println("testCarQuery done");
    
    ManufacturerQuery mq =(new ManufacturerQuery(ofy.query(Manufacturer.class)));
    ListPage<Manufacturer> manufacturerList =  mq.list(null, 20);
    assertThat(manufacturerList.size(), is(equalTo(2)));
  }

}
