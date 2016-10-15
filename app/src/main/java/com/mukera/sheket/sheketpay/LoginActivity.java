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
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.network.SignupResponse;
import com.mukera.sheket.client.network.SingupRequest;
import java.util.Arrays;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by fuad on 9/8/16.
 */
public class LoginActivity extends AppCompatActivity {
    //private LoginButton mFacebookSignInButton;
    private FancyButton mFacebookButton;

    private ProgressDialog mProgress = null;
    private CallbackManager mFacebookCallbackManager;

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
                Toast.makeText(getApplicationContext(), "Login Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
                new SignInTask("").execute();
                /*
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,
                        Arrays.asList("public_profile"));
                        */
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
        private String mToken;
        private String errMsg;

        public SignInTask(String token) {
            super();
            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SignupResponse response = new SheketGRPCCall<SignupResponse>().runBlockingCall(new SheketGRPCCall.GRPCCallable<SignupResponse>() {
                    @Override
                    public SignupResponse runGRPCCall() throws Exception {
                        ManagedChannel managedChannel = ManagedChannelBuilder.
                                forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                                usePlaintext(true).
                                build();

                        SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                                SheketServiceGrpc.newBlockingStub(managedChannel);

                        SingupRequest request = SingupRequest.newBuilder().
                                setToken(mToken).build();
                        return blockingStub.userSignup(request);
                    }
                });

                Context context = LoginActivity.this;

                PrefUtil.setUserId(context, response.getUserId());
                PrefUtil.setLoginCookie(context, response.getLoginCookie());

            } catch (SheketGRPCCall.SheketException e) {
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
