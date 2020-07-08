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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import edu.indiana.d2i.sloan.Configuration;

public class DBConnections {
	private static Logger logger = LoggerFactory.getLogger(DBConnections.class);
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
			
//			dataSource.setMinPoolSize(5);
//			dataSource.setAcquireIncrement(5);
//			dataSource.setMaxPoolSize(20);
			
			dataSource.setMaxIdleTime(60); // 60 seconds
			dataSource.setIdleConnectionTestPeriod(55); // 55 seconds
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
