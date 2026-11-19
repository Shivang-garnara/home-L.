package com.example.launcherai

import com.example.launcherai.ai.bridge.AiToolBridge
import com.example.launcherai.ai.confirmation.HitlConfirmationManager
import com.example.launcherai.ai.model.AiMessage
import com.example.launcherai.ai.model.AiRole
import com.example.launcherai.ai.provider.AiEvent
import com.example.launcherai.ai.provider.LocalAgentProvider
import com.example.launcherai.ai.provider.RemoteProviderPlaceholder
import com.example.launcherai.data.db.ChatMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8AiAssistantTest {

    @Test
    fun testToolRegistrationInBridge() {
        val searchTool = AiToolBridge.getTool("search_device_packages")
        val launchTool = AiToolBridge.getTool("launch_application")
        val gridTool = AiToolBridge.getTool("modify_workspace_grid")
        val hideTool = AiToolBridge.getTool("toggle_hidden_package")
        val folderTool = AiToolBridge.getTool("create_workspace_folder")

        assertNotNull(searchTool)
        assertNotNull(launchTool)
        assertNotNull(gridTool)
        assertNotNull(hideTool)
        assertNotNull(folderTool)
    }

    @Test
    fun testHitlConfirmationRules() {
        // Direct launch and search tools do NOT require HITL confirmation
        assertFalse(HitlConfirmationManager.requiresConfirmation("search_device_packages"))
        assertFalse(HitlConfirmationManager.requiresConfirmation("launch_application"))

        // State-altering launcher tools REQUIRE HITL confirmation
        assertTrue(HitlConfirmationManager.requiresConfirmation("modify_workspace_grid"))
        assertTrue(HitlConfirmationManager.requiresConfirmation("toggle_hidden_package"))
        assertTrue(HitlConfirmationManager.requiresConfirmation("create_workspace_folder"))
    }

    @Test
    fun testUnregisteredToolLookup() {
        val tool = AiToolBridge.getTool("unregistered_tool")
        assertNull(tool)
    }

    @Test
    fun testLocalAgentProviderStreamingEvents() = runBlocking {
        val provider = LocalAgentProvider()
        assertTrue(provider.isConfigured)
        assertEquals("Local On-Device Launcher Agent", provider.providerName)

        val events = provider.streamPrompt("Search my installed apps").toList()
        assertTrue(events.any { it is AiEvent.Reasoning })
        assertTrue(events.any { it is AiEvent.ToolCall })
        assertTrue(events.any { it is AiEvent.Token })
        assertEquals(AiEvent.Complete, events.last())
    }

    @Test
    fun testRemoteProviderPlaceholderDisconnectedStatus() = runBlocking {
        val provider = RemoteProviderPlaceholder()
        assertFalse(provider.isConfigured)

        val events = provider.streamPrompt("Hello AI").toList()
        assertTrue(events.any { it is AiEvent.Error })
        val errorEvent = events.first { it is AiEvent.Error } as AiEvent.Error
        assertTrue(errorEvent.errorMessage.contains("disconnected"))
    }

    @Test
    fun testChatMessageEntityModel() {
        val entity = ChatMessageEntity(
            text = "Test prompt",
            isUser = true,
            reasoning = "Testing...",
            toolCallName = "launch_application",
            requiresConfirmation = false
        )

        assertEquals("Test prompt", entity.text)
        assertTrue(entity.isUser)
        assertEquals("launch_application", entity.toolCallName)
        assertFalse(entity.requiresConfirmation)
    }

    @Test
    fun testAiMessageDomainModel() {
        val message = AiMessage(
            role = AiRole.USER,
            text = "Search camera app"
        )

        assertEquals(AiRole.USER, message.role)
        assertEquals("Search camera app", message.text)
    }
}
