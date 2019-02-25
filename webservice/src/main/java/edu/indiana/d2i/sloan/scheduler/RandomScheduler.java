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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.UserBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmUserRole;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.db.DBSchema;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.vm.*;

public class RandomScheduler extends Scheduler {

	private RandomScheduler() {

	}

	@Override
	protected VmInfoBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException, NoItemIsFoundInDBException {
		//PortsPool portsPool = new PortsPool();
		boolean success = false;

		String workDir = request.getWorkDir();

		int scheduleIndex;
		Random rand = new Random(System.currentTimeMillis());

		int maxNumAttempts = Integer.parseInt(Configuration.getInstance()
				.getString(
						Configuration.PropertyName.SCHEDULER_MAX_NUM_ATTEMPTS,
						Constants.DEFAULT_SCHEDULER_MAX_NUM_ATTEMPTS));

		int numAttempts = 0;

		while (!success && numAttempts < maxNumAttempts) {
			numAttempts++;
			scheduleIndex = rand.nextInt(hosts.length);

			VMPorts vmhost = PortsPool.getInstance().nextAvailablePortPairAtHost(request.getVmId(), hosts[scheduleIndex]);

			if (vmhost != null) {
				DBOperations.getInstance().addVM(request.getUserName(),
						request.getVmId(), request.getImageName(),
						request.getVncLoginID(), request.getVncLoginPasswd(),
						vmhost, workDir, request.getVcpu(), 
						request.getMemory(), request.getVolumeSizeInGB(),
						request.getType(), request.getTitle(), request.isConsent(), request.getDesc_nature(),
						request.getDesc_requirement(), request.getDesc_links(), request.getDesc_outside_data(),
						request.getRr_data_files(), request.getRr_result_usage(), request.isFull_access(),
						request.getDesc_shared());

				DBOperations.getInstance().addPorts(request.getVmId(), vmhost);

				List<VmUserRole> roles = new ArrayList<VmUserRole>();
				String email = DBOperations.getInstance().getUserEmail(request.getUserName());
				roles.add(new VmUserRole(email, VMRole.OWNER_CONTROLLER, true, request.getUserName(), request.isFull_access()));

				return new VmInfoBean(request.getVmId(), vmhost.publicip, workDir, 
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
						request.getRr_data_files(), request.getRr_result_usage(), request.isFull_access(), roles,
						request.getDesc_shared());
			}
		}

		throw new NoResourceAvailableException("No port resource available.");
	}

}
