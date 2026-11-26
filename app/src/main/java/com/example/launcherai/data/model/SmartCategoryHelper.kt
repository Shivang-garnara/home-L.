package com.example.launcherai.data.model

import com.example.launcherai.data.db.WorkspaceItemEntity
import java.util.Locale

/**
 * Built-in default categories matching Nova Launcher 8.9.9 (e40.b Smart Folders + presets),
 * offline package/engine classification, and pure planning helpers for auto-organizing folders.
 */
enum class SmartFolderCategory(
    val id: String,
    val defaultTitle: String,
    val isNovaAutoSmart: Boolean,
    val aliases: Set<String> = emptySet()
) {
    ENTERTAINMENT("ENTERTAINMENT", AppInfo.CATEGORY_ENTERTAINMENT, true, setOf("Media")),
    SOCIAL("SOCIAL", AppInfo.CATEGORY_SOCIAL, true, setOf(AppInfo.CATEGORY_COMMUNICATION)),
    GAMES("GAMES", AppInfo.CATEGORY_GAMES, true),
    FINANCE("FINANCE", AppInfo.CATEGORY_FINANCE, true),
    SHOPPING("SHOPPING", AppInfo.CATEGORY_SHOPPING, true),
    FOOD("FOOD", AppInfo.CATEGORY_FOOD, true),
    TRAVEL("TRAVEL", AppInfo.CATEGORY_TRAVEL, true),
    PRODUCTIVITY("PRODUCTIVITY", AppInfo.CATEGORY_PRODUCTIVITY, false, setOf("Work")),
    PHOTOGRAPHY("PHOTOGRAPHY", AppInfo.CATEGORY_PHOTOGRAPHY, false),
    TOOLS("TOOLS", AppInfo.CATEGORY_TOOLS, false);

    companion object {
        val NOVA_AUTO_CATEGORIES: List<SmartFolderCategory> = entries.filter { it.isNovaAutoSmart }
        val PRESET_CATEGORIES: List<SmartFolderCategory> = entries

        fun fromIdOrName(value: String?): SmartFolderCategory? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.trim()
            return entries.find { cat ->
                cat.id.equals(trimmed, ignoreCase = true) ||
                    cat.defaultTitle.equals(trimmed, ignoreCase = true) ||
                    cat.aliases.any { it.equals(trimmed, ignoreCase = true) }
            }
        }
    }
}

data class SmartCategorySuggestion(
    val category: SmartFolderCategory,
    val matchingApps: List<AppInfo>,
    val existingDisabledFolderId: Long? = null
)

data class SmartFolderSyncPlan(
    val foldersToCreate: List<Pair<SmartFolderCategory, List<AppInfo>>> = emptyList(),
    val folderIdsToEnable: List<Long> = emptyList(),
    val folderIdsToDisable: List<Long> = emptyList(),
    val appsToAddByFolderId: Map<Long, List<AppInfo>> = emptyMap(),
    val markInitialSyncDone: Boolean = false
)

object SmartCategoryHelper {

    const val CATEGORY_PACKAGE_PREFIX = "smart_category:"

    // Bitmask stored in WorkspaceItemEntity.cellY for CONTAINER_DRAWER folders (mirroring Nova xy/s flags)
    const val FLAG_SMART_DISABLED = 128
    const val FLAG_SMART_USER_DISMISSED = 256
    const val FLAG_AUTO_ADD_NEW_APPS = 512
    const val FLAG_SMART_AUTO_CREATED = 1024

    // Nova initial Smart Folder auto-enable & suggestion chip threshold (ha.a0.b & ShortcutChipsRowView.j)
    const val SMART_AUTO_ENABLE_MIN_APPS = 3

    // Standard Android ApplicationInfo.category integer constants (API 26+)
    private const val ANDROID_CATEGORY_UNDEFINED = -1
    private const val ANDROID_CATEGORY_GAME = 0
    private const val ANDROID_CATEGORY_AUDIO = 1
    private const val ANDROID_CATEGORY_VIDEO = 2
    private const val ANDROID_CATEGORY_IMAGE = 3
    private const val ANDROID_CATEGORY_SOCIAL = 4
    private const val ANDROID_CATEGORY_MAPS = 5
    private const val ANDROID_CATEGORY_NEWS = 6
    private const val ANDROID_CATEGORY_PRODUCTIVITY = 7
    private const val ANDROID_CATEGORY_ACCESSIBILITY = 8

