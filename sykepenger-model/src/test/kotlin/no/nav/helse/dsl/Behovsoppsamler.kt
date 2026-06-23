package no.nav.helse.dsl

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person. EventSubscription
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spill_av_im.Forespørsel
import no.nav.helse.spill_av_im.FørsteFraværsdag
import no.nav.helse.spill_av_im.Periode
import org.junit.jupiter.api.Assertions.assertTrue

sealed class Behovsoppsamler(private val log: DeferredLog): EventSubscription {
    private val behovsdetaljer = mutableListOf<Behovsdetaljer>()
    internal inline fun <reified R: Behovsdetaljer>behovsdetaljer() = behovsdetaljer.filterIsInstance<R>()

    protected fun registrer(behovsdetaljer: Behovsdetaljer) {
        this.behovsdetaljer.add(behovsdetaljer)
    }
    internal fun besvart(behovsdelajer: Behovsdetaljer) {
        this.behovsdetaljer.remove(behovsdelajer)
    }

    override fun inntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent) =
        registrer(Behovsdetaljer.InntektsmeldingReplay(event.opplysninger.vedtaksperiodeId, forespørsel = Forespørsel(
            fnr = event.opplysninger.personidentifikator.toString(),
            orgnr = event.opplysninger.arbeidstaker.organisasjonsnummer,
            vedtaksperiodeId = event.opplysninger.vedtaksperiodeId,
            skjæringstidspunkt = event.opplysninger.skjæringstidspunkt,
            førsteFraværsdager = event.opplysninger.førsteFraværsdager.map { FørsteFraværsdag(it.arbeidstaker.organisasjonsnummer, it.førsteFraværsdag) },
            sykmeldingsperioder = event.opplysninger.sykmeldingsperioder.map { Periode(it.start, it.endInclusive) },
            egenmeldinger = event.opplysninger.egenmeldingsperioder.map { Periode(it.start, it.endInclusive) },
            harForespurtArbeidsgiverperiode = EventSubscription.Arbeidsgiverperiode in event.opplysninger.forespurteOpplysninger
        )))

    fun  loggUbesvarteBehov() {
        log.log("Etter testen er det ${behovsdetaljer.size} behov uten svar: [${behovsdetaljer.joinToString { "${it::class.simpleName}"  }}]")
    }

    override fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        val vedtaksperiodebehov = behovsdetaljer
            // kvitterer ikke ut utbetalingsbehov som følge av at vedtaksperioden endrer tilstand, det gjøres som følge av utbetaling-events
            .filterNot { behovsdetaljer -> behovsdetaljer is Behovsdetaljer.Utbetaling }
            // kvitterer ikke ut replay forespørsler, det gjøres når det håndteres replay
            .filterNot { behovsdetaljer -> behovsdetaljer is Behovsdetaljer.InntektsmeldingReplay }
            .filter { it.vedtaksperiodeId == event.vedtaksperiodeId }
            // TrengerOppdatertHistorikkFraInfotrygdEvent er bare knagget på person, med kvitterer de ut når vedtaksperioden endrer tilstand
            .plus(behovsdetaljer.filterIsInstance<Behovsdetaljer.OppdatertHistorikkFraInfotrygd>())
            .takeUnless { it.isEmpty() } ?: return

        log.log("Fjerner ${vedtaksperiodebehov.size} behov (${vedtaksperiodebehov.joinToString { "${it::class.simpleName}" }})")
        behovsdetaljer.removeAll(vedtaksperiodebehov)
        log.log(" -> Det er nå ${behovsdetaljer.size} behov (${vedtaksperiodebehov.joinToString { "${it::class.simpleName}" }})")
    }

    override fun utbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent) {
        assertTrue(behovsdetaljer.removeAll { it.utbetalingId == event.utbetalingId }) {
            "Utbetaling ble utbetalt, men ingen behov om utbetaling er registrert"
        }
    }

    override fun annullering(event: EventSubscription.UtbetalingAnnullertEvent) {
        behovsdetaljer.removeAll { it.utbetalingId == event.utbetalingId }
    }

    sealed interface Behovsdetaljer {
        val id: UUID
        val utbetalingId: UUID? get() = null
        val vedtaksperiodeId: UUID? get() = null

        data class Utbetaling(
            override val vedtaksperiodeId: UUID,
            val behandlingId: UUID,
            val organisasjonsnummer: String,
            override val utbetalingId: UUID,
            val fagsystemId: String,
            val fagområde: String,
            val maksdato: LocalDate?,
            val linjer: List<Linje>,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer {
            data class Linje(
                val statuskode: String?
            )
        }

        data class Simulering(
            override val vedtaksperiodeId: UUID,
            override val utbetalingId: UUID,
            val fagsystemId: String,
            val fagområde: String,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class Godkjenning(
            val behandlingId: UUID,
            override val utbetalingId: UUID,
            override val vedtaksperiodeId: UUID,
            val event: EventSubscription.GodkjenningEvent,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class Feriepengeutbetaling(
            override val utbetalingId: UUID,
            val event: EventSubscription.UtbetalFeriepengerEvent,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class InitiellHistorikFraInfotrygd(
            override val vedtaksperiodeId: UUID,
            val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class OppdatertHistorikkFraInfotrygd(
            val periode: no.nav.helse.hendelser.Periode,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class InformasjonTilBeregningAvArbeidstaker(
            override val vedtaksperiodeId: UUID,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class InformasjonTilBeregningAvSelvstendig(
            override val vedtaksperiodeId: UUID,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class InformasjonTilVilkårsprøving(
            override val vedtaksperiodeId: UUID,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer

        data class InntektsmeldingReplay(
            override val vedtaksperiodeId: UUID,
            val forespørsel: Forespørsel,
            override val id: UUID = UUID.randomUUID()
        ): Behovsdetaljer
    }

    class FraEventBus(log: DeferredLog = DeferredLog()): Behovsoppsamler(log) {
        override fun utbetal(event: EventSubscription.UtbetalingEvent) =
            registrer(Behovsdetaljer.Utbetaling(
                vedtaksperiodeId = event.vedtaksperiodeId,
                behandlingId = event.behandlingId,
                organisasjonsnummer = event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                utbetalingId = event.utbetalingId,
                fagområde = event.oppdragsdetaljer.fagområde,
                fagsystemId = event.oppdragsdetaljer.fagsystemId,
                maksdato = event.oppdragsdetaljer.maksdato,
                linjer = event.oppdragsdetaljer.linjer.map { Behovsdetaljer.Utbetaling.Linje(it.statuskode) }
            ))
        override fun simuler(event: EventSubscription.SimuleringEvent) =
            registrer(Behovsdetaljer.Simulering(event.vedtaksperiodeId, event.utbetalingId, event.oppdragsdetaljer.fagsystemId, event.oppdragsdetaljer.fagområde))
        override fun trengerGodkjenning(event: EventSubscription.GodkjenningEvent) =
            registrer(Behovsdetaljer.Godkjenning(event.behandlingId, event.utbetalingId, event.vedtaksperiodeId, event))
        override fun utbetalFeriepenger(event: EventSubscription.UtbetalFeriepengerEvent) =
            registrer(Behovsdetaljer.Feriepengeutbetaling(event.utbetalingId, event))
        override fun trengerInitiellHistorikkFraInfotrygd(event: EventSubscription.TrengerInitiellHistorikkFraInfotrygdEvent) =
            registrer(Behovsdetaljer.InitiellHistorikFraInfotrygd(event.vedtaksperiodeId, event.yrkesaktivitetssporing))
        override fun trengerOppdatertHistorikkFraInfotrygd(event: EventSubscription.TrengerOppdatertHistorikkFraInfotrygdEvent) =
            registrer(Behovsdetaljer.OppdatertHistorikkFraInfotrygd(event.periode))
        override fun trengerInformasjonTilBeregning(event: EventSubscription.TrengerInformasjonTilBeregningEvent) = when (event.yrkesaktivitetssporing is Behandlingsporing.Yrkesaktivitet.Selvstendig) {
            true -> registrer(Behovsdetaljer.InformasjonTilBeregningAvSelvstendig(event.vedtaksperiodeId))
            false -> registrer(Behovsdetaljer.InformasjonTilBeregningAvArbeidstaker(event.vedtaksperiodeId))
        }
        override fun trengerInformasjonTilVilkårsprøving(event: EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent) =
            registrer(Behovsdetaljer.InformasjonTilVilkårsprøving(event.vedtaksperiodeId))
    }
}
