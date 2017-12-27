package com.microtechmd.pda.ui.activity.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microtechmd.pda.R;



public class FragmentProgress extends FragmentBase
{
	private String mComment = null;


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
		@Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_progress, container, false);

		if (mComment == null)
		{
			mComment = getResources().getString(R.string.connecting);
		}

		setComment(view, mComment);

		return view;
	}


	public void setComment(String comment)
	{
		if (comment == null)
		{
			return;
		}

		mComment = comment;
		View view = getView();

		if (view != null)
		{
			setComment(view, comment);
		}
	}


	private void setComment(View view, String comment)
	{
		TextView textViewComment =
			(TextView)view.findViewById(R.id.text_view_comment);

		if (comment != null)
		{
			textViewComment.setVisibility(View.VISIBLE);
			textViewComment.setText(comment);
		}
	}
}
