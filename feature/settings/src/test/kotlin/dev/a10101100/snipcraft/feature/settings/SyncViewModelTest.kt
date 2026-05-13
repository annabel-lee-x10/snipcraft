package dev.a10101100.snipcraft.feature.settings

import dev.a10101100.snipcraft.core.sync.SyncConfig
import dev.a10101100.snipcraft.core.sync.SyncConfigStore
import dev.a10101100.snipcraft.core.sync.SyncEngine
import dev.a10101100.snipcraft.core.sync.SyncResult
import dev.a10101100.snipcraft.core.sync.WebDavClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockConfigStore = mockk<SyncConfigStore>(relaxed = true)
    private val mockSyncEngine  = mockk<SyncEngine>(relaxed = true)
    private val mockWebDavClient = mockk<WebDavClient>(relaxed = true)

    @BeforeEach fun setUp()    { Dispatchers.setMain(testDispatcher) }
    @AfterEach  fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): SyncViewModel {
        every { mockConfigStore.load() } returns null
        return SyncViewModel(mockConfigStore, mockSyncEngine, mockWebDavClient)
    }

    @Test
    fun `initial state has empty server url and no last sync result`() {
        val vm = vm()
        assertEquals("", vm.uiState.value.serverUrl)
        assertNull(vm.uiState.value.lastSyncResult)
    }

    @Test
    fun `loads saved config on init`() {
        every { mockConfigStore.load() } returns SyncConfig(
            serverUrl = "https://nc.example.com",
            username = "alice",
            password = "secret",
        )
        val vm = SyncViewModel(mockConfigStore, mockSyncEngine, mockWebDavClient)
        assertEquals("https://nc.example.com", vm.uiState.value.serverUrl)
        assertEquals("alice", vm.uiState.value.username)
    }

    @Test
    fun `testConnection shows connected when PROPFIND returns Found`() = runTest {
        coEvery { mockWebDavClient.propFind(any(), any(), any()) } returns
            WebDavClient.PropFindResult.Found
        val vm = vm()
        vm.onServerUrlChange("https://example.com")
        vm.onUsernameChange("user")
        vm.onPasswordChange("pass")

        vm.onTestConnection()

        val result = vm.uiState.value.testResult
        assertNotNull(result)
        assertTrue(result!!.contains("connected", ignoreCase = true) ||
                   result.contains("found", ignoreCase = true))
    }

    @Test
    fun `testConnection shows not found when PROPFIND returns NotFound`() = runTest {
        coEvery { mockWebDavClient.propFind(any(), any(), any()) } returns
            WebDavClient.PropFindResult.NotFound
        val vm = vm()
        vm.onServerUrlChange("https://example.com")
        vm.onUsernameChange("user")
        vm.onPasswordChange("pass")

        vm.onTestConnection()

        assertNotNull(vm.uiState.value.testResult)
    }

    @Test
    fun `syncNow calls engine and stores result in state`() = runTest {
        coEvery { mockSyncEngine.sync(any()) } returns SyncResult(pushed = 3, pulled = 1)
        val vm = vm()
        vm.onServerUrlChange("https://example.com")
        vm.onUsernameChange("user")
        vm.onPasswordChange("pass")

        vm.onSyncNow()

        assertNotNull(vm.uiState.value.lastSyncResult)
        assertEquals(3, vm.uiState.value.lastSyncResult!!.pushed)
    }

    @Test
    fun `saveConfig calls configStore save`() = runTest {
        val vm = vm()
        vm.onServerUrlChange("https://nextcloud.local")
        vm.onUsernameChange("admin")
        vm.onPasswordChange("secret123")

        vm.onSaveConfig()

        coVerify { mockConfigStore.save(match { it.serverUrl == "https://nextcloud.local" }) }
    }

    @Test
    fun `intervalHours change updates state`() {
        val vm = vm()
        vm.onIntervalChange(24)
        assertEquals(24, vm.uiState.value.intervalHours)
    }
}
