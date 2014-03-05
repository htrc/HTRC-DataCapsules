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
