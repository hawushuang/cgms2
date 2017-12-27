package com.microtechmd.pda.control.platform;


import com.microtechmd.pda.library.entity.EntityMessage;


class JNIInterface
{
	private static JNIInterface sInstance = null;
	private static Callback sCallback = null;


	interface Callback
	{
		int onHandleEvent(int address, int sourcePort, int targetPort,
			int event);


		int onHandleCommand(int address, int sourcePort, int targetPort,
			int mode, int operation, int parameter, byte[] data);
	}


	private JNIInterface()
	{
	}


	static synchronized JNIInterface getInstance()
	{
		if (sInstance == null)
		{
			sInstance = new JNIInterface();
		}

		return sInstance;
	}


	static void setCallback(Callback callback)
	{
		sCallback = callback;
	}


	private static int handleEvent(int address, int sourcePort, int targetPort,
		int event)
	{
		if (sCallback == null)
		{
			return EntityMessage.FUNCTION_FAIL;
		}

		return sCallback.onHandleEvent(address, sourcePort, targetPort, event);
	}


	private static int handleCommand(int address, int sourcePort,
		int targetPort, int mode, int operation, int parameter, byte[] data)
	{
		if (sCallback == null)
		{
			return EntityMessage.FUNCTION_FAIL;
		}

		return sCallback.onHandleCommand(address, sourcePort, targetPort, mode,
			operation, parameter, data);
	}


	native void open();


	native void close();


	native int send(int address, int sourcePort, int targetPort, int mode,
		int operation, int parameter, byte[] data);


	native int query(int address);


	native int switchLink(int address, int value);


	native void setLED(int color, int brightness);


	native int getLED(int color);


	static
	{
		System.loadLibrary("jni_interface");
	}
}
