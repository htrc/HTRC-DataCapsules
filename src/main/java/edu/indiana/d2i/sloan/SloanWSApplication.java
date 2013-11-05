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
		
		s.add(HelloWorld.class);
		return s;
	}

	@PostConstruct
	private void init() {
		configureLogger(servletConfig);
		Configuration.getInstance(); // load configurations at first
	}

	@PreDestroy
	private void fin() {

	}

	private void configureLogger(ServletConfig servletConfig) {
		String log4jPropertiesPath = (String) servletConfig
				.getInitParameter("log4j.properties.path");
		PropertyConfigurator.configure(log4jPropertiesPath);
		logger.info("logger configured as " + log4jPropertiesPath);
	}
}
