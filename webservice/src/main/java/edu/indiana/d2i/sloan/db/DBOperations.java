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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.indiana.d2i.sloan.bean.*;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class DBOperations {
	private static Logger logger = Logger.getLogger(DBOperations.class);
	private static DBOperations instance = null;
	
	private final java.text.SimpleDateFormat DATE_FORMATOR = 
		     new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private DBOperations() {

	}

	static {
		instance = new DBOperations();
	}

	private void executeTransaction(final List<String> updates)
			throws SQLException {
		Connection connection = null;
		Statement st = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			connection.setAutoCommit(false);
			st = connection.createStatement();

			for (String update : updates) {
				st.executeUpdate(update);
			}
			connection.commit();
			logger.info("Commit updates " + updates.toString());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			if (connection != null) {
				try {
					connection.rollback();
					logger.info("Rollback updates " + updates.toString());
				} catch (SQLException ex1) {
					throw ex1;
				}
			}
		} finally {
			if (st != null)
				st.close();
			if (connection != null)
				connection.close();
		}
	}


	public List<VmKeyInfoBean> getVmKeyInfo() throws SQLException {
		String sql = "SELECT " + DBSchema.VmTable.VM_ID + ","
				+ DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.HOST + ","
				+ DBSchema.VmTable.STATE + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.HostTable.CPU_CORES + ","
				+ DBSchema.HostTable.MEMORY_GB + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.UserTable.USER_NAME + ","
				+ DBSchema.UserTable.USER_EMAIL + " FROM "
				+ DBSchema.VmTable.TABLE_NAME + ", "
				+ DBSchema.HostTable.TABLE_NAME + ", "
				+ DBSchema.UserTable.TABLE_NAME + " WHERE "
				+ DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_NAME + " = "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.USERNAME + " AND "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.HOST + "="
				+ DBSchema.HostTable.TABLE_NAME + "." + DBSchema.HostTable.HOST_NAME + " AND "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.STATE + "!= \""
				+ VMState.DELETED.toString() + "\"";
		logger.debug(sql);
		List<VmKeyInfoBean> res = new ArrayList<VmKeyInfoBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				VmKeyInfoBean vmKeyInfoBean = new VmKeyInfoBean(
						rs.getString(DBSchema.VmTable.VM_ID),
						rs.getString(DBSchema.UserTable.USER_NAME),
						rs.getString(DBSchema.UserTable.USER_EMAIL),
						rs.getInt(DBSchema.VmTable.NUM_CPUS),
						rs.getInt(DBSchema.VmTable.MEMORY_SIZE),
						VMMode.valueOf(rs
								.getString(DBSchema.VmTable.VM_MODE)),
						VMState.valueOf(rs
								.getString(DBSchema.VmTable.STATE)),
						rs.getString(DBSchema.VmTable.HOST),
						rs.getInt(DBSchema.HostTable.CPU_CORES),
						rs.getInt(DBSchema.HostTable.MEMORY_GB)
				);
                res.add(vmKeyInfoBean);
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
		return res;
	}

	private List<VmInfoBean> getVmInfoInternal(final String sql)
			throws SQLException {
		logger.debug(sql);

		List<VmInfoBean> res = new ArrayList<VmInfoBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance()
					.getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				VmInfoBean vminfo = new VmInfoBean(
						rs.getString(DBSchema.VmTable.VM_ID),
						rs.getString(DBSchema.VmTable.HOST),
						rs.getString(DBSchema.VmTable.WORKING_DIR),
						null, // image path
						null, // policy path
						rs.getInt(DBSchema.VmTable.SSH_PORT),
						rs.getInt(DBSchema.VmTable.VNC_PORT),
						rs.getInt(DBSchema.VmTable.NUM_CPUS),
						rs.getInt(DBSchema.VmTable.MEMORY_SIZE),
						rs.getInt(DBSchema.VmTable.DISK_SPACE),
						VMMode.valueOf(rs
								.getString(DBSchema.VmTable.VM_MODE)),
						VMState.valueOf(rs
								.getString(DBSchema.VmTable.STATE)),
						rs.getString(DBSchema.VmTable.VNC_USERNAME),
						rs.getString(DBSchema.VmTable.VNC_PASSWORD),
						rs.getString(DBSchema.ImageTable.IMAGE_LOGIN_ID),
						rs.getString(DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD),
						rs.getString(DBSchema.VmTable.IMAGE_NAME),
						null, null);
				res.add(vminfo);
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
		return res;
	}

	public static DBOperations getInstance() {
		return instance;
	}

	public boolean quotasNotExceedLimit(CreateVmRequestBean request)
			throws SQLException {
		int requestedDiskAmount = request.getVolumeSizeInGB();
		int requestedCPUNum = request.getVcpu();
		int requestedMemory = request.getMemory();

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT ").append(DBSchema.UserTable.DISK_LEFT_QUOTA)
				.append(",").append(DBSchema.UserTable.CPU_LEFT_QUOTA)
				.append(",").append(DBSchema.UserTable.MEMORY_LEFT_QUOTA)
				.append(" FROM ").append(DBSchema.UserTable.TABLE_NAME)
				.append(" WHERE ").append(DBSchema.UserTable.USER_NAME)
				.append("=")
				.append(String.format("\"%s\"", request.getUserName()));

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		boolean satisfiable = false;

		try {
			connection = DBConnections.getInstance().getConnection();

			pst = connection.prepareStatement(sql.toString());

			rs = pst.executeQuery();
			if (rs.next()) {
				int leftDiskQuota = rs
						.getInt(DBSchema.UserTable.DISK_LEFT_QUOTA);
				int leftCPUQuota = rs.getInt(DBSchema.UserTable.CPU_LEFT_QUOTA);
				int leftMemoryQuota = rs
						.getInt(DBSchema.UserTable.MEMORY_LEFT_QUOTA);

				satisfiable = (leftDiskQuota >= requestedDiskAmount)
						&& (leftCPUQuota >= requestedCPUNum)
						&& (leftMemoryQuota >= requestedMemory);

				if (satisfiable) {
					/* update user table */
					StringBuilder updateSql = new StringBuilder();
					updateSql
							.append("UPDATE ")
							.append(DBSchema.UserTable.TABLE_NAME)
							.append(" SET ")
							.append(String.format("%s=%d, %s=%d, %s=%d",
									DBSchema.UserTable.DISK_LEFT_QUOTA,
									leftDiskQuota - requestedDiskAmount,
									DBSchema.UserTable.CPU_LEFT_QUOTA,
									leftCPUQuota - requestedCPUNum,
									DBSchema.UserTable.MEMORY_LEFT_QUOTA,
									leftMemoryQuota - requestedMemory))
							.append(" WHERE ")
							.append(DBSchema.UserTable.USER_NAME)
							.append("=")
							.append(String.format("\"%s\"",
									request.getUserName()));

					executeTransaction(Collections
							.<String> singletonList(updateSql.toString()));

				}

			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}

		return satisfiable;
	}

	public void insertUserIfNotExists(String userName, String userEmail) throws SQLException {
		Connection connection = null;
		PreparedStatement pst1 = null;
		PreparedStatement pst2 = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String queryUser = "SELECT * FROM " + DBSchema.UserTable.TABLE_NAME
					+ " WHERE " + DBSchema.UserTable.USER_NAME + "=(?)";
			pst1 = connection.prepareStatement(queryUser);
			pst1.setString(1, userName);
			rs = pst1.executeQuery();
			if (!rs.next()) {
				// ignore the error if there is a duplicate
				String insertUser = String.format(
					"INSERT IGNORE INTO " + DBSchema.UserTable.TABLE_NAME
							+ "(%s, %s, %s, %s, %s) VALUES" + "(?, ?, ?, ?, ?)",
					DBSchema.UserTable.USER_NAME, DBSchema.UserTable.USER_EMAIL, 
					DBSchema.UserTable.DISK_LEFT_QUOTA, DBSchema.UserTable.CPU_LEFT_QUOTA, 
					DBSchema.UserTable.MEMORY_LEFT_QUOTA);
				pst2 = connection.prepareStatement(insertUser);
				pst2.setString(1, userName);
				pst2.setString(2, userEmail);
				pst2.setInt(3, Configuration.getInstance().
					getInt(Configuration.PropertyName.USER_DISK_QUOTA_IN_GB)); // volume size in GB
				pst2.setInt(4, Configuration.getInstance().
					getInt(Configuration.PropertyName.USER_CPU_QUOTA_IN_NUM)); // vcpus
				pst2.setInt(5, Configuration.getInstance().
					getInt(Configuration.PropertyName.USER_MEMORY_QUOTA_IN_MB)); // memory size in MB
				pst2.executeUpdate();
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst1 != null)
				pst1.close();
			if (pst2 != null)
				pst2.close();
			if (connection != null)
				connection.close();
		}
	}

	public List<VmInfoBean> getExistingVmInfo() throws SQLException {
		String sql = "SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD 
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "=" 
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.STATE + "!= \""
				+ VMState.DELETED.toString() + "\"";
		return getVmInfoInternal(sql);
	}

	public List<VmInfoBean> getVmInfo(String userName) throws SQLException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD
				// + image path & policy path
				+ " FROM " + DBSchema.ImageTable.TABLE_NAME + ","
				+ DBSchema.VmTable.TABLE_NAME 
				+ " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.USERNAME + "=\"%s\""
				+ " AND " + DBSchema.VmTable.STATE + "!= \"" + VMState.DELETED.toString() + "\"", userName);
		return getVmInfoInternal(sql);
	}

	public VmInfoBean getVmInfo(String userName, String vmid)
			throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD 
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.USERNAME + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "."
				+ DBSchema.VmTable.VM_ID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.STATE + "!= \"" + VMState.DELETED.toString() + "\"", userName, vmid);

		logger.debug(sql);

		List<VmInfoBean> res = getVmInfoInternal(sql);
		if (res.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"VM %s with user %s is not found in DB.", vmid, userName));
		return res.get(0);
	}

	private VmInfoBean getVmInfoByID(String vmid)
			throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "."
				+ DBSchema.VmTable.VM_ID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.STATE + "!= \"" + VMState.DELETED.toString() + "\"", vmid);

		logger.debug(sql);

		List<VmInfoBean> res = getVmInfoInternal(sql);
		if (res.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"VM %s is not found in DB.", vmid));
		return res.get(0);
	}

	public void addVM(String userName, String vmid, String imageName,
			String vncLoginId, String vncLoginPwd, VMPorts host,
			String workDir, int numCPUs, int memorySize, int diskSpace)
			throws SQLException {
		String insertvmsql = String
				.format("INSERT INTO "
						+ DBSchema.VmTable.TABLE_NAME
						+ " ("
						+ DBSchema.VmTable.VM_ID
						+ ","
						+ DBSchema.VmTable.STATE
						+ ","
						+ DBSchema.VmTable.VM_MODE
						+ ","
						+ DBSchema.VmTable.HOST
						+ ","
						+ DBSchema.VmTable.SSH_PORT
						+ ","
						+ DBSchema.VmTable.VNC_PORT
						+ ","
						+ DBSchema.VmTable.WORKING_DIR
						+ ","
						+ DBSchema.VmTable.IMAGE_NAME
						+ ","
						+ DBSchema.VmTable.VNC_USERNAME
						+ ","
						+ DBSchema.VmTable.VNC_PASSWORD
						+ ","
						+ DBSchema.VmTable.NUM_CPUS
						+ ","
						+ DBSchema.VmTable.MEMORY_SIZE
						+ ","
						+ DBSchema.VmTable.DISK_SPACE
						+ ","
						+ DBSchema.VmTable.USERNAME
						+ ") VALUES"
						+ "(\"%s\", \"%s\", \"%s\", \"%s\", %d, %d, \"%s\", \"%s\", \"%s\", \"%s\", %d, %d, %d, \"%s\")",
						vmid, VMState.CREATE_PENDING.toString(),
						VMMode.NOT_DEFINED.toString(), host.publicip,
						host.sshport, host.vncport, workDir, imageName,
						vncLoginId, vncLoginPwd, numCPUs, memorySize,
						diskSpace, userName);


		logger.debug(insertvmsql);

		List<String> updates = new ArrayList<String>();
		updates.add(insertvmsql);

		String insertActivitySQL = getInsertActivitySQL(vmid, VMMode.NOT_DEFINED.toString(),
				VMMode.NOT_DEFINED.toString(),VMState.SHUTDOWN.toString(),
				VMState.SHUTDOWN.toString(), userName);
		logger.debug(insertActivitySQL);
		updates.add(insertActivitySQL);

		executeTransaction(updates);
	}

	private String getInsertActivitySQL(String vmid, String prevMode, String curMode,
										String prevState, String curState, String operator) {
		String insertActivitySQL = String.format("INSERT INTO "
						+ DBSchema.ActivityTable.TABLE_NAME
						+ " ("
						+ DBSchema.ActivityTable.VM_ID
						+ ", "
						+ DBSchema.ActivityTable.PREV_MODE
						+ ", "
						+ DBSchema.ActivityTable.CURR_MODE
						+ ", "
						+ DBSchema.ActivityTable.PREV_STATE
						+ ", "
						+ DBSchema.ActivityTable.CURR_STATE
						+ ", "
						+ DBSchema.ActivityTable.USERNAME
						+ ") VALUES (\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\")",
				vmid, prevMode, curMode, prevState, curState, operator
		);
		return insertActivitySQL;
	}

	public void deleteVMs(String username, VmInfoBean vmInfo)
		throws SQLException, NoItemIsFoundInDBException {
		deleteVMs(username, username, vmInfo);
	}

	public void deleteVMs(String username, String operator, VmInfoBean vmInfo)
			throws SQLException, NoItemIsFoundInDBException {

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		
		List<String> updates = new ArrayList<String>();

		String markdeletedsql = String.format("UPDATE %s SET %s=\"%s\" WHERE %s=\"%s\"",
			DBSchema.VmTable.TABLE_NAME, DBSchema.VmTable.STATE, VMState.DELETED.toString(),
			DBSchema.VmTable.VM_ID,	vmInfo.getVmid());

		/* restore quota */

		// first query remaining quota
		StringBuilder sql = new StringBuilder();

		sql.append("SELECT ").append(DBSchema.UserTable.DISK_LEFT_QUOTA)
				.append(",").append(DBSchema.UserTable.CPU_LEFT_QUOTA)
				.append(",").append(DBSchema.UserTable.MEMORY_LEFT_QUOTA)
				.append(" FROM ").append(DBSchema.UserTable.TABLE_NAME)
				.append(" WHERE ").append(DBSchema.UserTable.USER_NAME)
				.append("=").append(String.format("\"%s\"", username));

		StringBuilder updateUserTableSql = new StringBuilder();

		try {
			connection = DBConnections.getInstance().getConnection();

			pst = connection.prepareStatement(sql.toString());

			rs = pst.executeQuery();
			if (rs.next()) {
				int leftDiskQuota = rs
						.getInt(DBSchema.UserTable.DISK_LEFT_QUOTA);
				int leftCPUQuota = rs.getInt(DBSchema.UserTable.CPU_LEFT_QUOTA);
				int leftMemoryQuota = rs
						.getInt(DBSchema.UserTable.MEMORY_LEFT_QUOTA);

				// compose update sql
				updateUserTableSql = new StringBuilder();
				updateUserTableSql
						.append("UPDATE ")
						.append(DBSchema.UserTable.TABLE_NAME)
						.append(" SET ")
						.append(String.format("%s=%d, %s=%d, %s=%d",
								DBSchema.UserTable.DISK_LEFT_QUOTA,
								leftDiskQuota + vmInfo.getVolumeSizeInGB(),
								DBSchema.UserTable.CPU_LEFT_QUOTA, leftCPUQuota
										+ vmInfo.getNumCPUs(),
								DBSchema.UserTable.MEMORY_LEFT_QUOTA,
								leftMemoryQuota + vmInfo.getMemorySizeInMB()))
						.append(" WHERE ").append(DBSchema.UserTable.USER_NAME)
						.append("=").append(String.format("\"%s\"", username));
			}

		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}

		updates.add(markdeletedsql);

		if (updateUserTableSql.length() > 0) {
			updates.add(updateUserTableSql.toString());
		}

		String insertActivitySQL = getInsertActivitySQL(vmInfo.getVmid(), vmInfo.getVmmode().toString(),
				VMMode.NOT_DEFINED.toString(), vmInfo.getVmstate().toString(),
				VMState.DELETED.toString(), operator);
		logger.debug(insertActivitySQL);
		updates.add(insertActivitySQL);

		executeTransaction(updates);
	}

	public void restoreQuota(String username, int cpu, int memory, int diskspace)
			throws SQLException, NoItemIsFoundInDBException {

			Connection connection = null;
			PreparedStatement pst = null;
			ResultSet rs = null;

			List<String> updates = new ArrayList<String>();


			/* restore quota */

			// first query remaining quota
			StringBuilder sql = new StringBuilder();

			sql.append("SELECT ").append(DBSchema.UserTable.DISK_LEFT_QUOTA)
					.append(",").append(DBSchema.UserTable.CPU_LEFT_QUOTA)
					.append(",").append(DBSchema.UserTable.MEMORY_LEFT_QUOTA)
					.append(" FROM ").append(DBSchema.UserTable.TABLE_NAME)
					.append(" WHERE ").append(DBSchema.UserTable.USER_NAME)
					.append("=").append(String.format("\"%s\"", username));

			StringBuilder updateUserTableSql = new StringBuilder();

			try {
				connection = DBConnections.getInstance().getConnection();

				pst = connection.prepareStatement(sql.toString());

				rs = pst.executeQuery();
				if (rs.next()) {
					int leftDiskQuota = rs
							.getInt(DBSchema.UserTable.DISK_LEFT_QUOTA);
					int leftCPUQuota = rs.getInt(DBSchema.UserTable.CPU_LEFT_QUOTA);
					int leftMemoryQuota = rs
							.getInt(DBSchema.UserTable.MEMORY_LEFT_QUOTA);

					// compose update sql
					updateUserTableSql = new StringBuilder();
					updateUserTableSql
							.append("UPDATE ")
							.append(DBSchema.UserTable.TABLE_NAME)
							.append(" SET ")
							.append(String.format("%s=%d, %s=%d, %s=%d",
									DBSchema.UserTable.DISK_LEFT_QUOTA,
									leftDiskQuota + diskspace,
									DBSchema.UserTable.CPU_LEFT_QUOTA, leftCPUQuota
											+ cpu,
									DBSchema.UserTable.MEMORY_LEFT_QUOTA,
									leftMemoryQuota + memory))
							.append(" WHERE ").append(DBSchema.UserTable.USER_NAME)
							.append("=").append(String.format("\"%s\"", username));
				}

			} finally {
				if (rs != null)
					rs.close();
				if (pst != null)
					pst.close();
				if (connection != null)
					connection.close();
			}

			if (updateUserTableSql.length() > 0) {
				updates.add(updateUserTableSql.toString());
				executeTransaction(updates);
			}
	}

	// This function is just for test purpose and should not be called
	public void updateVMState(String vmid, VMState state) throws SQLException {
		this.updateVMState(vmid, state, "TEST");
	}


	public void updateVMState(String vmid, VMState state, String operator) throws SQLException {
		List<String> updates = new ArrayList<String>();
		String updatevmsql = String.format("UPDATE "
				+ DBSchema.VmTable.TABLE_NAME + " SET "
				+ DBSchema.VmTable.STATE + "=\"%s\" WHERE "
				+ DBSchema.VmTable.VM_ID + "=\"%s\"", state.toString(), vmid);
        updates.add(updatevmsql);

		VmInfoBean vmInfo = null;
		try {
			vmInfo = getVmInfoByID(vmid);
		} catch (NoItemIsFoundInDBException e) {
			logger.debug("Cannot find VM with id: " + vmid + "to update state");
		}

		if (vmInfo != null) {
			String insertActivitySQL = getInsertActivitySQL(vmid,
					vmInfo.getVmmode().toString(),
					vmInfo.getVmmode().toString(),
					vmInfo.getVmstate().toString(),
					state.toString(),
					operator);
			updates.add(insertActivitySQL);
		}
		executeTransaction(updates);
	}

	// This function is just for test purpose and should not be called
	public void updateVMMode(String vmid, VMMode mode) throws SQLException {
	    updateVMMode(vmid, mode, "TEST");
    }

	public void updateVMMode(String vmid, VMMode mode, String operator) throws SQLException {
		List<String> updates = new ArrayList<String>();
		StringBuilder sql = new StringBuilder().append("UPDATE ")
				.append(DBSchema.VmTable.TABLE_NAME)
				.append(" SET ")
				.append(DBSchema.VmTable.VM_MODE)
				.append(String.format("=\"%s\" WHERE %s=\"%s\"",
						mode.toString(), DBSchema.VmTable.VM_ID, vmid));
        updates.add(sql.toString());

		VmInfoBean vmInfo = null;
		try {
			vmInfo = getVmInfoByID(vmid);
		} catch (NoItemIsFoundInDBException e) {
			logger.debug("Cannot find VM with id: " + vmid + "to update state");
		}

		if (vmInfo != null) {
			String insertActivitySQL = getInsertActivitySQL(vmid,
					vmInfo.getVmmode().toString(),
					mode.toString(),
					vmInfo.getVmstate().toString(),
					vmInfo.getVmstate().toString(),
					operator);
			updates.add(insertActivitySQL);
		}
		executeTransaction(updates);
	}

	public String getImagePath(String imageName) throws SQLException {
		Connection connection = null;
		PreparedStatement pst1 = null;
		PreparedStatement pst2 = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String queryUser = "SELECT * FROM "
					+ DBSchema.ImageTable.TABLE_NAME + " WHERE "
					+ DBSchema.ImageTable.IMAGE_NAME + "=(?)";
			pst1 = connection.prepareStatement(queryUser);
			pst1.setString(1, imageName);
			rs = pst1.executeQuery();
			if (rs.next()) {
				return rs.getString(DBSchema.ImageTable.IMAGE_PATH);
			} else {
				return null;
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst1 != null)
				pst1.close();
			if (pst2 != null)
				pst2.close();
			if (connection != null)
				connection.close();
		}
	}
	
	public List<ImageInfoBean> getImageInfo() throws SQLException {
		Connection connection = null;
		PreparedStatement pst1 = null;
		PreparedStatement pst2 = null;
		ResultSet rs = null;

		List<ImageInfoBean> res = new ArrayList<ImageInfoBean>();
		try {
			connection = DBConnections.getInstance().getConnection();
			String queryUser = "SELECT * FROM " + DBSchema.ImageTable.TABLE_NAME;
			pst1 = connection.prepareStatement(queryUser);
			rs = pst1.executeQuery();
			while (rs.next()) {
				res.add(new ImageInfoBean(rs.getString(DBSchema.ImageTable.IMAGE_NAME),
						rs.getString(DBSchema.ImageTable.IMAGE_STATUS),
					rs.getString(DBSchema.ImageTable.IMAGE_DESCRIPTION)));
			} 
		} finally {
			if (rs != null)
				rs.close();
			if (pst1 != null)
				pst1.close();
			if (pst2 != null)
				pst2.close();
			if (connection != null)
				connection.close();
		}
		return res;
	}
	
	public ResultBean getResult(String randomid) throws 
		SQLException, NoItemIsFoundInDBException, ParseException {
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			String getResult = "SELECT * FROM " + DBSchema.ResultTable.TABLE_NAME + 
				" WHERE " + DBSchema.ResultTable.RESULT_ID + "=\"" + randomid + "\"";			
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(getResult);
			rs = pst.executeQuery();

			if (rs.next()) {
				return new ResultBean(rs.getBinaryStream(DBSchema.ResultTable.DATA_FIELD), 
					DATE_FORMATOR.parse(rs.getString(DBSchema.ResultTable.CREATE_TIME)));
			} else {
				throw new NoItemIsFoundInDBException("Result of " + randomid + " can't be found in db!");
			}
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public ResultInfoBean getResultInfo(String randomid) throws SQLException, NoItemIsFoundInDBException, ParseException
	{
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			String getResultInfo = "SELECT * FROM " + DBSchema.ResultTable.TABLE_NAME +
					" WHERE " + DBSchema.ResultTable.RESULT_ID + "=\"" + randomid + "\"";
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(getResultInfo);
			rs = pst.executeQuery();

			if (rs.next()) {
				return new ResultInfoBean(rs.getString("vmid"),
						rs.getString("resultid"),
						rs.getString("datafield"),
						rs.getString("createtime"),
						rs.getString("notified"),
						rs.getString("notifiedtime")
				);
			} else {
				throw new NoItemIsFoundInDBException("Result of " + randomid + " can't be found in db!");
			}
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public List<ResultInfoBean> getUnreleased() throws SQLException
	{
		String sql;
		sql = new String("select * from results where notified= 'NO' ");
		logger.debug(sql);

		List<ResultInfoBean> res = new ArrayList<ResultInfoBean>();

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {

			conn = DBConnections.getInstance().getConnection();
			pst = conn.prepareStatement(sql);
			rs = pst.executeQuery();
		//
            while (rs.next()) {
				ResultInfoBean result = new ResultInfoBean(rs.getString("vmid"),
						rs.getString("resultid"),
						rs.getString("datafield"),
						rs.getString("createtime"),
						rs.getString("notified"),
						rs.getString("notifiedtime")
				);
				res.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (conn != null)
				conn.close();
		}

		return res;

	}

    public List<ResultInfoBean> getReleased() throws SQLException
    {
        String sql;
        sql = new String("select * from results where notified= 'YES' ");
        logger.debug(sql);

        List<ResultInfoBean> res = new ArrayList<ResultInfoBean>();

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {

            conn = DBConnections.getInstance().getConnection();
            pst = conn.prepareStatement(sql);
            rs = pst.executeQuery();
            while (rs.next()) {
                ResultInfoBean result = new ResultInfoBean(rs.getString("vmid"),
                        rs.getString("resultid"),
                        rs.getString("datafield"),
                        rs.getString("createtime"),
                        rs.getString("notified"),
                        rs.getString("notifiedtime")
                );
                res.add(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null)
                rs.close();
            if (pst != null)
                pst.close();
            if (conn != null)
                conn.close();
        }

        return res;

    }


	public void insertResult(String vmid, String randomid, InputStream input) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			java.util.Date dt = new java.util.Date();
			String currentTime = DATE_FORMATOR.format(dt);
			String insertResult = String.format(
				"INSERT INTO " + DBSchema.ResultTable.TABLE_NAME + " (%s, %s, %s, %s) VALUES" + "(?, ?, ?, ?)", 
				DBSchema.ResultTable.VM_ID, DBSchema.ResultTable.RESULT_ID, 
				DBSchema.ResultTable.DATA_FIELD, DBSchema.ResultTable.CREATE_TIME);
			
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(insertResult);
			pst.setString(1, vmid);
			pst.setString(2, randomid);
			pst.setBinaryStream(3, input);
			pst.setString(4, currentTime);
			
			pst.executeUpdate();
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}
	
	public List<UserResultBean> getResultsUnnotified() throws SQLException {
		List<UserResultBean> res = new ArrayList<UserResultBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format("SELECT %s, %s, %s FROM %s, %s WHERE %s=%s AND %s=%s AND %s=%s",
				// selected fields
				DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_NAME, 
				DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_EMAIL,
				DBSchema.ResultTable.TABLE_NAME + "." + DBSchema.ResultTable.RESULT_ID,
				// selected tables
				DBSchema.UserTable.TABLE_NAME,  DBSchema.ResultTable.TABLE_NAME,
				// where clause
				DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_NAME,
					DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.USERNAME,
				DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID,
					DBSchema.ResultTable.TABLE_NAME + "." + DBSchema.ResultTable.VM_ID,
				DBSchema.ResultTable.NOTIFIED, "0");
			logger.debug(query);
			
			pst = connection.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				UserResultBean bean = new UserResultBean(
					rs.getString(DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_NAME), 
					rs.getString(DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_EMAIL), 
					rs.getString(DBSchema.ResultTable.TABLE_NAME + "." + DBSchema.ResultTable.RESULT_ID));
				res.add(bean);
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
		return res;
	}
	
	public void updateResultAsNotified(String resultId) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String updateResult = String.format(
				"UPDATE %s SET %s=%s WHERE %s=%s", DBSchema.ResultTable.TABLE_NAME,
				DBSchema.ResultTable.NOTIFIED, "1", DBSchema.ResultTable.RESULT_ID, "\""+ resultId + "\"");
			logger.debug(updateResult);
			
			pst = connection.prepareStatement(updateResult);			
			pst.executeUpdate();
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public void updateResultTimeStamp(String resultid, java.sql.Timestamp timestamp) throws SQLException
	{
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String updateResult = String.format(
					"UPDATE %s SET %s=%s WHERE %s=%s", DBSchema.ResultTable.TABLE_NAME,
					DBSchema.ResultTable.CREATE_TIME, timestamp, DBSchema.ResultTable.RESULT_ID, "\""+ resultid + "\"");
			logger.debug(updateResult);

			pst = connection.prepareStatement(updateResult);
			pst.executeUpdate();
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	
	public UserBean getUserWithVmid(String vmid) throws SQLException, 
		NoItemIsFoundInDBException {
		Connection connection = null;
		PreparedStatement pst = null;
		
		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
				"SELECT * FROM %s, %s WHERE %s=%s AND %s=\"%s\"",
				DBSchema.UserTable.TABLE_NAME, DBSchema.VmTable.TABLE_NAME,
				DBSchema.UserTable.TABLE_NAME+"."+DBSchema.UserTable.USER_NAME,
				DBSchema.VmTable.TABLE_NAME+"."+DBSchema.VmTable.USERNAME,
				DBSchema.VmTable.VM_ID, vmid);
			pst = connection.prepareStatement(query);
			
			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return new UserBean(result.getString(DBSchema.UserTable.USER_NAME), 
					result.getString(DBSchema.UserTable.USER_EMAIL));
			} else {
				throw new NoItemIsFoundInDBException(vmid + " is not associated with any user!");
			}
			
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}
	
	public void close() {
		DBConnections.getInstance().close();
	}
}
