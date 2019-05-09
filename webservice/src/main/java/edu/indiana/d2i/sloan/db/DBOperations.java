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

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMRole;
import edu.indiana.d2i.sloan.vm.VMState;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class DBOperations {
	private static Logger logger = Logger.getLogger(DBOperations.class);
	private static DBOperations instance = null;
	
	private final java.text.SimpleDateFormat DATE_FORMATOR = 
		     new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String DELETE = "DELETE";

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
				+ DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.TYPE + ","
				+ DBSchema.HostTable.CPU_CORES + ","
				+ DBSchema.HostTable.MEMORY_GB + " FROM "
				+ DBSchema.VmTable.TABLE_NAME + ", "
				+ DBSchema.HostTable.TABLE_NAME + " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.HOST + "="
				+ DBSchema.HostTable.TABLE_NAME + "." + DBSchema.HostTable.HOST_NAME + " AND "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.STATE + " NOT LIKE \"%%" + DELETE + "%%\"";
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
				String vmid = rs.getString(DBSchema.VmTable.VM_ID);
				List<VmUserRole> roles = getRolesWithVmid(vmid, true);

				VmKeyInfoBean vmKeyInfoBean = new VmKeyInfoBean(
						vmid,
						null,
						null,
						rs.getInt(DBSchema.VmTable.NUM_CPUS),
						rs.getInt(DBSchema.VmTable.MEMORY_SIZE),
						VMMode.valueOf(rs
								.getString(DBSchema.VmTable.VM_MODE)),
						VMState.valueOf(rs
								.getString(DBSchema.VmTable.STATE)),
						rs.getString(DBSchema.VmTable.HOST),
						DATE_FORMATOR.format(rs.getTimestamp(DBSchema.VmTable.CREATED_AT)),
						rs.getInt(DBSchema.HostTable.CPU_CORES),
						rs.getInt(DBSchema.HostTable.MEMORY_GB),
						rs.getString(DBSchema.VmTable.TYPE),
						roles
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
				String vmid = rs.getString(DBSchema.VmTable.VM_ID);
				List<VmUserRole> roles = getRolesWithVmid(vmid, true);
				VmUserRole owner = roles.stream()
						.filter(role -> role.getRole().equals(VMRole.OWNER_CONTROLLER) || role.getRole().equals(VMRole.OWNER))
						.collect(Collectors.toList()).get(0);
				VmInfoBean vminfo = new VmInfoBean(
						rs.getString(DBSchema.VmTable.VM_ID),
						rs.getString(DBSchema.VmTable.HOST),
						DATE_FORMATOR.format(rs.getTimestamp(DBSchema.VmTable.CREATED_AT)),
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
						null, null,
						rs.getString(DBSchema.VmTable.TYPE),
						rs.getString(DBSchema.VmTable.TITLE),
						rs.getObject(DBSchema.VmTable.CONSENT) == null ? null : rs.getBoolean(DBSchema.VmTable.CONSENT),
						rs.getString(DBSchema.VmTable.DESC_NATURE),
						rs.getString(DBSchema.VmTable.DESC_REQUIREMENT),
						rs.getString(DBSchema.VmTable.DESC_LINKS),
						rs.getString(DBSchema.VmTable.DESC_OUTSIDE_DATA),
						rs.getString(DBSchema.VmTable.RR_DATA_FILES),
						rs.getString(DBSchema.VmTable.RR_RESULT_USAGE),
						owner.isFull_access(), // owner's full_access is the real full_access of the VM
						roles,
						rs.getString(DBSchema.VmTable.DESC_SHARED));
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
				.append(" WHERE ").append(DBSchema.UserTable.GUID)
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
							.append(DBSchema.UserTable.GUID)
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

	public boolean userExists(String username) throws SQLException {
		Connection connection = null;
		PreparedStatement pst1 = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String queryUser = "SELECT * FROM " + DBSchema.UserTable.TABLE_NAME
					+ " WHERE " + DBSchema.UserTable.GUID + "=(?)";
			pst1 = connection.prepareStatement(queryUser);
			pst1.setString(1, username);
			rs = pst1.executeQuery();
			if (!rs.next()) {
				return false;
			} else {
				return true;
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst1 != null)
				pst1.close();
			if (connection != null)
				connection.close();
		}
	}

	public void insertUserIfNotExists(String userName, String userEmail) throws SQLException {
		Connection connection = null;
		PreparedStatement pst1 = null;
		PreparedStatement pst2 = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String queryUser = "SELECT * FROM " + DBSchema.UserTable.TABLE_NAME
					+ " WHERE " + DBSchema.UserTable.GUID + "=(?)";
			pst1 = connection.prepareStatement(queryUser);
			pst1.setString(1, userName);
			rs = pst1.executeQuery();
			if (!rs.next()) {
				// ignore the error if there is a duplicate
				String insertUser = String.format(
					"INSERT IGNORE INTO " + DBSchema.UserTable.TABLE_NAME
							+ "(%s, %s, %s, %s, %s) VALUES" + "(?, ?, ?, ?, ?)",
					DBSchema.UserTable.GUID, DBSchema.UserTable.USER_EMAIL,
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

	public List<VmInfoBean> getAllVmInfo() throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + "," + DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.VmTable.TYPE + "," + DBSchema.VmTable.TITLE + ","
				+ DBSchema.VmTable.CONSENT + "," + DBSchema.VmTable.DESC_NATURE + ","
				+ DBSchema.VmTable.DESC_REQUIREMENT + "," + DBSchema.VmTable.DESC_LINKS + ","
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "," + DBSchema.VmTable.RR_DATA_FILES + ","
				+ DBSchema.VmTable.RR_RESULT_USAGE + "," + DBSchema.VmTable.DESC_SHARED + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD 
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "=" 
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.STATE + "!= \""
				+ VMState.DELETED.toString() + "\"");

		List<VmInfoBean> res = getVmInfoInternal(sql);
		if (res.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"No VMs found in DB."));
		return res;

	}

	public VmInfoBean getAllVmInfoByID(String vmid) throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + "," + DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.VmTable.TYPE + "," + DBSchema.VmTable.TITLE + ","
				+ DBSchema.VmTable.CONSENT + "," + DBSchema.VmTable.DESC_NATURE + ","
				+ DBSchema.VmTable.DESC_REQUIREMENT + "," + DBSchema.VmTable.DESC_LINKS + ","
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "," + DBSchema.VmTable.RR_DATA_FILES + ","
				+ DBSchema.VmTable.RR_RESULT_USAGE + "," + DBSchema.VmTable.DESC_SHARED + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.STATE + "!= \""
				+ VMState.DELETED.toString() + "\"", vmid);

		List<VmInfoBean> res = getVmInfoInternal(sql);
		if (res.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"VM %s is not found in DB.", vmid));
		return res.get(0);

	}

	public List<VmInfoBean> getExistingVmInfo() throws SQLException {
		String sql = "SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + "," + DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.VmTable.TYPE + "," + DBSchema.VmTable.TITLE + ","
				+ DBSchema.VmTable.CONSENT + "," + DBSchema.VmTable.DESC_NATURE + ","
				+ DBSchema.VmTable.DESC_REQUIREMENT + "," + DBSchema.VmTable.DESC_LINKS + ","
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "," + DBSchema.VmTable.RR_DATA_FILES + ","
				+ DBSchema.VmTable.RR_RESULT_USAGE + "," + DBSchema.VmTable.DESC_SHARED + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.STATE + " NOT LIKE \"%%" + DELETE + "%%\"";
		return getVmInfoInternal(sql);
	}

	public List<VmInfoBean> getVmInfo(String userName) throws SQLException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + "," + DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.VmTable.TYPE + "," + DBSchema.VmTable.TITLE + ","
				+ DBSchema.VmTable.CONSENT + "," + DBSchema.VmTable.DESC_NATURE + ","
				+ DBSchema.VmTable.DESC_REQUIREMENT + "," + DBSchema.VmTable.DESC_LINKS + ","
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "," + DBSchema.VmTable.RR_DATA_FILES + ","
				+ DBSchema.VmTable.RR_RESULT_USAGE + "," + DBSchema.VmTable.DESC_SHARED + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD
				// + image path & policy path
				+ " FROM " + DBSchema.ImageTable.TABLE_NAME + ","
				+ DBSchema.VmTable.TABLE_NAME
				+ "," + DBSchema.UserVmMapTable.TABLE_NAME
				+ " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.GUID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID + "="
				+ DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.VM_ID
				+ " AND " + DBSchema.VmTable.STATE + " NOT LIKE \"%%" + DELETE + "%%\"", userName);
		return getVmInfoInternal(sql);
	}

	public VmInfoBean getVmInfo(String userName, String vmid)
			throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format("SELECT " + DBSchema.VmTable.VM_MODE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID
				+ "," + DBSchema.VmTable.HOST + "," + DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.VmTable.TYPE + "," + DBSchema.VmTable.TITLE + ","
				+ DBSchema.VmTable.CONSENT + "," + DBSchema.VmTable.DESC_NATURE + ","
				+ DBSchema.VmTable.DESC_REQUIREMENT + "," + DBSchema.VmTable.DESC_LINKS + ","
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "," + DBSchema.VmTable.RR_DATA_FILES + ","
				+ DBSchema.VmTable.RR_RESULT_USAGE + "," + DBSchema.VmTable.DESC_SHARED + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD 
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ "," + DBSchema.UserVmMapTable.TABLE_NAME
				+ " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.GUID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID + "="
				+ DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.VM_ID
				+ " AND " + DBSchema.VmTable.STATE + " NOT LIKE \"%%" + DELETE + "%%\"", userName, vmid);

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
				+ "," + DBSchema.VmTable.HOST + "," + DBSchema.VmTable.CREATED_AT + ","
				+ DBSchema.VmTable.STATE + "," + DBSchema.VmTable.SSH_PORT
				+ "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR + ","
				+ DBSchema.VmTable.VNC_PASSWORD + ","
				+ DBSchema.VmTable.VNC_USERNAME + ","
				+ DBSchema.VmTable.NUM_CPUS + ","
				+ DBSchema.VmTable.MEMORY_SIZE + ","
				+ DBSchema.VmTable.DISK_SPACE + ","
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + ","
				+ DBSchema.VmTable.TYPE + "," + DBSchema.VmTable.TITLE + ","
				+ DBSchema.VmTable.CONSENT + "," + DBSchema.VmTable.DESC_NATURE + ","
				+ DBSchema.VmTable.DESC_REQUIREMENT + "," + DBSchema.VmTable.DESC_LINKS + ","
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "," + DBSchema.VmTable.RR_DATA_FILES + ","
				+ DBSchema.VmTable.RR_RESULT_USAGE + "," + DBSchema.VmTable.DESC_SHARED + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_ID + ","
				+ DBSchema.ImageTable.IMAGE_LOGIN_PASSWORD
				// + image path & policy path
				+ " FROM " + DBSchema.VmTable.TABLE_NAME + "," + DBSchema.ImageTable.TABLE_NAME
				+ " WHERE "
				+ DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.IMAGE_NAME + "="
				+ DBSchema.ImageTable.TABLE_NAME + "." + DBSchema.ImageTable.IMAGE_NAME
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "."
				+ DBSchema.VmTable.VM_ID + "=\"%s\""
				+ " AND " + DBSchema.VmTable.STATE + " NOT LIKE \"%%" + DELETE + "%%\"", vmid);

		logger.debug(sql);

		List<VmInfoBean> res = getVmInfoInternal(sql);
		if (res.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"VM %s is not found in DB.", vmid));
		return res.get(0);
	}

	public void addVM(String userName, String vmid, String imageName,
			String vncLoginId, String vncLoginPwd, VMPorts host, String created_at,
			String workDir, int numCPUs, int memorySize, int diskSpace, String type, String title, Boolean consent,
			String desc_nature, String desc_requirement, String desc_links, String desc_outside_data,
			String rr_data_files, String rr_result_usage, Boolean full_access, String desc_shared)
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
								+ DBSchema.VmTable.CREATED_AT
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
								+ DBSchema.VmTable.GUID // TODO-UN remove
								+ ","
								+ DBSchema.VmTable.TYPE
								+ ","
								+ DBSchema.VmTable.TITLE
								+ ","
								+ DBSchema.VmTable.CONSENT
								+ ","
								+ DBSchema.VmTable.DESC_NATURE
								+ ","
								+ DBSchema.VmTable.DESC_REQUIREMENT
								+ ","
								+ DBSchema.VmTable.DESC_LINKS
								+ ","
								+ DBSchema.VmTable.DESC_OUTSIDE_DATA
								+ ","
								+ DBSchema.VmTable.RR_DATA_FILES
								+ ","
								+ DBSchema.VmTable.RR_RESULT_USAGE
								+ ","
								+ DBSchema.VmTable.DESC_SHARED
								+ ") VALUES"
								+ "(\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", %d, %d, \"%s\", \"%s\", \"%s\", \"%s\", %d, %d, %d, \"%s\"" +
								", \"%s\", %s, %s, %s, %s, %s, %s, %s, %s, %s)",
						vmid, VMState.CREATE_PENDING.toString(),
						VMMode.NOT_DEFINED.toString(), host.publicip, created_at,
						host.sshport, host.vncport, workDir, imageName,
						vncLoginId, vncLoginPwd, numCPUs, memorySize,
						diskSpace, userName, type,
						title != null ? "\"" + StringEscapeUtils.escapeJava(title) + "\"" : title,
						consent,
						desc_nature != null ? "\"" + StringEscapeUtils.escapeJava(desc_nature) + "\"" : desc_nature,
						desc_requirement != null ? "\"" + StringEscapeUtils.escapeJava(desc_requirement) + "\"" : desc_requirement,
						desc_links != null ? "\"" + StringEscapeUtils.escapeJava(desc_links) + "\"" : desc_links,
						desc_outside_data != null ? "\"" + StringEscapeUtils.escapeJava(desc_outside_data) + "\"" : desc_outside_data,
						rr_data_files != null ? "\"" + StringEscapeUtils.escapeJava(rr_data_files) + "\"" : rr_data_files,
						rr_result_usage != null ? "\"" + StringEscapeUtils.escapeJava(rr_result_usage) + "\"" : rr_result_usage,
						desc_shared != null ? "\"" + StringEscapeUtils.escapeJava(desc_shared) + "\"" : desc_shared);

		logger.debug(insertvmsql);

		String insertUserVmMapsql = String
				.format("INSERT INTO "
								+ DBSchema.UserVmMapTable.TABLE_NAME
								+ " ("
								+ DBSchema.UserVmMapTable.VM_ID
								+ ","
								+ DBSchema.UserVmMapTable.GUID
								+ ","
								+ DBSchema.UserVmMapTable.ROLE
								+ ","
								+ DBSchema.UserVmMapTable.TOU
								+ ","
								+ DBSchema.UserVmMapTable.FULL_ACCESS
								+ ") VALUES"
								+ "(\"%s\", \"%s\", \"%s\", %s, %s)",
						vmid, userName, VMRole.OWNER_CONTROLLER.getName(), true, full_access);

		List<String> updates = new ArrayList<String>();
		updates.add(insertvmsql);
		updates.add(insertUserVmMapsql);

		String insertActivitySQL = getInsertActivitySQL(vmid, VMMode.NOT_DEFINED.toString(),
				VMMode.NOT_DEFINED.toString(),VMState.SHUTDOWN.toString(),
				VMState.SHUTDOWN.toString(), userName);
		logger.debug(insertActivitySQL);
		updates.add(insertActivitySQL);

		executeTransaction(updates);
	}

	public void addVmSharee(String vmid, VmUserRole vmUserRole, String desc_shared) throws SQLException {
		List<String> updates = new ArrayList<String>();
		String insertUserVmMapsql = String
				.format("INSERT INTO "
								+ DBSchema.UserVmMapTable.TABLE_NAME
								+ " ("
								+ DBSchema.UserVmMapTable.VM_ID
								+ ","
								+ DBSchema.UserVmMapTable.GUID
								+ ","
								+ DBSchema.UserVmMapTable.ROLE
								+ ","
								+ DBSchema.UserVmMapTable.TOU
								+ ","
								+ DBSchema.UserVmMapTable.FULL_ACCESS
								+ ") VALUES"
								+ "(\"%s\", \"%s\", \"%s\", %s, %s)",
						vmid, vmUserRole.getGuid(), vmUserRole.getRole().getName(),
						vmUserRole.getTou(), vmUserRole.isFull_access());
		logger.debug(insertUserVmMapsql);
		updates.add(insertUserVmMapsql);
		if (desc_shared != null) {
			String updateDescShared = String.format("UPDATE "
							+ DBSchema.VmTable.TABLE_NAME + " SET "
							+ DBSchema.VmTable.DESC_SHARED + "=CONCAT(IFNULL(" + DBSchema.VmTable.DESC_SHARED + ",''),\"%s\") "
							+ "WHERE "
							+ DBSchema.VmTable.VM_ID + "=\"%s\"", desc_shared + "\n", vmid);
			logger.debug(updateDescShared);
			updates.add(updateDescShared);
		}
		executeTransaction(updates);
	}

	public void removeVmSharee(String vmid, List<String> guid_list) throws SQLException {
		List<String> updates = new ArrayList<String>();
		// remove sharees from UserVmMap table
		String deleteUserVmMapsql = "DELETE FROM " + DBSchema.UserVmMapTable.TABLE_NAME + " WHERE "
						+ DBSchema.UserVmMapTable.VM_ID
						+ "=\"" + vmid + "\" AND "
						+ DBSchema.UserVmMapTable.GUID + " IN (\""
						+ StringUtils.join(guid_list, "\",\"")
						+ "\")";
		logger.debug(deleteUserVmMapsql);
		updates.add(deleteUserVmMapsql);

		// remove sharees from users table if there are no foreign key constraints in vms,vmacitivty or uservmmap tables
		for(String guid : guid_list) {
			String deleteUser = "DELETE FROM " + DBSchema.UserTable.TABLE_NAME + " WHERE "
					+ DBSchema.UserVmMapTable.GUID + "=\"" + guid + "\" AND"
					+ " NOT EXISTS (SELECT " + DBSchema.ActivityTable.GUID + " FROM " + DBSchema.ActivityTable.TABLE_NAME
					+ " WHERE " + DBSchema.ActivityTable.GUID + "=\"" + guid + "\") AND"
					+ " NOT EXISTS (SELECT " + DBSchema.VmTable.GUID + " FROM " + DBSchema.VmTable.TABLE_NAME
					+ " WHERE " + DBSchema.VmTable.GUID + "=\"" + guid + "\") AND"
					+ " NOT EXISTS (SELECT " + DBSchema.UserVmMapTable.GUID + " FROM " + DBSchema.UserVmMapTable.TABLE_NAME
					+ " WHERE " + DBSchema.UserVmMapTable.GUID + "=\"" + guid + "\")";
			logger.debug(deleteUser);
			updates.add(deleteUser);
		}
		executeTransaction(updates);
	}

	// assign owner_role to owner_guid and sharee_role to sharee_guid
	public void manageController(String vmid, String owner_guid, VMRole owner_role, String sharee_guid, VMRole sharee_role)
			throws SQLException {
		List<String> updates = new ArrayList<String>();
		String updateownersql = String.format("UPDATE "
						+ DBSchema.UserVmMapTable.TABLE_NAME + " SET "
						+ DBSchema.UserVmMapTable.ROLE + "=\"%s\" WHERE "
						+ DBSchema.UserVmMapTable.GUID + "=\"%s\" AND " + DBSchema.UserVmMapTable.VM_ID + "=\"%s\""
				, owner_role.getName(), owner_guid, vmid);
		logger.debug(updateownersql);
		updates.add(updateownersql);
		String updateshareesql = String.format("UPDATE "
						+ DBSchema.UserVmMapTable.TABLE_NAME + " SET "
						+ DBSchema.UserVmMapTable.ROLE + "=\"%s\" WHERE "
						+ DBSchema.UserVmMapTable.GUID + "=\"%s\" AND " + DBSchema.UserVmMapTable.VM_ID + "=\"%s\""
				, sharee_role.getName(), sharee_guid, vmid);
		logger.debug(updateshareesql);
		updates.add(updateshareesql);

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
						+ DBSchema.ActivityTable.GUID
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
				.append(" WHERE ").append(DBSchema.UserTable.GUID)
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
						.append(" WHERE ").append(DBSchema.UserTable.GUID)
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
					.append(" WHERE ").append(DBSchema.UserTable.GUID)
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
							.append(" WHERE ").append(DBSchema.UserTable.GUID)
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
			vmInfo = getAllVmInfoByID(vmid);
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
			String sql = "SELECT * FROM " + DBSchema.ResultTable.TABLE_NAME +
				" WHERE " + DBSchema.ResultTable.RESULT_ID + "=\"" + randomid + "\"";			
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();

			if (rs.next()) {
				java.util.Date startDate = rs.getString(DBSchema.ResultTable.NOTIFIED_TIME) != null
						? DATE_FORMATOR.parse(rs.getString(DBSchema.ResultTable.NOTIFIED_TIME))
						: null;
				//return new ResultBean(rs.getBinaryStream(DBSchema.ResultTable.DATA_FIELD), startDate);
				return new ResultBean(startDate);
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

	public List<ResultInfoBean> getVMResults(String vmid) throws SQLException{
		List<ResultInfoBean> res = new ArrayList<ResultInfoBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			String sql = "SELECT * FROM " + DBSchema.ResultTable.TABLE_NAME +
					" WHERE " + DBSchema.ResultTable.VM_ID + "=\"" + vmid + "\"";
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				ResultInfoBean resultInfoBean = new ResultInfoBean(rs.getString(DBSchema.ResultTable.VM_ID),
						rs.getString(DBSchema.ResultTable.RESULT_ID),
						rs.getString(DBSchema.ResultTable.CREATE_TIME),
						rs.getString(DBSchema.ResultTable.NOTIFIED),
						rs.getString(DBSchema.ResultTable.NOTIFIED_TIME),
						rs.getString(DBSchema.ResultTable.REVIEWER),
						rs.getString(DBSchema.ResultTable.STATUS),
						rs.getString(DBSchema.ResultTable.COMMENT)
				);
				res.add(resultInfoBean);
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

	/*
		This utility method is used one time to retrieve result file from DB and save in file system. After migration
		"datafield" is not a valid column in 'results' table
	 */
	public InputStream getResultInputStream(String randomid) throws
		SQLException, NoItemIsFoundInDBException, ParseException {
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			String sql = "SELECT * FROM " + DBSchema.ResultTable.TABLE_NAME +
				" WHERE " + DBSchema.ResultTable.RESULT_ID + "=\"" + randomid + "\"";
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();

			if (rs.next()) {
				java.util.Date startDate = rs.getString(DBSchema.ResultTable.NOTIFIED_TIME) != null
						? DATE_FORMATOR.parse(rs.getString(DBSchema.ResultTable.NOTIFIED_TIME))
						: null;
				return rs.getBinaryStream("datafield");
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
			String sql = "SELECT * FROM " + DBSchema.ResultTable.TABLE_NAME +
					" WHERE " + DBSchema.ResultTable.RESULT_ID + "=\"" + randomid + "\"";
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();

			if (rs.next()) {
				return new ResultInfoBean(rs.getString("vmid"),
						rs.getString("resultid"),
						rs.getString("createtime"),
						rs.getString("notified"),
						rs.getString("notifiedtime"),
						rs.getString("reviewer"),
						rs.getString("status"),
						rs.getString("comment")
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


	public ResultBean viewRleaseFile(String resultid) throws SQLException, NoItemIsFoundInDBException {
		//String sql = "SELECT "+ DBSchema.ResultTable.DATA_FIELD + "," + DBSchema.ResultTable.CREATE_TIME+
		String sql = "SELECT "+ DBSchema.ResultTable.CREATE_TIME +
				" FROM "+DBSchema.ResultTable.TABLE_NAME +  " WHERE " + DBSchema.ResultTable.RESULT_ID + " =\""+resultid+"\";";
		logger.debug(sql);
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		ResultBean res;

		Blob blob = null;
		ZipEntry zip = null;
		File file = null;
		try {
			conn = DBConnections.getInstance().getConnection();
			pst = conn.prepareStatement(sql);
			rs = pst.executeQuery();
			if(rs.next()){
				/*blob = rs.getBlob("datafield");
				InputStream in = blob.getBinaryStream();*/
				Date dt = rs.getDate("createtime");
				return (new ResultBean(dt));
			}else{
				throw new NoItemIsFoundInDBException("Result of " + resultid + " can't be found in db");
			}
		}finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (conn != null)
				conn.close();
		}
	}

	public List<ReviewInfoBean> getReviewData() throws SQLException
	{

		String sql = "SELECT " + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.RESULT_ID + " AS resultid," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.VM_ID + " AS vmid, " +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.NOTIFIED + " AS notified, " +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.STATUS + " AS status," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.REVIEWER + " AS reviewer," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.COMMENT +" AS comment, "+
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.CREATE_TIME+" AS createtime"+
				" FROM "+ DBSchema.VmTable.TABLE_NAME + " INNER JOIN "+ DBSchema.ResultTable.TABLE_NAME +
				" ON " + DBSchema.VmTable.TABLE_NAME+"."+DBSchema.VmTable.VM_ID + "=" + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.VM_ID;
		logger.debug(sql);

		List<ReviewInfoBean> res = new ArrayList<ReviewInfoBean>();

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {

			conn = DBConnections.getInstance().getConnection();
			pst = conn.prepareStatement(sql);
			rs = pst.executeQuery();

			while (rs.next()) {
				//get roles of the VM
				String vmid = rs.getString("vmid");
				List<VmUserRole> roles = getRolesWithVmid(vmid, true);

				ReviewInfoBean result = new ReviewInfoBean(
						vmid,
						rs.getString("resultid"),
						rs.getString("notified"),
						rs.getString("status"),
						null,
						null,
						rs.getString("reviewer"),
						rs.getString("comment"),
						rs.getString("createtime"),
						roles
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


    public List<ReviewInfoBean> getReleased() throws SQLException
    {
		String sql = "SELECT " + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.RESULT_ID + " AS resultid," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.VM_ID + " AS vmid, " +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.NOTIFIED + " AS notified, " +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.STATUS + " AS status," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.REVIEWER + " AS reviewer," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.COMMENT +" AS comment, "+
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.CREATE_TIME+" AS createtime"+
				" FROM "+ DBSchema.VmTable.TABLE_NAME + " INNER JOIN "+ DBSchema.ResultTable.TABLE_NAME +
				" ON " + DBSchema.VmTable.TABLE_NAME+"."+DBSchema.VmTable.VM_ID + "=" + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.VM_ID +
				" WHERE " + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.NOTIFIED + "=\"YES\"";
		logger.debug(sql);

        List<ReviewInfoBean> res = new ArrayList<ReviewInfoBean>();

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {

            conn = DBConnections.getInstance().getConnection();
            pst = conn.prepareStatement(sql);
            rs = pst.executeQuery();
            while (rs.next()) {
				//get roles of the VM
				String vmid = rs.getString("vmid");
				List<VmUserRole> roles = getRolesWithVmid(vmid, true);

                ReviewInfoBean result = new ReviewInfoBean(
						vmid,
                        rs.getString("resultid"),
                        "",
                        "",
						null,
						null,
						rs.getString("reviewer"),
						"",
						rs.getString("createtime"),
						roles
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


	public List<ReviewInfoBean> getUnreleased() throws SQLException
	{
		String sql = "SELECT " + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.RESULT_ID + " AS resultid," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.VM_ID + " AS vmid, " +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.NOTIFIED + " AS notified, " +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.STATUS + " AS status," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.REVIEWER + " AS reviewer," +
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.COMMENT +" AS comment, "+
				DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.CREATE_TIME+" AS createtime"+
				" FROM "+ DBSchema.VmTable.TABLE_NAME + " INNER JOIN "+ DBSchema.ResultTable.TABLE_NAME +
				" ON " + DBSchema.VmTable.TABLE_NAME+"."+DBSchema.VmTable.VM_ID + "=" + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.VM_ID +
				" WHERE " + DBSchema.ResultTable.TABLE_NAME+"."+DBSchema.ResultTable.NOTIFIED + "=\"NO\"";
		logger.debug(sql);

		List<ReviewInfoBean> res = new ArrayList<ReviewInfoBean>();

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {

			conn = DBConnections.getInstance().getConnection();
			pst = conn.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				//get roles of the VM
				String vmid = rs.getString("vmid");
				List<VmUserRole> roles = getRolesWithVmid(vmid, true);

				ReviewInfoBean result = new ReviewInfoBean(
						vmid,
						rs.getString("resultid"),
						"",
						"",
						null,
						null,
						rs.getString("reviewer"),
						"",
						rs.getString("createtime"),
						roles
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


	public String getStatus(String resulitid) throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format("SELECT %s FROM %s WHERE %s=\"%s\";",
				DBSchema.ResultTable.STATUS, DBSchema.ResultTable.TABLE_NAME,
					DBSchema.ResultTable.RESULT_ID, resulitid
		);
		logger.debug(sql);

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String res = null;
		try {

			conn = DBConnections.getInstance().getConnection();
			pst = conn.prepareStatement(sql);
			rs = pst.executeQuery();
			if (rs.next()) {
				res = rs.getString(DBSchema.ResultTable.STATUS);
			} else {
				throw new NoItemIsFoundInDBException("Result of " + resulitid + " can't be found in db!");
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


	public String getComment(String resultid) throws SQLException{
		String sql =String.format("SELECT %s FROM %s WHERE %s=\"%s\";",
				DBSchema.ResultTable.COMMENT, DBSchema.ResultTable.TABLE_NAME,
								DBSchema.ResultTable.RESULT_ID, resultid);
		logger.debug(sql);

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String res = null;
		try {

			conn = DBConnections.getInstance().getConnection();
			pst = conn.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				res = rs.getString("comment");
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

	public void insertResult(String vmid, String randomid) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			java.util.Date dt = new java.util.Date();
			String currentTime = DATE_FORMATOR.format(dt);
			String insertResult = String.format(
				"INSERT INTO " + DBSchema.ResultTable.TABLE_NAME + " (%s, %s, %s) VALUES" + "(?, ?, ?)",
				DBSchema.ResultTable.VM_ID, DBSchema.ResultTable.RESULT_ID, 
				DBSchema.ResultTable.CREATE_TIME);
				//DBSchema.ResultTable.DATA_FIELD, DBSchema.ResultTable.CREATE_TIME);

			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(insertResult);
			pst.setString(1, vmid);
			pst.setString(2, randomid);
			//pst.setBinaryStream(3, input);
			pst.setString(3, currentTime);
			
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
			String query = String.format("SELECT %s, %s FROM %s, %s WHERE %s=%s AND %s=%s",
				// selected fields
				DBSchema.ResultTable.TABLE_NAME + "." + DBSchema.ResultTable.RESULT_ID,
				DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID,
				// selected tables
				DBSchema.ResultTable.TABLE_NAME, DBSchema.VmTable.TABLE_NAME,
				// where clause
				DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID,
					DBSchema.ResultTable.TABLE_NAME + "." + DBSchema.ResultTable.VM_ID,
				DBSchema.ResultTable.NOTIFIED, "\"NO\"");
			logger.debug(query);
			
			pst = connection.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				//get roles of the vm
				String vmid = rs.getString(DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID);
				List<VmUserRole> vmUserRoles = DBOperations.getInstance().getRolesWithVmid(vmid, true);
				String resultId = rs.getString(DBSchema.ResultTable.TABLE_NAME + "." + DBSchema.ResultTable.RESULT_ID);

				UserResultBean bean = new UserResultBean(null, null, resultId, vmid, vmUserRoles);
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
	
	public void updateResultAsNotified(String resultid) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String updateResult = String.format(
				"UPDATE %s SET %s=%s WHERE %s=%s", DBSchema.ResultTable.TABLE_NAME,
				DBSchema.ResultTable.NOTIFIED, "\"YES\"", DBSchema.ResultTable.RESULT_ID, "\""+ resultid + "\"");
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


	public void updateResult(String resultid, String status) throws SQLException{
		Connection connection = null;
		PreparedStatement pst = null;
		java.util.Date dateobj = new java.util.Date();
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = df.format(dateobj);

		try {
			connection = DBConnections.getInstance().getConnection();
			String updateResult = String.format(
					"UPDATE %s SET %s=%s, %s=\"%s\", %s=%s WHERE %s=%s", DBSchema.ResultTable.TABLE_NAME,
					DBSchema.ResultTable.NOTIFIED, "\"YES\"",
					DBSchema.ResultTable.STATUS, status,
					DBSchema.ResultTable.NOTIFIED_TIME, "\"" + currentTime + "\"",
					DBSchema.ResultTable.RESULT_ID, "\""+ resultid + "\"");
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


	public void updateResultAsReleased(String resultid, String comment, String reviewer) throws SQLException{
		Connection connection = null;
		PreparedStatement pst = null;
		java.util.Date dateobj = new java.util.Date();
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = df.format(dateobj);

		try {
			connection = DBConnections.getInstance().getConnection();
			String updateResult = String.format(
					"UPDATE %s SET %s=%s, %s=%s, %s=%s, %s=%s, %s=%s WHERE %s=%s", DBSchema.ResultTable.TABLE_NAME,
					DBSchema.ResultTable.NOTIFIED, "\"YES\"",
					DBSchema.ResultTable.STATUS,"\"Released\"",
					DBSchema.ResultTable.COMMENT, "\""+comment+"\"",
					DBSchema.ResultTable.NOTIFIED_TIME, "\"" + currentTime + "\"",
					DBSchema.ResultTable.REVIEWER, "\"" + reviewer + "\"",
					DBSchema.ResultTable.RESULT_ID, "\""+ resultid + "\"");
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
	public void updateResultAsRejected(String resultid, String comment, String reviewer) throws SQLException{
		Connection connection = null;
		PreparedStatement pst = null;
		java.util.Date dateobj = new java.util.Date();
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentTime = df.format(dateobj);

		try {
			connection = DBConnections.getInstance().getConnection();
			String updateResult = String.format(
					"UPDATE %s SET %s=%s, %s=%s, %s=%s, %s=%s, %s=%s WHERE %s=%s", DBSchema.ResultTable.TABLE_NAME,
					DBSchema.ResultTable.NOTIFIED, "\"YES\"", DBSchema.ResultTable.STATUS,"\"Rejected\"",
					DBSchema.ResultTable.COMMENT, "\""+comment+"\"",
					DBSchema.ResultTable.NOTIFIED_TIME, "\"" + currentTime + "\"",
					DBSchema.ResultTable.REVIEWER, "\"" + reviewer + "\"",
					DBSchema.ResultTable.RESULT_ID, "\""+ resultid + "\"");
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
				DBSchema.UserTable.TABLE_NAME+"."+DBSchema.UserTable.GUID,
				DBSchema.VmTable.TABLE_NAME+"."+DBSchema.VmTable.GUID,
				DBSchema.VmTable.VM_ID, vmid);
			pst = connection.prepareStatement(query);
			
			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return new UserBean(result.getString(DBSchema.UserTable.GUID),
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

	public VmUserRole getUserRoleWithVmid(String username, String vmid) throws SQLException,
		NoItemIsFoundInDBException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
				"SELECT %s, %s, %s, %s, %s FROM %s, %s WHERE %s=%s AND %s=\"%s\" AND %s=\"%s\"",
				DBSchema.UserTable.USER_EMAIL, DBSchema.UserVmMapTable.ROLE,
				DBSchema.UserVmMapTable.TABLE_NAME + "." + 	DBSchema.UserVmMapTable.GUID,
				DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.TOU,
				DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS,
				DBSchema.UserVmMapTable.TABLE_NAME, DBSchema.UserTable.TABLE_NAME,
				DBSchema.UserVmMapTable.TABLE_NAME+"."+DBSchema.UserVmMapTable.GUID,
				DBSchema.UserTable.TABLE_NAME+"."+DBSchema.UserTable.GUID,
				DBSchema.UserVmMapTable.VM_ID, vmid,
				DBSchema.UserVmMapTable.TABLE_NAME+"."+DBSchema.UserVmMapTable.GUID, username);
			pst = connection.prepareStatement(query);

			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return new VmUserRole(result.getString(DBSchema.UserTable.USER_EMAIL),
					VMRole.fromName(result.getString(DBSchema.UserVmMapTable.ROLE)),
					result.getBoolean(DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.TOU),
					result.getString(DBSchema.UserVmMapTable.TABLE_NAME + "." + 	DBSchema.UserVmMapTable.GUID),
					result.getObject(
							DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS) == null
							? null
							: result.getBoolean(DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS)
				);
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

	public VmUserRole getOwnerOfVM(String vmid) throws SQLException, NoItemIsFoundInDBException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = "SELECT " +
					DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.GUID + ", " +
					DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_EMAIL + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.ROLE + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.TOU + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS + " " +
					"FROM " +
					DBSchema.UserTable.TABLE_NAME + ", " + DBSchema.UserVmMapTable.TABLE_NAME + " " +
					"WHERE " + DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.GUID + "=" +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.GUID + " " +
					"AND " + DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.VM_ID + "=\"" +
					vmid + "\" " +
					"AND " + DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.ROLE +
					" LIKE \"%%" + VMRole.OWNER + "%%\"";
			pst = connection.prepareStatement(query);
			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return new VmUserRole(
						result.getString(DBSchema.UserTable.USER_EMAIL),
						VMRole.fromName(result.getString(DBSchema.UserVmMapTable.ROLE)),
						result.getBoolean(DBSchema.UserVmMapTable.TOU),
						result.getString(DBSchema.UserTable.GUID),
						result.getObject(
								DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS) == null
								? null
								: result.getBoolean(DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS));
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

	public List<VmUserRole> getRolesWithVmid(String vmid, boolean getUsername) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		List<VmUserRole> roles = new ArrayList<VmUserRole>();

		try {
			connection = DBConnections.getInstance().getConnection();
			String role_sql = "SELECT " +
					DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.GUID + ", " +
					DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.USER_EMAIL + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.ROLE + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.TOU + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS + " " +
					"FROM " +
					DBSchema.VmTable.TABLE_NAME + ", " +
					DBSchema.UserTable.TABLE_NAME + ", " +
					DBSchema.UserVmMapTable.TABLE_NAME + " " +
					"WHERE " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID + "=" +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.VM_ID + " " +
					"AND "  + DBSchema.UserTable.TABLE_NAME + "." + DBSchema.UserTable.GUID + "=" +
					DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.GUID + " " +
					"AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.VmTable.VM_ID + "=\"" + vmid + "\"";
			pst = connection.prepareStatement(role_sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				VmUserRole role;
				if(getUsername) {
					role = new VmUserRole(
							rs.getString(DBSchema.UserTable.USER_EMAIL),
							VMRole.fromName(rs.getString(DBSchema.UserVmMapTable.ROLE)),
							rs.getBoolean(DBSchema.UserVmMapTable.TOU),
							rs.getString(DBSchema.UserTable.GUID),
							rs.getObject(
									DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS) == null
									? null
									: rs.getBoolean(DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS));
				} else {
					role = new VmUserRole(
							rs.getString(DBSchema.UserTable.USER_EMAIL),
							VMRole.fromName(rs.getString(DBSchema.UserVmMapTable.ROLE)),
							rs.getBoolean(DBSchema.UserVmMapTable.TOU),
							rs.getObject(
									DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS) == null
									? null
									: rs.getBoolean(DBSchema.UserVmMapTable.TABLE_NAME + "." + DBSchema.UserVmMapTable.FULL_ACCESS));
				}
				roles.add(role);
			}
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
		return roles;
	}


	public VMPorts getPortsWithVMId(String vmid) throws SQLException,
		NoItemIsFoundInDBException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
				"SELECT * FROM %s WHERE %s=\"%s\"",
				DBSchema.VmTable.TABLE_NAME,
				DBSchema.VmTable.VM_ID, vmid);
			pst = connection.prepareStatement(query);

			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return new VMPorts(result.getString(DBSchema.VmTable.HOST),
					result.getInt(DBSchema.VmTable.SSH_PORT), result.getInt(DBSchema.VmTable.VNC_PORT));
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


	public String getVMIDWithResultid(String resultid)
			throws SQLException, NoItemIsFoundInDBException
	{

		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();

			String query = String.format(
					"SELECT %s FROM %s WHERE %s=\"%s\"",
					DBSchema.VmTable.VM_ID,
					DBSchema.ResultTable.TABLE_NAME,
					DBSchema.ResultTable.RESULT_ID, resultid);

			pst = connection.prepareStatement(query);

			ResultSet result = pst.executeQuery();

			if (result.next()) {
				String res = result.getString(DBSchema.ResultTable.VM_ID);
				return res;
			} else {
				throw new NoItemIsFoundInDBException(resultid + " is not associated with any user!");
			}

		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public String getUserPubKey(String userName) throws SQLException,
			NoItemIsFoundInDBException, UnsupportedEncodingException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
					"SELECT %s FROM %s WHERE %s=\"%s\"",
					DBSchema.UserTable.PUB_KEY, DBSchema.UserTable.TABLE_NAME,
					DBSchema.UserTable.TABLE_NAME+"."+DBSchema.UserTable.GUID,
					userName);
			pst = connection.prepareStatement(query);

			ResultSet result = pst.executeQuery();
			if (result.next()) {
				String encodedKey = result.getNString(DBSchema.UserTable.PUB_KEY);
				if(encodedKey != null ) {
					byte[] asBytes = Base64.getDecoder().decode(encodedKey);
					return new String(asBytes, "utf-8");
				} else {
					return null;
				}
			} else {
				throw new NoItemIsFoundInDBException(userName + " user could not be found!");
			}

		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public String getUserEmail(String userName) throws SQLException, NoItemIsFoundInDBException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
					"SELECT %s FROM %s WHERE %s=\"%s\"",
					DBSchema.UserTable.USER_EMAIL, DBSchema.UserTable.TABLE_NAME,
					DBSchema.UserTable.TABLE_NAME+"."+DBSchema.UserTable.GUID,
					userName);
			pst = connection.prepareStatement(query);

			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return result.getString(DBSchema.UserTable.USER_EMAIL);
			} else {
				throw new NoItemIsFoundInDBException(userName + " user could not be found!");
			}

		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public void updateUserPubKey(String userName, String sshKey) throws SQLException, UnsupportedEncodingException {
		List<String> updates = new ArrayList<String>();
		String encodedKey = null;
		if(sshKey != null) {
			encodedKey = Base64.getEncoder().encodeToString(sshKey.getBytes("utf-8"));
		}
		String updateusersql = String.format("UPDATE "
				+ DBSchema.UserTable.TABLE_NAME + " SET "
				+ DBSchema.UserTable.PUB_KEY + (sshKey ==  null ? "=%s" : "=\"%s\"")  + " WHERE "
				+ DBSchema.UserTable.GUID + "=\"%s\"", encodedKey, userName);
		updates.add(updateusersql);
		executeTransaction(updates);
	}


	public boolean getUserTOU(String userName) throws SQLException,
			NoItemIsFoundInDBException, UnsupportedEncodingException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
					"SELECT %s FROM %s WHERE %s=\"%s\"",
					DBSchema.UserTable.TOU, DBSchema.UserTable.TABLE_NAME,
					DBSchema.UserTable.TABLE_NAME+"."+DBSchema.UserTable.GUID,
					userName);
			pst = connection.prepareStatement(query);

			ResultSet result = pst.executeQuery();
			if (result.next()) {
				return result.getBoolean(DBSchema.UserTable.TOU);
			} else {
				throw new NoItemIsFoundInDBException(userName + " user could not be found!");
			}

		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public void updateVmUserTOU(String userName, String vmid, boolean tou) throws SQLException, UnsupportedEncodingException {
		List<String> updates = new ArrayList<String>();
		String updateusersql = String.format("UPDATE "
				+ DBSchema.UserVmMapTable.TABLE_NAME + " SET "
				+ DBSchema.UserVmMapTable.TOU + "=%s WHERE "
				+ DBSchema.UserVmMapTable.GUID + "=\"%s\" AND " + DBSchema.UserVmMapTable.VM_ID + "=\"%s\""
				, tou, userName, vmid);
		updates.add(updateusersql);
		executeTransaction(updates);
	}

	public void updateUserTOU(String userName, boolean tou) throws SQLException, UnsupportedEncodingException {
		List<String> updates = new ArrayList<String>();
		String updateusersql = String.format("UPDATE "
				+ DBSchema.UserTable.TABLE_NAME + " SET "
				+ DBSchema.UserTable.TOU + "=%s WHERE "
				+ DBSchema.UserTable.GUID + "=\"%s\"", tou, userName);
		updates.add(updateusersql);
		executeTransaction(updates);
	}

	public void updateUserEmail(String userName, String email) throws SQLException, UnsupportedEncodingException {
		List<String> updates = new ArrayList<String>();
		String updateusersql = String.format("UPDATE "
				+ DBSchema.UserTable.TABLE_NAME + " SET "
				+ DBSchema.UserTable.USER_EMAIL + "=\"%s\" WHERE "
				+ DBSchema.UserTable.GUID + "=\"%s\"", email, userName);
		updates.add(updateusersql);
		executeTransaction(updates);
	}

	public void updateVm(String vmid, String type, String title, Boolean consent,
						 String desc_nature, String desc_requirement, String desc_links, String desc_outside_data,
						 String rr_data_files, String rr_result_usage, Boolean full_access, String desc_shared)
			throws SQLException {
		List<String> updates = new ArrayList<String>();
		String updatevmsql = String.format("UPDATE "
				+ DBSchema.VmTable.TABLE_NAME + " SET "
				+ DBSchema.VmTable.TYPE + "=\"%s\", "
				+ DBSchema.VmTable.TITLE + "=\"%s\", "
				+ DBSchema.VmTable.CONSENT + "=%s, "
				+ DBSchema.VmTable.DESC_NATURE + "=\"%s\", "
				+ DBSchema.VmTable.DESC_REQUIREMENT + "=\"%s\", "
				+ DBSchema.VmTable.DESC_LINKS + "=\"%s\", "
				+ DBSchema.VmTable.DESC_OUTSIDE_DATA + "=\"%s\", "
				+ DBSchema.VmTable.RR_DATA_FILES + "=\"%s\", "
				+ DBSchema.VmTable.RR_RESULT_USAGE + "=\"%s\", "
				+ DBSchema.VmTable.DESC_SHARED + "=\"%s\" "
				+ "WHERE "
				+ DBSchema.VmTable.VM_ID + "=\"%s\"",
				type
				, StringEscapeUtils.escapeJava(title)
				, consent
				, StringEscapeUtils.escapeJava(desc_nature)
				, StringEscapeUtils.escapeJava(desc_requirement)
				, StringEscapeUtils.escapeJava(desc_links)
				, StringEscapeUtils.escapeJava(desc_outside_data)
				, StringEscapeUtils.escapeJava(rr_data_files)
				, StringEscapeUtils.escapeJava(rr_result_usage)
				, desc_shared != null ? StringEscapeUtils.escapeJava(desc_shared) + "\n" : ""
				, vmid);
		String update_uservmmap_sql = String.format("UPDATE "
						+ DBSchema.UserVmMapTable.TABLE_NAME + " SET "
						+ DBSchema.UserVmMapTable.FULL_ACCESS + "=%s "
						+ "WHERE "
						+ DBSchema.UserVmMapTable.VM_ID + "=\"%s\"",
				full_access, vmid);
		updates.add(updatevmsql);
		updates.add(update_uservmmap_sql);
		executeTransaction(updates);
	}

	public void updateVmType(String vmid, String type, Boolean full_access, List<String> guid_list) throws SQLException {
		List<String> updates = new ArrayList<String>();
		if(type != null) {
			String updatevmsql = String.format("UPDATE "
							+ DBSchema.VmTable.TABLE_NAME + " SET "
							+ DBSchema.VmTable.TYPE + "=\"%s\" "
							+ "WHERE "
							+ DBSchema.VmTable.VM_ID + "=\"%s\"",
					type, vmid);
			updates.add(updatevmsql);
		}
		String update_uservmmap_sql = "UPDATE "
				+ DBSchema.UserVmMapTable.TABLE_NAME + " SET "
				+ DBSchema.UserVmMapTable.FULL_ACCESS + "=" + full_access + " "
				+ "WHERE ("
				+ DBSchema.UserVmMapTable.VM_ID + "=\"" + vmid + "\") AND "
				+ DBSchema.UserVmMapTable.GUID + " IN (\""
				+ StringUtils.join(guid_list, "\",\"")
				+ "\")";
		updates.add(update_uservmmap_sql);
		executeTransaction(updates);
	}

	public void updateVmHostAndPorts(String vmid, VMPorts vmports) throws SQLException {
		List<String> updates = new ArrayList<String>();
		String updatevmsql = String.format("UPDATE "
				+ DBSchema.VmTable.TABLE_NAME + " SET "
				+ DBSchema.VmTable.HOST + "=\"%s\", "
				+ DBSchema.VmTable.SSH_PORT + "=%d, "
				+ DBSchema.VmTable.VNC_PORT + "=%d "
				+ "WHERE "
				+ DBSchema.VmTable.VM_ID + "=\"%s\"",
				vmports.publicip, vmports.sshport, vmports.vncport, vmid);
		updates.add(updatevmsql);
		executeTransaction(updates);
	}

	public void close() {
		DBConnections.getInstance().close();
	}

	public void addPorts(String vmid, VMPorts vmPorts) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;
		try {
			String insertResult = String.format(
					"INSERT INTO " + DBSchema.PortTable.TABLE_NAME + " (%s, %s, %s, %s) VALUES" + "(?, ?, ?, ?)",
					DBSchema.PortTable.VM_ID, DBSchema.PortTable.HOST,
					DBSchema.PortTable.SSH_PORT, DBSchema.PortTable.VNC_PORT);

			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(insertResult);
			pst.setString(1, vmid);
			pst.setString(2, vmPorts.publicip);
			pst.setInt(3, vmPorts.sshport);
			pst.setInt(4, vmPorts.vncport);
			pst.executeUpdate();
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public void deletePort(String vmid, VMPorts vmPorts) throws SQLException {
		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
					"DELETE FROM %s WHERE %s=\"%s\" AND %s=\"%s\" AND %s=%d AND %s=%d" ,
					DBSchema.PortTable.TABLE_NAME,
					DBSchema.PortTable.VM_ID, vmid,
					DBSchema.PortTable.HOST, vmPorts.publicip,
					DBSchema.PortTable.SSH_PORT, vmPorts.sshport,
					DBSchema.PortTable.VNC_PORT, vmPorts.vncport
			);
			pst = connection.prepareStatement(query);
			pst.executeUpdate();
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}
	}

	public boolean portsExists(String vmid, VMPorts vmPorts) throws SQLException {
		boolean exists = false;

		Connection connection = null;
		PreparedStatement pst = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format(
					"SELECT * FROM %s WHERE %s=\"%s\" AND %s=\"%s\" AND %s=%d AND %s=%d" ,
					DBSchema.PortTable.TABLE_NAME,
					DBSchema.PortTable.VM_ID, vmid,
					DBSchema.PortTable.HOST, vmPorts.publicip,
					DBSchema.PortTable.SSH_PORT, vmPorts.sshport,
					DBSchema.PortTable.VNC_PORT, vmPorts.vncport
			);
			pst = connection.prepareStatement(query);
			ResultSet result = pst.executeQuery();
			if (result.next()) {
				exists = true;
			}
		} finally {
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}

		return exists;
	}

	public List<PortBean> getPorts() throws SQLException {
		List<PortBean> res = new ArrayList<PortBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format("SELECT * FROM %s", DBSchema.PortTable.TABLE_NAME);
			logger.debug(query);

			pst = connection.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				PortBean bean = new PortBean(
						rs.getString(DBSchema.PortTable.VM_ID),
						rs.getString(DBSchema.PortTable.HOST),
						rs.getInt(DBSchema.PortTable.SSH_PORT),
						rs.getInt(DBSchema.PortTable.VNC_PORT));
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

	public List<Integer> getPortsOfHost(String host) throws SQLException {
		List<Integer> res = new ArrayList<Integer>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format("SELECT * FROM %s WHERE %s=\"%s\"",
					DBSchema.PortTable.TABLE_NAME,
					DBSchema.PortTable.HOST, host);
			logger.debug(query);

			pst = connection.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				res.add(rs.getInt(DBSchema.PortTable.SSH_PORT));
				res.add(rs.getInt(DBSchema.PortTable.VNC_PORT));
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

	public List<VMPorts> getPortsOfVm(String vmid) throws SQLException {
		List<VMPorts> res = new ArrayList<VMPorts>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			String query = String.format("SELECT * FROM %s WHERE %s=\"%s\"",
					DBSchema.PortTable.TABLE_NAME,
					DBSchema.PortTable.VM_ID, vmid);
			logger.debug(query);

			pst = connection.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				VMPorts vmPort = new VMPorts(rs.getString(DBSchema.PortTable.HOST),
						rs.getInt(DBSchema.PortTable.SSH_PORT), rs.getInt(DBSchema.PortTable.VNC_PORT));
				res.add(vmPort);
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
}
