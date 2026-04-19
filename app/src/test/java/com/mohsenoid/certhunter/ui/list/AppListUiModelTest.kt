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
    fun `given no query and system apps visible when filteredApps then all apps returned sorted by name`() {
        // given / when
        val filtered = modelWith().filteredApps

        // then
        assertEquals(listOf(appA, appB, sysC), filtered)
    }

    @Test
    fun `given system apps hidden when filteredApps then only user apps returned`() {
        // given / when
        val filtered = modelWith(showSystemApps = false).filteredApps

        // then
        assertEquals(listOf(appA, appB), filtered)
    }

    @Test
    fun `given query matching app name when filteredApps then only matching apps returned`() {
        // given / when
        val filtered = modelWith(query = "app a").filteredApps

        // then
        assertEquals(listOf(appA), filtered)
    }

    @Test
    fun `given query matching package name when filteredApps then matching app returned`() {
        // given / when
        val filtered = modelWith(query = "com.b").filteredApps

        // then
        assertEquals(listOf(appB), filtered)
    }

    @Test
    fun `given blank query when filteredApps then all apps returned`() {
        // given / when
        val filtered = modelWith(query = "   ").filteredApps

        // then
        assertEquals(3, filtered.size)
    }

    @Test
    fun `given query with no match when filteredApps then empty list returned`() {
        // given / when
        val filtered = modelWith(query = "zzznomatch").filteredApps

        // then
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `given name ascending sort when filteredApps then apps ordered A to Z`() {
        // given
        val apps = listOf(appB, sysC, appA)

        // when
        val filtered = modelWith(apps = apps, sortOrder = AppSortOrder.NameAscending).filteredApps

        // then
        assertEquals(listOf(appA, appB, sysC), filtered)
    }

    @Test
    fun `given name descending sort when filteredApps then apps ordered Z to A`() {
        // given
        val apps = listOf(appA, appB, sysC)

        // when
        val filtered = modelWith(apps = apps, sortOrder = AppSortOrder.NameDescending).filteredApps

        // then
        assertEquals(listOf(sysC, appB, appA), filtered)
    }

    @Test
    fun `given install date newest sort when filteredApps then most recently installed first`() {
        // given / when — appA=3000, sysC=2000, appB=1000
        val filtered = modelWith(sortOrder = AppSortOrder.InstallDateNewest).filteredApps

        // then
        assertEquals(listOf(appA, sysC, appB), filtered)
    }

    @Test
    fun `given install date oldest sort when filteredApps then oldest installed first`() {
        // given / when — appB=1000, sysC=2000, appA=3000
        val filtered = modelWith(sortOrder = AppSortOrder.InstallDateOldest).filteredApps

        // then
        assertEquals(listOf(appB, sysC, appA), filtered)
    }

    @Test
    fun `given query and system apps hidden when filteredApps then only matching user apps returned`() {
        // given / when
        val filtered = modelWith(query = "App", showSystemApps = false).filteredApps

        // then
        assertEquals(listOf(appA, appB), filtered)
    }

    @Test
    fun `given empty app list when filteredApps then empty list returned regardless of filters`() {
        // given / when
        val filtered = modelWith(
            apps = emptyList(),
            query = "anything",
            showSystemApps = false,
            sortOrder = AppSortOrder.InstallDateNewest,
        ).filteredApps

        // then
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `given query and system apps hidden and install date sort when filteredApps then matching user apps returned newest first`() {
        // given — appA(3000ms), appB(1000ms) match "App", sysC is excluded
        // when
        val filtered = modelWith(
            query = "App",
            showSystemApps = false,
            sortOrder = AppSortOrder.InstallDateNewest,
        ).filteredApps

        // then — appA installed most recently
        assertEquals(listOf(appA, appB), filtered)
    }
}
