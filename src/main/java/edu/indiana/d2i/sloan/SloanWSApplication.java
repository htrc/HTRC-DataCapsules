package edu.indiana.d2i.sloan;

import java.util.Enumeration;
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
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(CreateVM.class);
		s.add(HelloWorld.class);
		return s;
	}

	@PostConstruct
	private void init() {
		configureLogger(servletConfig);
		loadProperties();
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

	private void loadProperties() {
		Configuration config = Configuration.getInstance();
		Enumeration<?> parameterNames = servletConfig.getInitParameterNames();
        while(parameterNames.hasMoreElements()) {
            String parameterName = (String)parameterNames.nextElement();
            String value = (String)servletConfig.getInitParameter(parameterName);
            logger.debug(parameterName + " = " + value);
            config.setProperty(parameterName, value);
        }
        logger.info("finish loading properties");
	}
}
