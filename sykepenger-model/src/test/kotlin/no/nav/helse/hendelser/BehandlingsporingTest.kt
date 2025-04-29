package no.nav.helse.hendelser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BehandlingsporingTest {

    @Test
    fun likhet() {
        val arbeidstaker = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a1")
        assertEquals(arbeidstaker, Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a1"))
        assertNotEquals(arbeidstaker, Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a2"))
        assertEquals(Behandlingsporing.Yrkesaktivitet.Arbeidsledig, Behandlingsporing.Yrkesaktivitet.Arbeidsledig)
        assertEquals(Behandlingsporing.Yrkesaktivitet.Selvstendig, Behandlingsporing.Yrkesaktivitet.Selvstendig)
        assertEquals(Behandlingsporing.Yrkesaktivitet.Frilans, Behandlingsporing.Yrkesaktivitet.Frilans)

        assertTrue(arbeidstaker.erLik(Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a1")))
        assertFalse(arbeidstaker.erLik(Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a2")))
        assertTrue(Behandlingsporing.Yrkesaktivitet.Arbeidsledig.erLik(Behandlingsporing.Yrkesaktivitet.Arbeidsledig))
        assertTrue(Behandlingsporing.Yrkesaktivitet.Selvstendig.erLik(Behandlingsporing.Yrkesaktivitet.Selvstendig))
        assertTrue(Behandlingsporing.Yrkesaktivitet.Frilans.erLik(Behandlingsporing.Yrkesaktivitet.Frilans))
    }
}