    fun encodeCategoryPackageName(categoryId: String): String =
        "$CATEGORY_PACKAGE_PREFIX${categoryId.trim().uppercase(Locale.ROOT)}"

    fun decodeCategoryId(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        if (!packageName.startsWith(CATEGORY_PACKAGE_PREFIX)) return null
        return packageName.removePrefix(CATEGORY_PACKAGE_PREFIX).trim().takeIf { it.isNotEmpty() }
    }

    fun decodeCategory(packageName: String?): SmartFolderCategory? =
        SmartFolderCategory.fromIdOrName(decodeCategoryId(packageName))

    fun isFolderDisabled(entity: WorkspaceItemEntity): Boolean =
        entity.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
            (entity.cellY and FLAG_SMART_DISABLED) != 0

    fun isSmartAutoCreated(entity: WorkspaceItemEntity): Boolean =
        entity.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
            (entity.cellY and FLAG_SMART_AUTO_CREATED) != 0

    fun isAutoAddEnabled(entity: WorkspaceItemEntity): Boolean {
        if (decodeCategoryId(entity.packageName) == null) return false
        if (isFolderDisabled(entity)) return false
        return (entity.cellY and (FLAG_AUTO_ADD_NEW_APPS or FLAG_SMART_AUTO_CREATED)) != 0
    }

    fun excludedAppKey(categoryId: String, packageName: String, activityName: String?): String =
        "${categoryId.uppercase(Locale.ROOT)}|${DrawerFolderHelper.componentKey(packageName, activityName)}"

    /**
     * Classifies an application using:
     * 1) Curated high-confidence package overrides (Finance, Food, Shopping, Travel, Games)
     * 2) Android ApplicationInfo.category (API 26+)
     * 3) Nova's game-engine heuristics (Unity, Unreal, Cocos2d-x, RenPy, Corona, Gameloft, NativeActivity)
     * 4) Broad offline package/label keyword rules across all 10 built-in categories
     */
    fun classifySmartCategory(
        appCategoryInt: Int,
        packageName: String,
        activityName: String = "",
        label: String = ""
    ): SmartFolderCategory? {
        val pkgLower = packageName.lowercase(Locale.ROOT)
        val actLower = activityName.lowercase(Locale.ROOT)
        val lblLower = label.lowercase(Locale.ROOT)

        // Avoid misclassifying launchers or Play Store itself as generic "shop"/"play games"
        if (pkgLower == "com.android.vending") {
            return SmartFolderCategory.TOOLS
        }
        if (pkgLower.contains("launcher") && !pkgLower.contains("game")) {
            return null
        }

        // 1. High-precision vertical package matching (covers categories Android ApplicationInfo doesn't split
        //    or bundles under CATEGORY_SOCIAL such as browsers and email clients)
        when {
            matchesFinanceKeywords(pkgLower, lblLower) -> return SmartFolderCategory.FINANCE
            matchesFoodKeywords(pkgLower, lblLower) -> return SmartFolderCategory.FOOD
            matchesShoppingKeywords(pkgLower, lblLower) -> return SmartFolderCategory.SHOPPING
            matchesTravelKeywords(pkgLower, lblLower) -> return SmartFolderCategory.TRAVEL
            matchesBrowserOrUtilityOverride(pkgLower) -> return SmartFolderCategory.TOOLS
            matchesProductivityOverride(pkgLower) -> return SmartFolderCategory.PRODUCTIVITY
        }

        // 2. Nova e40/d.java game-engine fallback checked early so Unity/Unreal engines always classify as Games
        if (matchesNovaGameEngineHeuristics(packageName, activityName)) {
            return SmartFolderCategory.GAMES
        }

        // 3. Official Android API 26+ ApplicationInfo.category mapping
        if (appCategoryInt != ANDROID_CATEGORY_UNDEFINED) {
            when (appCategoryInt) {
                ANDROID_CATEGORY_GAME -> return SmartFolderCategory.GAMES
                ANDROID_CATEGORY_AUDIO,
                ANDROID_CATEGORY_VIDEO,
                ANDROID_CATEGORY_NEWS -> return SmartFolderCategory.ENTERTAINMENT
                ANDROID_CATEGORY_IMAGE -> return SmartFolderCategory.PHOTOGRAPHY
                ANDROID_CATEGORY_SOCIAL -> return SmartFolderCategory.SOCIAL
                ANDROID_CATEGORY_MAPS -> return SmartFolderCategory.TRAVEL
                ANDROID_CATEGORY_PRODUCTIVITY -> return SmartFolderCategory.PRODUCTIVITY
                ANDROID_CATEGORY_ACCESSIBILITY -> return SmartFolderCategory.TOOLS
            }
        }

        // 4. Broad offline keyword rules
        return when {
            matchesGamesKeywords(pkgLower, actLower, lblLower) -> SmartFolderCategory.GAMES
            matchesEntertainmentKeywords(pkgLower, lblLower) -> SmartFolderCategory.ENTERTAINMENT
            matchesSocialKeywords(pkgLower, lblLower) -> SmartFolderCategory.SOCIAL
            matchesPhotographyKeywords(pkgLower, lblLower) -> SmartFolderCategory.PHOTOGRAPHY
            matchesProductivityKeywords(pkgLower, lblLower) -> SmartFolderCategory.PRODUCTIVITY
            matchesToolsKeywords(pkgLower, lblLower) -> SmartFolderCategory.TOOLS
            else -> null
        }
    }

