package edu.indiana.d2i.sloan.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.bean.VmStatusBean;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.vm.VMPorts;
import edu.indiana.d2i.sloan.vm.VMMode;
import edu.indiana.d2i.sloan.vm.VMState;

public class DBOperations {
	private static Logger logger = Logger.getLogger(DBOperations.class);
	private static DBOperations instance = null;

	private DBOperations() {

	}
	
	static {
		instance = new DBOperations();
	}
	
	private void executeTransaction(List<String> updates) throws SQLException {
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
			if (st != null) st.close();
			if (connection != null) connection.close();
		}
	}
	
	private List<VmInfoBean> getVmInfoInternal(String sql) throws SQLException {
		logger.debug(sql);
		
		List<VmInfoBean> res = new ArrayList<VmInfoBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		
		try {
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				VmInfoBean vminfo = new VmInfoBean(
						rs.getString(DBSchema.VmTable.VM_ID), rs.getString(DBSchema.VmTable.PUBLIC_IP),
						rs.getString(DBSchema.VmTable.WORKING_DIR), null, null,
						rs.getInt(DBSchema.VmTable.SSH_PORT), rs.getInt(DBSchema.VmTable.VNC_PORT),
						VMMode.valueOf(rs.getString(DBSchema.VmTable.VM_MODE)), VMState.valueOf(rs.getString(DBSchema.VmTable.STATE)) 
						);
				res.add(vminfo);
			}
		} finally {
			if (rs != null) rs.close();
			if (pst != null) pst.close();
			if (connection != null) connection.close();
		}
		return res;
	}

	public static DBOperations getInstance() {
		return instance;
	}

	public boolean quotaExceedsLimit(CreateVmRequestBean request) {
		return false;
	}
	
	public boolean vmExists(String userName, String vmid) {
		return true;
	}
	
	public void insertUserIfNotExists(String userName) throws SQLException {
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
					+ "(%s) VALUES"
					+ "(?)", DBSchema.UserTable.USER_NAME);
				pst2 = connection.prepareStatement(insertUser);
				pst2.setString(1, userName);
				pst2.executeUpdate();
			} 			
		} finally {
			if (rs != null) rs.close();
			if (pst1 != null) pst1.close();
			if (pst2 != null) pst2.close();
			if (connection != null) connection.close();
		}
	}
	
	public List<VmInfoBean> getVmInfo() throws SQLException {
		String sql = "SELECT " + DBSchema.VmTable.VM_MODE + "," 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + ","
				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR
//				+ image path & policy path 
				+ " FROM " 
				+ DBSchema.VmTable.TABLE_NAME;
		return getVmInfoInternal(sql);
//		List<VmInfoBean> vminfoList = getVmInfoInternal(sql);
//		List<VmStatusBean> status = new ArrayList<VmStatusBean>();
//		for (VmInfoBean vminfo : vminfoList) {
//			status.add(new VmStatusBean(vminfo.getVmid(), vminfo.getVmmode().toString(), 
//				vminfo.getVmstate().toString(), vminfo.getPublicip(), 
//				vminfo.getSshport(), vminfo.getVncport()));
//		}
//		return status;
	}
	
	public List<VmInfoBean> getVmInfo(String userName) throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format(
				"SELECT " + DBSchema.VmTable.VM_MODE + "," 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + ","
				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR
//				+ image path & policy path 
				+ " FROM " 
				+ DBSchema.UserVmTable.TABLE_NAME + ","
				+ DBSchema.VmTable.TABLE_NAME 
				+ " WHERE " 
				+ DBSchema.UserVmTable.TABLE_NAME + "."  +  DBSchema.UserVmTable.VM_ID + "=" 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID
				+ " AND " + DBSchema.UserVmTable.USER_NAME + "=\"%s\"", userName);
		return getVmInfoInternal(sql);
		
//		List<VmInfoBean> vminfoList = getVmInfoInternal(sql);
//		if (vminfoList.size() == 0) 
//			throw new NoItemIsFoundInDBException(String.format(
//				"No VM is found in DB for user %s.", userName));
//		
//		List<VmStatusBean> status = new ArrayList<VmStatusBean>();
//		for (VmInfoBean vminfo : vminfoList) {
//			status.add(new VmStatusBean(vminfo.getVmid(), vminfo.getVmmode().toString(), 
//				vminfo.getVmstate().toString(), vminfo.getPublicip(), 
//				vminfo.getSshport(), vminfo.getVncport()));
//		}
//		return status;
	}
	
