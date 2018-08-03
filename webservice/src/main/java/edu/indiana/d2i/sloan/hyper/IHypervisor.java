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

import edu.indiana.d2i.sloan.bean.VmInfoBean;

interface IHypervisor {
	public HypervisorResponse createVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse launchVM(VmInfoBean vminfo, String pubKey) throws Exception;

	public HypervisorResponse queryVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse switchVM(VmInfoBean vminfo, String pubKey) throws Exception;

	public HypervisorResponse stopVM(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse delete(VmInfoBean vminfo) throws Exception;

	public HypervisorResponse updatePubKey(VmInfoBean vminfo, String pubKey) throws Exception;
}
