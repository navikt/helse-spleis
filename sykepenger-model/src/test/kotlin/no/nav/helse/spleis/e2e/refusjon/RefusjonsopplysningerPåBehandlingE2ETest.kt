package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDateTime
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerPåBehandlingE2ETest : AbstractDslTest() {

    @Test
    fun `ny vedtaksperiode`() {
        håndterSøknad(januar)

        assertEquals(Beløpstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }

    @Test
    fun `IM før vedtaksperiode`() {
        val tidsstempel = LocalDateTime.now()
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = tidsstempel)
        håndterSøknad(januar)

        val kilde = Kilde(im, Avsender.ARBEIDSGIVER, tidsstempel)
        assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }
}