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
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.sistePeriodeForUtbetalinger
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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

    @Disabled
    @Test
    fun `to forskjellige utbetalinger med samme skjæringstidspunkt (ping-pong)`() {
        val perioder = mutableListOf<Pair<Utbetaling, Vedtaksperiode>>()
        val utbetaling1 = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true) to vedtaksperiode()
        val utbetaling2 = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true) to vedtaksperiode()
        perioder.add(utbetaling1)
        perioder.add(utbetaling2)
        assertEquals(2, perioder.sistePeriodeForUtbetalinger().size)
        assertEquals(17.januar til 31.januar, utbetaling1.first.inspektør.periode)
        assertEquals(17.januar til 31.januar, utbetaling2.first.inspektør.periode)
        assertEquals(utbetaling1.second, perioder.sistePeriodeForUtbetalinger()[0])
        assertEquals(utbetaling2.second, perioder.sistePeriodeForUtbetalinger()[1])
    }

    @Disabled
    @Test
    fun `lag revurdering`() {
        val utbetaling = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true)
        val revurdering = revurdering(tidslinjeOf(16.AP, 1.FRI, 14.NAV), utbetaling, utbetaling, 31.januar)
        assertEquals(utbetaling.inspektør.korrelasjonsId, revurdering.inspektør.korrelasjonsId)
        assertEquals(utbetaling.inspektør.arbeidsgiverOppdrag.fagsystemId(), revurdering.inspektør.arbeidsgiverOppdrag.fagsystemId())
        revurdering.inspektør.arbeidsgiverOppdrag.also {
            assertEquals(2, it.size)
            assertEquals(17.januar, it[0].inspektør.datoStatusFom)
            assertEquals(1, it[0].inspektør.delytelseId)
            assertEquals(null, it[0].inspektør.refDelytelseId)
            assertEquals(null, it[0].inspektør.refFagsystemId)
            assertEquals(Endringskode.ENDR, it[0].inspektør.endringskode)
            assertEquals(17.januar til 31.januar, it[0].periode)

            assertEquals(null, it[1].inspektør.datoStatusFom)
            assertEquals(2, it[1].inspektør.delytelseId)
            assertEquals(utbetaling.inspektør.arbeidsgiverOppdrag[0].inspektør.delytelseId, it[1].inspektør.refDelytelseId)
            assertEquals(it.fagsystemId(), it[1].inspektør.refFagsystemId)
            assertEquals(Endringskode.NY, it[1].inspektør.endringskode)
            assertEquals(18.januar til 31.januar, it[1].periode)
        }
        revurdering.assertDiff(-1200)
    }

    @Disabled
    @Test
    fun `lag revurdering begrenset til kuttdato`() {
        val utbetaling = utbetaling(tidslinjeOf(16.AP, 15.NAV, 16.NAV), utbetalt = true)
        val revurdering = revurdering(tidslinjeOf(16.AP, 1.FRI, 14.NAV, 1.FRI, 15.NAV), utbetaling, utbetaling, 31.januar)
        assertEquals(utbetaling.inspektør.korrelasjonsId, revurdering.inspektør.korrelasjonsId)
        revurdering.assertDiff(-1200) // Trekker tilbake 1 dag
        revurdering.inspektør.utbetalingstidslinje.inspektør.also {
            assertEquals(1, it.fridagTeller)
            assertEquals(22, it.navDagTeller)
        }

        revurdering.inspektør.arbeidsgiverOppdrag.also {
            assertEquals(2, it.size)
            assertEquals(17.januar, it[0].inspektør.datoStatusFom)
            assertEquals(1, it[0].inspektør.delytelseId)
            assertEquals(null, it[0].inspektør.refDelytelseId)
            assertEquals(null, it[0].inspektør.refFagsystemId)
            assertEquals(Endringskode.ENDR, it[0].inspektør.endringskode)
            assertEquals(17.januar til 16.februar, it[0].periode)

            assertEquals(null, it[1].inspektør.datoStatusFom)
            assertEquals(2, it[1].inspektør.delytelseId)
            assertEquals(it[0].inspektør.delytelseId, it[1].inspektør.refDelytelseId)
            assertEquals(it.fagsystemId(), it[1].inspektør.refFagsystemId)
            assertEquals(Endringskode.NY, it[1].inspektør.endringskode)
            assertEquals(18.januar til 16.februar, it[1].periode)
        }
    }

    @Disabled
    @Test
    fun `lag revurdering begrenset til kuttdato og deretter ny revurdering`() {
        val utbetaling = utbetaling(tidslinjeOf(16.AP, 15.NAV, 16.NAV), utbetalt = true)
        val revurdering = revurdering(tidslinjeOf(16.AP, 1.FRI, 14.NAV, 1.FRI, 15.NAV), utbetaling, utbetaling, 31.januar, utbetalt = true)
        val revurdering2 = revurdering(tidslinjeOf(16.AP, 1.FRI, 14.NAV, 1.ARB, 15.NAV), revurdering, revurdering, 16.februar)
        assertEquals(utbetaling.inspektør.korrelasjonsId, revurdering.inspektør.korrelasjonsId)
        assertEquals(revurdering.inspektør.korrelasjonsId, revurdering2.inspektør.korrelasjonsId)
        revurdering2.assertDiff(-1200)

        revurdering.inspektør.utbetalingstidslinje.inspektør.also {
            assertEquals(1, it.fridagTeller)
            assertEquals(0, it.arbeidsdagTeller)
            assertEquals(22, it.navDagTeller)
        }
        revurdering2.inspektør.utbetalingstidslinje.inspektør.also {
            assertEquals(1, it.fridagTeller)
            assertEquals(1, it.arbeidsdagTeller)
            assertEquals(21, it.navDagTeller)
        }

        revurdering2.inspektør.arbeidsgiverOppdrag.also {
            assertEquals(2, it.size)
            assertEquals(null, it[0].inspektør.datoStatusFom)
            assertEquals(2, it[0].inspektør.delytelseId)
            assertEquals(null, it[0].inspektør.refDelytelseId)
            assertEquals(null, it[0].inspektør.refFagsystemId)
            assertEquals(Endringskode.ENDR, it[0].inspektør.endringskode)
            assertEquals(18.januar til 31.januar, it[0].periode)

            assertEquals(null, it[1].inspektør.datoStatusFom)
            assertEquals(3, it[1].inspektør.delytelseId)
            assertEquals(it[0].inspektør.delytelseId, it[1].inspektør.refDelytelseId)
            assertEquals(it.fagsystemId(), it[1].inspektør.refFagsystemId)
            assertEquals(Endringskode.NY, it[1].inspektør.endringskode)
            assertEquals(2.februar til 16.februar, it[1].periode)
        }
    }

    @Disabled
    @Test
    fun `revurderingen kan strekkes forbi utbetalingen`() {
        val utbetaling = utbetaling(tidslinjeOf(16.AP, 15.NAV), utbetalt = true)
        val revurdering = revurdering(tidslinjeOf(16.AP, 20.NAV), utbetaling, utbetaling, 5.februar)
        revurdering.inspektør.utbetalingstidslinje.inspektør.apply {
            assertEquals(1.januar, førstedag.dato)
            assertEquals(5.februar, sistedag.dato)
        }

        revurdering.inspektør.arbeidsgiverOppdrag.also {
            assertEquals(1, it.size)
            assertEquals(null, it[0].inspektør.datoStatusFom)
            assertEquals(1, it[0].inspektør.delytelseId)
            assertEquals(null, it[0].inspektør.refDelytelseId)
            assertEquals(null, it[0].inspektør.refFagsystemId)
            assertEquals(Endringskode.ENDR, it[0].inspektør.endringskode)
            assertEquals(17.januar til 5.februar, it[0].periode)
        }
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

    private fun revurdering(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        forrige: Utbetaling,
        sisteDato: LocalDate,
        fødselsnummer: String = FNR.toString(),
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg,
        utbetalt: Boolean = false,
    ): Utbetaling {
        beregnUtbetalinger(tidslinje)
        return Utbetaling.lagRevurdering(
            tidligere?.let { listOf(tidligere) } ?: emptyList(),
            fødselsnummer,
            UUID.randomUUID(),
            orgnummer,
            tidslinje,
            sisteDato,
            aktivitetslogg,
            LocalDate.MAX,
            100,
            148,
            listOf(forrige)
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

    private fun Utbetaling.assertDiff(diff: Int) {
        assertEquals(diff, inspektør.nettobeløp)
    }
}