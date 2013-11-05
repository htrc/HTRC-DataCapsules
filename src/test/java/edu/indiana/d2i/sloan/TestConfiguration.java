package edu.indiana.d2i.sloan;

import junit.framework.Assert;

import org.junit.Test;

public class TestConfiguration {
	
	@Test
	public void testConfiguration() {
		Configuration.getInstance();
		Assert.assertEquals("edu.indiana.d2i.sloan.hyper.CapsuleHypervisor", 
			Configuration.getInstance().getString(Configuration.PropertyName.HYPERVISOR_FULL_CLASS_NAME));
		Assert.assertEquals(20480, Integer.valueOf(Configuration.getInstance().
			getString(Configuration.PropertyName.USER_MEMORY_QUOTA_IN_MB)).intValue());
	}
}
