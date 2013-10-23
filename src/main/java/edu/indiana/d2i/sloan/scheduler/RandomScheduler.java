package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;
import java.util.Random;

import org.apache.commons.io.FilenameUtils;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmRequestBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.vm.PortsPool;
import edu.indiana.d2i.sloan.vm.VMPorts;

public class RandomScheduler extends Scheduler {

	private RandomScheduler() {
		
	}
	
	@Override
	protected VmRequestBean doSchedule(CreateVmRequestBean request)
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
						request.getVmId(), request.getImageName(), vmhost,
						workDir);

				return new VmRequestBean(request, vmhost.publicip, vmhost.sshport, vmhost.vncport,
						workDir);
			}
		}

		throw new NoResourceAvailableException("No port resource available.");
	}

}
