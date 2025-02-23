package com.shapun.clickomater.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest.*
import com.shapun.clickomater.R
import com.shapun.clickomater.databinding.ActivityAppSelectorBinding
import com.shapun.clickomater.databinding.RowAppInfoBinding
import com.shapun.clickomater.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectorActivity : AppCompatActivity() {

    private var searchText =""
    private var mAllPackages: List<AppInfo>? = null
    private lateinit var binding: ActivityAppSelectorBinding
    private var mShowSystemApps = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            mShowSystemApps = it.getBoolean("show_system_apps",false)
        }
        binding = ActivityAppSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val spans = resources.displayMetrics.widthPixels/ Utils.dpToPx(this,100)
        binding.recyclerView.layoutManager = GridLayoutManager(this,spans)
         lifecycleScope.launch{
            mAllPackages = getPackages()
            refreshAppListBasedOnSearchAndShowSystemApp()
             binding.progressbar.visibility = View.GONE
        }
        if(Utils.isPremiumUser(this).not()){
            val adRequest = Builder().build()
            binding.adView.loadAd(adRequest)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("show_system_apps",mShowSystemApps)
    }

    override fun onPause() {
        super.onPause()
        sendBroadcast(Intent(APP_SELECTED))
        finish()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_selector, menu)
        menu.findItem(R.id.action_show_system_apps).isChecked = mShowSystemApps
        (menu.findItem(R.id.action_search).actionView as SearchView).setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String): Boolean {
                searchText = newText
                refreshAppListBasedOnSearchAndShowSystemApp()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_show_system_apps-> {
                mShowSystemApps = mShowSystemApps.not()
                item.isChecked = mShowSystemApps
                refreshAppListBasedOnSearchAndShowSystemApp()
            }
        }
        return false
    }

    fun refreshAppListBasedOnSearchAndShowSystemApp(){
        binding.recyclerView.adapter = mAllPackages?.let {
            AppListAdapter(it.filter { item ->
                (item.name.contains(searchText,true) or item.name.contains(searchText,true))  and (item.isSystemApp.not() or mShowSystemApps)
            })
        }
    }

    data class AppInfo(val name:String,val packageName: String,val icon: Drawable,val isSystemApp: Boolean)

    inner class AppListAdapter(val data: List<AppInfo>) :
        RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: RowAppInfoBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context
            return ViewHolder(RowAppInfoBinding.inflate(LayoutInflater.from(context),parent,false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.imageView.setImageDrawable(data[position].icon)
            holder.binding.textView.text = data[position].name
            /*holder.view.tvName.text = data[position].name
            holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(data[position].imagePath))
            holder.binding.tvCreatedTime.text = "Created " + DateUtils.getRelativeTimeSpanString(
                data[position].createdTime, System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            ).toString()
             */
            holder.itemView.setOnClickListener {
                Intent(APP_SELECTED).also {
                    it.putExtra(KEY_PACKAGE_NAME,data[position].packageName)
                    sendBroadcast(it)
                    finish()
                }
            }

        }

        override fun getItemCount(): Int = data.size
    }
    @Suppress("DEPRECATION")
    suspend fun  getPackages(): List<AppInfo> = withContext(Dispatchers.Default){
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } else {
            packageManager.getInstalledApplications(0)
        }
        packages.filter {  packageManager.getLaunchIntentForPackage(it.packageName) != null}.map {
            AppInfo(it.loadLabel(packageManager).toString(),it.packageName,it.loadIcon(packageManager),(it.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        }
    }

    companion object{
        const val APP_SELECTED = "app_selected"
        const val KEY_PACKAGE_NAME = "key_package_name"
    }
}