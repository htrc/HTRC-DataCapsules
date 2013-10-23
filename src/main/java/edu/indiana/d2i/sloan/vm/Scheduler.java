package edu.indiana.d2i.sloan.vm;

import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmRequestBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;

public class Scheduler {
	private static Logger logger = Logger.getLogger(Scheduler.class);
	private final String[] hosts;
	private int scheduleIndex = 0;
	
	private static Scheduler instance = null;
	private Scheduler() {
		hosts = Configuration.getInstance().getProperty(
			Configuration.PropertyName.HOSTS).split(";");
	}
	
	static {
		instance = new Scheduler();
	}
	
	public static Scheduler getInstance() {
		return instance;
	}
	
	public synchronized VmRequestBean schedule(CreateVmRequestBean request) 
		throws NoResourceAvailableException, SQLException {
		PortsPool portsPool = new PortsPool();		
		int start = scheduleIndex;
		String workDir = FilenameUtils.concat(Constants.DEFAULT_VM_WORKINGDIR_PREFIX, request.getVmId());
		do {
			VMPorts vmports = portsPool.nextAvailablePortPairAtHost(hosts[scheduleIndex]);
			scheduleIndex = (scheduleIndex + 1) % hosts.length;
			if (vmports != null) {
				DBOperations.getInstance().addVM(request.getUserName(), request.getVmId(), 
					request.getImageName(), vmports, workDir);
				logger.info("Assign " + vmports + " to " + request);
				return new VmRequestBean(request, vmports.publicip, vmports.sshport, vmports.vncport, workDir); 
			}
		} while (scheduleIndex != start);
		throw new NoResourceAvailableException("No port resource available.");
	}
}
