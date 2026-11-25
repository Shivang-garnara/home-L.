package com.example.launcherai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.launcherai.data.repository.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageMonitorReceiver(
    private val repositoryProvider: ((Context) -> WorkspaceRepository)? = null
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        Log.d(TAG, "Package broadcast received: action=$action, package=$packageName, isReplacing=$isReplacing")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = repositoryProvider?.invoke(context)
                    ?: (context.applicationContext as? com.example.launcherai.LauncherApplication)?.repository
                    ?: WorkspaceRepository(context)

                when (action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (!isReplacing) {
                            repository.onPackageAdded(packageName)
                        } else {
                            repository.onPackageReplaced(packageName)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        repository.onPackageRemoved(packageName, isReplacing)
                    }
                    Intent.ACTION_PACKAGE_CHANGED -> {
                        repository.onPackageChanged(packageName)
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        repository.onPackageReplaced(packageName)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling package broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PackageMonitorReceiver"
    }
}
