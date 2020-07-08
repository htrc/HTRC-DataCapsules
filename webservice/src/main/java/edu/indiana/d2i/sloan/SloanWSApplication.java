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
package edu.indiana.d2i.sloan;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SloanWSApplication extends Application {
	private static Logger logger = LoggerFactory.getLogger(SloanWSApplication.class);

	@Context
	private ServletConfig servletConfig;

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> s = new HashSet<Class<?>>();
		String[] names = Configuration.getInstance().getString(
			Configuration.PropertyName.RESOURCES_NAMES).split(";");
		for (String name : names) {
			try {
				s.add(Class.forName(name));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} 
		}		
		return s;
	}

	@PostConstruct
	private void init() {
		Configuration.getInstance(); // load configurations at first
		//configureLogger(servletConfig);
	}

	@PreDestroy
	private void fin() {

	}

	private void configureLogger(ServletConfig servletConfig) {
//		String log4jPropertiesPath = Configuration.getInstance().getString("log4j.properties.path");
//		if(log4jPropertiesPath != null && new File(log4jPropertiesPath).exists()){
//			PropertyConfigurator.configure(log4jPropertiesPath);
//			logger.info("logger configured as " + log4jPropertiesPath);
//		}
	}
}
