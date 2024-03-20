package no.nav.helse.dsl

import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.overlapper
import no.nav.helse.inspectors.VedtaksperiodeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.arbeidsgiver

internal class UgyldigeSituasjonerObservatør(private val person: Person): PersonObserver {

    private val arbeidsgivereMap = mutableMapOf<String, Arbeidsgiver>()
    private val gjeldendeTilstander = mutableMapOf<UUID, TilstandType>()
    private val arbeidsgivere get() = arbeidsgivereMap.values
    private val IM = Inntektsmeldinger()

    private val behandlingOpprettetEventer = mutableListOf<PersonObserver.BehandlingOpprettetEvent>()
    private val behandlingLukketEventer = mutableListOf<PersonObserver.BehandlingLukketEvent>()
    private val behandlingForkastetEventer = mutableListOf<PersonObserver.BehandlingForkastetEvent>()

    init {
        person.addObserver(this)
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        check(behandlingOpprettetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut opprettet event"
        }
        behandlingOpprettetEventer.add(event)
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingLukketEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut lukket event"
        }
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingForkastetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut forkastet event"
        }
    }

    private fun bekreftAtBehandlingFinnes(behandlingId: UUID) {
        val behandlingVarsletOmFør = { id: UUID ->
            behandlingOpprettetEventer.singleOrNull { it.behandlingId == id } != null
        }
        // gjelder tester som tar utgangspunkt i en serialisert personjson
        val behandlingFinnesHosArbeidsgiver = { id: UUID ->
            person.inspektør.vedtaksperioder().any { (_, perioder) ->
                perioder.any { periode ->
                    periode.inspektør.behandlinger.any { behandling ->
                        behandling.id == behandlingId
                    }
                }
            }
        }
        check(behandlingVarsletOmFør(behandlingId) || behandlingFinnesHosArbeidsgiver(behandlingId)) {
            "behandling $behandlingId forkastes uten at det er registrert et opprettet event"
        }
    }

    override fun vedtaksperiodeEndret(
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        arbeidsgivereMap.getOrPut(event.organisasjonsnummer) { person.arbeidsgiver(event.organisasjonsnummer) }
        gjeldendeTilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
    }

    override fun behandlingUtført() {
        bekreftIngenUgyldigeSituasjoner()
        IM.behandlingUtført()
    }

    override fun vedtaksperiodeVenter(event: PersonObserver.VedtaksperiodeVenterEvent) {
        if (event.venterPå.venteårsak.hva != "HJELP") return // Om vi venter på noe annet enn hjelp er det OK 👍
        if (event.revurderingFeilet()) return // For tester som ender opp i revurdering feilet er det riktig at vi trenger hjelp 🛟
        if (event.auuVilOmgjøres()) return // For tester som ikke lar en AUU gå videre i livet 🛟
        """
        Har du endret/opprettet en vedtaksperiodetilstand uten å vurdre konsekvensene av 'venteårsak'? 
        Eller har du klart å skriv en test vi ikke støtter? 
        ${event.tilstander()}
        $event
        """.let { throw IllegalStateException(it) }
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) = IM.håndtert()
    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, harPeriodeInnenfor16Dager: Boolean) = IM.ikkeHåndtert()
    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) = IM.førSøknad()
    override fun overstyringIgangsatt(event: PersonObserver.OverstyringIgangsatt) {
        check(event.berørtePerioder.isNotEmpty()) { "Forventet ikke en igangsatt overstyring uten berørte perioder." }
        if (event.årsak == "KORRIGERT_INNTEKTSMELDING") IM.korrigertInntekt()
    }

    private fun PersonObserver.VedtaksperiodeVenterEvent.revurderingFeilet() = gjeldendeTilstander[venterPå.vedtaksperiodeId] == REVURDERING_FEILET
    private fun PersonObserver.VedtaksperiodeVenterEvent.auuVilOmgjøres() =
        vedtaksperiodeId == venterPå.vedtaksperiodeId && gjeldendeTilstander[venterPå.vedtaksperiodeId] == AVSLUTTET_UTEN_UTBETALING && venterPå.venteårsak.hvorfor == "VIL_OMGJØRES"
    private fun PersonObserver.VedtaksperiodeVenterEvent.tilstander() = when (vedtaksperiodeId == venterPå.vedtaksperiodeId) {
        true -> "En vedtaksperiode i ${gjeldendeTilstander[vedtaksperiodeId]} trenger hjelp! 😱"
        false -> "En vedtaksperiode i ${gjeldendeTilstander[vedtaksperiodeId]} venter på en annen vedtaksperiode i ${gjeldendeTilstander[venterPå.vedtaksperiodeId]} som trenger hjelp! 😱"
    }

    internal fun bekreftIngenUgyldigeSituasjoner() {
        bekreftIngenOverlappende()
        bekreftAvsluttetUtenUtbetalingHarLukketBehandling()
        validerSykdomshistorikk()
        IM.bekreftEntydighåndtering()
    }

    private fun validerSykdomshistorikk() {
        arbeidsgivere.forEach { arbeidsgiver ->
            val perioderPerHendelse = arbeidsgiver.inspektør.sykdomshistorikk.inspektør.perioderPerHendelse()
            perioderPerHendelse.forEach { (hendelseId, perioder) ->
                check(!perioder.overlapper()) {
                    "Sykdomshistorikk inneholder overlappende perioder fra hendelse $hendelseId"
                }
            }
        }
    }

    private fun bekreftIngenOverlappende() {
        person.inspektør.vedtaksperioder()
            .filterValues { it.size > 1 }
            .forEach { (orgnr, perioder) ->
                var nåværende = perioder.first().inspektør
                perioder.subList(1, perioder.size).forEach { periode ->
                    val inspektør = periode.inspektør
                    check(!inspektør.periode.overlapperMed(nåværende.periode)) {
                        "For Arbeidsgiver $orgnr overlapper Vedtaksperiode ${inspektør.id} (${inspektør.periode}) og Vedtaksperiode ${nåværende.id} (${nåværende.periode}) med hverandre!"
                    }
                    nåværende = inspektør
                }
            }
    }

    private fun bekreftAvsluttetUtenUtbetalingHarLukketBehandling() {
        person.inspektør.vedtaksperioder()
            .flatMap { (_, perioder) -> perioder }
            .map { it.inspektør }
            .filter { it.tilstand == Vedtaksperiode.AvsluttetUtenUtbetaling }
            .all {
                it.behandlinger.last().let { sisteBehandling ->
                    sisteBehandling.avsluttet != null && sisteBehandling.tilstand == VedtaksperiodeInspektør.Behandling.Behandlingtilstand.AVSLUTTET_UTEN_VEDTAK
                }
            }
    }

    private class Inntektsmeldinger {
        private val signaler = mutableListOf<Signal>()
        fun håndtert() { signaler.add(Signal.HÅNDTERT) }
        fun ikkeHåndtert() { signaler.add(Signal.IKKE_HÅNDTERT) }
        fun førSøknad() { signaler.add(Signal.FØR_SØKNAD) }
        fun korrigertInntekt() { signaler.add(Signal.KORRIGERT_INNTEKT) }
        fun behandlingUtført() = signaler.clear()

        fun bekreftEntydighåndtering() {
            if (signaler.isEmpty()) return // En behandling uten håndtering av inntektsmeldinger 🤤
            val unikeSignaler = signaler.toSet()

            if (Signal.IKKE_HÅNDTERT in signaler) check(unikeSignaler == setOf(Signal.IKKE_HÅNDTERT)) {
                "Signalet om at inntektsmelding ikke er håndtert er sendt i kombinasjon med konflikterende signaler: $signaler"
            }

            if (Signal.FØR_SØKNAD in signaler) check(unikeSignaler == setOf(Signal.FØR_SØKNAD)) {
                "Signalet om at inntektsmelding kom før søknad er sendt i kombinasjon med konflikterende signaler: $signaler"
            }
        }

        private enum class Signal {
            HÅNDTERT,
            IKKE_HÅNDTERT,
            FØR_SØKNAD,
            KORRIGERT_INNTEKT,
        }
    }
}