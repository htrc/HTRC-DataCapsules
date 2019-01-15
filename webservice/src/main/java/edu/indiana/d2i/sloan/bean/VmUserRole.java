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
	private String username;
	private VMRole role;
	private boolean tou;

	public VmUserRole(String email, VMRole role, boolean tou) {
		this.email = email;
		this.role = role;
		this.tou = tou;
		this.username = null;
	}

	public VmUserRole(String email, VMRole role, boolean tou, String username) {
		this.email = email;
		this.role = role;
		this.tou = tou;
		this.username = username;
	}

	public String getEmail() {
		return email;
	}
	public String getUsername() {
		return username;
	}
	public VMRole getRole() {
		return role;
	}
	public boolean getTou() {
		return tou;
	}

	@Override
	public String toString() {
		return String.format("[username=%s, email=%s, role=%s, tou=%b]", username, email, role.getName(), tou);
	}
}
