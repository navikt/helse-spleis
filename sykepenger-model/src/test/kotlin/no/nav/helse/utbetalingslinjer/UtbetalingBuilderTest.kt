package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Inntektshistorikk.Inntektsopplysning
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjerFilter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class UtbetalingBuilderTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun clear() {
        aktivitetslogg = Aktivitetslogg()
        resetSeed()
    }

    @Test
    fun `utbetalinger for en arbeidsgiver`() {
        val utbetalinger = nyBuilder(1.januar til 31.januar)
            .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
            .arbeidsgiver(arbeidsgiver1, 31.S, inntektsmelding(1.januar, 1200.daglig))
            .vedtaksperiode(vedtaksperiode1, arbeidsgiver1, null)
            .utbetalinger()

        assertNotNull(utbetalinger.getValue(vedtaksperiode1))
        assertEquals(1, utbetalinger.size)
    }

    @Test
    fun `utbetalinger for flere arbeidsgivere`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val a1 = "123456789"
        val a2 = "987654321"
        val utbetalinger = nyBuilder(1.januar til 31.januar)
            .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
            .arbeidsgiver(a1, 31.S, inntektsmelding(1.januar, 1200.daglig))
            .arbeidsgiver(a2, 28.S, inntektsmelding(1.januar, 1200.daglig))
            .vedtaksperiode(v1, a1, null)
            .vedtaksperiode(v2, a2, null)
            .utbetalinger()

        assertNotNull(utbetalinger.getValue(v1))
        assertNotNull(utbetalinger.getValue(v2))
        assertEquals(2, utbetalinger.size)
    }

    @Test
    fun `arbeidsgiver uten vedtaksperiode`() {
        val a1 = "123456789"
        val builder = nyBuilder(1.januar til 31.januar)
            .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
            .arbeidsgiver(a1, 28.S, inntektsmelding(1.januar, 1200.daglig))

        assertThrows<UninitializedPropertyAccessException>("kan ikke ha satt arbeidsgiver uten å sette vedtaksperiode") {
            builder.utbetalinger()
        }
    }

    @Test
    fun `arbeidsgiver må være satt før vedtaksperiode settes`() {
        assertThrows<IllegalStateException> {
            nyBuilder(1.januar til 31.januar)
                .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
                .vedtaksperiode(UUID.randomUUID(), "123456789", null)
        }

        assertDoesNotThrow {
            nyBuilder(1.januar til 31.januar)
                .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
                .arbeidsgiver("123456789", 31.S, inntektsmelding(1.januar, 1200.daglig))
                .vedtaksperiode(UUID.randomUUID(), "123456789", null)
        }
    }

    @Test
    fun `avvisInngangsvilkårfilter må settes`() {
        assertThrows<UninitializedPropertyAccessException> {
            nyBuilder(1.januar til 31.januar)
                .arbeidsgiver("123456789", 31.S, inntektsmelding(1.januar, 1200.daglig))
                .vedtaksperiode(UUID.randomUUID(), "123456789", null)
                .utbetalinger()
        }
        resetSeed()
        assertDoesNotThrow {
            nyBuilder(1.januar til 31.januar)
                .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
                .arbeidsgiver("123456789", 31.S, inntektsmelding(1.januar, 1200.daglig))
                .vedtaksperiode(UUID.randomUUID(), "123456789", null)
                .utbetalinger()
        }
    }
    private fun nyBuilder(
        periode: Periode, infotrygdHistorikk:
        Infotrygdhistorikk = Infotrygdhistorikk()
    ) = Utbetaling.Builder(fødselsnummer, aktivitetslogg, periode, NullObserver, null, infotrygdHistorikk, NormalArbeidstaker)

    private companion object {
        private val dummyUtbetalingstidslinjerFilter = object : UtbetalingstidslinjerFilter {
            override fun filter(
                tidslinjer: List<Utbetalingstidslinje>,
                periode: Periode,
                aktivitetslogg: IAktivitetslogg,
                subsumsjonObserver: SubsumsjonObserver
            ) = tidslinjer
        }

        private val fødselsnummer = Fødselsnummer.tilFødselsnummer("12029240045")
        private const val arbeidsgiver1 = "987654321"
        private val vedtaksperiode1 = UUID.fromString("a0ec1467-4ac1-46a9-b131-436c6befc9a5")

        private fun inntektsmelding(skjæringstidspunkt: LocalDate, inntekt: Inntekt) =
            skjæringstidspunkt to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt)

        private fun Utbetaling.Builder.arbeidsgiver(
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            vararg inntekter: Pair<LocalDate, Inntektsopplysning>,
            skjæringstidspunkter: List<LocalDate> = inntekter.toMap().keys.toList()
        ): Utbetaling.Builder {
            resetSeed()
            return arbeidsgiver(organisasjonsnummer, sykdomstidslinje, skjæringstidspunkter, inntekter.toMap(), emptyList(), Refusjonshistorikk())
        }
    }
}