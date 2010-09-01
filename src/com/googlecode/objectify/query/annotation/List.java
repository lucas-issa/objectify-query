package com.googlecode.objectify.query.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @param singularName
 * @param pluralName
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface List
{
	public enum KeyType {
		Id,
		Name
	}
	
	KeyType keyType() default KeyType.Id;
	String singularName();
	String pluralName();
}