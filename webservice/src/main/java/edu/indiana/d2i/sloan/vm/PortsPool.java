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
import java.util.*;

import edu.indiana.d2i.sloan.bean.PortBean;
import edu.indiana.d2i.sloan.exception.InvalidHostNameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;

public class PortsPool {
	private static Logger logger = LoggerFactory.getLogger(PortsPool.class);
	
	private ArrayList<String> vmHosts;
	private final int PORT_RANGE_MIN, PORT_RANGE_MAX;
	
	private static PortsPool instance = null;

	private PortsPool() {
		// load hosts
		vmHosts = new ArrayList<String>();
		String[] hosts = Configuration.getInstance().getString(
				Configuration.PropertyName.HOSTS).split(";");
		for (String host : hosts) {
			vmHosts.add(host);
		}
		
		// load port range from configuration
		PORT_RANGE_MIN = Integer.valueOf(Configuration.getInstance()
			.getString(Configuration.PropertyName.PORT_RANGE_MIN));
		PORT_RANGE_MAX = Integer.valueOf(Configuration.getInstance()
			.getString(Configuration.PropertyName.PORT_RANGE_MAX));
	}

	public static PortsPool getInstance() {
		if (instance == null) {
			synchronized (PortsPool.class) {
				if(instance==null) {
					instance = new PortsPool();
				}
			}
		}
		return instance;
	}
	
	/**
	 * @param host
	 * @return null if no available port pair is found
	 */
	public synchronized VMPorts nextAvailablePortPairAtHost(String vmid, String host) throws SQLException {
		if (!vmHosts.contains(host)) {
			throw new IllegalArgumentException("Hostname " + host + " is illegal!");
		}
		List<Integer> portsUsed = DBOperations.getInstance().getPortsOfHost(host);
		VMPorts vmport = new VMPorts(host, -1, -1);
		for (int port = PORT_RANGE_MIN; port <= PORT_RANGE_MAX; port++) {
			if (!portsUsed.contains(port)) {
				if (vmport.sshport == -1) {
					vmport.sshport = port;
				} else {
					vmport.vncport = port;
					//DBOperations.getInstance().addPorts(vmid, vmport);
					logger.debug("port allocated : " + vmport.toString());
					return vmport;
				}
			}
		}
		return null;
	}

	public synchronized VMPorts getMigrationPortPair(String vmid, VMPorts vmPorts) throws InvalidHostNameException,
			SQLException {
		String host = vmPorts.publicip;
		if (!vmHosts.contains(host)) {
			throw new InvalidHostNameException("Hostname " + host + " is invalid!");
		}

		List<Integer> portsUsed = DBOperations.getInstance().getPortsOfHost(host);
		VMPorts vmport = new VMPorts(host, -1, -1);

		if (!portsUsed.contains(vmPorts.sshport)
				&& !portsUsed.contains(vmPorts.vncport)) {
			vmport.sshport = vmPorts.sshport;
			vmport.vncport = vmPorts.vncport;
			DBOperations.getInstance().addPorts(vmid, vmport);
			logger.debug("port allocated : " + vmport.toString());
			return vmport;
		}

		vmport = nextAvailablePortPairAtHost(vmid, host);
		if(vmport == null)
			return null;

		DBOperations.getInstance().addPorts(vmid, vmport);
		return vmport;
	}
	
	public synchronized void release(String vmid, VMPorts ports) throws SQLException, InvalidHostNameException {
		if (!vmHosts.contains(ports.publicip)) {
			throw new InvalidHostNameException("Hostname " + ports.publicip + " is illegal!");
		}
		DBOperations.getInstance().deletePort(vmid, ports);
		logger.debug("port released : " + ports.toString());
	}
}
