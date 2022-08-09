package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Inntektshistorikk.Inntektsopplysning
import no.nav.helse.person.Person
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.somVilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjerFilter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class UtbetalingBuilderTest {

    private lateinit var aktivitetslogg: Aktivitetslogg
    private val aktørId = "111111111111111"
    private val fødselsnummer = Fødselsnummer.tilFødselsnummer("12029240045")
    private val fødselsdato = 12.februar(1992)
    private val organisasjonsnummer = "987654321"
    private val maskinellJurist = MaskinellJurist()
    private val søknadId = UUID.randomUUID()
    private val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        fødselsdato = 12.februar(1992)
    )

    @BeforeEach
    internal fun clear() {
        aktivitetslogg = Aktivitetslogg()
        resetSeed()
    }

    @Test
    fun `utbetalinger for en arbeidsgiver`() {
        val vedtaksperiode = vedtaksperiode(1.januar til 31.januar)
        val utbetalinger = nyBuilder(1.januar til 31.januar)
            .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
            .arbeidsgiver(organisasjonsnummer, 31.S, inntektsmelding(1.januar, 1200.daglig))
            .vedtaksperiode(vedtaksperiode, organisasjonsnummer, null)
            .utbetalinger()

        assertNotNull(utbetalinger.getValue(vedtaksperiode))
        assertEquals(1, utbetalinger.size)
    }

    @Test
    fun `utbetalinger for flere arbeidsgivere`() {
        val a1 = "123456789"
        val a2 = "987654321"
        val v1 = vedtaksperiode(1.januar til 31.januar, a1)
        val v2 = vedtaksperiode(1.februar til 28.februar, a2)
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
        val a1 = "123456789"
        val a2 = "987654321"
        val periode = 1.januar til 31.januar
        assertThrows<IllegalStateException> {
            nyBuilder(periode)
                .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
                .vedtaksperiode(vedtaksperiode(periode, a1), a1, null)
        }

        assertDoesNotThrow {
            nyBuilder(periode)
                .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
                .arbeidsgiver(a2, 31.S, inntektsmelding(1.januar, 1200.daglig))
                .vedtaksperiode(vedtaksperiode(periode, a2), a2, null)
        }
    }

    @Test
    fun `avvisInngangsvilkårfilter må settes`() {
        val a1 = "123456789"
        val a2 = "987654321"
        val periode = 1.januar til 31.januar
        assertThrows<UninitializedPropertyAccessException> {
            nyBuilder(periode)
                .arbeidsgiver(a1, 31.S, inntektsmelding(1.januar, 1200.daglig))
                .vedtaksperiode(vedtaksperiode(periode, a1), a1, null)
                .utbetalinger()
        }
        resetSeed()
        assertDoesNotThrow {
            nyBuilder(periode)
                .avvisInngangsvilkårfilter(dummyUtbetalingstidslinjerFilter)
                .arbeidsgiver(a2, 31.S, inntektsmelding(1.januar, 1200.daglig))
                .vedtaksperiode(vedtaksperiode(periode, a2), a2, null)
                .utbetalinger()
        }
    }
    private fun nyBuilder(
        periode: Periode, infotrygdHistorikk:
        Infotrygdhistorikk = Infotrygdhistorikk()
    ) = Utbetaling.Builder(fødselsnummer, 1.januar(1970).alder, aktivitetslogg, periode, NullObserver, null, infotrygdHistorikk, NormalArbeidstaker)

    private fun vedtaksperiode(periode: Periode = 1.januar til 31.januar, organisasjonsnummer: String = this.organisasjonsnummer): Vedtaksperiode {
        val søknad = søknad(søknadId, periode)
        val sykdomstidslinje = periode.associateWith {
            Dag.Sykedag(
                it,
                økonomi = Økonomi.sykdomsgrad(100.prosent),
                kilde = SykdomstidslinjeHendelse.Hendelseskilde("Søknad", søknadId, LocalDateTime.now())
            )
        }
        return Vedtaksperiode(
            person = person(),
            arbeidsgiver = arbeidsgiver(organisasjonsnummer = organisasjonsnummer),
            søknad = søknad,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer.toString(),
            organisasjonsnummer = organisasjonsnummer,
            sykdomstidslinje = Sykdomstidslinje(sykdomstidslinje),
            dokumentsporing = Dokumentsporing.søknad(søknadId),
            periode = periode,
            jurist = maskinellJurist
        )
    }

    private fun person() = Person(aktørId, fødselsnummer, fødselsdato.alder, maskinellJurist)
    private fun arbeidsgiver(organisasjonsnummer: String) = Arbeidsgiver(person(), organisasjonsnummer, maskinellJurist)
    private fun søknad(søknadId: UUID, periode: Periode): Søknad {
        val søknadsperiode = Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent)
        return hendelsefabrikk.lagSøknad(
            id = søknadId,
            perioder = arrayOf(søknadsperiode)
        )
    }

    private companion object {
        private val dummyUtbetalingstidslinjerFilter = object : UtbetalingstidslinjerFilter {
            override fun filter(
                tidslinjer: List<Utbetalingstidslinje>,
                periode: Periode,
                aktivitetslogg: IAktivitetslogg,
                subsumsjonObserver: SubsumsjonObserver
            ) = tidslinjer
        }

        private fun inntektsmelding(skjæringstidspunkt: LocalDate, inntekt: Inntekt) =
            skjæringstidspunkt to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt)

        private fun Utbetaling.Builder.arbeidsgiver(
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            vararg inntekter: Pair<LocalDate, Inntektsopplysning>
        ): Utbetaling.Builder {
            resetSeed()
            return arbeidsgiver(organisasjonsnummer, sykdomstidslinje, inntekter.toMap().somVilkårsgrunnlagHistorikk(organisasjonsnummer), emptyList(), Refusjonshistorikk())
        }
    }
}