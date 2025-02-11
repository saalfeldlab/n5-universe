/**
 * Copyright (c) 2017, Stephan Saalfeld
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.universe.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtils {

	public static <T> void setFieldValue(
			final Object object,
			final String fieldName,
			final T value) throws NoSuchFieldException, IllegalAccessException {

		Field modifiersField;
		boolean isModifiersAccessible;
		try {
			modifiersField = Field.class.getDeclaredField("modifiers");
			isModifiersAccessible = modifiersField.isAccessible();
			modifiersField.setAccessible(true);
		} catch (final NoSuchFieldException e) {
			// Java 11+ does not allow to access modifiers
			modifiersField = null;
			isModifiersAccessible = false;
		}

		final Field field = getHierarchyField(object.getClass(), fieldName);
		final boolean isFieldAccessible = field.isAccessible();
		field.setAccessible(true);

		if (modifiersField != null) {
			final int modifiers = field.getModifiers();
			modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
			field.set(object, value);
			modifiersField.setInt(field, modifiers);
		} else {
			field.set(object, value);
		}

		field.setAccessible(isFieldAccessible);
		if (modifiersField != null) {
			modifiersField.setAccessible(isModifiersAccessible);
		}
	}

	
	public static Field getHierarchyField(Class<?> clazz, final String fieldName ) throws NoSuchFieldException {

		Class<?> current = clazz;
		NoSuchFieldException firstException = new NoSuchFieldException("weird");
		while (!current.equals(Object.class)) {

			try {
				return current.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				firstException = e;
				current = current.getSuperclass();
			} catch (SecurityException e) {
				return null;
			}

		}
		throw firstException;
	}
}
