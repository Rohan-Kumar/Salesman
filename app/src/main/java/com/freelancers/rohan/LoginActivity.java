package com.freelancers.rohan;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Calendar;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    AutoCompleteTextView userId;
    EditText password;
    TextView status;
    Button signInButton, signOutButton,order;
    String Response = "";
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    static LocationRequest locationRequest;
    static GoogleApiClient googleApiClient;
    boolean isSet = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        userId = (AutoCompleteTextView) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        signInButton = (Button) findViewById(R.id.email_sign_in_button);
        signOutButton = (Button) findViewById(R.id.email_sign_out_button);
        order = (Button) findViewById(R.id.order);
        status = (TextView) findViewById(R.id.status);

        preferences = getSharedPreferences("login", Context.MODE_PRIVATE);
        editor = preferences.edit();

        changeView(preferences.getString("status", "login"));

    }


    public void login(View v) {
        if (hasConnection(getApplicationContext())) {
            if (userId.getText().toString().equals("")) {
                userId.setError("User ID cannot be empty");
                userId.requestFocus();
                return;
            }
            if (password.getText().toString().equals("")) {
                password.setError("Password cannot be empty");
                password.requestFocus();
                return;
            }
            new UserLoginTask(userId.getText().toString(), password.getText().toString()).execute();
        } else
            Toast.makeText(LoginActivity.this, "No internet.. Please connect to internet and try again", Toast.LENGTH_SHORT).show();
    }

    public void logout(View v) {
        changeView("login");
        stopService(new Intent(LoginActivity.this, LocationService.class));
    }

    public void changeView(String view) {
        if (view.equals("logout")) {
            userId.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
            signInButton.setVisibility(View.INVISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
            order.setVisibility(View.VISIBLE);
            status.setVisibility(View.VISIBLE);
            if (!isSet)
                status.setText(preferences.getString("time", "").equals("") ? "Error" : "Your last login was at " + preferences.getString("time", ""));
            editor.putString("status", "logout");
        } else if (view.equals("login")) {
            userId.setText("");
            password.setText("");
            userId.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.INVISIBLE);
            order.setVisibility(View.INVISIBLE);
            status.setVisibility(View.INVISIBLE);
            editor.putString("status", "login");
//            connectToApi();

        }
        editor.apply();
    }

    public static boolean hasConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiNetwork = cm
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            return true;
        }

        NetworkInfo mobileNetwork = cm
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            return true;
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        }

        return false;
    }


    public void connectToApi() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
                googleApiClient.connect();
                Log.d("TAG", "connect");
            }
        } else {
            Log.e("TAG", "unable to connect to google play services.");
        }

    }

    public void checkGps() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
//                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        startService(new Intent(LoginActivity.this, LocationService.class));
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(LoginActivity.this, 1000);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });

    }


    @Override
    public void onConnected(Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // milliseconds
        locationRequest.setFastestInterval(1000); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        checkGps();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == RESULT_OK) {
                startService(new Intent(LoginActivity.this, LocationService.class));
            } else {
                Toast.makeText(LoginActivity.this, "Gps not on. App wont work.", Toast.LENGTH_SHORT).show();
                changeView("login");
            }
        }
    }

    public void order(View view) {
        startActivity(new Intent(LoginActivity.this,OrderActivity.class));
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Void> {

        private final String mEmail;
        private final String mPassword;
        ProgressDialog progressDialog;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(LoginActivity.this, "",
                    "Loading. Please wait...", true);
            progressDialog.setCancelable(true);


        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            authenticate(mEmail, mPassword);
            return null;
        }

        @Override
        protected void onPostExecute(final Void success) {
            if (Response.equals("wrong password")) {
                progressDialog.dismiss();
                password.setError("Invalid password");
                password.requestFocus();
                Response = "";
            } else if (Response.equals("no account found")) {
                progressDialog.dismiss();
                userId.setError("Invalid userId");
                userId.requestFocus();
                Response = "";
            } else {
                String id = Response.substring(0,Response.indexOf(","));
                String admin_id = Response.substring(Response.indexOf(",")+1);
                Log.d("ids",id+" "+admin_id);
                progressDialog.dismiss();
                changeView("logout");
                Calendar c = Calendar.getInstance();
                preferences = getApplicationContext().getSharedPreferences("login", Context.MODE_PRIVATE);
                editor = preferences.edit();
                editor.putString("id",id);
                editor.putString("admin_id",admin_id);
                editor.putString("time", (c.getTime().getHours() > 12 ? (c.getTime().getHours() - 12) : (c.getTime().getHours())) + "/" + c.getTime().getMinutes());
                editor.apply();
                connectToApi();
                Response = "";
                status.setText("Last login at " + (c.getTime().getHours() > 12 ? (c.getTime().getHours() - 12) : (c.getTime().getHours())) + "/" + c.getTime().getMinutes());
            }

        }

    }

    public void authenticate(String userid, String password) {
        URL url;
        try {
            url = new URL("http://204.152.203.111/salesman/salesman_login.php");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("phnumber", userid)
                    .appendQueryParameter("password", password);


            String query = builder.build().getEncodedQuery();

            OutputStream os = httpURLConnection.getOutputStream();

            BufferedWriter mBufferedWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            mBufferedWriter.write(query);
            mBufferedWriter.flush();
            mBufferedWriter.close();
            os.close();

            httpURLConnection.connect();
            BufferedReader mBufferedInputStream = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String inline;
            while ((inline = mBufferedInputStream.readLine()) != null) {
                Response += inline;
            }
            mBufferedInputStream.close();
            Log.d("response", Response);



        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
