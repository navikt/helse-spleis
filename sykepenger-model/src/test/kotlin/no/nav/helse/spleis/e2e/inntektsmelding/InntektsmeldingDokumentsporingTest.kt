package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Dokumentsporing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class InntektsmeldingDokumentsporingTest : AbstractDslTest() {

    //TODO slett når vi støtter å revudere agp
    @Test
    fun `Skal ikke få dokumentsporing for å ha håndtert dager når man er i en tilstand som ikke håndterer dager`() = Toggle.RevurdereAgpFraIm.disable {
        a1 {
            nyttVedtak(1.januar, 31.januar, arbeidsgiverperiode = listOf(2.januar til 17.januar))
            assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            val korrigerendeIm = håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            assertFalse(inspektør.hendelser { 1.vedtaksperiode }.contains(Dokumentsporing.inntektsmeldingDager(korrigerendeIm)))
        }
    }
}