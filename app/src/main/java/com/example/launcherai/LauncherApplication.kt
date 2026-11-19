package com.example.launcherai

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.example.launcherai.data.db.LauncherDatabase
import com.example.launcherai.data.repository.WorkspaceRepository
import com.example.launcherai.receiver.PackageMonitorReceiver

class LauncherApplication : Application() {

    lateinit var database: LauncherDatabase
        private set

    lateinit var repository: WorkspaceRepository
        private set

    private var packageReceiver: PackageMonitorReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = LauncherDatabase.getInstance(this)
        repository = WorkspaceRepository(this, database.workspaceDao())

        registerPackageMonitorReceiver()
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

    override fun onTerminate() {
        super.onTerminate()
        packageReceiver?.let {
            unregisterReceiver(it)
        }
    }

    companion object {
        lateinit var instance: LauncherApplication
            private set
    }
}
