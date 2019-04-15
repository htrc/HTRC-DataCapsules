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
package edu.indiana.d2i.sloan.bean;

import java.util.List;

public class VmStatusBean {
	private final VmInfoBean vminfo;
	private final boolean pub_key_exists;
	private final boolean tou;
	private final boolean vm_tou;
	private final Boolean user_full_access;
	private final String role;

	public VmStatusBean(VmInfoBean vminfo, boolean ssh_key_exists, boolean tou, VmUserRole vmUserRole) {
		this.vminfo = vminfo;
		this.pub_key_exists = ssh_key_exists;
		this.tou = tou;
		this.vm_tou = vmUserRole.getTou();
		this.role = vmUserRole.getRole().toString();
		this.user_full_access = vmUserRole.isFull_access();
	}
	
	public String getMode() {
		return vminfo.getVmmode().toString();
	}
	public String getState() {
		return vminfo.getVmstate().toString();
	}
	public String getPublicip() {
		return vminfo.getPublicip();
	}
	public String getCreated_at() {
		return vminfo.getCreated_at();
	}
	public int getVncport() {
		return vminfo.getVncport();
	}
	public int getSshport() {
		return vminfo.getSshport();
	}
	public String getVmid() {
		return vminfo.getVmid();
	}
	public String getVNCLoginId() {
		return vminfo.getVNCloginId();
	}
	public String getVNCLoginPassword() {
		return vminfo.getVNCloginPwd();
	}
	public int getVCPUs() {
		return vminfo.getNumCPUs();
	}
	public int getMemSize() {
		return vminfo.getMemorySizeInMB();
	}
	public int getVolumeSize() {
		return vminfo.getVolumeSizeInGB();
	}
	public String getImageName() {
		return vminfo.getImageName();
	}
	public String getVmInitialLoginId() {
		return vminfo.getVmLoginId();
	}
	public String getVmInitialLoginPassword() {
		return vminfo.getVmLoginPwd();
	}
	public boolean getPubKeyExists() {
		return pub_key_exists;
	}
	public String getType() {
		return vminfo.getType();
	}
	public String getTitle() {
		return vminfo.getTitle();
	}
	public Boolean isConsent() {
		return vminfo.isConsent();
	}
	public Boolean isFull_access() {
		return vminfo.isFull_access();
	}
	public Boolean isUser_full_access() {
		return user_full_access;
	}
	public String getDesc_nature() {
		return vminfo.getDesc_nature();
	}
	public String getDesc_requirement() {
		return vminfo.getDesc_requirement();
	}
	public String getDesc_links() {
		return vminfo.getDesc_links();
	}
	public String getDesc_outside_data() {
		return vminfo.getDesc_outside_data();
	}
	public String getRr_data_files() {
		return vminfo.getRr_data_files();
	}
	public String getRr_result_usage() {
		return vminfo.getRr_result_usage();
	}
	public String getDesc_shared() {
		return vminfo.getDesc_shared();
	}
	public boolean getTou() {
		return tou;
	}
	public boolean getVm_tou() {
		return vm_tou;
	}
	public String getRole() {
		return role;
	}
	public List<VmUserRole> getRoles() {
		return vminfo.getRoles();
	}
}
