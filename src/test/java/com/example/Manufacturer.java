package com.example;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Manufacturer {
	public @Id Long id;
	public String name;
}
