package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;

import edu.indiana.d2i.sloan.Constants;
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
		PortsPool portsPool = new PortsPool();
		int start = scheduleIndex;

		String workDir = FilenameUtils.concat(
				Constants.DEFAULT_VM_WORKINGDIR_PREFIX, request.getVmId());

		do {
			VMPorts vmhost = portsPool
					.nextAvailablePortPairAtHost(hosts[scheduleIndex]);
			scheduleIndex = (scheduleIndex + 1) % hosts.length;
			if (vmhost != null) {
				DBOperations.getInstance().addVM(request.getUserName(),
						request.getVmId(), request.getImageName(),
						request.getVmLoginID(), request.getVmLoginPasswd(),
						vmhost, workDir, request.getVcpu(), 
						request.getMemory(), request.getVolumeSizeInGB());
			
				return new VmInfoBean(request.getVmId(), vmhost.publicip, workDir, 
						null, // image path
						null, // policy path
						vmhost.sshport, vmhost.vncport, 
						request.getVcpu(), request.getMemory(), request.getVolumeSizeInGB(),
						VMMode.NOT_DEFINED, VMState.BUILDING,
						request.getVmLoginID(), request.getVmLoginPasswd(),
						request.getImageName(), null /* policy name */, VMMode.MAINTENANCE /* user requested vm mode when launching, currently default to maintenance */);
			}
		} while (scheduleIndex != start);

		throw new NoResourceAvailableException("No port resource available.");
	}

}
