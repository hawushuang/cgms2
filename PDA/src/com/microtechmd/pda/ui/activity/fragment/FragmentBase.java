package com.microtechmd.pda.ui.activity.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.microtechmd.pda.util.KeyNavigation;
import com.microtechmd.pda.library.utility.LogPDA;


public class FragmentBase extends Fragment
	implements KeyNavigation.OnClickViewListener
{
	protected LogPDA mLog = null;

	private KeyNavigation mKeyNavigation = null;


	public FragmentBase()
	{
		mLog = new LogPDA();
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mKeyNavigation = new KeyNavigation(getActivity(), this);
		resetKeyNavigation();
	}


	@Override
	public void onClick(View v)
	{
	}


	protected void resetKeyNavigation()
	{
		mKeyNavigation.resetNavigation(getView());
	}
}