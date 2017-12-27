package com.microtechmd.pda.library.service;


import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.utility.LogPDA;


public abstract class TaskBase
{
	protected ServiceBase mService = null;
	protected LogPDA mLog = null;


	public abstract void handleMessage(EntityMessage message);


	protected TaskBase(ServiceBase service)
	{
		if (mLog == null)
		{
			mLog = new LogPDA();
		}

		mService = service;
	}
}