    fun resolveAppCategoryName(
        appCategoryInt: Int,
        packageName: String,
        activityName: String = "",
        label: String = ""
    ): String = classifySmartCategory(appCategoryInt, packageName, activityName, label)?.defaultTitle
        ?: AppInfo.CATEGORY_OTHER

    /**
     * Checks whether [app] belongs to [categoryIdOrName], honoring AppInfo.category, aliases,
     * and offline keyword fallback so filters and preset pickers never disagree.
     */
    fun matchesCategory(app: AppInfo, categoryIdOrName: String): Boolean {
        if (categoryIdOrName.equals(AppInfo.CATEGORY_ALL, ignoreCase = true)) return true
        val targetCat = SmartFolderCategory.fromIdOrName(categoryIdOrName)
        if (targetCat == null) {
            return app.category.equals(categoryIdOrName, ignoreCase = true)
        }
        val appCat = SmartFolderCategory.fromIdOrName(app.category)
        if (appCat == targetCat) return true

        val inferred = classifySmartCategory(ANDROID_CATEGORY_UNDEFINED, app.packageName, app.activityName, app.label)
        return inferred == targetCat
    }

    /**
     * Picks a sensible default folder name when two Home Screen / Dock items are merged into a folder.
     * Uses their shared category name (e.g., "Games", "Entertainment", "Social") if both match, or "Folder".
     */
    fun inferFolderTitleForApps(first: AppInfo?, second: AppInfo?): String {
        if (first == null && second == null) return "Folder"
        val catA = first?.let {
            SmartFolderCategory.fromIdOrName(it.category)
                ?: classifySmartCategory(ANDROID_CATEGORY_UNDEFINED, it.packageName, it.activityName, it.label)
        }
        val catB = second?.let {
            SmartFolderCategory.fromIdOrName(it.category)
                ?: classifySmartCategory(ANDROID_CATEGORY_UNDEFINED, it.packageName, it.activityName, it.label)
        }
        return if (catA != null && catA == catB) catA.defaultTitle else "Folder"
    }

    /**
     * Ranks default category suggestions for an open folder based on the apps inside it first,
     * followed by the remaining built-in default presets.
     */
    fun getQuickRenameSuggestions(folderApps: List<AppInfo>, maxSuggestions: Int = 8): List<String> {
        val counts = LinkedHashMap<SmartFolderCategory, Int>()
        for (app in folderApps) {
            val cat = SmartFolderCategory.fromIdOrName(app.category)
                ?: classifySmartCategory(ANDROID_CATEGORY_UNDEFINED, app.packageName, app.activityName, app.label)
            if (cat != null) {
                counts[cat] = (counts[cat] ?: 0) + 1
            }
        }
        val ranked = counts.entries
            .sortedByDescending { it.value }
            .map { it.key.defaultTitle }
        val remaining = SmartFolderCategory.PRESET_CATEGORIES
            .map { it.defaultTitle }
            .filter { it !in ranked }
        return (ranked + remaining).take(maxSuggestions)
    }

