package com.example;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.query.annotation.List;


@Entity
public class Car {
  public @Id String vin; 
  public Key<Colour> colour;
  public int doors;
  public Key<Manufacturer> madeBy;
  
  @List(singularName = "MadeBy", pluralName = "Manufacturers")
  public Key<Manufacturer> getManufacturer() {
    return this.madeBy;
  }
}