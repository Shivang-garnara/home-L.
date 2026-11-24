package com.example.launcherai.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserManager

/**
 * Data model representing an application group containing one or more widgets.
 */
data class AppWidgetGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val widgets: List<WidgetModel>,
    var isExpanded: Boolean = false
)

/**
 * Data model representing an individual widget provider item.
 */
data class WidgetModel(
    val providerInfo: AppWidgetProviderInfo,
    val label: String,
    val appName: String,
    val packageName: String,
    val spanX: Int,
    val spanY: Int,
    val minWidthDp: Int,
    val minHeightDp: Int,
    val previewDrawable: Drawable?,
    val description: String? = null,
    val isConfigurable: Boolean = false
)

/**
 * Data model representing a packed row of 1 or 2 widget cells in Nova's TableLayout (hb.b.e).
 */
data class WidgetTableRow(
    val firstWidget: WidgetModel,
    val secondWidget: WidgetModel? = null
)

/**
 * Helper utility for dynamic application-based widget discovery, span calculation,
 * grouping, and sorting matching Nova Launcher parity.
 */
object WidgetDiscoveryHelper {

    /**
     * Discovers all available widget providers across all user profiles (including work profiles).
     */
    fun queryAllProviders(context: Context): List<AppWidgetProviderInfo> {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return emptyList()
        val providers = mutableListOf<AppWidgetProviderInfo>()

        // 1. Always query standard installed providers for the current profile
        try {
            val defaultProviders = appWidgetManager.installedProviders
            if (!defaultProviders.isNullOrEmpty()) {
                providers.addAll(defaultProviders)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Query installed providers across user profiles (e.g. work profiles)
        try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val profiles = userManager?.userProfiles
            if (!profiles.isNullOrEmpty()) {
                for (profile in profiles) {
                    try {
                        val profileProviders = appWidgetManager.getInstalledProvidersForProfile(profile)
                        if (profileProviders.isNotEmpty()) {
                            providers.addAll(profileProviders)
                        }
                    } catch (e: Exception) {
                        // Profile locked or restricted
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return providers.distinctBy {
            it.provider?.flattenToString() ?: System.identityHashCode(it).toString()
        }
    }

    /**
     * Calculates grid cell spans (columns and rows) from DP dimensions or target cell hints.
     */
    fun calculateCellSpan(
        sizeDp: Int,
        targetCell: Int = 0,
        maxSpan: Int = 4
    ): Int {
        if (targetCell > 0) {
            return targetCell.coerceIn(1, maxOf(1, maxSpan))
        }
        if (sizeDp <= 0) return 1
        // Nova / Android standard formula: ceil((sizeDp + 30) / 70.0)
        val span = Math.ceil((sizeDp + 30) / 70.0).toInt()
        return span.coerceIn(1, maxOf(1, maxSpan))
    }

    /**
     * Packs widgets into rows according to Nova's multi-column TableLayout algorithm (hb.b.e).
     * Widgets with spanX <= 2 are paired side-by-side; widgets with spanX >= 3 occupy a row alone.
     */
    fun packWidgetsIntoRows(widgets: List<WidgetModel>): List<WidgetTableRow> {
        val rows = mutableListOf<WidgetTableRow>()
        var i = 0
        while (i < widgets.size) {
            val current = widgets[i]
            if (current.spanX <= 2 && i + 1 < widgets.size && widgets[i + 1].spanX <= 2) {
                rows.add(WidgetTableRow(firstWidget = current, secondWidget = widgets[i + 1]))
                i += 2
            } else {
                rows.add(WidgetTableRow(firstWidget = current, secondWidget = null))
                i += 1
            }
        }
        return rows
    }

    /**
     * Pure grouping and sorting function that transforms a raw list of AppWidgetProviderInfos
     * into alphabetized application groups with sorted widgets.
     */
    fun groupAndSortProviders(
        providers: List<AppWidgetProviderInfo>,
        appLabelProvider: (String) -> String,
        appIconProvider: (String) -> Drawable?,
        widgetLabelProvider: (AppWidgetProviderInfo) -> String,
        previewProvider: (AppWidgetProviderInfo) -> Drawable?,
        descriptionProvider: (AppWidgetProviderInfo) -> String? = { null },
        packageNameExtractor: (AppWidgetProviderInfo) -> String = { it.provider?.packageName ?: "" },
        classNameExtractor: (AppWidgetProviderInfo) -> String = { it.provider?.className ?: "" },
        gridCols: Int = 4,
        gridRows: Int = 5,
        density: Float = 1.0f
    ): List<AppWidgetGroup> {
        if (providers.isEmpty()) return emptyList()

        val safeDensity = if (density <= 0f) 1.0f else density

        // Deduplicate providers by component name
        val distinctProviders = providers.distinctBy {
            val pkg = packageNameExtractor(it)
            val cls = classNameExtractor(it)
            if (pkg.isNotEmpty() || cls.isNotEmpty()) {
                "$pkg/$cls"
            } else {
                it.provider?.flattenToString() ?: "unknown_${System.identityHashCode(it)}"
            }
        }

        // Group by owning package name
        val groupedByPackage = distinctProviders.groupBy {
            packageNameExtractor(it).ifBlank { it.provider?.packageName ?: "unknown" }
        }
        val resultGroups = mutableListOf<AppWidgetGroup>()

        for ((pkg, providerList) in groupedByPackage) {
            val appLabel = appLabelProvider(pkg).ifBlank { pkg }
            val appIcon = appIconProvider(pkg)

            val widgetModels = providerList.map { provider ->
                val label = widgetLabelProvider(provider).ifBlank { appLabel }

                val minWDp = (provider.minWidth / safeDensity).toInt()
                val minHDp = (provider.minHeight / safeDensity).toInt()

                val targetCellW = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellWidth else 0
                val targetCellH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellHeight else 0

                val spanX = calculateCellSpan(minWDp, targetCellW, gridCols)
                val spanY = calculateCellSpan(minHDp, targetCellH, gridRows)

                val preview = previewProvider(provider)
                val description = descriptionProvider(provider)
                val isConfigurable = provider.configure != null

                WidgetModel(
                    providerInfo = provider,
                    label = label,
                    appName = appLabel,
                    packageName = pkg,
                    spanX = spanX,
                    spanY = spanY,
                    minWidthDp = minWDp,
                    minHeightDp = minHDp,
                    previewDrawable = preview,
                    description = description,
                    isConfigurable = isConfigurable
                )
            }.sortedWith(
                compareBy<WidgetModel> { it.label.lowercase() }
                    .thenBy { it.spanX * it.spanY }
            )

            resultGroups.add(
                AppWidgetGroup(
                    packageName = pkg,
                    appName = appLabel,
                    appIcon = appIcon,
                    widgets = widgetModels,
                    isExpanded = false
                )
            )
        }

        // Sort application groups alphabetically by application name
        return resultGroups.sortedBy { it.appName.lowercase() }
    }

    /**
     * Resolves widget preview drawable with fallback to provider icon or app icon.
     */
    fun loadWidgetPreview(
        context: Context,
        provider: AppWidgetProviderInfo,
        fallbackAppIcon: Drawable?
    ): Drawable? {
        return try {
            provider.loadPreviewImage(context, 0)
                ?: provider.loadIcon(context, 0)
                ?: fallbackAppIcon
        } catch (e: Exception) {
            try {
                provider.loadIcon(context, 0) ?: fallbackAppIcon
            } catch (e2: Exception) {
                fallbackAppIcon
            }
        }
    }

    /**
     * Discovers and builds groups asynchronously using actual Context and PackageManager.
     */
    fun loadGroupsFromSystem(
        context: Context,
        rawProviders: List<AppWidgetProviderInfo>?,
        gridCols: Int = 4,
        gridRows: Int = 5
    ): List<AppWidgetGroup> {
        val providers = rawProviders ?: queryAllProviders(context)
        if (providers.isEmpty()) return emptyList()

        val pm = context.packageManager
        val density = context.resources.displayMetrics.density

        return groupAndSortProviders(
            providers = providers,
            appLabelProvider = { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    if (label.isNotBlank()) label else pkg
                } catch (e: Exception) {
                    pkg
                }
            },
            appIconProvider = { pkg ->
                try {
                    pm.getApplicationIcon(pkg)
                } catch (e: Exception) {
                    null
                }
            },
            widgetLabelProvider = { provider ->
                try {
                    provider.loadLabel(pm)?.toString() ?: ""
                } catch (e: Exception) {
                    ""
                }
            },
            previewProvider = { provider ->
                val appIcon = try { pm.getApplicationIcon(provider.provider.packageName) } catch (e: Exception) { null }
                loadWidgetPreview(context, provider, appIcon)
            },
            descriptionProvider = { provider ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        provider.loadDescription(context)?.toString()
                    } else null
                } catch (e: Exception) {
                    null
                }
            },
            gridCols = gridCols,
            gridRows = gridRows,
            density = density
        ).filter { it.appName.isNotBlank() && it.widgets.isNotEmpty() }
    }
}

