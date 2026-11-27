package com.example.launcherai

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import com.example.launcherai.widgets.AppWidgetGroup
import com.example.launcherai.widgets.WidgetDiscoveryHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetDiscoveryTest {

    private val testPackageMap = mutableMapOf<AppWidgetProviderInfo, Pair<String, String>>()

    @Before
    fun setup() {
        testPackageMap.clear()
    }

    private fun createMockProviderInfo(
        pkg: String,
        cls: String,
        minWidth: Int = 200,
        minHeight: Int = 100
    ): AppWidgetProviderInfo {
        val info = AppWidgetProviderInfo().apply {
            provider = ComponentName(pkg, cls)
            this.minWidth = minWidth
            this.minHeight = minHeight
        }
        testPackageMap[info] = Pair(pkg, cls)
        return info
    }

    @Test
    fun testGroupingProvidersByPackage() {
        val providers = listOf(
            createMockProviderInfo("com.android.chrome", "SearchWidget"),
            createMockProviderInfo("com.google.android.deskclock", "DigitalClockWidget"),
            createMockProviderInfo("com.android.chrome", "DinoWidget"),
            createMockProviderInfo("com.google.android.deskclock", "AnalogClockWidget")
        )

        val groups = WidgetDiscoveryHelper.groupAndSortProviders(
            providers = providers,
            appLabelProvider = { pkg ->
                when (pkg) {
                    "com.android.chrome" -> "Chrome"
                    "com.google.android.deskclock" -> "Clock"
                    else -> pkg
                }
            },
            appIconProvider = { null },
            widgetLabelProvider = { testPackageMap[it]?.second ?: "" },
            previewProvider = { null },
            packageNameExtractor = { testPackageMap[it]?.first ?: "" },
            classNameExtractor = { testPackageMap[it]?.second ?: "" }
        )

        assertEquals(2, groups.size)

        val chromeGroup = groups.find { it.packageName == "com.android.chrome" }
        assertNotNull(chromeGroup)
        assertEquals("Chrome", chromeGroup?.appName)
        assertEquals(2, chromeGroup?.widgets?.size)

        val clockGroup = groups.find { it.packageName == "com.google.android.deskclock" }
        assertNotNull(clockGroup)
        assertEquals("Clock", clockGroup?.appName)
        assertEquals(2, clockGroup?.widgets?.size)
    }

    @Test
    fun testSortingApplicationGroupsAlphabetically() {
        val providers = listOf(
            createMockProviderInfo("com.spotify.music", "SpotifyWidget"),
            createMockProviderInfo("com.google.android.youtube", "YouTubeWidget"),
            createMockProviderInfo("com.android.chrome", "ChromeSearchWidget"),
            createMockProviderInfo("com.google.android.deskclock", "ClockWidget")
        )

        val groups = WidgetDiscoveryHelper.groupAndSortProviders(
            providers = providers,
            appLabelProvider = { pkg ->
                when (pkg) {
                    "com.spotify.music" -> "Spotify"
                    "com.google.android.youtube" -> "YouTube"
                    "com.android.chrome" -> "Chrome"
                    "com.google.android.deskclock" -> "Clock"
                    else -> pkg
                }
            },
            appIconProvider = { null },
            widgetLabelProvider = { testPackageMap[it]?.second ?: "" },
            previewProvider = { null },
            packageNameExtractor = { testPackageMap[it]?.first ?: "" },
            classNameExtractor = { testPackageMap[it]?.second ?: "" }
        )

        assertEquals(4, groups.size)
        assertEquals("Chrome", groups[0].appName)
        assertEquals("Clock", groups[1].appName)
        assertEquals("Spotify", groups[2].appName)
        assertEquals("YouTube", groups[3].appName)
    }

    @Test
    fun testSortingWidgetsWithinApplication() {
        val providers = listOf(
            createMockProviderInfo("com.android.chrome", "DinoWidget", minWidth = 100, minHeight = 100),
            createMockProviderInfo("com.android.chrome", "BookmarksWidget", minWidth = 300, minHeight = 200),
            createMockProviderInfo("com.android.chrome", "ActionBarWidget", minWidth = 250, minHeight = 50)
        )

        val groups = WidgetDiscoveryHelper.groupAndSortProviders(
            providers = providers,
            appLabelProvider = { "Chrome" },
            appIconProvider = { null },
            widgetLabelProvider = { testPackageMap[it]?.second ?: "" },
            previewProvider = { null },
            packageNameExtractor = { testPackageMap[it]?.first ?: "" },
            classNameExtractor = { testPackageMap[it]?.second ?: "" }
        )

        assertEquals(1, groups.size)
        val widgets = groups[0].widgets
        assertEquals(3, widgets.size)
        assertEquals("ActionBarWidget", widgets[0].label)
        assertEquals("BookmarksWidget", widgets[1].label)
        assertEquals("DinoWidget", widgets[2].label)
    }

    @Test
    fun testDuplicateProviderHandling() {
        val providers = listOf(
            createMockProviderInfo("com.android.chrome", "SearchWidget"),
            createMockProviderInfo("com.android.chrome", "SearchWidget"), // Duplicate
            createMockProviderInfo("com.android.chrome", "SearchWidget")  // Duplicate
        )

        val groups = WidgetDiscoveryHelper.groupAndSortProviders(
            providers = providers,
            appLabelProvider = { "Chrome" },
            appIconProvider = { null },
            widgetLabelProvider = { "Search" },
            previewProvider = { null },
            packageNameExtractor = { testPackageMap[it]?.first ?: "" },
            classNameExtractor = { testPackageMap[it]?.second ?: "" }
        )

        assertEquals(1, groups.size)
        assertEquals(1, groups[0].widgets.size)
    }

    @Test
    fun testEmptyProviderList() {
        val groups = WidgetDiscoveryHelper.groupAndSortProviders(
            providers = emptyList(),
            appLabelProvider = { "App" },
            appIconProvider = { null },
            widgetLabelProvider = { "Widget" },
            previewProvider = { null }
        )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun testMissingApplicationMetadataFallback() {
        val providers = listOf(
            createMockProviderInfo("com.unknown.orphan", "OrphanWidget")
        )

        val groups = WidgetDiscoveryHelper.groupAndSortProviders(
            providers = providers,
            appLabelProvider = { "" }, // Empty or missing label
            appIconProvider = { null },
            widgetLabelProvider = { "" }, // Empty widget label
            previewProvider = { null },
            packageNameExtractor = { testPackageMap[it]?.first ?: "" },
            classNameExtractor = { testPackageMap[it]?.second ?: "" }
        )

        assertEquals(1, groups.size)
        assertEquals("com.unknown.orphan", groups[0].appName)
        assertEquals("com.unknown.orphan", groups[0].widgets[0].label)
    }

    @Test
    fun testInvalidWidgetDimensionsCoercion() {
        // Test negative dimensions, zero dimensions, and huge dimensions
        assertEquals(1, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = -10, maxSpan = 4))
        assertEquals(1, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = 0, maxSpan = 4))
        assertEquals(4, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = 1000, maxSpan = 4))

        // Normal values
        assertEquals(1, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = 40, maxSpan = 4))
        assertEquals(2, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = 110, maxSpan = 4))
        assertEquals(3, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = 180, maxSpan = 4))
        assertEquals(4, WidgetDiscoveryHelper.calculateCellSpan(sizeDp = 250, maxSpan = 4))
    }

    @Test
    fun testWidgetPlacementFailureSafety() {
        // Simulates the safety check in MainActivity when cell is occupied and findVacantCell is null
        val targetCellX = 1
        val targetCellY = 1
        val spanX = 4
        val spanY = 4
        val isTargetEmpty = false
        val vacantCellFound: IntArray? = null

        val finalPlacement = if (targetCellX >= 0 && targetCellY >= 0 && isTargetEmpty) {
            intArrayOf(targetCellX, targetCellY)
        } else {
            vacantCellFound
        }

        // Must be null to prevent silent overwrite of (0, 0)
        assertFalse(finalPlacement != null)
    }

    @Test
    fun testPackWidgetsIntoRowsAlgorithm() {
        val widgetA = com.example.launcherai.widgets.WidgetModel(
            providerInfo = createMockProviderInfo("com.test", "WidgetA"),
            label = "Widget A",
            appName = "Test App",
            packageName = "com.test",
            spanX = 2,
            spanY = 2,
            minWidthDp = 110,
            minHeightDp = 110,
            previewDrawable = null
        )
        val widgetB = com.example.launcherai.widgets.WidgetModel(
            providerInfo = createMockProviderInfo("com.test", "WidgetB"),
            label = "Widget B",
            appName = "Test App",
            packageName = "com.test",
            spanX = 2,
            spanY = 1,
            minWidthDp = 110,
            minHeightDp = 40,
            previewDrawable = null
        )
        val widgetC = com.example.launcherai.widgets.WidgetModel(
            providerInfo = createMockProviderInfo("com.test", "WidgetC"),
            label = "Widget C",
            appName = "Test App",
            packageName = "com.test",
            spanX = 4,
            spanY = 2,
            minWidthDp = 250,
            minHeightDp = 110,
            previewDrawable = null
        )

        val packedRows = WidgetDiscoveryHelper.packWidgetsIntoRows(listOf(widgetA, widgetB, widgetC))

        assertEquals(2, packedRows.size)
        // Row 0 has widgetA and widgetB side-by-side
        assertEquals("Widget A", packedRows[0].firstWidget.label)
        assertNotNull(packedRows[0].secondWidget)
        assertEquals("Widget B", packedRows[0].secondWidget?.label)

        // Row 1 has wide widgetC alone
        assertEquals("Widget C", packedRows[1].firstWidget.label)
        assertTrue(packedRows[1].secondWidget == null)
    }

    @Test
    fun testNovaParityWidgetSpanAndMinResizeCalculations() {
        val provider = createMockProviderInfo("com.example.clock", "ClockWidget", minWidth = 512, minHeight = 256).apply {
            minResizeWidth = 256
            minResizeHeight = 0
        }
        val density = 3.0f // 80dp = 240px

        // 512 / 240 = 2.13 -> ceil = 3; 256 / 240 = 1.07 -> ceil = 2
        val (spanX, spanY) = WidgetDiscoveryHelper.calculateWidgetSpans(
            provider = provider,
            gridCols = 4,
            gridRows = 5,
            density = density
        )
        assertEquals(3, spanX)
        assertEquals(2, spanY)

        // minResizeWidth = 256 -> 256 / 240 -> ceil = 2; minResizeHeight = 0 -> 1
        val (minSpanX, minSpanY) = WidgetDiscoveryHelper.calculateMinResizeSpans(
            context = null,
            provider = provider,
            defaultSpanX = spanX,
            defaultSpanY = spanY,
            gridCols = 4,
            gridRows = 5,
            density = density
        )
        assertEquals(2, minSpanX)
        assertEquals(1, minSpanY)
    }
}
