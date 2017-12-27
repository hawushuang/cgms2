package com.microtechmd.pda.ui.activity;


import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.ImageView;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.util.MediaUtil;


public class ActivityBgApply extends ActivityPDA
{
	@Override
	public void onBackPressed()
	{
	}


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_bg_apply);
		((ApplicationPDA)getApplication()).registerMessageListener(
			ParameterGlobal.PORT_GLUCOSE, mMessageListener);
		initImgs();
	}


	@Override
	protected void onDestroy()
	{
		((ApplicationPDA)getApplication()).unregisterMessageListener(
			ParameterGlobal.PORT_GLUCOSE, mMessageListener);
		super.onDestroy();
	}


	@Override
	protected void onHomePressed()
	{

	}


	@Override
	protected void handleEvent(final EntityMessage message)
	{
		super.handleEvent(message);

		if (message.getTargetPort() != ParameterGlobal.PORT_GLUCOSE)
		{
			return;
		}
	}


	@Override
	protected void handleNotification(final EntityMessage message)
	{
		super.handleNotification(message);

		if ((message
			.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
			(message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE))
		{
			switch (message.getParameter())
			{
				case ParameterGlucose.PARAM_SIGNAL_PRESENT:
					break;

				case ParameterGlucose.PARAM_COUNT_DOWN:

					if (message.getData()[0] == 5)
					{
						Intent intent = new Intent(mBaseActivity,
							ActivityBgProcessing.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}

					finish();
					break;

				case ParameterGlucose.PARAM_ERROR:
					MediaUtil.playMp3ByType(this, "beep_ack.mp3", false);
					int errorStringID = R.string.glucose_error_unknown;

					switch (message.getData()[0])
					{
						case ParameterGlucose.ERROR_CODE:
							errorStringID = R.string.glucose_error_code;
							break;

						case ParameterGlucose.ERROR_CHANNEL:
							errorStringID = R.string.glucose_error_channel;
							break;

						case ParameterGlucose.ERROR_NBB:
							errorStringID = R.string.glucose_error_nbb;
							break;

						case ParameterGlucose.ERROR_TEMPERATURE:
							errorStringID = R.string.glucose_error_temperature;
							break;

						case ParameterGlucose.ERROR_BLOOD_FILLING:
							errorStringID =
								R.string.glucose_error_blood_filling;
							break;

						case ParameterGlucose.ERROR_BLOOD_NOT_ENOUGH:
							errorStringID =
								R.string.glucose_error_blood_not_enough;
							break;

						case ParameterGlucose.ERROR_STRIP:
							errorStringID = R.string.glucose_error_strip;
							break;

						default:
							break;
					}

					FragmentMessage fragmentMessage = new FragmentMessage();
					fragmentMessage.setComment(getString(errorStringID));
					showDialogConfirm(getString(R.string.glucose_error_title),
						"", null, fragmentMessage, false,
						new FragmentDialog.ListenerDialog()
						{
							@Override
							public boolean onButtonClick(int buttonID,
								Fragment content)
							{
								switch (buttonID)
								{
									case FragmentDialog.BUTTON_ID_POSITIVE:
										finish();
										break;

									default:
										break;
								}

								return true;
							}
						});

					break;

				default:
					break;
			}
		}
	}


	private void initImgs()
	{
		ImageView img = (ImageView)findViewById(R.id.image_view_calibration);
		img.setBackgroundResource(R.drawable.animation_calibration);
		final AnimationDrawable frameAnimation =
			(AnimationDrawable)img.getBackground();
		frameAnimation.setOneShot(false);
		img.setBackgroundDrawable(frameAnimation);
		img.post(new Runnable()
		{
			@Override
			public void run()
			{
				frameAnimation.start();
			}
		});
	}
}
