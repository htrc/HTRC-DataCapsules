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
package edu.indiana.d2i.sloan.vm;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;

public class PortsPool {
	private static Logger logger = Logger.getLogger(PortsPool.class);
	
	// <host, <ports_in_use>>
	private Map<String, Set<Integer>> portsUsed;
	private final int PORT_RANGE_MIN, PORT_RANGE_MAX;
	
//	private static PortsPool instance = null;
	public PortsPool() {
		// load hosts
		portsUsed = new HashMap<String, Set<Integer>>();
		String[] hosts = Configuration.getInstance().getString(
				Configuration.PropertyName.HOSTS).split(";");
		for (String host : hosts) {
			portsUsed.put(host, new HashSet<Integer>());
		}
		
		// load from db
		try {
			List<VmInfoBean> vmStatus = DBOperations.getInstance().getExistingVmInfo();
			for (VmInfoBean status : vmStatus) {
				if (!portsUsed.containsKey(status.getPublicip())) {
					portsUsed.put(status.getPublicip(), new HashSet<Integer>());
				}
				portsUsed.get(status.getPublicip()).add(status.getSshport());
				portsUsed.get(status.getPublicip()).add(status.getVncport());
			}
			logger.debug("Ports used: " + portsUsed.toString());
		} catch (SQLException e) {
			logger.fatal(e.getMessage(), e);
			throw new RuntimeException(e);
		}		
		
		// load port range from configuration
		PORT_RANGE_MIN = Integer.valueOf(Configuration.getInstance()
			.getString(Configuration.PropertyName.PORT_RANGE_MIN));
		PORT_RANGE_MAX = Integer.valueOf(Configuration.getInstance()
			.getString(Configuration.PropertyName.PORT_RANGE_MAX));
	}
	
//	static {
//		instance = new PortsPool();
//	}
//	
//	public static PortsPool getInstance() {
//		return instance;
//	}
	
	/**
	 * @param host
	 * @return null if no available port pair is found
	 */
	public VMPorts nextAvailablePortPairAtHost(String host) {
		synchronized (portsUsed) {
			if (!portsUsed.containsKey(host)) {
				throw new IllegalArgumentException("Hostname " + host + " is illegal!");
			}
			
			VMPorts vmport = new VMPorts(host, -1, -1);
			for (int port = PORT_RANGE_MIN; port <= PORT_RANGE_MAX; port++) {
				if (!portsUsed.get(host).contains(port)) {
					if (vmport.sshport == -1) {
						vmport.sshport = port;
						portsUsed.get(host).add(port);
					} else {
						vmport.vncport = port;
						portsUsed.get(host).add(port);
						return vmport;
					}
				}
			}
			return null;
		}
	}

	public VMPorts getMigrationPortPair(VMPorts vmPorts) {
		synchronized (portsUsed) {
			String host = vmPorts.publicip;

			if (!portsUsed.containsKey(host)) {
				throw new IllegalArgumentException("Hostname " + host + " is illegal!");
			}

			VMPorts vmport = new VMPorts(host, -1, -1);

			if (!portsUsed.get(host).contains(vmPorts.sshport)
					&& !portsUsed.get(host).contains(vmPorts.vncport)) {
				portsUsed.get(host).add(vmPorts.sshport);
				portsUsed.get(host).add(vmPorts.vncport);
				vmport.sshport = vmPorts.sshport;
				vmport.vncport = vmPorts.vncport;
				return vmport;
			}

			return nextAvailablePortPairAtHost(host);
		}
	}
	
//	public synchronized void release(VMPorts ports) {
//		synchronized (portsUsed) {
//			if (!portsUsed.containsKey(ports.host)) {
//				throw new IllegalArgumentException("Hostname " + ports.host + " is illegal!");
//			}
//			
//			portsUsed.get(ports.host).remove(ports.sshport);
//			portsUsed.get(ports.host).remove(ports.vncport);
//		}
//	}
}
