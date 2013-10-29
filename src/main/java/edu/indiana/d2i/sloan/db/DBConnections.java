package edu.indiana.d2i.sloan.db;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import edu.indiana.d2i.sloan.Configuration;

class DBConnections {
	private static Logger logger = Logger.getLogger(DBConnections.class);
	private static DBConnections instance = null;
	private static ComboPooledDataSource dataSource = null; 
	
	private DBConnections() {
		try {
			dataSource = new ComboPooledDataSource();
			dataSource.setDriverClass(Configuration.getInstance().getString(
				Configuration.PropertyName.DB_DRIVER_CLASS));
			dataSource.setJdbcUrl(Configuration.getInstance().getString(
				Configuration.PropertyName.JDBC_URL));
			dataSource.setUser(Configuration.getInstance().getString(
				Configuration.PropertyName.DB_USER));
			dataSource.setPassword(Configuration.getInstance().getString(
				Configuration.PropertyName.DB_PWD));
			
			dataSource.setMinPoolSize(5);
			dataSource.setAcquireIncrement(5);
			dataSource.setMaxPoolSize(20);
		} catch (PropertyVetoException ex) {
			logger.error(ex.getMessage(), ex);
			throw new RuntimeException(ex);
		}
	}
	
	static {
		instance = new DBConnections();
	}
	
	public static DBConnections getInstance() {
		return instance;
	}
	
	public void close() {
		dataSource.close();
	}
	
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}
}
