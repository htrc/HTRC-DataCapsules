package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmInfoBean;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;

public abstract class Scheduler {
	protected static final String[] hosts;

	static {
		hosts = Configuration.getInstance()
				.getProperty(Configuration.PropertyName.HOSTS).split(";");
	}

	public synchronized final VmInfoBean schedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException {
		return doSchedule(request);
	}

	abstract protected VmInfoBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException;
}
