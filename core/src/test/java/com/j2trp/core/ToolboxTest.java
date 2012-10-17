package com.j2trp.core;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
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
}
