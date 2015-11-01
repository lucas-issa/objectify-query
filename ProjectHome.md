
# Introduction #
## Why? ##
[objectify-appengine](http://code.google.com/p/objectify-appengine/) is a simple powerful library that makes using the appengine datastore easy, but the filtering concerns me.  If I make a spelling mistake in my filtering or refactor some code (such as changing a field from indexed to unindexed), incorrect code will still compile.

## Features ##
objectify-query is a JSR 269 annotation processor that processes objectify-appengine's annotations to generate "safer" query classes.  The generated query class provides type-safe methods to filter each of the entities indexed properties.
Code won't compile if:
  * the field name is changed.
  * the field is unindexed and you are trying to filter against it.
objectify-query can also
  * return the list of parents entities.
  * return the list of related entities.
  * return data a "page" at a time, which is useful when a gwt client is requesting a list of data over rpc requests.

## Is this useful? ##

I created this library primarily for my own usage.  If you find it useful please let me know in the discussion groups, and suggest improvements by raising issues.

# Usage #
Given the following class:
```
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
}
```
I can use generated Query class to filter data and return "pages" of data:
```
Objectify ofy = ObjectifyService.begin();
Key<Colour> red = ofy.get(Colour.class, "red");
// use "strong" filterBy methods
CarQuery query = (new CarQuery(ofy().query(Car.class))).filterByColour(red).filterByDoors(2);

// retrieve a "page" of cars
ListPage<Car> cars = query.list(null, 20);
for(Car car : cars) {
  // ...
}

// is this page the last page?
if(cars.more()) {
  ListPage<Car> moreCars = query.list(cars.cursor(), 20);
  // ...
}

// list related entities - manufacturers of red cars with 2 doors
ListPage<Manufacturer> whoMakesRedCars = query.listManufacturers(null, 20);
```
See GeneratedOutput for full source code of the generated class.

# Project configuration #
## With maven ##
```
<dependency>
   <groupId>com.googlecode.objectify.query</groupId>
   <artifactId>objectify-query</artifactId>
   <version>0.1</version>
</dependency>
```

I find maven-processor-plugin is more reliable than using
maven-complier-plugin for annotation processing, so you may want to add the following plugins to your pom.xml.

```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-compiler-plugin</artifactId>
	<version>${maven-compiler-plugin.version}</version>
	<configuration>
		<source>1.6</source>
		<target>1.6</target>
		<proc>none</proc>  <!-- use maven-processor-plugin instead -->
	</configuration>
</plugin>

<plugin>
	<groupId>org.bsc.maven</groupId>
	<artifactId>maven-processor-plugin</artifactId>
	<version>${maven-processor-plugin.version}</version>
	<executions>
		<!-- Run annotation processors on src/main/java sources -->
		<execution>
			<id>process</id>
			<goals>
				<goal>process</goal>
			</goals>
			<phase>generate-sources</phase>
		</execution>
		<!-- Run annotation processors on src/test/java sources -->
		<execution>
			<id>process-test</id>
			<goals>
				<goal>process-test</goal>
			</goals>
			<phase>generate-test-sources</phase>
		</execution>
	</executions>
</plugin>
```

If you are using both eclipse and maven, you might want these lines to you maven-eclipse-plugin configuration.
```
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-eclipse-plugin</artifactId>
	<version>${maven-eclipse-plugin.version}</version>
	<configuration>
		...
		<additionalConfig>
			<file>
				<name>.factorypath</name>
				<content><![CDATA[<factorypath>
<factorypathentry kind="VARJAR" id="M2_REPO/com/googlecode/objectify/objectify/${objectify.version}/objectify-${objectify.version}.jar" enabled="true" runInBatchMode="true" />
<factorypathentry kind="VARJAR" id="M2_REPO/com/googlecode/objectify-query/objectify-query/${objectify-query.version}/objectify-query-${objectify-query.version}.jar" enabled="true" runInBatchMode="true"/>
</factorypath>]]></content>
			</file>
			<file>
				<name>.settings/org.eclipse.jdt.apt.core.prefs</name>
				<content><![CDATA[eclipse.preferences.version=1
org.eclipse.jdt.apt.aptEnabled=true
org.eclipse.jdt.apt.genSrcDir=target/generated-sources/apt
org.eclipse.jdt.apt.reconcileEnabled=true ]]></content>
			</file>
		</additionalConfig>
	</configuration>
</plugin>

```

## With eclipse ##

In Eclipse, the annotation processor kicks in as soon as you save the file you're working on and incrementally changes only the required files.

To enable objectify-query annotation processing in eclipse:
  1. Open the properties for your project
  1. Ticking all the boxes on the Annotation Processing page (under Java Compiler). <br />![http://img138.imageshack.us/img138/6223/eclipseannotationproces.png](http://img138.imageshack.us/img138/6223/eclipseannotationproces.png)
  1. Add the objectify and objectify-query jars to the Factory Path. <br />![http://img135.imageshack.us/img135/9900/eclipse1.png](http://img135.imageshack.us/img135/9900/eclipse1.png)
