package edu.indiana.d2i.sloan.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.indiana.d2i.sloan.Configuration;

public class TestDBConnection {
	@BeforeClass
	public static void beforeClass() {
		Configuration.getInstance().setProperty(
			Configuration.PropertyName.DB_DRIVER_CLASS, "com.mysql.jdbc.Driver");
		Configuration.getInstance().setProperty(
			Configuration.PropertyName.JDBC_URL, "jdbc:mysql://localhost:3306/vmdb");
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.DB_USER, "root");
		Configuration.getInstance().setProperty(
				Configuration.PropertyName.DB_PWD, "root");
	}
	
	@Test
	public void testQuery() {
		Connection connection = null;
		PreparedStatement pst = null;
		
		String sql = "SELECT VERSION()";
		try {
			connection = DBConnections.getInstance().getConnection();
			pst = connection.prepareStatement(sql);
			ResultSet result = pst.executeQuery();
			if (result.next()) {
				System.out.println(result.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBConnections.getInstance().close();
		}
	}
}
