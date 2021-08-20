package com.ninjabotscoloringbookapp.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.ninjabotscoloringbookapp.util.Constants;
import com.ninjabotscoloringbookapp.util.JSONParser;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.ninjabotscoloringbookapp.R;
import com.ninjabotscoloringbookapp.application.MyApplication;
import com.thekhaeng.pushdownanim.PushDownAnim;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.ninjabotscoloringbookapp.util.Constants.BACK_IMAGE_URL;
import static com.ninjabotscoloringbookapp.util.Constants.showRatingDialog;
import static com.thekhaeng.pushdownanim.PushDownAnim.DEFAULT_PUSH_DURATION;
import static com.thekhaeng.pushdownanim.PushDownAnim.DEFAULT_RELEASE_DURATION;


public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    ImageView btn_play, btn_my_creation, btn_share, btn_rate,btn_privacy_policy,btnaddremove;
    ProgressDialog progressDialog;
    JSONObject jobj = null;
    JSONParser jsonparser = new JSONParser();
    ImageView bgimg;
    String IMAGE;
    private BillingClient mBillingClient;
    public String SKU__LIFE = "life_time_plan";
    private String ITEM_SKU = "",selectedType="";
    SharedPreferences sharedpreferences;
    boolean INAPP=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);setContentView(R.layout.activity_main);

        if (!checkPermission(getApplicationContext())) {
            requestPermission();
        }

        init();
        setClick();

        checkadd();

        setBillingClient();

        new GetOnlineImagesData().execute();
    }

    private void checkadd() {
        try {
            sharedpreferences = getSharedPreferences("mypref", Context.MODE_PRIVATE);
            INAPP=sharedpreferences.getBoolean(getResources().getString(R.string.pref_remove_ads_key),false);

            if(!INAPP)
            {
                MobileAds.initialize(this, new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus initializationStatus) {

                    }
                });
            }else
            {
                btnaddremove.setImageDrawable(getResources().getDrawable(R.drawable.removeadd));
            }

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setBillingClient(){
        mBillingClient = BillingClient.newBuilder(MainActivity.this).enablePendingPurchases().setListener(this).build();
        if (!mBillingClient.isReady()) {
            selectedType=BillingClient.SkuType.INAPP;
            ITEM_SKU = SKU__LIFE;
            StartConnection();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        MyApplication.activityPaused();// On Pause notify the Application
    }

    @Override
    protected void onResume() {

        super.onResume();
        MyApplication.activityResumed();// On Resume notify the Application
    }

    private void setClick() {
        btn_play.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SelectImage.class);
            startActivity(intent);
        });

        btn_my_creation.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MyCreation.class);
            startActivity(intent);
        });

        btn_share.setOnClickListener(view -> {
            share();
        });

        btn_rate.setOnClickListener(view -> {

            showRatingDialog(MainActivity.this);
        });
        btn_privacy_policy.setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_link)));
            startActivity(browserIntent);
        });

        btnaddremove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(INAPP)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("You have already subscribe...");
                    builder.setPositiveButton("OK", (dialogInterface, i) ->Log.d("ADS","yes"));
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }else
                {
                    selectedType=BillingClient.SkuType.INAPP;
                    ITEM_SKU = SKU__LIFE;

                    Log.e("ITEM_SKU","-"+ITEM_SKU);

                    if (!ITEM_SKU.equalsIgnoreCase("")) {
//            mBillingClient = BillingClient.newBuilder(InAppActivity.this).enablePendingPurchases().setListener(this).build();
//            if (!mBillingClient.isReady()) {
//                StartConnection();
//            }
                        if(mBillingClient.isReady()){
                            setProductID();
                            queryPurchases();
                        }else{
                            StartConnection();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Something Went Wrong! Please Try Again.", Toast.LENGTH_LONG).show();
                        onBackPressed();
                    }
                }

            }
        });
    }

    public void StartConnection() {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                try {
//                    Log.e("TAG", String.valueOf(billingResult.getResponseCode())+BillingClient.BillingResponseCode.OK);
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // The billing client is ready. You can query purchases here.
//                    setProductID();
                    queryPurchases();
                    } else {
                        Toast.makeText(MainActivity.this, billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }

            }

            @Override
            public void onBillingServiceDisconnected() {
//                Toast.makeText(InAppActivity.this, getResources().getString(R.string.billing_connection_failure), Toast.LENGTH_SHORT);
            }
        });
    }

    private void queryPurchases() {

        //this things may be return all history like subscription and expired also
        /*mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS,
                new PurchaseHistoryResponseListener() {
                    @Override
                    public void onPurchaseHistoryResponse(BillingResult billingResult, List<PurchaseHistoryRecord> purchasesList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                && purchasesList != null) {
                            for (PurchaseHistoryRecord purchase : purchasesList) {
                                // Process the result.
                                Log.e("PurchaseHistoryRecord","-"+purchase.getSku());
                            }
                        }
                    }
                });*/

        //Method not being used for now, but can be used if purchases ever need to be queried in the future
        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(selectedType);
        if (purchasesResult != null) {
            List<Purchase> purchasesList = purchasesResult.getPurchasesList();
            if (purchasesList == null) {
                return;
            }
//            Log.e("query purchase size", "-" + purchasesList.size());
            if (!purchasesList.isEmpty()) {
                for (Purchase purchase : purchasesList) {
                    if (purchase.getSku().equals(ITEM_SKU)) {
                        Log.e("same", "same");
                        sharedpreferences.edit().putBoolean(getResources().getString(R.string.pref_remove_ads_key), true).commit();
                        checkadd();
//                        sharedpreferences.edit().putBoolean(getResources().getString(R.string.pref_remove_ads_key), true).commit();
                    }
                }
            }
        }
    }

    private void setProductID() {
        List<String> skuList = new ArrayList<>();
        skuList.add(ITEM_SKU);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();

        params.setSkusList(skuList).setType(selectedType);
        mBillingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (billingResult == null) {
                            Log.wtf("result", "onSkuDetailsResponse: null BillingResult");
                            return;
                        }

                        if (skuDetailsList != null) {
//                            Toast.makeText(context," "+skuDetailsList.size(),Toast.LENGTH_SHORT).show();
                            if (skuDetailsList.size() > 0) {
                                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                        .setSkuDetails(skuDetailsList.get(0))
                                        .build();
                                BillingResult responseCode = mBillingClient.launchBillingFlow(MainActivity.this, flowParams);
                                if (responseCode.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                                }
                                Log.e("getResponseCode", "-" + responseCode.getResponseCode());
                            } else {
                                Log.e("skuDetailsList size", "-" + skuDetailsList.size());
                            }
                        } else {
                            Log.e("skulist null", "null sku list");
                        }
                    }

                });

    }

    public boolean checkPermission(Context context) {
        int i3 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int i4 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return i3 == PackageManager.PERMISSION_GRANTED
                && i4 == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 2);
    }

    private void init() {
        btn_play = findViewById(R.id.btn_play);
        btn_my_creation = findViewById(R.id.btn_my_creation);
        btn_share = findViewById(R.id.btn_share);
        btn_rate = findViewById(R.id.btn_rate);
        btn_privacy_policy = findViewById(R.id.btn_privacy_policy);
        btnaddremove=findViewById(R.id.btnaddremove);
        bgimg=findViewById(R.id.bgimg);

        PushDownAnim.setPushDownAnimTo(btn_rate, btn_play, btn_my_creation, btn_share).
                setScale(PushDownAnim.MODE_STATIC_DP, 10).setDurationPush(DEFAULT_PUSH_DURATION)
                .setDurationRelease(DEFAULT_RELEASE_DURATION)
                .setInterpolatorPush(PushDownAnim.DEFAULT_INTERPOLATOR)
                .setInterpolatorRelease(PushDownAnim.DEFAULT_INTERPOLATOR);

    }

    public void share() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        share.putExtra(Intent.EXTRA_SUBJECT, "Xyz");
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.SHARE_APP_LINK)
                + getPackageName());
        startActivity(Intent.createChooser(share, "Share Link!"));

    }
    
    @Override
    public void onBackPressed() {
        ActivityCompat.finishAffinity(this);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        try {

//            Log.e("TAG", String.valueOf(billingResult.getResponseCode())+BillingClient.BillingResponseCode.OK);
            if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.DEVELOPER_ERROR)
            {
                Log.e("Developer error size", "size- " + purchases.size());
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.e("purchases size", "size- " + purchases.size());
                if(purchases != null)
                {
                    for (Purchase purchase : purchases) {
                        handlePurchase(purchase);
                    }
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
                Log.e("TAG", "User Canceled" + billingResult.getResponseCode());
//                onBackPressed();
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                sharedpreferences.edit().putBoolean(getResources().getString(R.string.pref_remove_ads_key), true).commit();
                checkadd();
            } else {
                Log.e("TAG", "Other code" + billingResult.getResponseCode());
//                onBackPressed();//check flow require or not bhk
                // Handle any other error codes.
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void handlePurchase(Purchase purchase) {

        //below bhk need for new version
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge purchase and grant the item to the user
//            Log.e("purchase success", "success");
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                        Log.e("acknowledge res code: ", "" + billingResult.getResponseCode());
                        sharedpreferences.edit().putBoolean(getResources().getString(R.string.pref_remove_ads_key), true).commit();
                        checkadd();
                    }
                });
            }
//            ShowDialog(); bhavesh

        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            // Here you can confirm to the user that they've started the pending
            // purchase, and to complete it, they should follow instructions that
            // are given to them. You can also choose to remind the user in the
            // future to complete the purchase if you detect that it is still
            // pending.
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    class GetOnlineImagesData extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... arg0) {

            jobj = jsonparser.getJSONFromUrl(BACK_IMAGE_URL);
            // check your log for json response
            JSONArray resultList = null;


            try {

                if (jobj != null) {
                    progressDialog.dismiss();
                    JSONObject json2 = jobj.getJSONObject(getString(R.string.data));
                    resultList = json2.getJSONArray("settings");


                    Log.e("jobj===", "" + jobj.toString());
                    Log.e("jobj1===", "" + resultList.length());

                    JSONObject jsonobject = (JSONObject) resultList.get(0);
                    IMAGE=(Constants.UPLOAD_URL +jsonobject.optString("bgimage"));

                    Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(IMAGE)
                            .skipMemoryCache(true)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    bgimg.setImageBitmap(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                }
            } catch (JSONException e) {
                progressDialog.dismiss();
                e.printStackTrace();
            }
            return "ab";
        }

        protected void onPostExecute(String ab) {
        }
    }
}



