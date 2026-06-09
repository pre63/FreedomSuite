package org.freedomsuite.core.llm

import kotlinx.coroutines.runBlocking
import org.freedomsuite.core.llm.engine.LocalLlmEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FreedomLlmServiceRoutingTest {
    private val request = LlmRequest(
        messages = listOf(LlmMessage(LlmRole.USER, "hi")),
    )

    @Test
    fun onDeviceOnlyUsesLocal() = runBlocking {
        val service = FreedomLlmService(
            onDevice = OnDeviceLlmBackend(FakeEngine("local-ok")),
            remote = FakeRemote(error = true),
            routing = LlmRouting.ON_DEVICE_ONLY,
        )
        val response = service.complete(request)
        assertEquals("local-ok", response.text)
        assertEquals(LlmSource.ON_DEVICE, response.source)
    }

    @Test
    fun remoteFallbackWhenRemoteFails() = runBlocking {
        val service = FreedomLlmService(
            onDevice = OnDeviceLlmBackend(FakeEngine("local-fallback")),
            remote = FakeRemote(error = true),
            routing = LlmRouting.REMOTE_WITH_ON_DEVICE_FALLBACK,
        )
        val response = service.complete(request)
        assertEquals("local-fallback", response.text)
        assertFalse(response.isError)
    }

    @Test
    fun remotePreferredWhenRemoteSucceeds() = runBlocking {
        val service = FreedomLlmService(
            onDevice = OnDeviceLlmBackend(FakeEngine("local")),
            remote = FakeRemote(error = false, text = "remote-ok"),
            routing = LlmRouting.REMOTE_WITH_ON_DEVICE_FALLBACK,
        )
        val response = service.complete(request)
        assertEquals("remote-ok", response.text)
        assertEquals(LlmSource.REMOTE, response.source)
    }

    private class FakeEngine(private val text: String) : LocalLlmEngine {
        override fun modelState(): LlmModelState = LlmModelState.READY
        override suspend fun generate(request: LlmRequest): Result<String> = Result.success(text)
    }

    private class FakeRemote(
        private val error: Boolean,
        private val text: String = "remote",
    ) : LlmBackend {
        override val id: String = "fake"
        override val displayName: String = "fake"
        override suspend fun isAvailable(): Boolean = true
        override suspend fun complete(request: LlmRequest): LlmResponse =
            if (error) {
                LlmResponse("fail", LlmSource.REMOTE, isError = true)
            } else {
                LlmResponse(text, LlmSource.REMOTE)
            }
    }
}
