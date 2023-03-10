package com.pow.networkapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pow.networkapp.R;
import com.pow.networkapp.databinding.ActivityWalletBinding;
import com.pow.networkapp.model.User;
import com.pow.networkapp.util.NetworkChangeListener;

import java.util.HashMap;

public class WalletActivity extends AppCompatActivity {

    private ActivityWalletBinding binding;
    private FirebaseUser firebaseUser;
    private final int minWithdrawal = 15000;
    private int myTotalBalance;
    private int myRequest;
    private NetworkChangeListener networkChangeListener = new NetworkChangeListener();
    private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();


        loadBanner();
        loadAds();
        getBalanceInfo();

        if (binding.formLayout.getVisibility()==View.VISIBLE){
            binding.confirmBtn.setOnClickListener(view -> {
                if (binding.walletAddressEt.getText().toString().trim().length()>1 && binding.withdrawalEt.getText().toString().trim().length()>0){
                    String address = binding.walletAddressEt.getText().toString().trim();
                    try {
                        myTotalBalance = Integer.parseInt(binding.totalBalance.getText().toString());
                        myRequest = Integer.parseInt(binding.withdrawalEt.getText().toString());
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                    if (myTotalBalance >= myRequest){

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                        String id = reference.push().getKey();
                        long time = System.currentTimeMillis();
                        HashMap<String,Object> map = new HashMap<>();
                        map.put("address",address);
                        map.put("userId",firebaseUser.getUid());
                        map.put("time",time);
                        map.put("withdrawal",myRequest);
                        map.put("id",id);

                        if (id != null) {
                            reference.child("MyTransactions").child(firebaseUser.getUid()).child(id)
                                    .setValue(map).addOnSuccessListener(unused -> {
                                        lostBalance(myRequest);
                                        HashMap<String,Object> hashMap = new HashMap<>();
                                        hashMap.put("address",address);
                                        hashMap.put("userId",firebaseUser.getUid());
                                        hashMap.put("withdrawal",myRequest);
                                        hashMap.put("time",time);
                                        hashMap.put("id",id);
                                        reference.child("Transactions").child(id).setValue(hashMap);
                                        finish();

                                    }).addOnFailureListener(e -> Toast.makeText(WalletActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show());
                        }





                    }





                }
            });


        }

    }


    private void loadBanner(){
        Appodeal.initialize(this,getString(R.string.appodeal_app_id),Appodeal.BANNER);
        Appodeal.show(this,Appodeal.BANNER);
        Appodeal.isLoaded(Appodeal.BANNER);
        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override
            public void onBannerLoaded(int i, boolean b) {
            }

            @Override
            public void onBannerFailedToLoad() {

            }

            @Override
            public void onBannerShown() {

            }

            @Override
            public void onBannerShowFailed() {

            }

            @Override
            public void onBannerClicked() {

            }

            @Override
            public void onBannerExpired() {

            }
        });
    }

    private void lostBalance(int lostValue){
        FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            User user = snapshot.getValue(User.class);
                            HashMap<String,Object> map = new HashMap<>();
                            if (user != null) {
                                map.put("balance",user.getBalance() - lostValue);
                            }
                            FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid()).updateChildren(map);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }



    private void getBalanceInfo(){
        FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            User user = snapshot.getValue(User.class);
                            binding.claimedBalance.setText(new StringBuilder().append("").append(user.getClaimed()).toString());
                            binding.referralBalance.setText(new StringBuilder().append("").append(user.getReferral()).toString());

                            binding.totalBalance.setText(new StringBuilder().append(user.getBalance()).toString());

                            binding.currentBalance.setText(new StringBuilder().append(user.getBalance()).toString());


                            int a = (int) (user.getBalance() * 100 / minWithdrawal);
                            binding.earningProgress.setProgress((Math.min(a, 100)));

                            binding.earningPercent.setText(new StringBuilder().append(Math.min(a, 100)).append("%").toString());




                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }



    private void loadAds(){
        MobileAds.initialize(WalletActivity.this, initializationStatus -> {

        });
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(WalletActivity.this, getString(R.string.intersId), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                //Toast.makeText(WatchAdsActivity.this, "t??kland??", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                //Toast.makeText(WatchAdsActivity.this, "kapand??", Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
                                //Toast.makeText(WatchAdsActivity.this, "t??kland??2", Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
                                //Toast.makeText(getContext(), "g??steriyor", Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
                                //Toast.makeText(getContext(), "full", Toast.LENGTH_SHORT).show();

                            }
                        });
                    }

                });
    }





    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, intentFilter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInterstitialAd!=null){
            mInterstitialAd.show(this);
        }
    }

}