    /**
     * Computes what Smart Folders should be created, enabled, disabled, or incrementally updated.
     */
    fun planSmartFolderSync(
        installedApps: List<AppInfo>,
        allWorkspaceItems: List<WorkspaceItemEntity>,
        smartFoldersEnabled: Boolean,
        didInitialLoad: Boolean,
        dismissedCategoryIds: Set<String>,
        excludedKeys: Set<String> = emptySet(),
        hiddenPackages: Set<String> = emptySet()
    ): SmartFolderSyncPlan {
        val nonHiddenApps = installedApps.filter { !it.isFolder && it.packageName !in hiddenPackages }
        val drawerFolders = allWorkspaceItems.filter {
            it.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
                it.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER
        }

        if (!smartFoldersEnabled) {
            val toDisable = drawerFolders
                .filter { isSmartAutoCreated(it) && !isFolderDisabled(it) }
                .map { it.id }
            return SmartFolderSyncPlan(folderIdsToDisable = toDisable)
        }

        val activeFolders = drawerFolders.filter { !isFolderDisabled(it) }
        val folderIdsToEnable = drawerFolders
            .filter { entity ->
                val cat = decodeCategory(entity.packageName)
                cat != null &&
                    isSmartAutoCreated(entity) &&
                    isFolderDisabled(entity) &&
                    cat.id !in dismissedCategoryIds
            }
            .map { it.id }

        val effectiveActiveFolders = drawerFolders.filter {
            !isFolderDisabled(it) || it.id in folderIdsToEnable
        }
        val effectiveActiveIds = effectiveActiveFolders.map { it.id.toInt() }.toHashSet()

        // ALREADY_PROCESSED: every app currently assigned inside any active drawer folder
        val alreadyPlacedKeys = HashSet<String>()
        val folderExistingKeys = HashMap<Long, MutableSet<String>>()
        for (item in allWorkspaceItems) {
            if (item.itemType == WorkspaceItemEntity.ITEM_TYPE_APP && item.container in effectiveActiveIds) {
                val pkg = item.packageName ?: continue
                val key = DrawerFolderHelper.componentKey(pkg, item.className)
                alreadyPlacedKeys.add(key)
                folderExistingKeys.getOrPut(item.container.toLong()) { HashSet() }.add(key)
            }
        }

        val activeCategoryIds = effectiveActiveFolders.mapNotNull { decodeCategory(it.packageName)?.id }.toHashSet()
        val appsToAddByFolderId = LinkedHashMap<Long, MutableList<AppInfo>>()

        // 1. Incrementally route ungrouped matching apps into active category-bound folders
        for (folder in effectiveActiveFolders) {
            if (!isAutoAddEnabled(folder) && folder.id !in folderIdsToEnable) continue
            val cat = decodeCategory(folder.packageName) ?: continue
            val existingForFolder = folderExistingKeys[folder.id] ?: emptySet()
            for (app in nonHiddenApps) {
                val key = DrawerFolderHelper.componentKey(app.packageName, app.activityName)
                if (key in existingForFolder || key in alreadyPlacedKeys) continue
                if (excludedAppKey(cat.id, app.packageName, app.activityName) in excludedKeys) continue
                if (matchesCategory(app, cat.id)) {
                    appsToAddByFolderId.getOrPut(folder.id) { mutableListOf() }.add(app)
                    alreadyPlacedKeys.add(key)
                }
            }
        }

        // 2. Auto-create smart category folders automatically every time for any unorganized category apps
        val foldersToCreate = mutableListOf<Pair<SmartFolderCategory, List<AppInfo>>>()
        for (cat in SmartFolderCategory.PRESET_CATEGORIES) {
            if (cat.id in activeCategoryIds || cat.id in dismissedCategoryIds) continue
            val candidates = nonHiddenApps.filter { app ->
                val key = DrawerFolderHelper.componentKey(app.packageName, app.activityName)
                key !in alreadyPlacedKeys &&
                    excludedAppKey(cat.id, app.packageName, app.activityName) !in excludedKeys &&
                    matchesCategory(app, cat.id)
            }
            if (candidates.isNotEmpty()) {
                foldersToCreate.add(cat to candidates)
                candidates.forEach {
                    alreadyPlacedKeys.add(DrawerFolderHelper.componentKey(it.packageName, it.activityName))
                }
            }
        }

        return SmartFolderSyncPlan(
            foldersToCreate = foldersToCreate,
            folderIdsToEnable = folderIdsToEnable,
            appsToAddByFolderId = appsToAddByFolderId,
            markInitialSyncDone = !didInitialLoad
        )
    }

