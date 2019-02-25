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
package edu.indiana.d2i.sloan.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.Assert;

import org.junit.*;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.Constants;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.db.DBSchema;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;
import edu.indiana.d2i.sloan.scheduler.SchedulerFactory;

@Ignore
public class TestScheduler {
	@BeforeClass
	public static void beforeClass() {
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.DB_DRIVER_CLASS, Configuration.getInstance().getString(
						Configuration.PropertyName.DB_DRIVER_CLASS));
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.JDBC_URL, Configuration.getInstance().getString(
						Configuration.PropertyName.JDBC_URL));
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.DB_USER, Configuration.getInstance().getString(
						Configuration.PropertyName.DB_USER));
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.DB_PWD, Configuration.getInstance().getString(
						Configuration.PropertyName.DB_PWD));

		// configurations for scheduler
		/* test round-robin scheduler */
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.SCHEDULER_IMPL_CLASS,
				Constants.DEFAULT_SCHEDULER_IMPL_CLASS);
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.SCHEDULER_MAX_NUM_ATTEMPTS,
				Constants.DEFAULT_SCHEDULER_MAX_NUM_ATTEMPTS);

		// one host can have two VMs
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.PORT_RANGE_MIN, "2000");
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.PORT_RANGE_MAX, "2003");
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.HOSTS, "host1;host2;");
	}

	@AfterClass
	public static void afterClass() {
		DBOperations.getInstance().close();
	}

	@Before
	public void before() throws Exception {
		Class.forName(Configuration.getInstance().getString(Configuration.PropertyName.DB_DRIVER_CLASS));
		Connection connection = DriverManager.getConnection(Configuration.getInstance().getString(
				Configuration.PropertyName.JDBC_URL), Configuration.getInstance().getString(
				Configuration.PropertyName.DB_USER),  Configuration.getInstance().getString(
				Configuration.PropertyName.DB_PWD));
		ScriptRunner script = new ScriptRunner(connection, false, false);
		script.runScript(new java.io.FileReader(
				"src/main/resources/dc_schema.sql"));
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
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}
	
	private void loadDataToImageTable(int records) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;
		
		try {
			String insertTableSQL = "INSERT INTO images"
				+ "(imagename, imagepath, imagedescription, loginusername, loginpassword) " +
				"VALUES (?, ?, ?, ?, ?)";
			connection = DBConnections.getInstance().getConnection();
			
			int count = records;
			for (int i = 0; i < count; i++) {
				pst = connection.prepareStatement(insertTableSQL);
				pst.setString(1, "imagename-" + i);
				pst.setString(2, "/var/instance/imagename-" + i);
				pst.setString(3, "This is " + i + " image");
				pst.setString(4, "user" + i);
				pst.setString(5, "pwd" + i);
				pst.executeUpdate();
				pst.close();
			}
		} finally {
			if (pst != null) pst.close();
			if (connection != null) connection.close();
		}
	}

	@Test(expected = NoResourceAvailableException.class)
	public void testSequentialScheduling() throws NoResourceAvailableException, NoItemIsFoundInDBException,
			SQLException {
		int scheduled = 4;
		loadDataToUserTable(scheduled);
		loadDataToImageTable(scheduled);

		try {
			for (int i = 0; i < scheduled; i++) {
				CreateVmRequestBean request = new CreateVmRequestBean("user-"
						+ i, "imagename-"+i, "vmid-" + i, "vncusername-" + i,
						"vncpassword-" + i, 1024, 2, 10, "/path/to/work/dir",
						"DEMO", null, null, null, null, null, null, null, null, null, null);
				SchedulerFactory.getInstance().schedule(request);
			}
		} catch (NoResourceAvailableException e) {
			throw new RuntimeException(e);
		}

		CreateVmRequestBean request2 = new CreateVmRequestBean("user-2",
				"imagename", "vmid-" + scheduled, "vmusername-2",
				"vmpassword-2", 1024, 2, 10, "/path/to/work/dir",
				"DEMO", null, null, null, null, null, null, null, null,null, null);
		SchedulerFactory.getInstance().schedule(request2); // exception
	}

	@Test
	public void testScheduleAndRelease() throws SQLException,
			NoResourceAvailableException, NoItemIsFoundInDBException {
		int records = 5;
		int scheduled = 4;

		loadDataToUserTable(records);
		loadDataToImageTable(records);

		// schedule some
		for (int i = 0; i < scheduled; i++) {
			CreateVmRequestBean request = new CreateVmRequestBean("user-" + i,
					"imagename-"+i, "vmid-" + i, "vmusername-" + i, "vmpassword-"
							+ i, 1024, 2, 10, "/path/to/work/dir",
					"DEMO", null, null, null, null, null, null, null, null, null, null );
			SchedulerFactory.getInstance().schedule(request);
		}

		// release one
		VmInfoBean vmInfo = new VmInfoBean("vmid-" + (scheduled - 1), null,
				null, null, null, 0, 0, 2, 1024, 10, null, null, null, null,
				null, null, null, null, null
				, "DEMO", null, null, null, null, null, null, null, null, null, null, null);

		DBOperations.getInstance().deleteVMs("user-" + (scheduled - 1), vmInfo);

		// schedule even more
		for (int i = scheduled; i < records; i++) {
			CreateVmRequestBean request = new CreateVmRequestBean("user-" + i,
					"imagename-"+i, "vmid-" + i, "vmusername-" + i, "vmpassword-"
							+ i, 1024, 2, 10, "/path/to/work/dir",
					"DEMO", null, null, null, null, null, null, null, null, null, null );
			SchedulerFactory.getInstance().schedule(request);
		}

		Assert.assertEquals(scheduled, DBOperations.getInstance().getExistingVmInfo()
				.size());
	}
}
