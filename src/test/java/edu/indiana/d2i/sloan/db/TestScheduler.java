package edu.indiana.d2i.sloan.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.db.DBSchema;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.vm.Scheduler;

public class TestScheduler {
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
		
		// one host can have two VMs
		Configuration.getInstance().setProperty(Configuration.PropertyName.PORT_RANGE_MIN, "2000");
		Configuration.getInstance().setProperty(Configuration.PropertyName.PORT_RANGE_MAX, "2003");
		Configuration.getInstance().setProperty(Configuration.PropertyName.HOSTS, "host1;host2;");
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
	
	@Test(expected=NoResourceAvailableException.class)
	public void testSequentialScheduling() throws NoResourceAvailableException, SQLException {
		int scheduled = 4;
		loadDataToUserTable(scheduled);
		
		try {
			for (int i = 0; i < scheduled; i++) {
				CreateVmRequestBean request = new CreateVmRequestBean("user-"+i,
					"imagename", "vmid-" + i, "password", 1024, 2, 10);
				Scheduler.getInstance().schedule(request);
			}
		} catch (NoResourceAvailableException e) {
			throw new RuntimeException(e);
		}
		
		CreateVmRequestBean request2 = new CreateVmRequestBean("user-2",
			"imagename", "vmid-"+scheduled, "password", 1024, 2, 10);
		Scheduler.getInstance().schedule(request2); // exception		
	}
	
	@Test
	public void testScheduleAndRelease() throws SQLException, NoResourceAvailableException {
		int records = 5;
		int scheduled = 4;
		
		loadDataToUserTable(records);
		
		// schedule some
		for (int i = 0; i < scheduled; i++) {
			CreateVmRequestBean request = new CreateVmRequestBean("user-"+i,
				"imagename", "vmid-" + i, "password", 1024, 2, 10);
			Scheduler.getInstance().schedule(request);
		}
		
		// release one
		DBOperations.getInstance().deleteVMs("vmid-"+(scheduled-1));
		
		// schedule some more
		for (int i = scheduled; i < records; i++) {
			CreateVmRequestBean request = new CreateVmRequestBean("user-"+i,
				"imagename", "vmid-" + i, "password", 1024, 2, 10);
			Scheduler.getInstance().schedule(request);
		}
		
		Assert.assertEquals(scheduled, DBOperations.getInstance().getVmInfo().size());
	}
}