    /**
     * Returns Nova-style "Add '<Category>' Folder" chip candidates when an uncreated default category
     * currently has >= 3 ungrouped installed apps and hasn't been dismissed.
     */
    fun getSuggestionCandidates(
        installedApps: List<AppInfo>,
        allWorkspaceItems: List<WorkspaceItemEntity>,
        smartFoldersEnabled: Boolean,
        dismissedCategoryIds: Set<String>,
        excludedKeys: Set<String> = emptySet(),
        hiddenPackages: Set<String> = emptySet(),
        searchQuery: String = ""
    ): List<SmartCategorySuggestion> {
        if (!smartFoldersEnabled) return emptyList()

        val nonHiddenApps = installedApps.filter { !it.isFolder && it.packageName !in hiddenPackages }
        val drawerFolders = allWorkspaceItems.filter {
            it.container == WorkspaceItemEntity.CONTAINER_DRAWER &&
                it.itemType == WorkspaceItemEntity.ITEM_TYPE_FOLDER
        }
        val activeFolderIds = drawerFolders.filter { !isFolderDisabled(it) }.map { it.id.toInt() }.toHashSet()
        val alreadyPlacedKeys = allWorkspaceItems
            .filter { it.itemType == WorkspaceItemEntity.ITEM_TYPE_APP && it.container in activeFolderIds }
            .mapNotNull { item ->
                val pkg = item.packageName ?: return@mapNotNull null
                DrawerFolderHelper.componentKey(pkg, item.className)
            }
            .toHashSet()

        val activeCategoryIds = drawerFolders
            .filter { !isFolderDisabled(it) }
            .mapNotNull { decodeCategory(it.packageName)?.id }
            .toHashSet()

        val disabledFolderByCatId = drawerFolders
            .filter { isFolderDisabled(it) }
            .mapNotNull { entity ->
                decodeCategory(entity.packageName)?.id?.let { it to entity.id }
            }
            .toMap()

        val trimmedQuery = searchQuery.trim().lowercase(Locale.ROOT)
        val suggestions = mutableListOf<SmartCategorySuggestion>()

        for (cat in SmartFolderCategory.NOVA_AUTO_CATEGORIES) {
            if (cat.id in activeCategoryIds || cat.id in dismissedCategoryIds) continue
            val matching = nonHiddenApps.filter { app ->
                val key = DrawerFolderHelper.componentKey(app.packageName, app.activityName)
                key !in alreadyPlacedKeys &&
                    excludedAppKey(cat.id, app.packageName, app.activityName) !in excludedKeys &&
                    matchesCategory(app, cat.id)
            }
            if (matching.size >= SMART_AUTO_ENABLE_MIN_APPS) {
                val matchesQuery = trimmedQuery.isEmpty() ||
                    cat.defaultTitle.lowercase(Locale.ROOT).contains(trimmedQuery) ||
                    matching.any { it.label.lowercase(Locale.ROOT).contains(trimmedQuery) }
                if (matchesQuery) {
                    suggestions.add(
                        SmartCategorySuggestion(
                            category = cat,
                            matchingApps = matching,
                            existingDisabledFolderId = disabledFolderByCatId[cat.id]
                        )
                    )
                }
            }
        }
        return suggestions
    }

    private fun matchesNovaGameEngineHeuristics(packageName: String, activityName: String): Boolean {
        if (activityName.contains("Unity")) return true
        if (activityName == "android.app.NativeActivity") return true
        if (
            activityName.startsWith("com.apportable.") ||
            activityName.startsWith("com.epicgames.") ||
            activityName.startsWith("org.cocos2dx.") ||
            activityName.startsWith("org.renpy.") ||
            activityName.startsWith("com.ansca.corona.") ||
            activityName.startsWith("com.toast.gamebase.") ||
            activityName.startsWith("com.gamesalad.")
        ) {
            return true
        }
        return packageName.endsWith(".humble") || packageName.startsWith("com.gameloft.")
    }

