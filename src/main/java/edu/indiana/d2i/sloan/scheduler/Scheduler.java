package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;

import edu.indiana.d2i.sloan.Configuration;
import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmRequestBean;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;

public abstract class Scheduler {
	protected static final String[] hosts;

	static {
		hosts = Configuration.getInstance()
				.getProperty(Configuration.PropertyName.HOSTS).split(";");
	}

	protected static Scheduler instance = null;

	public static Scheduler getInstance() {
		return instance;
	}

	public synchronized final VmRequestBean schedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException {
		return doSchedule(request);
	}

	abstract protected VmRequestBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException;
}
