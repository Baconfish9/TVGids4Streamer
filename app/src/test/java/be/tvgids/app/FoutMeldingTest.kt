package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException

class FoutMeldingTest {

    @Test
    fun geweigerdCertificaatWijstNaarDeKlok() {
        val fout = SSLHandshakeException("java.security.cert.CertPathValidatorException: Chain validation failed")
        assertTrue(foutMelding(fout).contains("datum en tijd"))
        assertTrue(isCertificaatFout(fout))
    }

    @Test
    fun certificaatfoutAlsOorzaakWordtHerkend() {
        val fout = IOException("mislukt", CertificateException(CertPathValidatorException("Chain validation failed")))
        assertTrue(foutMelding(fout).contains("datum en tijd"))
    }

    @Test
    fun andereFoutenBlijvenOngewijzigd() {
        assertEquals("HTTP 404", foutMelding(IOException("HTTP 404")))
        assertTrue(!isCertificaatFout(IOException("HTTP 404")))
    }

    @Test
    fun gegevensBuitenDeGidsWijzenNaarDeKlok() {
        val info = ParseInfo().apply { voorGekoppeld = 10; inVenster = 0 }
        assertTrue(info.samenvatting().contains("datum en tijd"))
        info.inVenster = 5
        assertTrue(!info.samenvatting().contains("datum en tijd"))
    }
}
