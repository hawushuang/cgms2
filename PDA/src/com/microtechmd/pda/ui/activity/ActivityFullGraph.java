package com.microtechmd.pda.ui.activity;

import android.os.Bundle;

import com.microtechmd.pda.R;
import com.microtechmd.pda.ui.activity.fragment.FragmentCombinedGraph;


public class ActivityFullGraph extends ActivityPDA {
    private FragmentCombinedGraph fragmentNewGraph = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_graph);
        if (fragmentNewGraph == null) {
            fragmentNewGraph = new FragmentCombinedGraph();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_fragment, fragmentNewGraph).commit();
    }

}
