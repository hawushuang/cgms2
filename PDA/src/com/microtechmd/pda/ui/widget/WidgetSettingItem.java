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


public class WidgetSettingItem extends RelativeLayout {
    private boolean mIsWizard = false;


    @Override
    public boolean performClick() {
        toggleCheckState();
        return super.performClick();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case ApplicationPDA.KEY_CODE_BOLUS:
                toggleCheckState();
                break;

            default:
                break;
        }

        return super.onKeyUp(keyCode, event);
    }


    public WidgetSettingItem(Context context) {
        super(context);
        initializeLayout(context);
    }


    public WidgetSettingItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray =
                context.getTheme().obtainStyledAttributes(attrs,
                        R.styleable.WidgetSettingItem, 0, 0);

        try {
            mIsWizard =
                    typedArray.getBoolean(R.styleable.WidgetSettingItem_isWizard,
                            false);
            initializeLayout(context);
            setCheckBox(typedArray.getBoolean(
                    R.styleable.WidgetSettingItem_isCheckBox, false));
            setItemName(typedArray
                    .getString(R.styleable.WidgetSettingItem_itemName));
            setItemValue(typedArray
                    .getString(R.styleable.WidgetSettingItem_itemValue));
            setNextIconVisible(typedArray.getBoolean(
                    R.styleable.WidgetSettingItem_hasNext, true));
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


    public boolean getCheckBox() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.rb_checkbox);

        if (checkBox.getVisibility() == View.VISIBLE) {
            return true;
        } else {
            return false;
        }
    }


    public boolean getCheckState() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.rb_checkbox);

        if (checkBox.isChecked()) {
            return true;
        } else {
            return false;
        }
    }

    public void setNextIconVisible(boolean hasNext) {
        if (hasNext) {
            findViewById(R.id.iv_icon).setVisibility(VISIBLE);
        } else {
            findViewById(R.id.iv_icon).setVisibility(INVISIBLE);
        }
    }

    public void setItemName(String itemName) {
        setViewText(R.id.tv_item_name, itemName);
    }


    public void setItemValue(String itemValue) {
        setViewText(R.id.tv_item_value, itemValue);
    }


    public void setCheckBox(boolean isCheckBox) {
        ImageView icon = (ImageView) findViewById(R.id.iv_icon);
        CheckBox checkBox = (CheckBox) findViewById(R.id.rb_checkbox);

        if (isCheckBox) {
            checkBox.setVisibility(View.VISIBLE);
            icon.setVisibility(View.INVISIBLE);
        } else {
            checkBox.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
        }
    }


    public void setCheckState(boolean checkState) {
        CheckBox checkBox = (CheckBox) findViewById(R.id.rb_checkbox);
        checkBox.setChecked(checkState);
    }


    private void toggleCheckState() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.rb_checkbox);

        if (checkBox.getVisibility() == View.VISIBLE) {
            checkBox.setChecked(!checkBox.isChecked());
        }
    }


    private void initializeLayout(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (mIsWizard) {
            inflater.inflate(R.layout.widget_setting_wizard_item, this, true);
        } else {
            inflater.inflate(R.layout.widget_setting_item, this, true);
        }
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