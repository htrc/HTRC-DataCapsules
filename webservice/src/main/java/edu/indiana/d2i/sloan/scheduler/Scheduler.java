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
package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;

public abstract class Scheduler {
	protected static final String[] hosts;

	static {
		hosts = Configuration.getInstance()
				.getString(Configuration.PropertyName.HOSTS).split(";");
	}

	public synchronized final VmInfoBean schedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException, NoItemIsFoundInDBException {
		return doSchedule(request);
	}

	abstract protected VmInfoBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException, NoItemIsFoundInDBException;
}
