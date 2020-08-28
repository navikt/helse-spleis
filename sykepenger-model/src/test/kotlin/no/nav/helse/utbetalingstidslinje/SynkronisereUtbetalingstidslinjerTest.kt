package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.inntektsdatoer
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SynkronisereUtbetalingstidslinjerTest {
    private lateinit var arb1: Arbeidsgiver
    private lateinit var arb2: Arbeidsgiver
    private lateinit var arb3: Arbeidsgiver
    private lateinit var arb4: Arbeidsgiver

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
    }

    @BeforeEach
    internal fun setup() {
        val person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        arb1 = Arbeidsgiver(person, "A1")
        arb2 = Arbeidsgiver(person, "A2")
        arb3 = Arbeidsgiver(person, "A3")
        arb4 = Arbeidsgiver(person, "A4")
        arb1.håndter(sykmelding(1.januar til 31.januar, "A1"))
        arb1.håndter(sykmelding(8.april til 31.mai, "A1"))
        arb2.håndter(sykmelding(8.januar til 28.februar, "A2"))
        arb3.håndter(sykmelding(15.januar til 7.februar, "A3"))
        arb4.håndter(sykmelding(1.april til 30.april, "A4"))
        arb4.håndter(sykmelding(1.mai til 8.mai, "A4"))
    }

    @Test
    fun `Inntektsdatoer for flere perioder`() {
        assertEquals(listOf(31.desember(2017), 31.mars), listOf(arb1, arb2, arb3, arb4).inntektsdatoer())
    }

    private fun sykmelding(periode: Periode, orgnummer: String): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(Sykmeldingsperiode(periode.start, periode.endInclusive, 100)),
            mottatt = periode.endInclusive.atStartOfDay()
        )
    }
}
