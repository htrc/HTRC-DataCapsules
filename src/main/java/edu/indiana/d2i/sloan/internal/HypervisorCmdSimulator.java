package edu.indiana.d2i.sloan.internal;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public abstract class HypervisorCmdSimulator extends CommandSimulator {
	public static enum ERROR_STATE {
		INVALID_INPUT_ARGS, IMAGE_NOT_EXIST, NOT_ENOUGH_CPU, NOT_ENOUGH_MEM
	}

	/* key is error type enum, value is error code */
	public static Map<ERROR_STATE, Integer> ERROR_CODE;

	static {
		ERROR_CODE = new HashMap<ERROR_STATE, Integer>() {
			{
				put(ERROR_STATE.INVALID_INPUT_ARGS, -1);
				put(ERROR_STATE.IMAGE_NOT_EXIST, -2);
				put(ERROR_STATE.NOT_ENOUGH_CPU, -3);
				put(ERROR_STATE.NOT_ENOUGH_MEM, -4);
			}
		};

	}

	public static boolean checkVMImageExist(String imagePath) {
		return new File(imagePath).exists();
	}

}
