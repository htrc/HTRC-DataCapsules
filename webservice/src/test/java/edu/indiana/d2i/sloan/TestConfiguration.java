/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.indiana.d2i.sloan;

import junit.framework.Assert;

import org.junit.Test;

public class TestConfiguration {
	
	@Test
	public void testConfiguration() {
		Configuration.getInstance();
		
		System.out.println(Configuration.getInstance().getString(
			Configuration.PropertyName.RESOURCES_NAMES));
		
		Assert.assertEquals("edu.indiana.d2i.sloan.hyper.CapsuleHypervisor", 
			Configuration.getInstance().getString(Configuration.PropertyName.HYPERVISOR_FULL_CLASS_NAME));
		Assert.assertEquals(20480, Integer.valueOf(Configuration.getInstance().
			getString(Configuration.PropertyName.USER_MEMORY_QUOTA_IN_MB)).intValue());
	}
}
