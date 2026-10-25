package com.freelibrary.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * The root certificate bundled for gutenberg.org must be exactly the one SSL.com publishes
 * (https://www.ssl.com/repository/), so a changed or damaged file fails here.
 */
class GutenbergRootCertificateTest {
    private val file = File("src/main/res/raw/ssl_com_tls_rsa_root_ca_2022.pem")

    private fun load(): X509Certificate {
        require(file.exists()) { "${file.path} not found from ${File(".").absolutePath}" }
        return file.inputStream().use { CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate }
    }

    @Test
    fun `the bundled root has the fingerprint SSL com publishes`() {
        val digest = MessageDigest.getInstance("SHA-256").digest(load().encoded)

        assertEquals(EXPECTED_SHA256, digest.joinToString("") { "%02X".format(it) })
    }

    @Test
    fun `the bundled root is a self-signed certificate authority`() {
        val certificate = load()

        assertTrue(certificate.subjectX500Principal.name.contains("SSL.com TLS RSA Root CA 2022"))
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal)
        assertTrue(certificate.basicConstraints >= 0)
        certificate.verify(certificate.publicKey)
    }

    private companion object {
        const val EXPECTED_SHA256 = "8FAF7D2E2CB4709BB8E0B33666BF75A5DD45B5DE480F8EA8D4BFE6BEBC17F2ED"
    }
}
