package edu.indiana.d2i.sloan.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

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
	
	private List<VmStatusBean> getVmStatusInternal(String sql) throws SQLException {
		logger.debug(sql);
		
		List<VmStatusBean> res = new ArrayList<VmStatusBean>();
		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		
		try {
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				VmStatusBean status = new VmStatusBean(
					rs.getString(DBSchema.VmTable.VM_ID), rs.getString(DBSchema.VmTable.VM_MODE), 
					rs.getString(DBSchema.VmTable.STATE), rs.getString(DBSchema.VmTable.PUBLIC_IP), 
					rs.getInt(DBSchema.VmTable.SSH_PORT), rs.getInt(DBSchema.VmTable.VNC_PORT));
				res.add(status);
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

	public boolean quotaExceedsLimit(String userName) {
		return false;
	}
	
	public boolean vmExists(String userName, String vmid) {
		return true;
	}
	
	public List<VmStatusBean> getVmStatus() throws SQLException {
		String sql = "SELECT " + DBSchema.VmTable.VM_MODE + "," 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + "," 
				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT 
				+ " FROM " 
				+ DBSchema.VmTable.TABLE_NAME;
		return getVmStatusInternal(sql);
	}
	
	public List<VmStatusBean> getVmStatus(String userName) throws SQLException {
		String sql = String.format(
				"SELECT " + DBSchema.VmTable.VM_MODE + "," 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + "," 
				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT 
				+ " FROM " 
				+ DBSchema.UserVmTable.TABLE_NAME + ","
				+ DBSchema.VmTable.TABLE_NAME 
				+ " WHERE " 
				+ DBSchema.UserVmTable.TABLE_NAME + "."  +  DBSchema.UserVmTable.VM_ID + "=" 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID
				+ " AND " + DBSchema.UserVmTable.USER_NAME + "=\"%s\"", userName);
		return getVmStatusInternal(sql);
	}
	
	public List<VmStatusBean> getVmStatus(String userName, String vmid) 
		throws SQLException, NoItemIsFoundInDBException {
		String sql = String.format(
				"SELECT " + DBSchema.VmTable.VM_MODE + "," 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID + ","
				+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.STATE + "," 
				+ DBSchema.VmTable.SSH_PORT + "," + DBSchema.VmTable.VNC_PORT 
				+ " FROM " 
				+ DBSchema.UserVmTable.TABLE_NAME + ","
				+ DBSchema.VmTable.TABLE_NAME 
				+ " WHERE " 
				+ DBSchema.UserVmTable.TABLE_NAME + "."  +  DBSchema.UserVmTable.VM_ID + "=" 
				+ DBSchema.VmTable.TABLE_NAME + "."  +  DBSchema.VmTable.VM_ID
				+ " AND " + DBSchema.UserVmTable.USER_NAME + "=\"%s\""
				+ " AND " + DBSchema.VmTable.TABLE_NAME + "." + DBSchema.UserVmTable.VM_ID + "=\"%s\"", userName, vmid);
		List<VmStatusBean> status = getVmStatusInternal(sql);
		if (status.size() == 0) 
			throw new NoItemIsFoundInDBException(String.format(
				"VM %s with user %s is not found in DB.", vmid, userName));
		return status;
	}
	
	public void addVM(String userName, String vmid, String imageName, VMPorts host, String workDir) throws SQLException {		
		String insertvmsql = String.format("INSERT INTO " + DBSchema.VmTable.TABLE_NAME + " ("
			+ DBSchema.VmTable.VM_ID + "," + DBSchema.VmTable.STATE + "," 
			+ DBSchema.VmTable.PUBLIC_IP + "," + DBSchema.VmTable.SSH_PORT + "," 
			+ DBSchema.VmTable.VNC_PORT + "," + DBSchema.VmTable.WORKING_DIR + "," + DBSchema.VmTable.IMAGE_NAME + ") VALUES"
			+ "(\"%s\", \"%s\", \"%s\", %d, %d, \"%s\", \"%s\")", 
			vmid, VMState.BUILDING.toString(), host.host, host.sshport, host.vncport, workDir, imageName);
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
	
	public void updateVMStatus(String vmid, VMState state) throws SQLException {
		// use row affected to tell whether value exists before ??
		List<String> updates = new ArrayList<String>(); 
		String updatevmsql = String.format("DELETE FROM " + DBSchema.VmTable.TABLE_NAME + 
				" where " + DBSchema.VmTable.VM_ID + "=\"%s\"", vmid);
		updates.add(updatevmsql);
		executeTransaction(updates);
	}
	
	public void close() {
		DBConnections.getInstance().close();
	}
}
