package com.mukera.sheket.sheketpay;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;


import com.mukera.sheket.client.network.IssuePaymentRequest;
import com.mukera.sheket.client.network.IssuePaymentResponse;
import com.mukera.sheket.client.network.SheketAuth;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Vector;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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

        final Drawable successIcon = getResources().getDrawable(R.mipmap.ic_action_success);
        successIcon.setBounds(new Rect(0, 0, successIcon.getIntrinsicWidth(), successIcon.getIntrinsicHeight()));

        mEditPaymentNumber = (EditText) findViewById(R.id.edit_payment_number);
        mEditPaymentNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String id = s.toString().
                        // remove any space
                                replaceAll("\\s+", "").
                        // also remove any non-alphanumeric characters
                                replaceAll("\\W+", "");

                if (IdEncoderUtil.isValidEncodedCompanyId(id)) {
                    // I know it is weird to call {@code setError} for telling success
                    // but we don't have an API for the success.
                    mEditPaymentNumber.setError("Correct ID", successIcon);
                } else {
                    mEditPaymentNumber.setError(null);
                }
                updateViews();
            }
        });

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

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateViews();
            }
        };

        mEditDuration.addTextChangedListener(watcher);
        mEditEmployees.addTextChangedListener(watcher);
        mEditBranches.addTextChangedListener(watcher);
        mEditItems.addTextChangedListener(watcher);

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
                        updateViews();
                    }
                };

        mCheckEmployees.setOnCheckedChangeListener(listener);
        mCheckBranches.setOnCheckedChangeListener(listener);
        mCheckItems.setOnCheckedChangeListener(listener);
    }

    String trimmed(EditText editText) {
        return editText.getText().toString().trim();
    }

    void updateViews() {
        mLayoutLimit.setVisibility(
                // only make visible if position 2(the GOLD license) is selected
                mSpinnerType.getSelectedItemPosition() == 2 ?
                        View.VISIBLE : View.GONE);

        if (mSpinnerType.getSelectedItemPosition() == 0 ||
                !IdEncoderUtil.isValidEncodedCompanyId(trimmed(mEditPaymentNumber)) ||
                trimmed(mEditDuration).isEmpty()) {
            setPayBtnState(false);
            return;
        }

        // if it is the GOLD license, make sure the fields are filled in
        if (mSpinnerType.getSelectedItemPosition() == 2) {
            if (mCheckEmployees.isChecked() &&
                    trimmed(mEditEmployees).isEmpty()) {
                setPayBtnState(false);
                return;
            }
            if (mCheckBranches.isChecked() &&
                    trimmed(mEditBranches).isEmpty()) {
                setPayBtnState(false);
                return;
            }
            if (mCheckItems.isChecked() &&
                    trimmed(mEditItems).isEmpty()) {
                setPayBtnState(false);
                return;
            }
        }
        setPayBtnState(true);
    }

    void setPayBtnState(boolean enabled) {
        mBtnMakePayment.setEnabled(enabled);
        mBtnMakePayment.setBackgroundColor(
                enabled ? 0xff039be5 : 0xff78909c
        );
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

        updateViews();
        is_resetting = false;
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
                // ignore if this was triggered by resetting UI.
                // we might end up in infinite recursion otherwise!!!
                if (is_resetting)
                    return;

                updateViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // this will "start-things-off"
        resetUI();

        mLayoutLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).
                        setTitle("Log-out").
                        setMessage("Are you sure?").
                        setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PrefUtil.logoutUser(MainActivity.this);
                                finish();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                            }
                        }).
                        setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        mBtnMakePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final JSONObject paymentRequest = new JSONObject();

                int company_id = (int) IdEncoderUtil.decodeEncodedId(trimmed(mEditPaymentNumber),
                        IdEncoderUtil.ID_TYPE_COMPANY);
                int contract_type = mSpinnerType.getSelectedItemPosition();
                int duration = Integer.parseInt(trimmed(mEditDuration));

                final int LIMIT_NONE = -1;
                int limit_employee = LIMIT_NONE, limit_branch = LIMIT_NONE, limit_item = LIMIT_NONE;

                // if it is the GOLD license, fetch the limits
                if (mSpinnerType.getSelectedItemPosition() == 2) {
                    if (mCheckEmployees.isChecked())
                        limit_employee = Integer.parseInt(trimmed(mEditEmployees));
                    if (mCheckBranches.isChecked())
                        limit_branch = Integer.parseInt(trimmed(mEditBranches));
                    if (mCheckItems.isChecked())
                        limit_item = Integer.parseInt(trimmed(mEditItems));
                }

                final IssuePaymentRequest request = IssuePaymentRequest.newBuilder().
                        setSheketAuth(
                                SheketAuth.
                                        newBuilder().
                                        setLoginCookie(
                                                PrefUtil.getLoginCookie(MainActivity.this)
                                        )).
                        setCompanyId(company_id).
                        setContractType(contract_type).
                        setDurationDays(duration).
                        setEmployeeLimit(limit_employee).
                        setBranchLimit(limit_branch).
                        setItemLimit(limit_item).build();

                final ProgressDialog verificationProgress = ProgressDialog.show(
                        MainActivity.this, "Making payment", "Please Wait...", true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final boolean[] success = new boolean[1];
                        final String[] errMsg = new String[1];
                        try {
                            IssuePaymentResponse response = new SheketGRPCCall<IssuePaymentResponse>().runBlockingCall(new SheketGRPCCall.GRPCCallable<IssuePaymentResponse>() {
                                @Override
                                public IssuePaymentResponse runGRPCCall() throws Exception {
                                    ManagedChannel managedChannel = ManagedChannelBuilder.
                                            forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                                            usePlaintext(true).
                                            build();

                                    SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                                            SheketServiceGrpc.newBlockingStub(managedChannel);
                                    return blockingStub.issuePayment(request);
                                }
                            });
                            success[0] = true;
                        } catch (SheketGRPCCall.SheketInvalidLoginException e) {
                            PrefUtil.logoutUser(MainActivity.this);
                            finish();
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        } catch (SheketGRPCCall.SheketInternetException e) {
                            success[0] = false;
                            errMsg[0] = "Internet problem";
                        } catch (SheketGRPCCall.SheketException e) {
                            success[0] = false;
                            errMsg[0] = e.getMessage();
                        }

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verificationProgress.dismiss();
                                if (success[0]) {
                                    new AlertDialog.Builder(MainActivity.this).
                                            setIcon(android.R.drawable.ic_dialog_info).
                                            setMessage("Payment successful").
                                            show();
                                } else {
                                    new AlertDialog.Builder(MainActivity.this).
                                            setIcon(android.R.drawable.ic_dialog_alert).
                                            setTitle("Payment Error").
                                            setMessage(errMsg[0]).show();
                                }
                            }
                        });
                    }
                }).start();
            }
        });
    }
}
