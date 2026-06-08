package org.freedomsuite.testing.integration

import kotlinx.coroutines.runBlocking
import org.freedomsuite.protocol.imap.ImapClientFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImapIntegrationTest : IntegrationTestBase() {
    @Test
    fun fetchInboxFromMockServer() = runBlocking {
        val client = ImapClientFactory.create(plainText = true)
        val account = testMailAccount()

        client.connect(account, testPassword()).getOrThrow()
        val messages = client.fetchInbox(limit = 10).getOrThrow()
        client.disconnect()

        assertEquals(2, messages.size)
        assertEquals("Second message", messages.first().subject)
        assertEquals("Welcome to FreedomSuite", messages.last().subject)
        assertTrue(messages.last().from.contains("sender"))
    }

    @Test
    fun moveToArchiveOnMockServer() = runBlocking {
        val client = ImapClientFactory.create(plainText = true)
        val account = testMailAccount()

        client.connect(account, testPassword()).getOrThrow()
        client.moveToArchive("INBOX", 2).getOrThrow()
        val remaining = client.fetchInbox(limit = 10).getOrThrow()
        client.disconnect()

        assertEquals(1, remaining.size)
        assertEquals(1L, remaining.first().uid)
        assertTrue(mock.imap.archivedUids.contains(2L))
    }

    @Test
    fun fetchMessageBodyFromMockServer() = runBlocking {
        val client = ImapClientFactory.create(plainText = true)
        val account = testMailAccount()

        client.connect(account, testPassword()).getOrThrow()
        val body = client.fetchMessageBody("INBOX", 1).getOrThrow()
        client.disconnect()

        assertEquals("Hello from mock IMAP.", body)
    }

    @Test
    fun listFoldersFromMockServer() = runBlocking {
        val client = ImapClientFactory.create(plainText = true)
        val account = testMailAccount()

        client.connect(account, testPassword()).getOrThrow()
        val folders = client.listFolders().getOrThrow()
        client.disconnect()

        assertTrue(folders.contains("INBOX"))
        assertTrue(folders.contains("Archive"))
        assertTrue(folders.contains("Sent"))
    }

    @Test
    fun searchFolderOnMockServer() = runBlocking {
        val client = ImapClientFactory.create(plainText = true)
        val account = testMailAccount()

        client.connect(account, testPassword()).getOrThrow()
        val matches = client.searchFolder("INBOX", "FreedomSuite", limit = 10).getOrThrow()
        client.disconnect()

        assertEquals(1, matches.size)
        assertEquals("Welcome to FreedomSuite", matches.first().subject)
    }
}
