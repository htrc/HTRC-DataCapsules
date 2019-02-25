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

public class CreateVmRequestBean {
	private String userName, imageName, vmid, vncLoginID, vncLoginPasswd, workDir;
	private int memory, vcpu, volumeSizeInGB;
	private Boolean consent, full_access;
	private String type, title, desc_nature, desc_requirement, desc_links,
			desc_outside_data, rr_data_files, rr_result_usage, desc_shared;

	public CreateVmRequestBean(String userName, String imageName, String vmid,
			String vncLoginID, String vncLoginPasswd, int memory, int vcpu,
			int volumeSizeInGB, String workDir, String type, String title, Boolean consent, String desc_nature,
			String desc_requirement, String desc_links, String desc_outside_data,
			String rr_data_files, String rr_result_usage, Boolean full_access, String desc_shared) {
		this.userName = userName;
		this.imageName = imageName;
		this.vmid = vmid;
		this.vncLoginID = vncLoginID;
		this.vncLoginPasswd = vncLoginPasswd;
		this.memory = memory;
		this.vcpu = vcpu;
		this.volumeSizeInGB = volumeSizeInGB;
		this.workDir = workDir;
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
	}

	public String getUserName() {
		return userName;
	}

	public String getImageName() {
		return imageName;
	}

	public String getVmId() {
		return vmid;
	}

	public String getVncLoginID() {
		return vncLoginID;
	}

	public String getVncLoginPasswd() {
		return vncLoginPasswd;
	}
	
	public int getMemory() {
		return memory;
	}

	public int getVcpu() {
		return vcpu;
	}

	public int getVolumeSizeInGB() {
		return volumeSizeInGB;
	}
	
	public String getWorkDir() {
		return workDir;
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

	@Override
	public String toString() {
		return String
				.format("[username=%s, imagename=%s, vmid=%s, vmloginID=%s vmloginPasswd=%s memory=%d, vcpu=%d, volumesize=%dGB, type=%s]",
						userName, imageName, vmid, vncLoginID, vncLoginPasswd,
						memory, vcpu, volumeSizeInGB, type);
	}

}
