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

import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

import java.util.List;

public class VmInfoBean {
	private String vmid, publicip, created_at, workDir, imagepath, policypath, vncloginId,
			vncloginPwd, imagename, policyname, vmloginid, vmloginpwd;
	private int sshport, vncport, numCPUs, memorySize, volumeSize;
	private VMMode vmmode;
	private VMMode requestedVMMode;
	private VMState vmstate;
	private Boolean consent;
	private Boolean full_access;
	private String type, title, desc_nature, desc_requirement, desc_links,
			desc_outside_data, rr_data_files, rr_result_usage, desc_shared;
	private List<VmUserRole> roles;

	public void setImagePath(String imagePath) {
		this.imagepath = imagePath;
	}
	
	public void setRequestedVMMode(VMMode requestedVMMode) {
		this.requestedVMMode = requestedVMMode;
	}
	
	public void setVmState(VMState vmstate) {
		this.vmstate = vmstate;
	}
	
	public void setPolicypath(String policypath) {
		this.policypath = policypath;
	}

	public VmInfoBean(String vmid, List<VmUserRole> roles, Boolean full_access) {
		this.vmid = vmid;
		this.roles = roles;
		this.publicip = "";
		this.workDir = "";
		this.imagepath = "";
		this.policypath = "";
		this.sshport = -1;
		this.vncport = -1;
		this.numCPUs = -1;
		this.memorySize = -1;
		this.volumeSize = -1;
		this.vmmode = VMMode.NOT_DEFINED;
		this.vmstate = VMState.NOT_DEFINED;
		this.vncloginId = "";
		this.vncloginPwd = "";
		this.imagename = "";
		this.policyname = "";
		this.requestedVMMode = VMMode.NOT_DEFINED;
		this.vmloginid = "";
		this.vmloginpwd = "";
		this.consent = null;
		this.full_access = full_access;
		this.type = "";
		this.title = "";
		this.desc_nature = "";
		this.desc_requirement = "";
		this.desc_links = "";
		this.desc_outside_data = "";
		this.rr_data_files = "";
		this.rr_result_usage = "";
		this.desc_shared = "";
	}

	public VmInfoBean(String vmid, String publicip, String created_at, String workDir,
			String imagepath, String policypath, int sshport, int vncport,
			int numCPUs, int memorySize, int diskSpace, VMMode vmmode,
			VMState vmstate, String vncloginId, String vncloginPwd,
			String vmloginid, String vmloginpwd,
			String imagename, String policyname, VMMode requestedVMMode, String type, String title,
					  Boolean consent, String desc_nature, String desc_requirement, String desc_links, String desc_outside_data,
			String rr_data_files, String rr_result_usage, Boolean full_access, List<VmUserRole> roles, String desc_shared) {

		this.vmid = vmid;
		this.publicip = publicip;
		this.created_at = created_at;
		this.workDir = workDir;
		this.imagepath = imagepath;
		this.policypath = policypath;
		this.sshport = sshport;
		this.vncport = vncport;
		this.numCPUs = numCPUs;
		this.memorySize = memorySize;
		this.volumeSize = diskSpace;
		this.vmmode = vmmode;
		this.vmstate = vmstate;
		this.vncloginId = vncloginId;
		this.vncloginPwd = vncloginPwd;
		this.imagename = imagename;
		this.policyname = policyname;
		this.requestedVMMode = requestedVMMode;
		this.vmloginid = vmloginid;
		this.vmloginpwd = vmloginpwd;
		this.consent = consent;
		this.full_access = full_access;
		this.type = type;
		this.title = title;
		this.desc_nature = desc_nature;
		this.desc_requirement = desc_requirement;
		this.desc_links = desc_links;
		this.desc_outside_data = desc_outside_data;
		this.rr_data_files = rr_data_files;
		this.rr_result_usage = rr_result_usage;
		this.desc_shared = desc_shared;
		this.roles = roles;
	}

	public String getVmid() {
		return vmid;
	}
	public String getPublicip() {
		return publicip;
	}
	public String getCreated_at() {
		return created_at;
	}
	public String getWorkDir() {
		return workDir;
	}
	public int getSshport() {
		return sshport;
	}
	public int getVncport() {
		return vncport;
	}
	public VMMode getVmmode() {
		return vmmode;
	}
	public VMState getVmstate() {
		return vmstate;
	}

	public String getImagepath() {
		return imagepath;
	}

	public String getPolicypath() {
		return policypath;
	}

	public String getVNCloginId() {
		return vncloginId;
	}

	public String getVNCloginPwd() {
		return vncloginPwd;
	}

	public int getNumCPUs() {
		return numCPUs;
	}

	public int getMemorySizeInMB() {
		return memorySize;
	}

	public int getVolumeSizeInGB() {
		return volumeSize;
	}

	public String getPolicyName() {
		return policyname;
	}

	public String getImageName() {
		return imagename;
	}

	public VMMode getRequestedVMMode() {
		return requestedVMMode;
	}
	
	public String getVmLoginId() {
		return vmloginid;
	}
	
	public String getVmLoginPwd() {
		return vmloginpwd;
	}

	public Boolean isConsent() {
		return consent;
	}

	public void setConsent(Boolean consent) {
		this.consent = consent;
	}

	public Boolean isFull_access() {
		return full_access;
	}

	public void setFull_access(Boolean full_access) {
		this.full_access = full_access;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDesc_nature() {
		return desc_nature;
	}

	public void setDesc_nature(String desc_nature) {
		this.desc_nature = desc_nature;
	}

	public String getDesc_requirement() {
		return desc_requirement;
	}

	public void setDesc_requirement(String desc_requirement) {
		this.desc_requirement = desc_requirement;
	}

	public String getDesc_links() {
		return desc_links;
	}

	public void setDesc_links(String desc_links) {
		this.desc_links = desc_links;
	}

	public String getDesc_outside_data() {
		return desc_outside_data;
	}

	public void setDesc_outside_data(String desc_outside_data) {
		this.desc_outside_data = desc_outside_data;
	}

	public String getRr_data_files() {
		return rr_data_files;
	}

	public void setRr_data_files(String rr_data_files) {
		this.rr_data_files = rr_data_files;
	}

	public String getRr_result_usage() {
		return rr_result_usage;
	}

	public void setRr_result_usage(String rr_result_usage) {
		this.rr_result_usage = rr_result_usage;
	}

	public String getDesc_shared() {
		return desc_shared;
	}

	public void setDesc_shared(String desc_shared) {
		this.desc_shared = desc_shared;
	}

	public List<VmUserRole> getRoles() {
		return this.roles;
	}

	@Override
	public String toString() {
		return String.format("[vmid=%s, publicip=%s, workDir=%s, imagename=%s, " +
			"imagepath=%s, sshport=%d, vncport=%d, vmloginId=%s, vmloginPwd=%s, " +
			"numCPUs=%d, memorySize=%d, volumeSize=%d, policypath=%s, vmmode=%s," +
			"vmstate=%s, requestmode=%s, type=%s, roles=%s]",  vmid, publicip, workDir, imagename, imagepath, sshport,
			vncport, vncloginId, vncloginPwd, numCPUs, memorySize, volumeSize, policypath,
			(vmmode != null) ? vmmode.toString(): null, 
			(vmstate != null) ? vmstate.toString(): null, 
			(requestedVMMode != null) ? requestedVMMode.toString(): null, type, roles.toString());
	}
}
