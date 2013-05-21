package com.j2trp.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "J2TRP")
public class ToolboxTest {
	public void testMerge() {
		Integer[] org = new Integer[3];
		org[0] = 1;
		org[1] = 2;
		org[2] = 3;
		
		Assert.assertEquals(3, Toolbox.merge(org, null, Integer[].class).length);
		
		Integer[] added = new Integer[3];
		added[0] = 4;
		added[1] = 5;
		added[2] = 6;
		
		Assert.assertEquals(6, Toolbox.merge(org, added, Integer[].class).length);
	}
	
	public void testGetValue() {
		Integer one = 1;
		Integer two = 2;
		
		Assert.assertEquals(one, Toolbox.getValue(one, null, Integer.class));
		Assert.assertEquals(two, Toolbox.getValue(one, two, Integer.class));
	}
	
	public void testGetValueWithDefault() {
		Long defaultValue = -1L;
		
		Assert.assertEquals(defaultValue, Toolbox.getValue(null, null, -1L, Long.class));
		Assert.assertSame(defaultValue, Toolbox.getValue(null, null, defaultValue, Long.class));
	}
	
	public void testMergeCollection() {
		Enumeration<String> enum123 = Collections.enumeration(Arrays.asList("1", "2", "3"));
		Enumeration<String> enum456 = Collections.enumeration(Arrays.asList("4", "5", "6"));
		Enumeration<String> control = Collections.enumeration(Arrays.asList("1", "2", "3", "4", "5", "6"));
		
		Enumeration<String> enumUnderTest = Toolbox.mergeCollection(enum123, enum456);
		
		Assert.assertTrue(enumUnderTest.hasMoreElements());
		
		while (control.hasMoreElements()) {
			Assert.assertEquals(enumUnderTest.nextElement(), control.nextElement());
		}
		
		enum123 = Collections.enumeration(Arrays.asList("1", "2", "3"));
		control = enum123;
		Assert.assertSame(control, Toolbox.mergeCollection(enum123, null));
		
		
		Enumeration<?> testResult = Toolbox.mergeCollection(null, null);
	}
	
}