    private fun matchesFinanceKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "paytm", "phonepe", "paisa", "gpay", "google.pay", "paypal", "venmo", "cashapp",
            "robinhood", "coinbase", "binance", "zerodha", "kite", "groww", "upstox", "cred",
            "splitwise", "mint", "chase", "sbi", "hdfc", "icici", "kotak", "axisbank", "yono",
            "bhim", "finance", "banking", "wallet", "expense", "loan", "budget"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }

    private fun matchesBrowserOrUtilityOverride(pkg: String): Boolean {
        val tokens = listOf(
            "android.chrome", "brave.browser", "firefox", "opera", "microsoft.emmx",
            "duckduckgo", "kiwibrowser", "vivaldi", "browser", "android.settings",
            "deskclock", "calculator"
        )
        return tokens.any { pkg.contains(it) }
    }

    private fun matchesProductivityOverride(pkg: String): Boolean {
        val tokens = listOf(
            "google.android.gm", "microsoft.office", "microsoft.word", "microsoft.excel",
            "apps.docs", "sheets", "slides", "google.android.apps.docs", "google.android.keep",
            "google.android.calendar", "notion", "slack", "trello", "todoist", "evernote", "outlook"
        )
        return tokens.any { pkg.contains(it) }
    }

    private fun matchesFoodKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "zomato", "swiggy", "ubereats", "doordash", "grubhub", "dominos", "pizzahut",
            "mcdonalds", "kfc", "starbucks", "blinkit", "zepto", "bigbasket", "instacart",
            "foodpanda", "dunkin", "yummly", "recipe"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }

    private fun matchesShoppingKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "amazon.mshop", "amazon.in", "amazon.shopping", "flipkart", "myntra", "ajio",
            "meesho", "nykaa", "ebay", "walmart", "target", "etsy", "shein", "aliexpress",
            "temu", "shopify", "tatacliq", "snapdeal", "olx"
        )
        return tokens.any { pkg.contains(it) } ||
            lbl.contains("amazon") && !pkg.contains("avod") && !pkg.contains("music") ||
            lbl.contains("flipkart") || lbl.contains("shopping")
    }

    private fun matchesTravelKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "apps.maps", "uber", "lyft", "olacabs", "rapido", "airbnb", "booking", "expedia",
            "makemytrip", "goibibo", "irctc", "redbus", "yatra", "tripadvisor", "skyscanner",
            "waze", "moovit", "transit", "google.earth", "streetview", "flightradar"
        )
        return tokens.any { pkg.contains(it) } || lbl == "maps" || lbl.contains("travel")
    }

    private fun matchesGamesKeywords(pkg: String, act: String, lbl: String): Boolean {
        return pkg.contains("game") ||
            pkg.contains("play.games") ||
            pkg.contains("supercell") ||
            pkg.contains("rovio") ||
            pkg.contains("miniclip") ||
            pkg.contains("pubg") ||
            pkg.contains("roblox") ||
            pkg.contains("mojang") ||
            pkg.contains("minecraft") ||
            pkg.contains("chess") ||
            pkg.contains("ludo") ||
            pkg.contains("sudoku") ||
            pkg.contains("solitaire") ||
            act.contains("game") ||
            lbl.contains("game")
    }

    private fun matchesEntertainmentKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "youtube", "music", "video", "spotify", "netflix", "avod", "primevideo",
            "disney", "hotstar", "jiocinema", "sonyliv", "zee5", "vlc", "mxtech",
            "mediacenter", "player", "vidmate", "fun.app", "podcast", "audible",
            "twitch", "magazines", "news", "imdb", "soundcloud", "gaana", "wynk",
            "radio", "media"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }

    private fun matchesSocialKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "whatsapp", "telegram", "facebook", "katana", "orca", "instagram", "twitter",
            "snapchat", "discord", "viber", "skype", "linkedin", "reddit", "pinterest",
            "tiktok", "musically", "barcelona", "threads", "signal", "tachyon",
            "truecaller", "messaging", "mms", "social", "chat", "messenger", "meet"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }

    private fun matchesPhotographyKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "camera", "photos", "gallery", "snapseed", "lightroom", "vsco",
            "picsart", "canva", "remini", "inshot", "capcut"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }

    private fun matchesProductivityKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "google.android.gm", "gmail", "outlook", "apps.docs", "sheets", "slides",
            "drive", "office", "microsoft.word", "microsoft.excel", "notion", "trello",
            "slack", "teams", "zoom", "calendar", "keep", "evernote", "todoist",
            "acrobat", "pdf", "scanner", "notes"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }

    private fun matchesToolsKeywords(pkg: String, lbl: String): Boolean {
        val tokens = listOf(
            "calculator", "deskclock", "clock", "settings", "documentsui", "filemanager",
            "chrome", "brave", "firefox", "opera", "browser", "soundrecorder",
            "recorder", "translate", "authenticator", "bitwarden", "1password", "compass"
        )
        return tokens.any { pkg.contains(it) || lbl.contains(it) }
    }
}
