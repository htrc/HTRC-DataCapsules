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
package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;

import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.vm.PortsPool;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMState;

public class RoundRobinScheduler extends Scheduler {
	private int scheduleIndex = 0;

	private RoundRobinScheduler() {

	}

	@Override
	protected VmInfoBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException {
		//PortsPool portsPool = new PortsPool();
		int start = scheduleIndex;

		String workDir = request.getWorkDir();

		do {
			VMPorts vmhost = PortsPool.getInstance().nextAvailablePortPairAtHost(request.getVmId(), hosts[scheduleIndex]);
			scheduleIndex = (scheduleIndex + 1) % hosts.length;
			if (vmhost != null) {
				java.util.Date dt = new java.util.Date();
				String created_at = DATE_FORMATOR.format(dt);

				DBOperations.getInstance().addVM(request.getUserName(),
						request.getVmId(), request.getImageName(),
						request.getVncLoginID(), request.getVncLoginPasswd(),
						vmhost, created_at, workDir, request.getVcpu(),
						request.getMemory(), request.getVolumeSizeInGB(),
						request.getType(), request.getTitle(), request.isConsent(), request.getDesc_nature(),
						request.getDesc_requirement(), request.getDesc_links(), request.getDesc_outside_data(),
						request.getRr_data_files(), request.getRr_result_usage(), request.isFull_access());

				DBOperations.getInstance().addPorts(request.getVmId(), vmhost);

				return new VmInfoBean(request.getVmId(), vmhost.publicip, created_at, workDir,
						null, // image path
						null, // policy path
						vmhost.sshport, vmhost.vncport, 
						request.getVcpu(), request.getMemory(), request.getVolumeSizeInGB(),
						VMMode.NOT_DEFINED, VMState.CREATE_PENDING,
						request.getVncLoginID(), request.getVncLoginPasswd(),
						request.getImageName(), 
						null, null, /* login username && login password */
						null /* policy name */, 
						VMMode.MAINTENANCE /* user requested vm mode when launching, currently default to maintenance */,
						request.getType(), request.getTitle(), request.isConsent(), request.getDesc_nature(),
						request.getDesc_requirement(), request.getDesc_links(), request.getDesc_outside_data(),
						request.getRr_data_files(), request.getRr_result_usage(), request.isFull_access());
			}
		} while (scheduleIndex != start);

		throw new NoResourceAvailableException("No port resource available.");
	}

}
