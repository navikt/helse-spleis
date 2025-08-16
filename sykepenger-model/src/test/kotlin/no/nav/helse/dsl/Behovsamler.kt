package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spill_av_im.Forespørsel
import org.junit.jupiter.api.Assertions.assertTrue

internal class Behovsamler(private val log: DeferredLog) : PersonObserver {
    private val behov = mutableListOf<Behov>()
    private val tilstander = mutableMapOf<UUID, TilstandType>()
    private val replays = mutableSetOf<Forespørsel>()
    private val hånderteInntektsmeldinger = mutableSetOf<UUID>()

    internal fun håndterteInntektsmeldinger() = hånderteInntektsmeldinger.toSet()

    internal fun registrerBehov(aktivitetslogg: Aktivitetslogg) {
        val nyeBehov = aktivitetslogg.behov.takeUnless { it.isEmpty() } ?: return
        log.log("Registrerer ${nyeBehov.size} nye behov (${nyeBehov.joinToString { it.type.toString() }})")
        behov.addAll(nyeBehov)
        log.log(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
    }

    internal fun harBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) =
        harBehov(vedtaksperiodebehov(vedtaksperiodeId), *behovtyper)

    internal fun harBehov(orgnummer: String, vararg behovtyper: Behovtype) =
        harBehov(orgnummerbehov(orgnummer), *behovtyper)

    internal fun harBehov(filter: (Behov) -> Boolean, vararg behovtyper: Behovtype): Boolean {
        val behover = behov.filter(filter).map { it.type }
        return behovtyper.all { behovtype -> behovtype in behover }
    }

    internal fun <R> fangInntektsmeldingReplay(block: () -> R, behandleReplays: (Set<Forespørsel>) -> Unit): R {
        val før = replays.toSet()
        val retval = block()
        behandleReplays(replays.toSet() - før)
        return retval
    }

    internal fun bekreftBehovOppfylt() {
        val ubesvarte = behov.filterNot { it.type == Behovtype.Sykepengehistorikk }.takeUnless { it.isEmpty() } ?: return
        log.log("Etter testen er det ${behov.size} behov uten svar: [${behov.joinToString { it.type.toString() }}]")
    }

    internal fun harBedtOmReplay(vedtaksperiodeId: UUID) =
        replays.any { it.vedtaksperiodeId == vedtaksperiodeId }

    internal fun bekreftOgKvitterReplay(vedtaksperiodeId: UUID) {
        assertTrue(replays.removeAll { it.vedtaksperiodeId == vedtaksperiodeId }) { "Vedtaksperioden har ikke bedt om replay. Den står i ${tilstander.getValue(vedtaksperiodeId)}" }
    }

    internal fun bekreftBehov(vedtaksperiodeId: UUID, vararg behovtyper: Behovtype) {
        bekreftBehov(vedtaksperiodebehov(vedtaksperiodeId), *behovtyper) { "Vedtaksperioden står i ${tilstander.getValue(vedtaksperiodeId)}" }
    }

    internal fun bekreftBehov(orgnummer: String, vararg behovtyper: Behovtype) {
        bekreftBehov(orgnummerbehov(orgnummer), *behovtyper)
    }

    private fun bekreftBehov(filter: (Behov) -> Boolean, vararg behovtyper: Behovtype, melding: () -> String = { "" }) {
        assertTrue(harBehov(filter, *behovtyper)) {
            val behover = behov.filter(filter)
            "Forventer at [${behovtyper.joinToString { it.toString() }}] skal være etterspurt. Fant bare: [${behover.joinToString { it.type.toString() }}]. ${melding()}"
        }
    }

    internal fun detaljerFor(orgnummer: String, behovtype: Behovtype) =
        detaljerFor(orgnummerbehov(orgnummer), behovtype)

    internal fun detaljerFor(vedtaksperiodeId: UUID, behovtype: Behovtype) =
        detaljerFor(vedtaksperiodebehov(vedtaksperiodeId), behovtype)

    internal fun detaljerFor(filter: (Behov) -> Boolean, behovtype: Behovtype) =
        behov.filter { filter(it) && it.type == behovtype }.map { it.detaljer() to it.alleKontekster }

    private fun kvitterVedtaksperiode(vedtaksperiodeId: UUID) {
        val vedtaksperiodebehov = behov.filter(vedtaksperiodebehov(vedtaksperiodeId)).takeUnless { it.isEmpty() } ?: return
        log.log("Fjerner ${vedtaksperiodebehov.size} behov (${vedtaksperiodebehov.joinToString { it.type.toString() }})")
        behov.removeAll { behov -> vedtaksperiodeId == behov.vedtaksperiodeId }
        log.log(" -> Det er nå ${behov.size} behov (${behov.joinToString { it.type.toString() }})")
        if (replays.removeAll { it.vedtaksperiodeId == vedtaksperiodeId }) {
            log.log("-> Vedtaksperioden ba om replay, men det ble ikke utført")
        }
    }

    override fun utbetalingUtbetalt(
        event: PersonObserver.UtbetalingUtbetaltEvent
    ) {
        assertTrue(behov.removeAll { it.utbetalingId == event.utbetalingId }) {
            "Utbetaling ble utbetalt, men ingen behov om utbetaling er registrert"
        }
    }

    override fun inntektsmeldingReplay(
        event: PersonObserver.TrengerArbeidsgiveropplysningerEvent
    ) {
        replays.add(
            Forespørsel(
                fnr = event.personidentifikator.toString(),
                orgnr = event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                vedtaksperiodeId = event.vedtaksperiodeId,
                skjæringstidspunkt = event.skjæringstidspunkt,
                førsteFraværsdager = event.førsteFraværsdager.map { no.nav.helse.spill_av_im.FørsteFraværsdag(it.yrkesaktivitetssporing.somOrganisasjonsnummer, it.førsteFraværsdag) },
                sykmeldingsperioder = event.sykmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
                egenmeldinger = event.egenmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
                harForespurtArbeidsgiverperiode = PersonObserver.Arbeidsgiverperiode in event.forespurteOpplysninger
            )
        )
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        hånderteInntektsmeldinger.add(inntektsmeldingId)
    }

    override fun vedtaksperiodeEndret(
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        tilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
        kvitterVedtaksperiode(event.vedtaksperiodeId)
    }

    private companion object {
        private val Behov.utbetalingId
            get() =
                alleKontekster["utbetalingId"]?.let { UUID.fromString(it) }
        private val Behov.vedtaksperiodeId
            get() =
                alleKontekster["vedtaksperiodeId"]?.let { UUID.fromString(it) }
        private val Behov.orgnummer get() = alleKontekster["organisasjonsnummer"]

        private val vedtaksperiodebehov = { vedtaksperiodeId: UUID -> { behov: Behov -> behov.vedtaksperiodeId == vedtaksperiodeId } }
        private val orgnummerbehov = { orgnummer: String -> { behov: Behov -> behov.orgnummer == orgnummer } }
    }
}
