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

public class VmInfoBean {
	private String vmid, publicip, workDir, imagepath, policypath, vncloginId,
			vncloginPwd, imagename, policyname, vmloginid, vmloginpwd;
	private int sshport, vncport, numCPUs, memorySize, volumeSize;
	private VMMode vmmode;
	private VMMode requestedVMMode;
	private VMState vmstate;

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

	public VmInfoBean(String vmid, String publicip, String workDir,
			String imagepath, String policypath, int sshport, int vncport,
			int numCPUs, int memorySize, int diskSpace, VMMode vmmode,
			VMState vmstate, String vncloginId, String vncloginPwd,
			String vmloginid, String vmloginpwd,
			String imagename, String policyname, VMMode requestedVMMode) {
		this.vmid = vmid;
		this.publicip = publicip;
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
	}

	public String getVmid() {
		return vmid;
	}
	public String getPublicip() {
		return publicip;
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
	
	@Override
	public String toString() {
		return String.format("[vmid=%s, publicip=%s, workDir=%s, imagename=%s, " +
			"imagepath=%s, sshport=%d, vncport=%d, vmloginId=%s, vmloginPwd=%s, " +
			"numCPUs=%d, memorySize=%d, volumeSize=%d, policypath=%s, vmmode=%s," +
			"vmstate=%s, requestmode=%s]",  vmid, publicip, workDir, imagename, imagepath, sshport,
			vncport, vncloginId, vncloginPwd, numCPUs, memorySize, volumeSize, policypath,
			(vmmode != null) ? vmmode.toString(): null, 
			(vmstate != null) ? vmstate.toString(): null, 
			(requestedVMMode != null) ? requestedVMMode.toString(): null);
	}
}
