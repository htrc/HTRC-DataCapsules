package edu.indiana.d2i.sloan.vm;

public enum VMState {
	CREATE_PENDING,
	LUANCH_PENDING,
	RUNNING,
	SWITCH_TO_MAINTENANCE_PENDING,
	SWITCH_TO_SECURE_PENDING,
	SHUTDOWN_PENDING,
	SHUTDOWN,
	DELETE_PENDING,
	ERROR
}