//	public List<VmStatusBean> getVmStatus(String userName, String vmid) 
//		throws SQLException, NoItemIsFoundInDBException {
//		String sql = String.format(
//				"SELECT " + DBSchema.VmTable.VM_MODE + "," 
//				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + ","
//				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
//				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT + ","
//				+ DBSchema.VmTable.WORKING_DIR
////				+ image path & policy path 
//				+ " FROM " 
//				+ DBSchema.UserVmTable.TABLE_NAME + ","
//				+ DBSchema.VmTable.TABLE_NAME 
//				+ " WHERE " 
//				+ DBSchema.UserVmTable.TABLE_NAME + "."  +  DBSchema.UserVmTable.VM_ID + "=" 
//				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID
//				+ " AND " + DBSchema.UserVmTable.USER_NAME + "=\"%s\""
//				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.UserVmTable.VM_ID + "=\"%s\"", userName, vmid);
//		List<VmInfoBean> vminfoList = getVmInfoInternal(sql);
//		if (vminfoList.size() == 0) 
//			throw new NoItemIsFoundInDBException(String.format(
//				"VM %s with user %s is not found in DB.", vmid, userName));
//		
//		List<VmStatusBean> status = new ArrayList<VmStatusBean>();
//		for (VmInfoBean vminfo : vminfoList) {
//			status.add(new VmStatusBean(vmid, vminfo.getVmmode().toString(), 
//				vminfo.getVmstate().toString(), vminfo.getPublicip(), 
//				vminfo.getSshport(), vminfo.getVncport()));
//		}
//		return status;
//	}
	
	public VmInfoBean getVmInfo(String userName, String vmid) throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format(
				"SELECT " + DBSchema.VmTable.VM_MODE + "," 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + ","
				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT + ","
				+ DBSchema.VmTable.WORKING_DIR
//				+ image path & policy path
				+ " FROM " 
				+ DBSchema.UserVmTable.TABLE_NAME + ","
				+ DBSchema.VmTable.TABLE_NAME 
				+ " WHERE " 
				+ DBSchema.UserVmTable.TABLE_NAME + "."  +  DBSchema.UserVmTable.VM_ID + "=" 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID
				+ " AND " + DBSchema.UserVmTable.USER_NAME + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.UserVmTable.VM_ID + "=\"%s\"", userName, vmid);
		
		logger.debug(sql);
		
		List<VmInfoBean> res = getVmInfoInternal(sql);
		if (res.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"VM %s with user %s is not found in DB.", vmid, userName));
		return res.get(0);
	}
	
	public void addVM(String userName, String vmid, String imageName, VMPorts host, String workDir) throws SQLException {		
		String insertvmsql = String.format("INSERT INTO " + DBSchema.VmTable.TABLE_NAME + " ("
			+ DBSchema.VmTable.VM_ID + "," + DBSchema.VmTable.STATE + "," 
			+ DBSchema.VmTable.VM_MODE + ","
			+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.SSH_PORT + "," 
			+ DBSchema.VmTable.VNC_PORT + "," + DBSchema.VmTable.WORKING_DIR + "," + DBSchema.VmTable.IMAGE_NAME + ") VALUES"
			+ "(\"%s\", \"%s\", \"%s\", \"%s\", %d, %d, \"%s\", \"%s\")", 
			vmid, VMState.BUILDING.toString(), VMMode.NOT_DEFINED.toString(), host.publicip, host.sshport, host.vncport, workDir, imageName);
		String insertvmusersql = String.format("INSERT INTO " + DBSchema.UserVmTable.TABLE_NAME + " ("
			+ DBSchema.UserVmTable.USER_NAME + "," + DBSchema.UserVmTable.VM_ID + ") VALUES"
			+ "(\"%s\", \"%s\")", 
			userName, vmid);
		
		logger.debug(insertvmsql);
		logger.debug(insertvmusersql);
		
		List<String> updates = new ArrayList<String>();
		updates.add(insertvmsql);
		updates.add(insertvmusersql);
		
		executeTransaction(updates);
	}
	
	public void deleteVMs(String vmid) throws SQLException {
		List<String> updates = new ArrayList<String>(); 
		String deletevmsql = String.format("DELETE FROM " + DBSchema.VmTable.TABLE_NAME + 
				" where " + DBSchema.VmTable.VM_ID + "=\"%s\"", vmid);
		updates.add(deletevmsql);
		executeTransaction(updates);	
	}
	
	public void updateVMState(String vmid, VMState state) throws SQLException {
		List<String> updates = new ArrayList<String>(); 
		String updatevmsql = String.format("UPDATE " + DBSchema.VmTable.TABLE_NAME + 
			" SET " + DBSchema.VmTable.STATE + "=\"%s\" WHERE " +
			DBSchema.VmTable.VM_ID + "=\"%s\"", state.toString(), vmid);
		updates.add(updatevmsql);
		executeTransaction(updates);
	}
	
	public void close() {
		DBConnections.getInstance().close();
	}
}
