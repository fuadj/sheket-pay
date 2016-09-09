package com.mukera.sheket.sheketpay;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by fuad on 9/8/16.
 */
public class LoginActivity extends AppCompatActivity {
    //private LoginButton mFacebookSignInButton;
    private FancyButton mFacebookButton;

    private ProgressDialog mProgress = null;
    private CallbackManager mFacebookCallbackManager;

    public static final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        // the user has logged in, start MainActivity
        if (PrefUtil.isUserLoggedIn(this)) {
            startMainActivity();
            return;
        }

        mFacebookCallbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        LoginManager.getInstance().registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (loginResult.getAccessToken() == null)
                    return;

                mFacebookButton.setVisibility(View.GONE);
                mProgress = ProgressDialog.show(LoginActivity.this,
                        "Logging in", "Please Wait", true);
                new SignInTask(loginResult.getAccessToken().getToken()).execute();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
            }
        });
        mFacebookButton = (FancyButton) findViewById(R.id.facebook_login);
        mFacebookButton.setText("Login with Facebook");
        mFacebookButton.setVisibility(View.VISIBLE);
        mProgress = null;
        setTitle(R.string.app_name);
        mFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,
                        Arrays.asList("public_profile"));
            }
        });
    }

    void startMainActivity() {
        this.finish();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    class SignInTask extends AsyncTask<Void, Void, Boolean> {
        public static final String REQUEST_TOKEN = "token";

        // this is what differentiates the normal sheket with this "pay" sheket.
        // we add this optional field and set its value to true. Server checks
        // if the user is authorized for this, and only then does it send the
        // login cookie.
        public static final String REQUEST_IS_SHEKET_PAY = "is_sheket_pay";

        public static final String RESPONSE_USER_ID = "user_id";

        static final String JSON_RESPONSE_ERR = "error_message";

        static final String JSON_REQUEST_LOGIN_COOKIE = "Set-Cookie";
        static final String JSON_RESPONSE_LOGIN_COOKIE = "Cookie";

        private String mToken;
        private String errMsg;

        public SignInTask(String token) {
            super();

            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Context context = LoginActivity.this;

                Request.Builder builder = new Request.Builder();
                builder.url(ServerAddress.getAddress() + "v1/signin/facebook");
                builder.post(
                        RequestBody.create(MediaType.parse("application/json"),
                                new JSONObject().put(REQUEST_TOKEN, mToken).
                                        put(REQUEST_IS_SHEKET_PAY, true).
                                        toString()
                        )
                );
                Response response = client.newCall(builder.build()).execute();
                if (!response.isSuccessful()) {
                    JSONObject err = new JSONObject(response.body().string());
                    errMsg = err.getString(JSON_RESPONSE_ERR);
                    return false;
                }

                String login_cookie = response.header(JSON_RESPONSE_LOGIN_COOKIE);

                JSONObject result = new JSONObject(response.body().string());

                long user_id = result.getLong(RESPONSE_USER_ID);

                PrefUtil.setUserId(context, user_id);
                PrefUtil.setLoginCookie(context, login_cookie);
            } catch (JSONException | IOException e) {
                errMsg = e.getMessage();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mProgress != null) {
                mProgress.dismiss();
                mProgress = null;
            }

            if (!success) {
                // remove any-facebook "logged-in" stuff
                Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                LoginManager.getInstance().logOut();
                mFacebookButton.setVisibility(View.VISIBLE);
                return;
            }

            startMainActivity();
        }

        @Override
        protected void onCancelled() {
            LoginManager.getInstance().logOut();
        }
    }
}
