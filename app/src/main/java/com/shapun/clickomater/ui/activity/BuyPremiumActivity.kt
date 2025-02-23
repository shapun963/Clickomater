package com.shapun.clickomater.ui.activity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.billingclient.api.*
import com.shapun.clickomater.Constants
import com.shapun.clickomater.databinding.ActivityBuyPremiumBinding
import com.shapun.clickomater.util.Utils

class BuyPremiumActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private lateinit var billingClient: BillingClient
    private lateinit var binding: ActivityBuyPremiumBinding
    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.let { handlePurchases(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityBuyPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = getSharedPreferences(Constants.DEFAULT_PREFERENCE,MODE_PRIVATE)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchaseUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object: BillingClientStateListener {
            @SuppressLint("SetTextI18n")
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, billingResult.debugMessage)
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK){
                    //Check if user brought premium from other device or had uninstalled and reinstalled app
                    val queryPurchasesParams = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                    val queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(Constants.ONE_TIME_PREMIUM)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build())
                    ).build()

                    billingClient.queryProductDetailsAsync(queryProductDetailsParams) { result, productDetailsList ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            val price = productDetailsList[0].oneTimePurchaseOfferDetails?.formattedPrice
                            Log.d(TAG,"hbj "+price.toString())
                            runOnUiThread {
                                binding.btnBuyPremium.text = "Buy Premium  " + (price ?: "")
                            }
                            val productDetailsParamsList = listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetailsList.first())
                                    .build()
                            )
                            binding.btnBuyPremium.setOnClickListener {
                                billingClient.launchBillingFlow(this@BuyPremiumActivity,BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build())
                            }
                        }
                    }

                    billingClient.queryPurchasesAsync (queryPurchasesParams){result,purchases->
                        Log.d(MainActivity.TAG,"Purchases: $purchases")
                        if (result.responseCode == BillingClient.BillingResponseCode.OK){
                            handlePurchases(purchases)
                        }else{
                            Utils.toast(this@BuyPremiumActivity,"An error occurred.Try again later.")
                            Log.d(TAG,result.debugMessage)
                            finish()
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
                Utils.toast(this@BuyPremiumActivity,"An error occurred.Try again later.")
                finish()
            }

        })

    }

    @SuppressLint("ApplySharedPref")
    private fun handlePurchases(it: MutableList<Purchase>) {
        binding.linPremium.visibility = View.GONE
        binding.linNonPremium.visibility = View.GONE
        it.forEach {
            if (it.products.contains(Constants.ONE_TIME_PREMIUM)) {
                binding.linPremium.visibility = View.VISIBLE
                binding.linNonPremium.visibility = View.GONE
                if (!preferences.getBoolean(Constants.KEY_IS_PREMIUM, false)) {
                    preferences.edit()
                        .putBoolean(Constants.KEY_IS_PREMIUM, true).commit()
                    recreate()
                }
                return
            }
        }
        binding.linNonPremium.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home->finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object  {
        const val TAG = "BuyPremiumActivity"
    }
}
