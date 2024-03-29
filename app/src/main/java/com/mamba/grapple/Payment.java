package com.mamba.grapple;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.Customization;
import com.devmarvel.creditcardentry.library.CardValidCallback;
import com.devmarvel.creditcardentry.library.CreditCard;
import com.devmarvel.creditcardentry.library.CreditCardForm;


import java.util.Calendar;

import io.card.payment.CardIOActivity;

public class Payment extends Activity {


    // service related variables
    private boolean mBound = false;
    DBService mService;

    // current user data
    LoginManager session;
    UserObject currentUser;

    private LinearLayout linearLayout;
    private CreditCardForm form;
    CreditCardForm creditForm;

    private Braintree braintree;
    String btreeToken = "";


    private ServiceConnection mConnection = new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service){
            DBService.LocalBinder binder = (DBService.LocalBinder) service;
            mService = binder.getService();
            mService.setSession(session);
            btreeToken = mService.btreeToken;
            mBound = true;
            Log.v("btreeToken", btreeToken);
        }

        public void onServiceDisconnected(ComponentName arg0){
            mBound = false;
        }
    };



    CardValidCallback cardValidCallback = new CardValidCallback(){
        @Override
        public void cardValid(CreditCard card) {
            Log.d("Payment", "valid card: " + card);
            Toast.makeText(Payment.this, "Card valid and complete", Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        creditForm = (CreditCardForm) findViewById(R.id.credit_card_form);
        creditForm.setOnCardValidCallback(cardValidCallback);

    }


    // check login status every time the activity gets shown
    protected void onStart(){
        super.onStart();
        Log.v("Main Activity", "Starting");
        invalidateOptionsMenu();
        session = new LoginManager(getApplicationContext());
        if(session.isLoggedIn()){
            currentUser = session.getCurrentUser();
            Log.v("Search Login Status", currentUser.getName() +  " has been logged in");
            startService(new Intent(this, DBService.class));
            bindService(new Intent(this, DBService.class), mConnection, Context.BIND_AUTO_CREATE);
        }
    }



    @Override
    protected void onStop(){
        super.onStop();
        // Unbind from the service
        if (mBound){
            Log.v("Unbinding Service", "Search Activity");
            unbindService(mConnection);
            mBound = false;
        }

    }



    public void onScanPress(View v) {
        Intent scanIntent = new Intent(this, CardIOActivity.class);

        // customize these values to suit your needs.
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, true); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, true); // default: false
        scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, true); // default: false

        // MY_SCAN_REQUEST_CODE is arbitrary and is only used within this activity.
        startActivityForResult(scanIntent, 123);
    }


    public void onBraintreeSubmit(View v) {
        Intent intent = new Intent(getApplicationContext(), BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, btreeToken);

        Customization customization = new Customization.CustomizationBuilder()
                .submitButtonText("Store")
                .build();
        intent.putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        startActivityForResult(intent, 100);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 123) {
            String resultDisplayStr;
            if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
                io.card.payment.CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);

                // Never log a raw card number. Avoid displaying it, but if necessary use getFormattedCardNumber()
                resultDisplayStr = "Card Number: " + scanResult.cardNumber + "\n";
                creditForm.setCardNumber(scanResult.cardNumber, true);
                creditForm.getCreditCard();


                // Do something with the raw number, e.g.:
                // myService.setCardNumber( scanResult.cardNumber );

                if (scanResult.isExpiryValid()) {
                    resultDisplayStr += "Expiration Date: " + scanResult.expiryMonth + "/" + scanResult.expiryYear + "\n";

                    String expDate;
                    if(scanResult.expiryMonth < 10){
                        expDate = "" + 0 + scanResult.expiryMonth;
                    }else{
                        expDate = "" + scanResult.expiryMonth;
                    }

                    expDate +=  "/" + (scanResult.expiryYear%100);
                    creditForm.setExpDate(expDate, true);
                }

                if (scanResult.cvv != null) {
                    // Never log or display a CVV
                    resultDisplayStr += "CVV has " + scanResult.cvv + " digits.\n";
                    creditForm.setSecurityCode(scanResult.cvv, false);
                }

                if (scanResult.postalCode != null){
                    resultDisplayStr += "Postal Code: " + scanResult.postalCode + "\n";
                    creditForm.setZipCode(scanResult.postalCode, false);
                }



            }
            else {
                resultDisplayStr = "Scan was canceled.";
            }

            Log.v("Scan results", resultDisplayStr);
            // do something with resultDisplayStr, maybe display it in a textView
            // resultTextView.setText(resultStr);
        }
        // else handle other activity results
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_payment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    }

