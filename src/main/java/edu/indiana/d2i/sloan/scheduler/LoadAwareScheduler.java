package edu.indiana.d2i.sloan.scheduler;

import java.sql.SQLException;

import edu.indiana.d2i.sloan.bean.CreateVmRequestBean;
import edu.indiana.d2i.sloan.bean.VmRequestBean;
import edu.indiana.d2i.sloan.exception.NoResourceAvailableException;

public class LoadAwareScheduler extends Scheduler {

	private LoadAwareScheduler() {

	}

	@Override
	protected VmRequestBean doSchedule(CreateVmRequestBean request)
			throws NoResourceAvailableException, SQLException {

		throw new UnsupportedOperationException();

	}

}
