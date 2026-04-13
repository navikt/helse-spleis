package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spill_av_im.Forespørsel
import no.nav.helse.spill_av_im.Periode
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

internal class EventBusBehovsamler(private val log: DeferredLog): Behovsamler {
    private val behov = mutableListOf<BehovEvent>()
    private val tilstander = mutableMapOf<UUID, TilstandType>()
    private val replays = mutableSetOf<Forespørsel>()
    private val hånderteInntektsmeldinger = mutableSetOf<UUID>()

    override fun registrerBehov(aktivitetslogg: Aktivitetslogg) {}

    private fun registrerBehov(event: EventSubscription.Event, vedtaksperiodeId: UUID? = null, utbetalingId: UUID? = null) {
        val behovEvent = BehovEvent(event, vedtaksperiodeId, utbetalingId)
        log.log("Registrerer behov for ${behovEvent::class.simpleName}")
        behov.add(behovEvent)
        log.log(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.navn }})")
    }

    override fun loggUbesvarteBehov() {
        log.log("Etter testen er det ${behov.size} behov uten svar: [${behov.joinToString { it.navn }}]")
    }

    private fun kvitterVedtaksperiode(vedtaksperiodeId: UUID) {
        val vedtaksperiodebehov = behov
            // kvitterer ikke ut utbetalingsbehov som følge av at vedtaksperioden endrer tilstand
            .filterNot { (event) -> event is EventSubscription.UtbetalingEvent }
            .filter { it.vedtaksperiodeId == vedtaksperiodeId }
            // TrengerHistorikkFraInfotrygdEvent er bare knagget på person, med kvitterer de ut når vedtaksperioden endrer tilstand
            .plus(behov.relevanteEventMedMetadata<EventSubscription.TrengerHistorikkFraInfotrygdEvent>())
            .takeUnless { it.isEmpty() } ?: return

        log.log("Fjerner ${vedtaksperiodebehov.size} behov (${vedtaksperiodebehov.joinToString { it.navn }})")
        behov.removeAll(vedtaksperiodebehov)
        log.log(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.navn }})")

        if (replays.removeAll { it.vedtaksperiodeId == vedtaksperiodeId }) {
            log.log("-> Vedtaksperioden ba om replay, men det ble ikke utført")
        }
    }

    override fun utbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent) {
        assertTrue(behov.removeAll { it.utbetalingId == event.utbetalingId }) {
            "Utbetaling ble utbetalt, men ingen behov om utbetaling er registrert"
        }
    }

    override fun annullering(event: EventSubscription.UtbetalingAnnullertEvent) {
        behov.removeAll { it.utbetalingId == event.utbetalingId }
    }

    override fun inntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent) {
        replays.add(
            Forespørsel(
                fnr = event.opplysninger.personidentifikator.toString(),
                orgnr = event.opplysninger.arbeidstaker.organisasjonsnummer,
                vedtaksperiodeId = event.opplysninger.vedtaksperiodeId,
                skjæringstidspunkt = event.opplysninger.skjæringstidspunkt,
                førsteFraværsdager = event.opplysninger.førsteFraværsdager.map { no.nav.helse.spill_av_im.FørsteFraværsdag(it.arbeidstaker.organisasjonsnummer, it.førsteFraværsdag) },
                sykmeldingsperioder = event.opplysninger.sykmeldingsperioder.map { Periode(it.start, it.endInclusive) },
                egenmeldinger = event.opplysninger.egenmeldingsperioder.map { Periode(it.start, it.endInclusive) },
                harForespurtArbeidsgiverperiode = EventSubscription.Arbeidsgiverperiode in event.opplysninger.forespurteOpplysninger
            )
        )
    }

    override fun inntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent) {
        hånderteInntektsmeldinger.add(event.meldingsreferanseId)
    }

    override fun vedtaksperiodeEndret(
        event: EventSubscription.VedtaksperiodeEndretEvent
    ) {
        tilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
        kvitterVedtaksperiode(event.vedtaksperiodeId)
    }

    override fun utbetalingsdetaljer(orgnummer: String): List<Behovsamler.Utbetalingsdetaljer> {
        return behov.relevantEvent<EventSubscription.UtbetalingEvent>().map {
            Behovsamler.Utbetalingsdetaljer(
                vedtaksperiodeId = it.vedtaksperiodeId,
                behandlingId = it.behandlingId,
                utbetalingId = it.utbetalingId,
                fagsystemId = it.oppdragsdetaljer.fagsystemId
            )
        }
        .groupBy { "${it.utbetalingId}-${it.fagsystemId}" }
        // velger bare siste behov per utbetalingId-fagsystemId-kombinasjon for å håndtere at vedtaksperioden kan ha blitt påminnet og produsert behovet flere ganger
        .mapValues { (_, utbetalingsdetaljer) -> utbetalingsdetaljer.last() }
        .values
        .toList()
        .also { if (it.isEmpty()) error("Forventet at det skal være spurt om utbetaling, men det var det ikke!") }
    }

    override fun simuleringsdetaljer(vedtaksperiodeId: UUID): List<Behovsamler.Simuleringsdetaljer> {
        return behov.relevantEvent<EventSubscription.SimuleringEvent>().map {
            Behovsamler.Simuleringsdetaljer(
                vedtaksperiodeId = it.vedtaksperiodeId,
                utbetalingId = it.utbetalingId,
                fagsystemId = it.oppdragsdetaljer.fagsystemId,
                fagområde = it.oppdragsdetaljer.fagområde
            )
        }.also { if (it.isEmpty()) error("Forventet at det skal være spurt om simulering, men det var det ikke!") }
    }

    override fun godkjenningsdetaljer(vedtaksperiodeId: UUID): Behovsamler.Godkjenningsdetaljer {
        val godkjenningsdetaljer = behov.relevantEvent<EventSubscription.GodkjenningEvent>().map {
            Behovsamler.Godkjenningsdetaljer(
                behandlingId = it.behandlingId,
                utbetalingId = it.utbetalingId
            )
        }
        assert(godkjenningsdetaljer.size == 1) { "Forventet at det skulle være forspurt nøyaktig én godkjenning. Fant ${godkjenningsdetaljer.size}"}
        return godkjenningsdetaljer.single()
    }

    override fun bekreftForespurtVilkårsprøving(vedtaksperiodeId: UUID) {
        assertItteNull(behov.relevantEvent<EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent>().firstOrNull { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være spurt om informasjon til vilkårsprøving for vedtaksperiode $vedtaksperiodeId"
        }
    }

    override fun bekreftForespurtBeregningAvSelvstendig(vedtaksperiodeId: UUID) {
        val event = assertItteNull(behov.relevantEvent<EventSubscription.TrengerInformasjonTilBeregningEvent>().firstOrNull { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være spurt om informasjon til beregning for vedtaksperiode $vedtaksperiodeId"
        }

        assert(event.trengerInformasjonOmSelvstendigForsikring) {
            "Forventet at det skulle være spurt om informasjon til beregning av selvstendig næringsdrivende for vedtaksperiode $vedtaksperiodeId"
        }
    }

    override fun bekreftForespurtBeregningAvArbeidstaker(vedtaksperiodeId: UUID) {
        assertItteNull(behov.relevantEvent<EventSubscription.TrengerInformasjonTilBeregningEvent>{
            it.vedtaksperiodeId == vedtaksperiodeId
        }.firstOrNull()) {
            "Forventet at det skulle være spurt om informasjon til beregning for vedtaksperiode $vedtaksperiodeId"
        }
    }

    override fun harForespurtHistorikkFraInfotrygd(vedtaksperiodeId: UUID) = behov.relevantEvent<EventSubscription.TrengerHistorikkFraInfotrygdEvent>().isNotEmpty()

    override fun feriepengerutbetalingsdetaljer(): List<Behovsamler.Feriepengerutbetalingsdetaljer> {
        return behov.relevantEvent<EventSubscription.UtbetalFeriepengerEvent>().map {
            Behovsamler.Feriepengerutbetalingsdetaljer(
                utbetalingId = it.utbetalingId
            )
        }.also { if (it.isEmpty()) error("Forventet at det skal være spurt om feriepengerutbetaling, men det var det ikke!") }
    }

    override fun <T> håndterForespørslerOmReplayAvInntektsmeldingSomFølgeAv(
        operasjon: () -> T?,
        håndterForespørsel: (forespørsel: Forespørsel, alleredeHåndterteInntektsmeldinger: Set<UUID>) -> Unit
    ): T? {
        val forespørslerFør = replays.toSet()
        val verdi = operasjon()
        val nyeForespørsler = replays.toSet() - forespørslerFør
        nyeForespørsler.forEach { forespørsel ->
            håndterForespørsel(forespørsel, hånderteInntektsmeldinger.toSet())
            replays.removeAll { it.vedtaksperiodeId == forespørsel.vedtaksperiodeId }
        }
        return verdi
    }

    override fun trengerInformasjonTilVilkårsprøving(event: EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent) = registrerBehov(event, vedtaksperiodeId = event.vedtaksperiodeId)

    override fun trengerInformasjonTilBeregning(event: EventSubscription.TrengerInformasjonTilBeregningEvent) = registrerBehov(event, vedtaksperiodeId = event.vedtaksperiodeId)

    override fun trengerHistorikkFraInfotrygd(event: EventSubscription.TrengerHistorikkFraInfotrygdEvent) = registrerBehov(event)

    override fun trengerGodkjenning(event: EventSubscription.GodkjenningEvent) = registrerBehov(event, vedtaksperiodeId = event.vedtaksperiodeId, utbetalingId = event.utbetalingId)

    override fun utbetal(event: EventSubscription.UtbetalingEvent) = registrerBehov(event, vedtaksperiodeId = event.vedtaksperiodeId, utbetalingId = event.utbetalingId)

    override fun simuler(event: EventSubscription.SimuleringEvent) = registrerBehov(event, vedtaksperiodeId = event.vedtaksperiodeId, utbetalingId = event.utbetalingId)

    override fun utbetalFeriepenger(event: EventSubscription.UtbetalFeriepengerEvent) = registrerBehov(event, utbetalingId = event.utbetalingId)

    private data class BehovEvent(val event: EventSubscription.Event, val vedtaksperiodeId: UUID? = null, val utbetalingId: UUID? = null) {
        val navn = "${event::class.simpleName}"
    }

    private companion object {
        inline fun <reified R: EventSubscription.Event> Iterable<BehovEvent>.relevanteEventMedMetadata(egetFilter: (event: R) -> Boolean = { true }) =
            filter { it.event is R && egetFilter(it.event) }

        inline fun <reified R: EventSubscription.Event> Iterable<BehovEvent>.relevantEvent(egetFilter: (event: R) -> Boolean = { true }) =
            relevanteEventMedMetadata(egetFilter).map { it.event as R }

        fun <T>assertItteNull(tingen: T?, feilmelding: () -> String): T {
            assertNotNull(tingen, feilmelding)
            return tingen!!
        }
    }
}
