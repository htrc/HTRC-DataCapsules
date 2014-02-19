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
package edu.indiana.d2i.sloan.hyper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.Configuration;

class HypervisorFactory {
	private static Logger logger = Logger.getLogger(HypervisorFactory.class);

	private static Constructor<?> constructor;

	static {
		String hyperClassName = Configuration.getInstance().getString(
				Configuration.PropertyName.HYPERVISOR_FULL_CLASS_NAME);
		logger.info("Load hyper as " + hyperClassName);

		try {
			Class<?> hyperClass = Class.forName(hyperClassName);
			constructor = hyperClass.getDeclaredConstructor();

			/* set accessible to true since constructor is private */
			constructor.setAccessible(true);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public static IHypervisor createHypervisor()
			throws IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		return (IHypervisor) constructor.newInstance();
	}
}
