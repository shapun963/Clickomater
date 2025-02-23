package com.shapun.clickomater.ui.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.shapun.clickomater.BuildConfig
import com.shapun.clickomater.Constants
import com.shapun.clickomater.R
import com.shapun.clickomater.databinding.ActivityMainBinding
import com.shapun.clickomater.model.TaskData
import com.shapun.clickomater.ui.adapter.ProjectListAdapter
import com.shapun.clickomater.ui.fragment.StartRecordingDialogFragment
import com.shapun.clickomater.ui.fragment.TaskInfoDialog
import com.shapun.clickomater.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity() {

    private var mIsPremiumUser: Boolean = false
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var projectListPath: Path
    private lateinit var mProjectList: MutableList<TaskData>
    private lateinit var binding: ActivityMainBinding
    private var mRewardedAd: RewardedAd? = null
    private lateinit var preferences: SharedPreferences
    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if(billingResult.responseCode == BillingResponseCode.OK) {
            purchases?.let { handlePurchases(it) }
        }
    }
    private lateinit var billingClient : BillingClient
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = Firebase.analytics
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = getSharedPreferences(Constants.DEFAULT_PREFERENCE,MODE_PRIVATE)
        mIsPremiumUser = preferences.getBoolean(Constants.KEY_IS_PREMIUM, false)
        initAds()
        initInAppPurchase()
        setSupportActionBar(binding.toolbar)
        projectListPath = Constants.getTaskListPath(this)
        loadProjects()
        binding.fab.setOnClickListener {
            if (mProjectList.size >= 8 && mIsPremiumUser.not()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Watch Ad")
                    .setMessage("Only 8 tasks are provided for free inorder to unlock more sloths you need to watch an video ad")
                    .setPositiveButton("Watch Ad") { _, _ ->
                        if (mRewardedAd != null) {
                            mRewardedAd?.show(this) {
                                Log.d(TAG, "User earned the reward.")
                                loadRewardedVideoAd()
                                StartRecordingDialogFragment.newInstance()
                                    .show(supportFragmentManager, "StartRecordingDialogFragment")
                            }
                        } else {
                            Utils.longToast(this, "Ad haven't loaded yet. Try again after some time")
                            Log.d(TAG, "The rewarded ad wasn't ready yet.")
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                StartRecordingDialogFragment.newInstance()
                    .show(supportFragmentManager, "StartRecordingDialogFragment")
            }
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadProjects()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initInAppPurchase(){
        billingClient = newBuilder(this)
            .setListener(purchaseUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object: BillingClientStateListener{
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if(billingResult.responseCode == BillingResponseCode.OK){
                    //Check if user brought premium from other device or had uninstalled and reinstalled app
                    val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
                    billingClient.queryPurchasesAsync (params){result,purchases->
                        Log.d(TAG,"Purchases: $purchases")
                        if (result.responseCode == BillingResponseCode.OK){
                            handlePurchases(purchases)
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() {
            }

        })
    }

    private fun loadProjects() = lifecycleScope.launch {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerview.visibility = View.GONE
        binding.noProjects.root.visibility = View.GONE
        val time = measureTimeMillis {
            mProjectList = getProjectList()
        }
        if(time<1000) delay(1000- time)
        val adapter = ProjectListAdapter(mProjectList)
        binding.recyclerview.adapter = adapter
        adapter.setItemClickListener {
            TaskInfoDialog.newInstance(mProjectList[it])
                .show(supportFragmentManager, "ProjectInfoDialog")
        }
        refreshRecyclerviewEmptyView()
        binding.progressBar.visibility = View.GONE
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_premium->startActivity(Intent(this,BuyPremiumActivity::class.java))
            R.id.action_rate -> {
                Utils.openUrl(this,"https://play.google.com/store/apps/details?id=com.shapun.clickomater&reviewId=0")
            }
            R.id.action_more_apps -> {
                Utils.openUrl(this,"https://play.google.com/store/apps/developer?id=Shapun+S+Poonja")
            }
            R.id.action_telegram->Utils.openUrl(this,"https://t.me/clickomater")
            R.id.action_report_bug -> {
                try {
                    startActivity(getEmailIntent("Clickomater Bug"))
                    return true
                } catch (e: ActivityNotFoundException) {
                    Utils.toast(this, "Missing apk to perform  action.")
                }
            }
            R.id.action_suggestions -> {
                try{
                    startActivity(getEmailIntent("Clickomater Suggestion"))
                    return true
                } catch (e: ActivityNotFoundException) {
                    Utils.toast(this, "Missing apk to perform  action.")
                }
            }
        }
        return false
    }

    private fun getEmailIntent(subject: String) = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL,Array(1){"shapun963dev@gmail.com"})
        putExtra(Intent.EXTRA_SUBJECT,subject)
    }



    private suspend fun  getProjectList(): MutableList<TaskData> = withContext(Dispatchers.IO) {
        try {
            Gson().fromJson(
                FileReader(projectListPath.toFile()),
                object : TypeToken<MutableList<TaskData>>() {}.type
            )
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun loadRewardedVideoAd(){
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, BuildConfig.ADMOB_VIDEO_AD_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                mRewardedAd = null
                loadRewardedVideoAd()
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                mRewardedAd = rewardedAd
            }
        })
    }
    private fun refreshRecyclerviewEmptyView(){
        if(mProjectList.size == 0){
            binding.noProjects.root.visibility = View.VISIBLE
            binding.recyclerview.visibility = View.GONE
        }else{
            binding.noProjects.root.visibility = View.GONE
            binding.recyclerview.visibility = View.VISIBLE
        }
    }

    fun deleteProject(uuid: String){
        for(index in 0 until mProjectList.size ){
            if(mProjectList[index].uuid == uuid){
                mProjectList.removeAt(index)
                binding.recyclerview.adapter?.notifyItemRemoved(index)
                binding.recyclerview.adapter?.notifyItemRangeChanged(index,mProjectList.size)
                FileWriter(projectListPath.toFile()).use {
                    GsonBuilder().setPrettyPrinting().create().toJson(mProjectList,it)
                }
                val dataDir = File(cacheDir, "data").toPath()
                val imgDir = File(cacheDir, "img").toPath()
                val imgPath = Paths.get(imgDir.absolutePathString(),"$uuid.png")
                imgPath.deleteIfExists()
                val dataPath = Paths.get(dataDir.absolutePathString(),"$uuid.json")
                dataPath.deleteIfExists()
                break
            }
        }
        refreshRecyclerviewEmptyView()
    }
    fun updateProject(updatedTaskData: TaskData){
        for(index in 0 until mProjectList.size){
            if(mProjectList[index].uuid == updatedTaskData.uuid){
                mProjectList.removeAt(index)
                mProjectList.add(index,updatedTaskData)
                binding.recyclerview.adapter?.notifyItemChanged(index)
                FileWriter(projectListPath.toFile()).use {
                    GsonBuilder().setPrettyPrinting().create().toJson(mProjectList,it)
                }
                break
            }
        }
    }

    private fun initAds(){
        if(!mIsPremiumUser) {
            MobileAds.initialize(this) {}
            loadRewardedVideoAd()
            val adRequest = AdRequest.Builder().build()
            binding.adView.loadAd(adRequest)
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun handlePurchases(purchases: List<Purchase>){
        purchases.forEach {
            if (it.products.contains(Constants.ONE_TIME_PREMIUM)) {
                Log.i(TAG, "Premium user")
                if (!preferences.getBoolean(Constants.KEY_IS_PREMIUM, false)) {
                    preferences.edit()
                        .putBoolean(Constants.KEY_IS_PREMIUM, true).commit()
                    recreate()
                }
            }
        }
    }
    companion object{
        const val TAG = "MainActivity"
    }
}
