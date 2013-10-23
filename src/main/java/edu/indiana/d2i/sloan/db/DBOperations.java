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
				} catch (SQLException sqle) {
					throw sqle;
				}
			}
		} finally {
			if (st != null)
				st.close();
			if (connection != null)
				connection.close();
		}
	}

	private List<VmStatusBean> getVmStatusInternal(String sql)
			throws SQLException {
		if (logger.isDebugEnabled())
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
						rs.getString(DBSchema.VmTable.VM_ID),
						rs.getString(DBSchema.VmTable.VM_MODE),
						rs.getString(DBSchema.VmTable.STATE),
						rs.getString(DBSchema.VmTable.PUBLIC_IP),
						rs.getInt(DBSchema.VmTable.SSH_PORT),
						rs.getInt(DBSchema.VmTable.VNC_PORT));
				res.add(status);
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

	public boolean quotaExceedsLimit(String userName,
			int requestedVolumeSize) throws SQLException,
			NoItemIsFoundInDBException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(DBSchema.UserTable.LEFT_QUOTA)
				.append(" FROM ").append(DBSchema.UserTable.TABLE_NAME)
				.append(" WHERE ").append(DBSchema.UserTable.USER_NAME)
				.append("=").append(String.format("\"%s\"", userName));

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql.toString());
			rs = pst.executeQuery();
			if (rs.next()) {
				return rs.getInt(DBSchema.UserTable.LEFT_QUOTA) > requestedVolumeSize;
			} else {
				throw new NoItemIsFoundInDBException(String.format(
						"Cannot find user record for user %s in table %s",
						userName, DBSchema.UserTable.TABLE_NAME));
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}

	}

	public boolean vmExists(String userName, String vmid) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(DBSchema.UserVmTable.VM_ID)
				.append(" FROM ").append(DBSchema.UserVmTable.TABLE_NAME)
				.append(" WHERE ").append(DBSchema.UserVmTable.USER_NAME)
				.append("=").append(String.format("\"%s\"", userName))
				.append(" AND ").append(DBSchema.UserVmTable.VM_ID).append("=")
				.append(String.format("\"%s\"", vmid));

		Connection connection = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql.toString());
			rs = pst.executeQuery();
			if (rs.next()) {
				return true;
			} else {
				return false;
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
			if (connection != null)
				connection.close();
		}

	}

	public List<VmStatusBean> getVmStatus() throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(DBSchema.VmTable.VM_MODE).append(",")
				.append(DBSchema.VmTable.TABLE_NAME).append(".")
				.append(DBSchema.VmTable.VM_ID).append(",")
				.append(DBSchema.VmTable.PUBLIC_IP).append(",")
				.append(DBSchema.VmTable.STATE).append(",")
				.append(DBSchema.VmTable.SSH_PORT).append(",")
				.append(DBSchema.VmTable.VNC_PORT).append(" FROM ")
				.append(DBSchema.VmTable.TABLE_NAME);

		return getVmStatusInternal(sql.toString());
	}

	public List<VmStatusBean> getVmStatus(String userName) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(DBSchema.VmTable.VM_MODE).append(",")
				.append(DBSchema.VmTable.TABLE_NAME).append(".")
				.append(DBSchema.VmTable.VM_ID).append(",")
				.append(DBSchema.VmTable.PUBLIC_IP).append(",")
				.append(DBSchema.VmTable.STATE).append(",")
				.append(DBSchema.VmTable.SSH_PORT).append(",")
				.append(DBSchema.VmTable.VNC_PORT).append(" FROM ")
				.append(DBSchema.UserVmTable.TABLE_NAME).append(",")
				.append(DBSchema.VmTable.TABLE_NAME).append(" WHERE ")
				.append(DBSchema.UserVmTable.TABLE_NAME).append(".")
				.append(DBSchema.UserVmTable.VM_ID).append("=")
				.append(DBSchema.VmTable.TABLE_NAME).append(".")
				.append(DBSchema.VmTable.VM_ID).append(" AND ")
				.append(DBSchema.UserVmTable.USER_NAME)
				.append(String.format("=\"%s\"", userName));

		return getVmStatusInternal(sql.toString());
	}

	public List<VmStatusBean> getVmStatus(String userName, String vmid)
			throws SQLException, NoItemIsFoundInDBException {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(DBSchema.VmTable.VM_MODE).append(",")
				.append(DBSchema.VmTable.TABLE_NAME).append(".")
				.append(DBSchema.VmTable.VM_ID).append(",")
				.append(DBSchema.VmTable.PUBLIC_IP).append(",")
				.append(DBSchema.VmTable.STATE).append(",")
				.append(DBSchema.VmTable.SSH_PORT).append(",")
				.append(DBSchema.VmTable.VNC_PORT).append(" FROM ")
				.append(DBSchema.UserVmTable.TABLE_NAME).append(",")
				.append(DBSchema.VmTable.TABLE_NAME).append(" WHERE ")
				.append(DBSchema.UserVmTable.TABLE_NAME).append(".")
				.append(DBSchema.UserVmTable.VM_ID).append("=")
				.append(DBSchema.VmTable.TABLE_NAME).append(".")
				.append(DBSchema.VmTable.VM_ID).append(" AND ")
				.append(DBSchema.UserVmTable.USER_NAME)
				.append(String.format("=\"%s\"", userName)).append(" AND ")
				.append(DBSchema.VmTable.TABLE_NAME).append(".")
				.append(DBSchema.VmTable.VM_ID)
				.append(String.format("=\"%s\"", vmid));

		List<VmStatusBean> status = getVmStatusInternal(sql.toString());
		if (status.size() == 0)
			throw new NoItemIsFoundInDBException(String.format(
					"VM %s with user %s is not found in DB.", vmid, userName));
		return status;
	}

	public void addVM(String userName, String vmid, String imageName,
			VMPorts host, String workDir) throws SQLException {
		StringBuilder insertvmsql = new StringBuilder();
		insertvmsql
				.append("INSERT INTO ")
				.append(DBSchema.VmTable.TABLE_NAME)
				.append(" (")
				.append(DBSchema.VmTable.VM_ID)
				.append(",")
				.append(DBSchema.VmTable.STATE)
				.append(",")
				.append(DBSchema.VmTable.PUBLIC_IP)
				.append(",")
				.append(DBSchema.VmTable.SSH_PORT)
				.append(",")
				.append(DBSchema.VmTable.VNC_PORT)
				.append(",")
				.append(DBSchema.VmTable.WORKING_DIR)
				.append(",")
				.append(DBSchema.VmTable.IMAGE_NAME)
				.append(") VALUES")
				.append(String.format(
						"(\"%s\", \"%s\", \"%s\", %d, %d, \"%s\", \"%s\")",
						vmid, VMState.BUILDING.toString(), host.host,
						host.sshport, host.vncport, workDir, imageName));

		StringBuilder insertvmusersql = new StringBuilder();
		insertvmusersql.append("INSERT INTO ")
				.append(DBSchema.UserVmTable.TABLE_NAME).append(" (")
				.append(DBSchema.UserVmTable.USER_NAME).append(",")
				.append(DBSchema.UserVmTable.VM_ID).append(") VALUES")
				.append(String.format("=\"%s\", \"%s\"", userName, vmid));

		if (logger.isDebugEnabled()) {
			logger.debug(insertvmsql);
			logger.debug(insertvmusersql);
		}

		List<String> updates = new ArrayList<String>();
		updates.add(insertvmsql.toString());
		updates.add(insertvmusersql.toString());

		executeTransaction(updates);
	}
	public void deleteVMs(String vmid) throws SQLException {
		List<String> updates = new ArrayList<String>();
		StringBuilder deletevmsql = new StringBuilder();
		deletevmsql.append("DELETE FROM ").append(DBSchema.VmTable.TABLE_NAME)
				.append(" WHERE ").append(DBSchema.VmTable.VM_ID)
				.append(String.format("=\"%s\"", vmid));

		updates.add(deletevmsql.toString());
		executeTransaction(updates);
	}
	public void updateVMStatus(String vmid, VMState state) throws SQLException {
		// use row affected to tell whether value exists before ??
		List<String> updates = new ArrayList<String>();
		StringBuilder updatevmsql = new StringBuilder();
		updatevmsql.append("DELETE FROM ").append(DBSchema.VmTable.TABLE_NAME)
				.append(" WHERE ").append(DBSchema.VmTable.VM_ID)
				.append(String.format("=\"%s\"", vmid));

		updates.add(updatevmsql.toString());
		executeTransaction(updates);
	}
	public void close() {
		DBConnections.getInstance().close();
	}
}
