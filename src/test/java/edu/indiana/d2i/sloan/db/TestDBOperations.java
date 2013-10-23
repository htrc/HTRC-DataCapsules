package edu.indiana.d2i.sloan.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmStatusBean;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMState;

public class TestDBOperations {
	private int[] portsUsed = null;
	private void loadDataToVmTable(int records) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;
		
		try {
			String insertTableSQL = "INSERT INTO vms"
					+ "(vmid, vmmode, vmstate, publicip, sshport, vncport, workingdir) VALUES"
					+ "(?, ?, ?, ?, ?, ?, ?)";
			connection = DBConnections.getInstance().getConnection();
			
			int count = records;
			portsUsed = new int[count * 2];
			for (int i = 0; i < count; i++) {
				pst = connection.prepareStatement(insertTableSQL);
				pst.setString(1, "vmid-" + i);
				pst.setString(2, VMMode.MAINTENANCE.toString());
				pst.setString(3, VMState.RUNNING.toString());
				pst.setString(4, "192.168.0." + (i+2));
				pst.setInt(5, 2000 + i*2);
				pst.setInt(6, 2000 + i*2 + 1);
				pst.setString(7, "/var/instance/" + "vmid-" + i);
				pst.executeUpdate();
				pst.close();
				
				portsUsed[i * 2] = 2000 + i*2;
				portsUsed[i * 2 + 1] = 2000 + i*2 + 1;
			}
		} finally {
			if (pst != null) pst.close();
			if (connection != null) connection.close();
		}
	}
	
	private void loadDataToUserTable(int records) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;
		
		try {
			String insertTableSQL = "INSERT INTO users"
				+ "(username, usertype) VALUES" + "(?, ?)";
			connection = DBConnections.getInstance().getConnection();
			
			int count = records;
			for (int i = 0; i < count; i++) {
				pst = connection.prepareStatement(insertTableSQL);
				pst.setString(1, "user-" + i);
				pst.setString(2, "testtype");
				
				pst.executeUpdate();
				pst.close();
			}
		} finally {
			if (pst != null) pst.close();
			if (connection != null) connection.close();
		}
	}
	
	@BeforeClass
	public static void beforeClass() {
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.DB_DRIVER_CLASS, "com.mysql.jdbc.Driver");
			Configuration.getInstance().setProperty(
				Configuration.PropertyName.JDBC_URL, "jdbc:mysql://localhost:3306/" + DBSchema.DB_NAME);
			Configuration.getInstance().setProperty(
					Configuration.PropertyName.DB_USER, "root");
			Configuration.getInstance().setProperty(
					Configuration.PropertyName.DB_PWD, "root");
	}
	
	@AfterClass
	public static void afterClass() {
		DBOperations.getInstance().close();
	}
	
	@Before
	public void before() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "root");
		ScriptRunner script = new ScriptRunner(connection, false, false);
		script.runScript(new java.io.FileReader("src/main/resources/createtables.sql"));
		connection.close();
	}
	
//	@Test
	public void testAddGetAndDeleteVM() throws SQLException, NoItemIsFoundInDBException {
		String userName, vmid, workDir;
		
		int count = 4;
		loadDataToUserTable(count);	
		int[] portsExpected = new int[count * 2];
		List<String> vmids = new ArrayList<String>();
		for (int index = 0; index < count; index++) {
			userName = "user-" + index;
			if (index == count-1) {
				userName = "user-" + (index-1);
			}
			
			vmid = "vmid-" + index;
			VMPorts host = new VMPorts("192.168.0." + (index+2), 2000 + index*2, 2000 + index*2 + 1);
			workDir = "/var/instance/" + "vmid-" + index;
			vmids.add(vmid);
			
			portsExpected[index*2] = 2000 + index*2;
			portsExpected[index*2+1] = 2000 + index*2 + 1;
			DBOperations.getInstance().addVM(userName, vmid, "/path/to/image", host, workDir);
		}
		
		// trigger error
		userName = "user-" + 0;
		vmid = "vmid-" + 0;
		VMPorts host = new VMPorts("192.168.0." + (0+2), 2000 + 0*2, 2000 + 0*2 + 1);
		workDir = "/var/instance/" + "vmid-" + 0;
		DBOperations.getInstance().addVM(userName, vmid, "/path/to/image", host, workDir);
		
		// read 1 vm back
		VmInfoBean vmInfo = DBOperations.getInstance().getVmInfo("user-" + 0, "vmid-" + 0);
		Assert.assertEquals(VMState.BUILDING, vmInfo.getVmstate());
		Assert.assertEquals(2000, vmInfo.getSshport());
		Assert.assertEquals(2001, vmInfo.getVncport());
		Assert.assertEquals("192.168.0.2", vmInfo.getPublicip());
		Assert.assertEquals("vmid-" + 0, vmInfo.getVmid());
		
		// read 2 vm back
		Assert.assertTrue(DBOperations.getInstance().getVmInfo("user-" + (count-2)).size() == 2);
		
		// read ports in use
		List<VmInfoBean> vmStatus = DBOperations.getInstance().getVmInfo();
		int[] ports = new int[vmStatus.size() * 2];
		for (int i = 0; i < vmStatus.size(); i++) {
			ports[i*2] = vmStatus.get(i).getSshport();
			ports[i*2+1] = vmStatus.get(i).getVncport();
		}
		Arrays.sort(portsExpected);
		Arrays.sort(ports);
		Assert.assertArrayEquals(portsExpected, ports);
		
		// delete vm
		for (String id : vmids) {
			DBOperations.getInstance().deleteVMs(id);
		}		
		
		vmStatus = DBOperations.getInstance().getVmInfo();
		Assert.assertEquals(0, vmStatus.size());		
	}
	
	@Test
	public void testInsertUserIfNotExists() throws SQLException {
		DBOperations.getInstance().insertUserIfNotExists("myusername-0");
		DBOperations.getInstance().insertUserIfNotExists("myusername-0");
	}
	
	@Test
	public void testUpdateVMStatus() throws SQLException {
		int count = 3;
		loadDataToVmTable(count);
		
		for (int i = 0; i < count; i++) {
			String vmid = "vmid-" + i;
			DBOperations.getInstance().updateVMState(vmid, VMState.SHUTDOWN);
		}
		
		List<VmInfoBean> vmStatus = DBOperations.getInstance().getVmInfo();
		Assert.assertEquals(count, vmStatus.size());
		for (int i = 0; i < count; i++) {
			Assert.assertEquals(VMState.SHUTDOWN, vmStatus.get(i).getVmstate());
		}
	}
}
