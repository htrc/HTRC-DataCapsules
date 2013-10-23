package edu.indiana.d2i.sloan.vm;

import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;

public class Scheduler {
//	private PortsPool portsPool;
	private final String[] hosts;
	private int scheduleIndex = 0;
	
	private static Scheduler instance = null;
	private Scheduler() {
		hosts = Configuration.getInstance().getProperty(
			Configuration.PropertyName.HOSTS).split(";");
//		portsPool = new PortsPool();
	}
	
	static {
		instance = new Scheduler();
	}
	
	public static Scheduler getInstance() {
		return instance;
	}
	
	public synchronized VmInfoBean schedule(CreateVmRequestBean request) 
		throws NoResourceAvailableException, SQLException {
		PortsPool portsPool = new PortsPool();		
		int start = scheduleIndex;
		String workDir = FilenameUtils.concat(Constants.DEFAULT_VM_WORKINGDIR_PREFIX, request.getVmId());
		do {
			VMPorts vmhost = portsPool.nextAvailablePortPairAtHost(hosts[scheduleIndex]);
			scheduleIndex = (scheduleIndex + 1) % hosts.length;
			if (vmhost != null) {
				DBOperations.getInstance().addVM(request.getUserName(), request.getVmId(), 
					request.getImageName(), vmhost, workDir);
				return new VmInfoBean(request, vmhost.sshport, vmhost.vncport, workDir); 
			}
		} while (scheduleIndex != start);
		throw new NoResourceAvailableException("No port resource available.");
	}
}
