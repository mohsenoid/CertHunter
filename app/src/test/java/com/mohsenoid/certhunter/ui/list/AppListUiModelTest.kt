package com.mohsenoid.certhunter.ui.list

import com.mohsenoid.certhunter.domain.model.AppItem
import com.mohsenoid.certhunter.domain.model.AppSortOrder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppListUiModelTest {

    private val appA = AppItem("App A", "com.a", isSystemApp = false, firstInstallTime = 3000L)
    private val appB = AppItem("App B", "com.b", isSystemApp = false, firstInstallTime = 1000L)
    private val sysC = AppItem("App C", "com.c", isSystemApp = true, firstInstallTime = 2000L)

    private fun modelWith(
        apps: List<AppItem> = listOf(appA, appB, sysC),
        query: String = "",
        showSystemApps: Boolean = true,
        sortOrder: AppSortOrder = AppSortOrder.NameAscending,
    ) = AppListUiModel(
        allApps = apps,
        isLoadingApps = false,
        searchQuery = query,
        showSystemApps = showSystemApps,
        sortOrder = sortOrder,
    )

    @Test
    fun `empty query with system apps shows all apps sorted by name`() {
        val filtered = modelWith().filteredApps
        assertEquals(listOf(appA, appB, sysC), filtered)
    }

    @Test
    fun `hiding system apps excludes system apps`() {
        val filtered = modelWith(showSystemApps = false).filteredApps
        assertEquals(listOf(appA, appB), filtered)
    }

    @Test
    fun `query filters by app name case-insensitively`() {
        val filtered = modelWith(query = "app a").filteredApps
        assertEquals(listOf(appA), filtered)
    }

    @Test
    fun `query filters by package name`() {
        val filtered = modelWith(query = "com.b").filteredApps
        assertEquals(listOf(appB), filtered)
    }

    @Test
    fun `blank query returns all apps`() {
        val filtered = modelWith(query = "   ").filteredApps
        assertEquals(3, filtered.size)
    }

    @Test
    fun `no matching query returns empty list`() {
        val filtered = modelWith(query = "zzznomatch").filteredApps
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `sort by name ascending orders a to z`() {
        val apps = listOf(appB, sysC, appA)
        val filtered = modelWith(apps = apps, sortOrder = AppSortOrder.NameAscending).filteredApps
        assertEquals(listOf(appA, appB, sysC), filtered)
    }

    @Test
    fun `sort by name descending orders z to a`() {
        val apps = listOf(appA, appB, sysC)
        val filtered = modelWith(apps = apps, sortOrder = AppSortOrder.NameDescending).filteredApps
        assertEquals(listOf(sysC, appB, appA), filtered)
    }

    @Test
    fun `sort by install date newest first`() {
        val filtered = modelWith(sortOrder = AppSortOrder.InstallDateNewest).filteredApps
        // appA=3000, sysC=2000, appB=1000
        assertEquals(listOf(appA, sysC, appB), filtered)
    }

    @Test
    fun `sort by install date oldest first`() {
        val filtered = modelWith(sortOrder = AppSortOrder.InstallDateOldest).filteredApps
        // appB=1000, sysC=2000, appA=3000
        assertEquals(listOf(appB, sysC, appA), filtered)
    }

    @Test
    fun `system app filter combined with search query`() {
        val filtered = modelWith(query = "App", showSystemApps = false).filteredApps
        assertEquals(listOf(appA, appB), filtered)
    }
}
