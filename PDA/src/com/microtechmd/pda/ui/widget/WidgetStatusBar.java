package com.microtechmd.pda.ui.widget;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.monitor.History;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class WidgetStatusBar {
    private static final int IMAGE_LEVEL_NO_SIGAL = 300;
    private static final int INTEGER_LENGTH = 4;

    private int mPumpBattery = 0;
    private int mPumpReservoir = 0;
    private int mRFSignal = 0;
    private int mPDABattery = 0;
    private boolean mPDACharger = false;
    private boolean mGlucose = false;
    private View mView = null;
    private History mAlarm = null;
    private ArrayList<History> mAlertList = null;


    public WidgetStatusBar() {
        mAlertList = new ArrayList<>();
    }


    public boolean getPDACharger() {
        return mPDACharger;
    }


    public boolean getGlucose() {
        return mGlucose;
    }


    public History getAlarm() {
        return mAlarm;
    }


    public ArrayList<History> getAlertList() {
        return mAlertList;
    }


    public byte[] getByteArray() {
        final DataOutputStream dataOutputStream;
        final ByteArrayOutputStream byteArrayOutputStream;

        byteArrayOutputStream = new ByteArrayOutputStream();
        dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            byteArrayOutputStream.reset();
            dataOutputStream.writeInt(mAlertList.size());

            for (int i = 0; i < mAlertList.size(); i++) {
                dataOutputStream.write(mAlertList.get(i).getByteArray());
            }

            if (mAlarm != null) {
                dataOutputStream.write(mAlarm.getByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }


    public void setView(final View view) {
        mView = view;
        setRFSignal(mRFSignal);
        setPDABattery(mPDABattery);
        setPDACharger(mPDACharger);
    }


    public void setPumpBattery(int value) {
        mPumpBattery = value;

        if (mView == null) {
            return;
        }

        ImageView ivBattery = (ImageView) mView.findViewById(R.id.iv_battery);

        if (ivBattery != null) {
            if (mRFSignal > 0) {
                ivBattery.setImageLevel(value);
            } else {
                ivBattery.setImageLevel(IMAGE_LEVEL_NO_SIGAL);
            }
        }
    }


    public void setPumpReservoir(int value) {
        mPumpReservoir = value;

        if (mView == null) {
            return;
        }

        ImageView ivWorkdrum = (ImageView) mView.findViewById(R.id.iv_workdrum);

        if (ivWorkdrum != null) {
            if (mRFSignal > 0) {
                ivWorkdrum.setImageLevel(value);
            } else {
                ivWorkdrum.setImageLevel(IMAGE_LEVEL_NO_SIGAL);
            }
        }
    }


    public void setRFSignal(int value) {
        mRFSignal = value;

        if (mView == null) {
            return;
        }

        ImageView ivSignal = (ImageView) mView.findViewById(R.id.iv_signal);

        if (ivSignal != null) {
            ivSignal.setImageLevel(value);
        }

        int pumpBattery = IMAGE_LEVEL_NO_SIGAL;
        int pumpReservoir = IMAGE_LEVEL_NO_SIGAL;

        if (value > 0) {
            pumpBattery = mPumpBattery;
            pumpReservoir = mPumpReservoir;
        }

        ImageView ivBattery = (ImageView) mView.findViewById(R.id.iv_battery);

        if (ivBattery != null) {
            ivBattery.setImageLevel(pumpBattery);
        }

        ImageView ivWorkdrum = (ImageView) mView.findViewById(R.id.iv_workdrum);

        if (ivWorkdrum != null) {
            ivWorkdrum.setImageLevel(pumpReservoir);
        }
    }


    public void setPDABattery(int value) {
        mPDABattery = value;

        if (mView == null) {
            return;
        }

        ImageView ivPDABattery =
                (ImageView) mView.findViewById(R.id.iv_pda_battery);
        TextView tv_battery_percent = (TextView) mView.findViewById(R.id.battery_percent);

        if (ivPDABattery != null) {
            ivPDABattery.setImageLevel(value);
        }
        if (tv_battery_percent != null) {
            tv_battery_percent.setText(value + "%");
        }
    }


    public void setPDACharger(boolean value) {
        if (value != mPDACharger) {
            mPDACharger = value;
            ImageView ivPDACharger =
                    (ImageView) mView.findViewById(R.id.iv_pda_charger);

            if (ivPDACharger != null) {
                if (value) {
                    ivPDACharger.setVisibility(View.VISIBLE);
                } else {
                    ivPDACharger.setVisibility(View.GONE);
                }
            }
        }
    }


    public void setGlucose(boolean glucose) {
        mGlucose = glucose;
    }


    public void setAlarm(final History alarm) {
        mAlarm = alarm;
    }


    public void setAlertList(final ArrayList<History> alertList) {
        if (alertList != null) {
            mAlertList = alertList;

            if (mView == null) {
                return;
            }
        }
    }


    public void setDateTime(long dateTime, boolean is24Hour) {
        if (mView == null) {
            return;
        }

        String template;

        if (is24Hour) {
            template = "H:mm";
        } else {
            template = "h:mm";
        }

        final Date date = new Date(dateTime);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template);
        String textDateTime = simpleDateFormat.format(date);

        TextView tvDateTime =
                (TextView) mView.findViewById(R.id.btn_status_down);

        if ((tvDateTime != null) &&
                (tvDateTime.getVisibility() == View.VISIBLE)) {
            tvDateTime.setText(textDateTime);
        } else {
            tvDateTime = (TextView) mView.findViewById(R.id.tv_status_time);

            if (tvDateTime != null) {
                if (!is24Hour) {
                    template = "h:mm a";
                    simpleDateFormat.applyPattern(template);
                    textDateTime = simpleDateFormat.format(date);
                }

                tvDateTime.setText(textDateTime);
            }
        }
    }


    public void setByteArray(byte[] byteArray) {
        if (byteArray == null) {
            return;
        }

        int presetLength = byteArray.length;

        if (presetLength >= INTEGER_LENGTH) {
            final DataInputStream dataInputStream;
            final ByteArrayInputStream byteArrayInputStream;

            byteArrayInputStream = new ByteArrayInputStream(byteArray);
            dataInputStream = new DataInputStream(byteArrayInputStream);

            try {
                int size = dataInputStream.readInt();
                int byteArrayLength = byteArray.length - INTEGER_LENGTH;
                mAlertList.clear();

                for (int i = 0; i < size; i++) {
                    if (byteArrayLength >= History.BYTE_ARRAY_LENGTH) {
                        final byte[] value =
                                new byte[History.BYTE_ARRAY_LENGTH];
                        dataInputStream.read(value, 0,
                                History.BYTE_ARRAY_LENGTH);
                        mAlertList.add(new History(value));
                        byteArrayLength -= History.BYTE_ARRAY_LENGTH;
                    } else {
                        mAlertList.clear();
                    }
                }

                if (byteArrayLength >= History.BYTE_ARRAY_LENGTH) {
                    final byte[] value = new byte[History.BYTE_ARRAY_LENGTH];
                    dataInputStream.read(value, 0, History.BYTE_ARRAY_LENGTH);
                    mAlarm = new History(value);
                } else {
                    mAlarm = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
