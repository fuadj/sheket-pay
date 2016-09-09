package com.mukera.sheket.sheketpay;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

    String trimmed(EditText editText) {
        return editText.getText().toString().trim();
    }

    void updateViews() {
        mLayoutLimit.setVisibility(
                // only make visible if position 2(the GOLD license) is selected
                mSpinnerType.getSelectedItemPosition() == 2 ?
                        View.VISIBLE : View.GONE);

        if (!IdEncoderUtil.isValidEncodedCompanyId(trimmed(mEditPaymentNumber)) ||
                trimmed(mEditDuration).isEmpty()) {
            mBtnMakePayment.setVisibility(View.INVISIBLE);
            return;
        }

        // if it is the GOLD license, make sure the fields are filled in
        if (mSpinnerType.getSelectedItemPosition() == 2) {
            if (mCheckEmployees.isChecked() &&
                    trimmed(mEditEmployees).isEmpty()) {
                mBtnMakePayment.setVisibility(View.INVISIBLE);
            }
            if (mCheckBranches.isChecked() &&
                    trimmed(mEditBranches).isEmpty()) {
                mBtnMakePayment.setVisibility(View.INVISIBLE);
            }
            if (mCheckItems.isChecked() &&
                    trimmed(mEditItems).isEmpty()) {
                mBtnMakePayment.setVisibility(View.INVISIBLE);
            }
        }
        mBtnMakePayment.setVisibility(View.VISIBLE);
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

                long company_id = IdEncoderUtil.decodeEncodedId(trimmed(mEditPaymentNumber),
                        IdEncoderUtil.ID_TYPE_COMPANY);
                int contract_type = mSpinnerType.getSelectedItemPosition();
                int duraiton = Integer.parseInt(trimmed(mEditDuration));

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

                try {
                    paymentRequest.put("company_id", company_id);
                    paymentRequest.put("contract_type", contract_type);
                    paymentRequest.put("duration", duraiton);
                    paymentRequest.put("employee_limit", limit_employee);
                    paymentRequest.put("branch_limit", limit_branch);
                    paymentRequest.put("item_limit", limit_item);

                } catch (JSONException e) {
                    // TODO: better error handling can't encode it
                    return;
                }

                final ProgressDialog verificationProgress = ProgressDialog.show(
                        MainActivity.this, "Making payment", "Please Wait...", true);

                new Thread() {
                    @Override
                    public void run() {
                        final Pair<Boolean, String> result = makePayment(paymentRequest);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verificationProgress.dismiss();
                                if (result.first == Boolean.TRUE) {
                                    new AlertDialog.Builder(MainActivity.this).
                                            setIcon(android.R.drawable.ic_dialog_info).
                                            setMessage("Payment successful").
                                            show();
                                } else {
                                    new AlertDialog.Builder(MainActivity.this).
                                            setIcon(android.R.drawable.ic_dialog_alert).
                                            setTitle("Payment Error").
                                            setMessage(result.second).show();
                                }
                            }
                        });
                    }
                }.start();
            }
        });
    }

    public static final OkHttpClient client = new OkHttpClient();

    Pair<Boolean, String> makePayment(JSONObject paymentRequestObject) {
        Request.Builder builder = new Request.Builder();
        builder.url(ServerAddress.getAddress() + "v1/payment/verify");
        builder.addHeader("Cookie",
                PrefUtil.getLoginCookie(MainActivity.this));
        builder.post(RequestBody.create(MediaType.parse("application/json"),
                paymentRequestObject.toString()));
        try {
            Response response = client.newCall(builder.build()).execute();

            JSONObject result = new JSONObject(response.body().string());

            if (!response.isSuccessful()) {
                return new Pair<>(Boolean.FALSE, result.getString("error_message"));
            }

            return new Pair<>(Boolean.TRUE, null);
        } catch (JSONException | IOException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }
}
