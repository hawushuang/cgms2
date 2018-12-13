package com.microtechmd.pda.ui.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;


public class WidgetSettingTipsItem extends RelativeLayout {

    public WidgetSettingTipsItem(Context context) {
        super(context);
        initializeLayout(context);
    }


    public WidgetSettingTipsItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray =
                context.getTheme().obtainStyledAttributes(attrs,
                        R.styleable.WidgetSettingItem, 0, 0);

        try {
            initializeLayout(context);
            setItemName(typedArray
                    .getString(R.styleable.WidgetSettingItem_itemName));
            setItemValue(typedArray
                    .getString(R.styleable.WidgetSettingItem_itemValue));
            ImageView imageView = (ImageView) findViewById(R.id.iv_edit);
            if (typedArray.getBoolean(R.styleable.WidgetSettingItem_isWizard, true)) {
                imageView.setVisibility(VISIBLE);
            } else {
                imageView.setVisibility(GONE);
            }
        } finally {
            typedArray.recycle();
        }
    }


    public String getItemName() {
        return ((TextView) findViewById(R.id.tv_item_name)).getText().toString();
    }


    public String getItemValue() {
        return ((TextView) findViewById(R.id.tv_item_value)).getText()
                .toString();
    }


    public void setItemName(String itemName) {
        setViewText(R.id.tv_item_name, itemName);
    }


    public void setItemValue(String itemValue) {
        setViewText(R.id.tv_item_value, itemValue);
    }

    private void initializeLayout(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_setting_tips_item, this, true);
    }


    private void setViewText(int viewID, final String string) {
        TextView view = (TextView) findViewById(viewID);

        if (string != null) {
            view.setText(string);

            if (!string.equals("")) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        } else {
            view.setText("");
            view.setVisibility(View.GONE);
        }
    }
}