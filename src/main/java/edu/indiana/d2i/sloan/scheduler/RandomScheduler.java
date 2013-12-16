package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;
import java.util.Random;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.vm.PortsPool;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMState;

public class RandomScheduler extends Scheduler {

	private RandomScheduler() {

	}

	@Override
	protected VmInfoBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException {
		PortsPool portsPool = new PortsPool();
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

			VMPorts vmhost = portsPool
					.nextAvailablePortPairAtHost(hosts[scheduleIndex]);

			if (vmhost != null) {
				DBOperations.getInstance().addVM(request.getUserName(),
						request.getVmId(), request.getImageName(),
						request.getVncLoginID(), request.getVncLoginPasswd(),
						vmhost, workDir, request.getVcpu(), 
						request.getMemory(), request.getVolumeSizeInGB());
				
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
						VMMode.MAINTENANCE /* user requested vm mode when launching, currently default to maintenance */);
			}
		}

		throw new NoResourceAvailableException("No port resource available.");
	}

}
