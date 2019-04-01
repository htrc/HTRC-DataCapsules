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

import edu.indiana.d2i.sloan.vm.VMRole;

public class VmUserRole {
	private String email;
	private String guid;
	private VMRole role;
	private boolean tou;
	private Boolean full_access;

	public VmUserRole(String email, VMRole role, boolean tou, Boolean full_access) {
		this.email = email;
		this.role = role;
		this.tou = tou;
		this.guid = null;
		this.full_access = full_access;
	}

	public VmUserRole(String email, VMRole role, boolean tou, String guid, Boolean full_access) {
		this.email = email;
		this.role = role;
		this.tou = tou;
		this.guid = guid;
		this.full_access = full_access;
	}

	public String getEmail() {
		return email;
	}
	public String getGuid() {
		return guid;
	}
	public VMRole getRole() {
		return role;
	}
	public boolean getTou() {
		return tou;
	}

	public Boolean isFull_access() {
		return full_access;
	}

	public void setFull_access(Boolean full_access) {
		this.full_access = full_access;
	}

	@Override
	public String toString() {
		return String.format("[guid=%s, email=%s, role=%s, tou=%b, full_access=%b]", guid, email, role.getName(), tou, full_access);
	}
}
