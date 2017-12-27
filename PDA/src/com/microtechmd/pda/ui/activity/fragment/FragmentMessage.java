package com.microtechmd.pda.ui.activity.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.microtechmd.pda.R;


public class FragmentMessage extends FragmentBase
{
	private String mComment = null;
	private boolean mIcon = true;


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
		@Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_message, container, false);

		setComment(view, mComment);
		setIcon(view, mIcon);

		return view;
	}


	public void setComment(String comment)
	{
		mComment = comment;
		View view = getView();

		if (view != null)
		{
			setComment(view, comment);
		}
	}


	public void setIcon(boolean icon)
	{
		mIcon = icon;
		View view = getView();

		if (view != null)
		{
			setIcon(view, icon);
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
		else
		{
			textViewComment.setVisibility(View.GONE);
		}
	}


	private void setIcon(View view, boolean icon)
	{
		ImageView imageViewIcon =
			(ImageView)view.findViewById(R.id.image_view_icon);

		if (icon)
		{
			imageViewIcon.setVisibility(View.VISIBLE);
		}
		else
		{
			imageViewIcon.setVisibility(View.GONE);
		}
	}
}
