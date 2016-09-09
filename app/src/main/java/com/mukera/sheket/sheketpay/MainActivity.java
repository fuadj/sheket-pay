package com.mukera.sheket.sheketpay;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Vector;

import mehdi.sakout.fancybuttons.FancyButton;

public class MainActivity extends AppCompatActivity {

    private View mLayoutLogout;
    private FancyButton mBtnMakePayment;

    private EditText mEditPaymentNumber;
    private EditText mEditDuration;

    private Spinner mSpinnerType;

    private View mLayoutLimit;
    private CheckBox mCheckEmployees;
    private CheckBox mCheckBranches;
    private CheckBox mCheckItems;

    private EditText mEditEmployees;
    private EditText mEditBranches;
    private EditText mEditItems;

    private FancyButton mBtnClear;

    void linkViews() {
        mLayoutLogout = findViewById(R.id.layout_logout);
        mBtnMakePayment = (FancyButton) findViewById(R.id.btn_make_payment);

        mEditPaymentNumber = (EditText) findViewById(R.id.edit_payment_number);
        mEditDuration = (EditText) findViewById(R.id.edit_duration);

        mSpinnerType = (Spinner) findViewById(R.id.spinner_type);

        mLayoutLimit = findViewById(R.id.layout_limits);

        mCheckEmployees = (CheckBox) findViewById(R.id.check_employees);
        mCheckBranches = (CheckBox) findViewById(R.id.check_branches);
        mCheckItems = (CheckBox) findViewById(R.id.check_items);

        mEditEmployees = (EditText) findViewById(R.id.edit_employees);
        mEditBranches = (EditText) findViewById(R.id.edit_branches);
        mEditItems = (EditText) findViewById(R.id.edit_items);

        mBtnClear = (FancyButton) findViewById(R.id.btn_clear);
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetUI();
            }
        });

        CompoundButton.OnCheckedChangeListener listener =
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int _id = buttonView.getId();

                if (_id == mCheckEmployees.getId()) {
                    mEditEmployees.setEnabled(isChecked);
                } else if (_id == mCheckBranches.getId()) {
                    mEditBranches.setEnabled(isChecked);
                } else if (_id == mCheckItems.getId()) {
                    mEditItems.setEnabled(isChecked);
                }
            }
        };

        mCheckEmployees.setOnCheckedChangeListener(listener);
        mCheckBranches.setOnCheckedChangeListener(listener);
        mCheckItems.setOnCheckedChangeListener(listener);
    }

    // when resetting the UI, the listeners might be called, just ignore it.
    boolean is_resetting = false;

    void resetUI() {
        is_resetting = true;
        mEditPaymentNumber.setText("");
        mEditDuration.setText("");

        mSpinnerType.setSelection(0);

        mCheckEmployees.setChecked(true);
        mCheckBranches.setChecked(true);
        mCheckItems.setChecked(true);

        mEditEmployees.setText("2");
        mEditBranches.setText("2");
        mEditItems.setText("50");

        adjustLimitLayoutVisibility();
        is_resetting = false;
    }

    void adjustLimitLayoutVisibility() {
        int selected = mSpinnerType.getSelectedItemPosition();
        if (selected == 2) {        // only applies to the GOLD user
            mLayoutLimit.setVisibility(View.VISIBLE);
        } else {
            mLayoutLimit.setVisibility(View.GONE);
        }
    }

    static final Vector<String> CONTRACT_TYPES;
    static {
        CONTRACT_TYPES = new Vector<>();
        CONTRACT_TYPES.add("--Select Contract--");
        CONTRACT_TYPES.add("Single Use");
        CONTRACT_TYPES.add("Gold");
        CONTRACT_TYPES.add("Platinum");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PrefUtil.isUserLoggedIn(this)) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_launcher);

        linkViews();

        mSpinnerType.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        CONTRACT_TYPES.toArray()
                )
        );

        mSpinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // ignore if this was triggered by resetting UI
                if (is_resetting)
                    return;

                adjustLimitLayoutVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        resetUI();

        mLayoutLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefUtil.logoutUser(MainActivity.this);
                finish();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        mBtnMakePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: contact server to make payment
            }
        });
    }
}
