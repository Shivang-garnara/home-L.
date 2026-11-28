package com.example.launcherai

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.UserHandle
import com.example.launcherai.data.db.LauncherDatabase
import com.example.launcherai.data.repository.WorkspaceRepository
import com.example.launcherai.receiver.PackageMonitorReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LauncherApplication : Application() {

    lateinit var database: LauncherDatabase
        private set

    lateinit var repository: WorkspaceRepository
        private set

    private var packageReceiver: PackageMonitorReceiver? = null
    private var launcherAppsCallback: LauncherApps.Callback? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = LauncherDatabase.getInstance(this)
        repository = WorkspaceRepository(this, database.workspaceDao())

        registerPackageMonitorReceiver()
        registerLauncherAppsCallback()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) {}
            override fun onActivityStarted(a: android.app.Activity) {}
            override fun onActivityResumed(a: android.app.Activity) {
                com.example.launcherai.ui.LauncherSettingsThemeApplier.applyIfSettingsActivity(a)
            }
            override fun onActivityPaused(a: android.app.Activity) {}
            override fun onActivityStopped(a: android.app.Activity) {}
            override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) {}
            override fun onActivityDestroyed(a: android.app.Activity) {}
        })
    }

    private fun registerPackageMonitorReceiver() {
        packageReceiver = PackageMonitorReceiver { repository }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    private fun registerLauncherAppsCallback() {
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.onPackageAdded(packageName)
                }
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.onPackageRemoved(packageName, false)
                }
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.onPackageChanged(packageName)
                }
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (!replacing) {
                        packageNames.forEach { repository.onPackageAdded(it) }
                    }
                }
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {}
        }
        launcherAppsCallback = callback
        launcherApps.registerCallback(callback)
    }

    override fun onTerminate() {
        super.onTerminate()
        packageReceiver?.let {
            unregisterReceiver(it)
        }
        launcherAppsCallback?.let {
            val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            launcherApps?.unregisterCallback(it)
        }
    }

    companion object {
        lateinit var instance: LauncherApplication
            private set
    }
}
