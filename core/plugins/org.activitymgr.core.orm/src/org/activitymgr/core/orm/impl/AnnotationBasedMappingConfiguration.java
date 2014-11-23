package org.activitymgr.core.orm.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.activitymgr.core.orm.IConverter;
import org.activitymgr.core.orm.annotation.AutoGenerated;
import org.activitymgr.core.orm.annotation.Column;
import org.activitymgr.core.orm.annotation.ColumnNamePrefix;
import org.activitymgr.core.orm.annotation.Converter;
import org.activitymgr.core.orm.annotation.PrimaryKey;
import org.activitymgr.core.orm.annotation.Table;

public class AnnotationBasedMappingConfiguration implements IMappgingConfiguration {

	@Override
	public String getSQLTableName(Class<?> theClass) {
		Table annotation = getClassAnnotation(theClass, Table.class);
		return annotation != null ? annotation.value() : theClass.getSimpleName().toUpperCase();
	}

	@Override
	public String getSQLColumnName(Class<?> theClass, Field attribute) {
		String columnName = null;
		Column annotation = attribute.getAnnotation(Column.class);
		if (annotation != null) {
			columnName = annotation.value();
		}
		else {
			columnName = attribute.getName().toUpperCase();
		}
		ColumnNamePrefix prefixAnnotation = getClassAnnotation(theClass, ColumnNamePrefix.class);
		if (prefixAnnotation != null) {
			columnName = prefixAnnotation.value() + columnName;
		}
		return columnName;
	}

	@Override
	public List<Field> getPrimaryKeyAttributes(Class<?> theClass) {
		List<Field> result = new ArrayList<Field>();
		for (Field field : ReflectionHelper.getFields(theClass)) {
			PrimaryKey annotation = field.getAnnotation(PrimaryKey.class);
			if (annotation != null) {
				result.add(field);
			}
		}
		return result;
	}

	@Override
	public Field getAutoGeneratedAttribute(Class<?> theClass) {
		for (Field field : ReflectionHelper.getFields(theClass)) {
			AutoGenerated annotation = field.getAnnotation(AutoGenerated.class);
			if (annotation != null) {
				return field;
			}
		}
		return null;
	}

	@Override
	public Class<? extends IConverter<?>> getAttributeConverter(
			Class<?> theClass, Field attribute) {
		Converter annotation = attribute.getAnnotation(Converter.class);
		return annotation != null ? annotation.value() : null;
	}

	private <TYPE extends Annotation> TYPE getClassAnnotation(Class<?> theClass, Class<TYPE> annotationClass) {
		TYPE annotation = null;
		if (theClass.getSuperclass() != null) {
			annotation = getClassAnnotation(theClass.getSuperclass(), annotationClass);
		}
		if (annotation == null) {
			annotation = theClass.getAnnotation(annotationClass);
		}
		return annotation;
	}

}
