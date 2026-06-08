package org.freedomsuite.testing.integration

import org.freedomsuite.core.account.EmailIdentity
import org.freedomsuite.core.account.MailAccount
import org.freedomsuite.testing.mockserver.MailboxOrgMockServer
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
abstract class IntegrationTestBase {
    protected lateinit var mock: MailboxOrgMockServer

    @Before
    fun startMockServer() {
        System.setProperty("socksProxyHost", "")
        System.setProperty("socksProxyPort", "")
        System.setProperty("socksNonProxyHosts", "localhost|127.0.0.1|0.0.0.0")
        java.net.ProxySelector.setDefault(object : java.net.ProxySelector() {
            override fun select(uri: java.net.URI?): List<java.net.Proxy> =
                listOf(java.net.Proxy.NO_PROXY)
            override fun connectFailed(uri: java.net.URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {}
        })
        mock = MailboxOrgMockServer()
        mock.start()
    }

    @After
    fun stopMockServer() {
        mock.stop()
    }

    protected fun testMailAccount(): MailAccount = MailAccount(
        email = mock.email,
        imapHost = mock.imapHost(),
        imapPort = mock.imapPort(),
        smtpHost = mock.smtpHost(),
        smtpPort = mock.smtpPort(),
        caldavUrl = mock.caldavUrl(),
        identities = listOf(EmailIdentity(mock.email, "Primary", isDefault = true)),
    )

    protected fun testPassword(): CharArray = mock.password.toCharArray()
}
