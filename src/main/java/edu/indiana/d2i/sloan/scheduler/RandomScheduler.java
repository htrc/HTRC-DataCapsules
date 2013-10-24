package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;
import java.util.Random;

import org.apache.commons.io.FilenameUtils;

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

		String workDir = FilenameUtils.concat(
				Constants.DEFAULT_VM_WORKINGDIR_PREFIX, request.getVmId());

		int scheduleIndex;
		Random rand = new Random(System.currentTimeMillis());

		int maxNumAttempts = Integer.parseInt(Configuration.getInstance()
				.getProperty(
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
						request.getVmLoginID(), request.getVmLoginPasswd(),
						vmhost, workDir, request.getVcpu(), 
						request.getMemory(), request.getVolumeSizeInGB());
				
				return new VmInfoBean(request.getVmId(), vmhost.publicip, workDir, 
						request.getImageName(), 
						null, // TODO: replace with path by looking up db
						vmhost.sshport, vmhost.vncport, 
						request.getVcpu(), request.getMemory(), request.getVolumeSizeInGB(),
						VMMode.NOT_DEFINED, VMState.BUILDING,
						request.getVmLoginID(), request.getVmLoginPasswd());
			}
		}

		throw new NoResourceAvailableException("No port resource available.");
	}

}
