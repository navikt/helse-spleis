package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.sistePeriodeForUtbetalinger
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class)
internal class LagUtbetalingForRevurderingTest {

    private lateinit var maskinellJurist: MaskinellJurist
    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private const val AKTØRID = "42"
        private val FNR = "12029240045".somFødselsnummer()
        private const val ORGNUMMER = "123456789"
        private val SØKNAD = UUID.randomUUID()
    }

    @BeforeEach
    fun beforeEach() {
        maskinellJurist = MaskinellJurist()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `enkel periode med én utbetaling`() {
        val perioder = mutableListOf<Pair<Utbetaling, Vedtaksperiode>>()
        val utbetaling1 = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true) to vedtaksperiode()
        perioder.add(utbetaling1)
        assertEquals(1, perioder.sistePeriodeForUtbetalinger().size)
        assertEquals(utbetaling1.second, perioder.sistePeriodeForUtbetalinger().first())
    }

    @Test
    fun `to perioder med forlengelse`() {
        val perioder = mutableListOf<Pair<Utbetaling, Vedtaksperiode>>()
        val utbetaling1 = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true) to vedtaksperiode()
        perioder.add(utbetaling1)
        val utbetaling2 =
            utbetaling(tidslinjeOf(16.AP, 15.NAV, 28.NAV), tidligere = utbetaling1.first) to vedtaksperiode(1.februar til 28.februar)
        perioder.add(utbetaling2)
        assertEquals(1, perioder.sistePeriodeForUtbetalinger().size)
        assertEquals(utbetaling2.second, perioder.sistePeriodeForUtbetalinger().first())
    }

    @Test
    fun `to perioder med agp-gap`() {
        val perioder = mutableListOf<Pair<Utbetaling, Vedtaksperiode>>()
        val utbetaling1 = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true) to vedtaksperiode()
        perioder.add(utbetaling1)
        val utbetaling2 =
            utbetaling(tidslinjeOf(16.AP, 15.NAV, 28.ARB, 16.AP, 15.NAV), tidligere = utbetaling1.first) to vedtaksperiode(1.mars til 31.mars)
        perioder.add(utbetaling2)
        assertEquals(2, perioder.sistePeriodeForUtbetalinger().size)
        assertEquals(utbetaling1.second, perioder.sistePeriodeForUtbetalinger()[0])
        assertEquals(utbetaling2.second, perioder.sistePeriodeForUtbetalinger()[1])
    }

    //usikkert om sistePeriodeForUtbetalinger bør ta høyde for flere skjæringstidspunkter innenfor samme agp
    @Test
    fun `to perioder med mindre enn 16 dager gap`() {
        val perioder = mutableListOf<Pair<Utbetaling, Vedtaksperiode>>()
        val utbetaling1 = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true) to vedtaksperiode()
        perioder.add(utbetaling1)
        val utbetaling2 =
            utbetaling(tidslinjeOf(16.AP, 15.NAV, 1.ARB, 27.NAV), tidligere = utbetaling1.first) to vedtaksperiode(2.februar til 28.februar)
        perioder.add(utbetaling2)
        assertEquals(1, perioder.sistePeriodeForUtbetalinger().size)
        assertEquals(utbetaling2.second, perioder.sistePeriodeForUtbetalinger()[0])
    }

    private fun utbetaling(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = FNR.toString(),
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg,
        utbetalt: Boolean = false
    ): Utbetaling {
        beregnUtbetalinger(tidslinje)
        return Utbetaling.lagUtbetaling(
            tidligere?.let { listOf(tidligere) } ?: emptyList(),
            fødselsnummer,
            UUID.randomUUID(),
            orgnummer,
            tidslinje,
            sisteDato,
            aktivitetslogg,
            LocalDate.MAX,
            100,
            148
        ).also { utbetaling ->
            utbetaling.opprett(aktivitetslogg)
            if (utbetalt) {
                godkjenn(utbetaling)
                listOf(utbetaling.inspektør.arbeidsgiverOppdrag, utbetaling.inspektør.personOppdrag)
                    .filter { it.harUtbetalinger() }
                    .map { it.fagsystemId() }
                    .onEach { overfør(utbetaling, it) }
                    .onEach { kvittèr(utbetaling, it) }
            }
        }
    }

    private fun godkjenn(utbetaling: Utbetaling, utbetalingGodkjent: Boolean = true) =
        Utbetalingsgodkjenning(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "ignore",
            fødselsnummer = "ignore",
            organisasjonsnummer = "ignore",
            utbetalingId = utbetaling.inspektør.utbetalingId,
            vedtaksperiodeId = "ignore",
            saksbehandler = "Z999999",
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            utbetalingGodkjent = utbetalingGodkjent,
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
        ).also {
            utbetaling.håndter(it)
        }

    private fun overfør(
        utbetaling: Utbetaling,
        fagsystemId: String = utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
        utbetalingId: UUID = utbetaling.inspektør.utbetalingId
    ) {
        utbetaling.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = "ignore",
                orgnummer = "ignore",
                fagsystemId = fagsystemId,
                utbetalingId = "$utbetalingId",
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
    }

    private fun kvittèr(
        utbetaling: Utbetaling,
        fagsystemId: String = utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT
    ) {
        utbetaling.håndter(
            UtbetalingHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = FNR.toString(),
                orgnummer = ORGNUMMER,
                fagsystemId = fagsystemId,
                utbetalingId = "${utbetaling.inspektør.utbetalingId}",
                status = status,
                melding = "hei",
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
    }

    private fun beregnUtbetalinger(tidslinje: Utbetalingstidslinje, infotrygdtidslinje: Utbetalingstidslinje = Utbetalingstidslinje()) = tidslinje.let {
        tidslinje.plus(infotrygdtidslinje) { spleisdag, infotrygddag ->
            when (infotrygddag) {
                is Utbetalingstidslinje.Utbetalingsdag.NavDag, is Utbetalingstidslinje.Utbetalingsdag.NavHelgDag -> Utbetalingstidslinje.Utbetalingsdag.UkjentDag(
                    spleisdag.dato,
                    spleisdag.økonomi
                )
                else -> spleisdag
            }
        }
    }.also { MaksimumUtbetalingFilter { 1.januar }.betal(listOf(tidslinje), tidslinje.periode(), aktivitetslogg, MaskinellJurist()) }

    private fun vedtaksperiode(periode: Periode = 1.januar til 31.januar, organisasjonsnummer: String = ORGNUMMER): Vedtaksperiode {
        val søknad = søknad(SØKNAD, periode)
        val sykdomstidslinje = periode.associateWith {
            Dag.Sykedag(
                it,
                økonomi = Økonomi.sykdomsgrad(100.prosent),
                kilde = SykdomstidslinjeHendelse.Hendelseskilde("Søknad", SØKNAD, LocalDateTime.now())
            )
        }
        return Vedtaksperiode(
            person = person(),
            arbeidsgiver = arbeidsgiver(organisasjonsnummer = organisasjonsnummer),
            søknad = søknad,
            aktørId = AKTØRID,
            fødselsnummer = FNR.toString(),
            organisasjonsnummer = organisasjonsnummer,
            sykdomstidslinje = Sykdomstidslinje(sykdomstidslinje),
            dokumentsporing = Dokumentsporing.søknad(SØKNAD),
            periode = periode,
            jurist = maskinellJurist
        )
    }

    private fun person() = Person(AKTØRID, FNR, maskinellJurist)
    private fun arbeidsgiver(organisasjonsnummer: String) = Arbeidsgiver(person(), organisasjonsnummer, maskinellJurist)
    private fun søknad(søknadId: UUID, periode: Periode): Søknad {
        val søknadsperiode = Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent)
        return Søknad(søknadId, FNR.toString(), AKTØRID, ORGNUMMER, listOf(søknadsperiode), emptyList(), LocalDateTime.now(), false, emptyList(), LocalDateTime.now())
    }
}