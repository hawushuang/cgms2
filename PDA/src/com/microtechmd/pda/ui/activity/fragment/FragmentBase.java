package com.microtechmd.pda.ui.activity.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.util.KeyNavigation;
import com.microtechmd.pda.library.utility.LogPDA;


public class FragmentBase extends Fragment
        implements KeyNavigation.OnClickViewListener {
    protected LogPDA mLog = null;

    protected KeyNavigation mKeyNavigation = null;

    protected ApplicationPDA app;

    public FragmentBase() {
        mLog = new LogPDA();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mKeyNavigation = new KeyNavigation(getActivity(), this);
        app = (ApplicationPDA) getActivity().getApplication();
        resetKeyNavigation();
    }


    @Override
    public void onClick(View v) {
    }

    protected void resetKeyNavigation() {
        mKeyNavigation.resetNavigation(getView());
    }
}