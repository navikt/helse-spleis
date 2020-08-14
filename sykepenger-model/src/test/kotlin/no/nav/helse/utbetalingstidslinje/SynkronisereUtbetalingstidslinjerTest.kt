package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.spleis.e2e.TestTidslinjeInspektør
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SynkronisereUtbetalingstidslinjerTest {
    private lateinit var person: Person
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
    fun `Sammenhengende periode for en spesifikk periode`() {
        assertEquals(1.januar to 28.februar, person.sammenhengendePeriode(1.januar til  31.januar))
        assertEquals(1.januar to 28.februar, person.sammenhengendePeriode(8.januar til 28.februar))
        assertEquals(1.januar to 28.februar, person.sammenhengendePeriode(15.januar til 7.februar))
        assertEquals(1.april to 30.juni, person.sammenhengendePeriode(8.april til 31.mai))
        assertEquals(1.april to 30.juni, person.sammenhengendePeriode(1.juni til 30.juni))
        assertEquals(1.april to 30.juni, person.sammenhengendePeriode(1.april til 30.april))
    }

    @Test
    fun `Utbetalingstidslinjer utvides til å overlappe med sammenhengende periode for person`() {
        val tidslinjer = ArbeidsgiverUtbetalinger(
            mapOf(
                arb1 to tidslinjeOf(31.NAV),
                arb2 to tidslinjeOf(24.NAV, 28.NAV, startDato = 8.januar),
                arb3 to tidslinjeOf(17.NAV, 7.NAV, startDato = 15.januar),
                arb4 to tidslinjeOf(30.NAV, startDato = 1.april)
            ),
            tidslinjeOf(),
            1.januar til 31.januar,
            Alder(UNG_PERSON_FNR_2018),
            NormalArbeidstaker,
            Aktivitetslogg(),
            arb1.organisasjonsnummer(),
            UNG_PERSON_FNR_2018
        )
        // TODO gjøre ferdig eller slette testen
    }

    @Test
    fun ` Padding utbetalingstidslinjene til sammenhengendeperiode`(){
        person.utbetalingstidslinjer(1.januar til 31.januar, ytelser("A1")).also {
            assertEquals(4, it.size)
            assertEquals(181, it[arb1]?.size)
            assertEquals(59, it[arb2]?.size)
            assertEquals(59, it[arb3]?.size)
            assertEquals(120, it[arb4]?.size)
            it[arb3]?.also { tidslinje ->
                TestTidslinjeInspektør(tidslinje).also { inspektør ->
                    assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
                    assertEquals(6, inspektør.dagtelling[NavDag::class])
                    assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
                    assertEquals(25, inspektør.dagtelling[Arbeidsdag::class])
                    assertEquals(10, inspektør.dagtelling[Fridag::class])
                }
            }
        }
    }

    private fun arbeidsgiver(orgnummer: String): Arbeidsgiver {
        class ArbeidsgiverFinder(person: Person): PersonVisitor {
            lateinit var result: Arbeidsgiver
            init {
                person.accept(this)
            }
            override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                if (orgnummer == organisasjonsnummer) result = arbeidsgiver
            }
        }
        return ArbeidsgiverFinder(person).result
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

    private fun ytelser(orgnummer: String): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        return Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = "vedtaksperiode1",
            utbetalingshistorikk = Utbetalingshistorikk(
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = "vedtaksperiode1",
                utbetalinger = emptyList(),
                inntektshistorikk = emptyList(),
                aktivitetslogg = aktivitetslogg
            ),
            foreldrepermisjon = Foreldrepermisjon(
                null,
                null,
                aktivitetslogg
            ),
            aktivitetslogg = aktivitetslogg
        )
    }
}
