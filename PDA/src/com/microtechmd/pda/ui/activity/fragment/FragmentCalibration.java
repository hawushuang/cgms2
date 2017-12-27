package com.microtechmd.pda.ui.activity.fragment;


import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.microtechmd.pda.R;
import com.microtechmd.pda.ui.activity.ActivityBgEnter;


public class FragmentCalibration extends FragmentBase
{
	private View mRootView = null;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState)
	{
		mRootView = inflater.inflate(R.layout.fragment_calibration, container,
			false);

		return mRootView;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		initializeAnimation(R.drawable.animation_calibration);
	}


	@Override
	public void onResume()
	{
		super.onResume();
	}


	@Override
	public void onClick(View v)
	{
		super.onClick(v);

		switch (v.getId())
		{
			case R.id.button_calibration:
				startActivity(new Intent(getActivity(), ActivityBgEnter.class));
				break;

			default:
				break;
		}
	}


	private void initializeAnimation(int resid)
	{
		ImageView imageView = (ImageView)mRootView.findViewById(R.id.image_view_calibration);

		if (imageView != null)
		{
			imageView.setBackgroundResource(resid);
			final AnimationDrawable frameAnimation =
				(AnimationDrawable)imageView.getBackground();
			frameAnimation.setOneShot(false);
			imageView.setBackgroundDrawable(frameAnimation);
			imageView.post(new Runnable()
			{
				@Override
				public void run()
				{
					frameAnimation.start();
				}
			});
		}
	}
}
