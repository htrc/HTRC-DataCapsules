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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class SloanWSApplication extends Application {
	private static Logger logger = Logger.getLogger(SloanWSApplication.class);

	@Context
	private ServletConfig servletConfig;

	@Override
	public Set<Class<?>> getClasses() {
		// TODO: get rid of the hard code loading!
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(CreateVM.class);
		s.add(QueryVM.class);
		s.add(LaunchVM.class);
		s.add(DeleteVM.class);
		s.add(StopVM.class);
		s.add(SwitchVM.class);
		s.add(ListImage.class);
		
		s.add(DownloadResult.class);
		s.add(UploadResult.class);
		
		s.add(HelloWorld.class);
		return s;
	}

	@PostConstruct
	private void init() {
		Configuration.getInstance(); // load configurations at first
		configureLogger(servletConfig);
	}

	@PreDestroy
	private void fin() {

	}

	private void configureLogger(ServletConfig servletConfig) {
		String log4jPropertiesPath = Configuration.getInstance().getString("log4j.properties.path"); 
		PropertyConfigurator.configure(log4jPropertiesPath);
		logger.info("logger configured as " + log4jPropertiesPath);
	}
}
