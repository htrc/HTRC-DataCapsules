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
package edu.indiana.d2i.sloan.hyper;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import edu.indiana.d2i.sloan.utils.SSHProxy.CmdsExecResult;
import edu.indiana.d2i.sloan.utils.SSHProxy.Commands;

public class TestHypervisorResponse {
	
	@Test
	public void testCmdResultToResponse() {
		// normal screen output
		CmdsExecResult result1 = new CmdsExecResult(
			new Commands(Arrays.asList(new String[]{"command1", "command2"}), true), 
			"hostname", 0, "2 \n test screen output \n k1 \t v1 \n k2 \t v2");
		HypervisorResponse response1 = HypervisorResponse.commandRes2HyResp(result1);
		
		Assert.assertEquals(2, response1.getResponseCode());
		Assert.assertEquals("test screen output", response1.getDescription());
		Assert.assertTrue(response1.getAttributes().containsKey("k1"));
		Assert.assertTrue(response1.getAttributes().containsKey("k2"));
		
		// malformed screen output
		CmdsExecResult result2 = new CmdsExecResult(
			new Commands(Arrays.asList(new String[]{"command1", "command2"}), true), 
			"hostname", 0, "test screen output \n k1 \t v1 \n k2 \t v2");
		HypervisorResponse response2 = HypervisorResponse.commandRes2HyResp(result2);
		Assert.assertEquals(0, response2.getResponseCode());
		
		CmdsExecResult result3 = new CmdsExecResult(
			new Commands(Arrays.asList(new String[]{"command1", "command2"}), true), 
			"hostname", 0, "test screen output");
		HypervisorResponse response3 = HypervisorResponse.commandRes2HyResp(result3);
		Assert.assertEquals(0, response3.getResponseCode());
		Assert.assertEquals("", response3.getDescription());
	}
}
