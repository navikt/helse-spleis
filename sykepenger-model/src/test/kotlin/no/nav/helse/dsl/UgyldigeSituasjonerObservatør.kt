package no.nav.helse.dsl

import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.overlapper
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.BehandlingView
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.VedtaksperiodeView
import no.nav.helse.person.aktivitetslogg.AktivitetsloggObserver
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import kotlin.check
import kotlin.checkNotNull
import kotlin.collections.any
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.filterNot
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.collections.getOrPut
import kotlin.collections.getValue
import kotlin.collections.intersect
import kotlin.collections.isNotEmpty
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.none
import kotlin.collections.set
import kotlin.collections.setOf
import kotlin.collections.singleOrNull
import kotlin.collections.toSet
import kotlin.error
import kotlin.let

internal class UgyldigeSituasjonerObservatør(private val person: Person): PersonObserver, AktivitetsloggObserver {

    private val arbeidsgivereMap = mutableMapOf<String, Arbeidsgiver>()
    private val gjeldendeTilstander = mutableMapOf<UUID, TilstandType>()
    private val gjeldendeBehandlingstatus = mutableMapOf<UUID, Behandlingstatus>()
    private val arbeidsgivere get() = arbeidsgivereMap.values
    private val IM = Inntektsmeldinger()
    private val søknader = mutableMapOf<UUID, UUID?>() // SøknadId -> VedtaksperiodeId

    private val behandlingOpprettetEventer = mutableListOf<PersonObserver.BehandlingOpprettetEvent>()
    private val behandlingLukketEventer = mutableListOf<PersonObserver.BehandlingLukketEvent>()
    private val behandlingForkastetEventer = mutableListOf<PersonObserver.BehandlingForkastetEvent>()

    init {
        person.addObserver(this)
    }

    override fun aktivitet(
        id: UUID,
        label: Char,
        melding: String,
        kontekster: List<SpesifikkKontekst>,
        tidsstempel: LocalDateTime
    ) {}

    override fun funksjonellFeil(
        id: UUID,
        label: Char,
        kode: Varselkode,
        melding: String,
        kontekster: List<SpesifikkKontekst>,
        tidsstempel: LocalDateTime
    ) {
    }

    override fun varsel(
        id: UUID,
        label: Char,
        kode: Varselkode?,
        melding: String,
        kontekster: List<SpesifikkKontekst>,
        tidsstempel: LocalDateTime
    ) {
        val vedtaksperiodekontekst = checkNotNull(kontekster.firstOrNull { it.kontekstType == "Vedtaksperiode" }) {
            "Det er opprettet et varsel utenom Vedtaksperiode"
        }
        val vedtaksperiodeId = UUID.fromString(vedtaksperiodekontekst.kontekstMap.getValue("vedtaksperiodeId"))
        check(gjeldendeBehandlingstatus[vedtaksperiodeId] == Behandlingstatus.ÅPEN) {
            "Det er opprettet et varsel utenom en åpen behandling"
        }
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        check(behandlingOpprettetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut opprettet event"
        }
        behandlingOpprettetEventer.add(event)
        gjeldendeBehandlingstatus[event.vedtaksperiodeId] = Behandlingstatus.ÅPEN
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingLukketEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut lukket event"
        }
        gjeldendeBehandlingstatus[event.vedtaksperiodeId] = Behandlingstatus.LUKKET
    }

    override fun avsluttetMedVedtak(event: PersonObserver.AvsluttetMedVedtakEvent) {
        gjeldendeBehandlingstatus[event.vedtaksperiodeId] = Behandlingstatus.AVSLUTTET
    }

    override fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        gjeldendeBehandlingstatus[event.vedtaksperiodeId] = Behandlingstatus.AVSLUTTET
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        gjeldendeBehandlingstatus[vedtaksperiodeAnnullertEvent.vedtaksperiodeId] = Behandlingstatus.ANNULLERT
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingForkastetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut forkastet event"
        }
        gjeldendeBehandlingstatus[event.vedtaksperiodeId] = Behandlingstatus.AVBRUTT
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

    override fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        søknader[søknadId] = null
    }

    override fun vedtaksperiodeVenter(event: PersonObserver.VedtaksperiodeVenterEvent) {
        sjekkUgyldigeVentesituasjoner(event)
        sjekkSøknadIdEierskap(event.vedtaksperiodeId, event.hendelser)
    }

    private fun sjekkUgyldigeVentesituasjoner(event: PersonObserver.VedtaksperiodeVenterEvent) {
        // En linje å kommentere inn om man kjeder seg 🫠
        //if (event.trengerNyInntektsmeldingEtterFlyttetSkjæringstidspunkt()) error("vedtaksperiode på ${event.organisasjonsnummer} venter på ${event.venterPå}")
        if (event.venterPå.venteårsak.hva != "HJELP") return // Om vi venter på noe annet enn hjelp er det OK 👍
        if (event.revurderingFeilet()) return // For tester som ender opp i revurdering feilet er det riktig at vi trenger hjelp 🛟
        if (event.venterPå.venteårsak.hvorfor == "FLERE_SKJÆRINGSTIDSPUNKT") return // Dette kan skje :(
        """
        Har du endret/opprettet en vedtaksperiodetilstand uten å vurdere konsekvensene av 'venteårsak'? 
        Eller har du klart å skriv en test vi ikke støtter? 
        ${event.tilstander()}
        $event
        """.let { throw IllegalStateException(it) }
    }

    private fun sjekkSøknadIdEierskap(vedtaksperiodeId: UUID, hendelseIder: Set<UUID>) {
        val søknadIder = hendelseIder.intersect(søknader.keys)
        søknadIder.forEach { søknadId ->
            val eier = søknader[søknadId]
            if (eier == null) søknader[søknadId] = vedtaksperiodeId
            else check(eier == vedtaksperiodeId) { "Både vedtaksperiode $eier og $vedtaksperiodeId peker på søknaden $søknadId" }
        }
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) = IM.håndtert(inntektsmeldingId)
    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, harPeriodeInnenfor16Dager: Boolean) = IM.ikkeHåndtert(inntektsmeldingId)
    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) = IM.førSøknad(event.inntektsmeldingId)
    override fun overstyringIgangsatt(event: PersonObserver.OverstyringIgangsatt) {
        check(event.berørtePerioder.isNotEmpty()) { "Forventet ikke en igangsatt overstyring uten berørte perioder." }
        if (event.årsak == "KORRIGERT_INNTEKTSMELDING") IM.korrigertInntekt(event.meldingsreferanseId)
    }

    private fun PersonObserver.VedtaksperiodeVenterEvent.revurderingFeilet() = gjeldendeTilstander[venterPå.vedtaksperiodeId] == REVURDERING_FEILET
    private fun PersonObserver.VedtaksperiodeVenterEvent.tilstander() = when (vedtaksperiodeId == venterPå.vedtaksperiodeId) {
        true -> "En vedtaksperiode i ${gjeldendeTilstander[vedtaksperiodeId]} trenger hjelp! 😱"
        false -> "En vedtaksperiode i ${gjeldendeTilstander[vedtaksperiodeId]} venter på en annen vedtaksperiode i ${gjeldendeTilstander[venterPå.vedtaksperiodeId]} som trenger hjelp! 😱"
    }

    internal fun bekreftIngenUgyldigeSituasjoner() {
        bekreftIngenOverlappende()
        validerSykdomshistorikk()
        validerSykdomstidslinjePåBehandlinger()
        validerTilstandPåSisteBehandlingForFerdigbehandledePerioder()
        IM.bekreftEntydighåndtering()
    }

    private fun validerSykdomshistorikk() {
        arbeidsgivere.forEach { arbeidsgiver ->
            val perioderPerHendelse = arbeidsgiver.view().sykdomshistorikk.inspektør.perioderPerHendelse()
            perioderPerHendelse.forEach { (hendelseId, perioder) ->
                check(!perioder.overlapper()) {
                    "Sykdomshistorikk inneholder overlappende perioder fra hendelse $hendelseId"
                }
            }
        }
    }

    private fun validerSykdomstidslinjePåBehandlinger() {
        arbeidsgivere.forEach {
            it.view().aktiveVedtaksperioder.forEach {
                it.behandlinger.behandlinger.forEach {
                    it.endringer.forEach {
                        val førsteIkkeUkjenteDag = it.sykdomstidslinje.firstOrNull { it !is UkjentDag }
                        val førsteDag = it.sykdomstidslinje[it.periode.start]
                        val normalSykdomstidslinje = førsteDag === førsteIkkeUkjenteDag
                        if (normalSykdomstidslinje) return
                        // Inntektsmeldingen driver selvfølgelig å lager noen ukjente dager i snuten når første fraværsdag blir SykedagNav 🫠
                        val førsteIkkeUkjenteDagErSykedagNav = it.sykdomstidslinje.inspektør.dager[it.sykdomstidslinje.inspektør.førsteIkkeUkjenteDag] is Dag.SykedagNav
                        if (førsteIkkeUkjenteDagErSykedagNav) return

                        error("""
                - Nå har det skjedd noe sprøtt.. sykdomstidslinjen starter med UkjentDag.. er du helt sikker på at det er så lurt?
                Sykdomstidslinje: ${it.sykdomstidslinje.toShortString()}
                Periode på sykdomstidslinje: ${it.sykdomstidslinje.periode()}
                FørsteIkkeUkjenteDag=${it.sykdomstidslinje.inspektør.førsteIkkeUkjenteDag}
                Periode på endring: ${it.periode}
            """)
                    }
                }
            }
        }
    }

    private fun BehandlingView.gyldigTilInfotrygd() = tilstand == BehandlingView.TilstandView.TIL_INFOTRYGD && avsluttet != null && vedtakFattet == null
    private fun BehandlingView.gyldigAvsluttetUtenUtbetaling() = tilstand == BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK && avsluttet != null && vedtakFattet == null
    private fun BehandlingView.gyldigAvsluttet() = tilstand == BehandlingView.TilstandView.VEDTAK_IVERKSATT && avsluttet != null && vedtakFattet != null
    private val BehandlingView.nøkkelinfo get() = "tilstand=$tilstand, avsluttet=$avsluttet, vedtakFattet=$vedtakFattet"

    private fun validerTilstandPåSisteBehandlingForFerdigbehandledePerioder() {
        arbeidsgivere.forEach { arbeidsgiver ->
            val view = arbeidsgiver.view()

            view.aktiveVedtaksperioder
                .filter { it.tilstand in setOf(TilstandType.AVSLUTTET, TilstandType.AVSLUTTET_UTEN_UTBETALING, TilstandType.TIL_INFOTRYGD) }
                .groupBy(keySelector = { it.tilstand }) {
                    it.behandlinger.behandlinger.last()
                }
                .forEach { (tilstand, sisteBehandlinger) ->
                    when (tilstand) {
                        TilstandType.TIL_INFOTRYGD -> sisteBehandlinger.filterNot { it.gyldigTilInfotrygd() }.let { check(it.isEmpty()) {
                            "Disse ${it.size} periodene i TilInfotrygd har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"}
                        }
                        TilstandType.AVSLUTTET_UTEN_UTBETALING -> sisteBehandlinger.filterNot { it.gyldigAvsluttetUtenUtbetaling() }.let { check(it.isEmpty()) {
                            "Disse ${it.size} periodene i AvsluttetUtenUtbetaling har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"}
                        }
                        TilstandType.AVSLUTTET -> sisteBehandlinger.filterNot { it.gyldigAvsluttet() }.let { check(it.isEmpty()) {
                            "Disse ${it.size} periodene i Avsluttet har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"}
                        }
                        else -> error("Svært snedig at perioder i ${tilstand::class.simpleName} er ferdig behandlet")
                    }
                }
        }
    }

    private fun bekreftIngenOverlappende() {
        arbeidsgivere.forEach { arbeidsgiver ->
            var forrigePeriode: VedtaksperiodeView? = null
            val view = arbeidsgiver.view()
            view.aktiveVedtaksperioder.forEach { current ->
                if (forrigePeriode?.periode?.overlapperMed(current.periode) == true) {
                    error("For Arbeidsgiver ${view.organisasjonsnummer} overlapper Vedtaksperiode ${current.id} (${current.periode}) og Vedtaksperiode ${forrigePeriode.id} (${forrigePeriode.periode}) med hverandre!")
                }
                forrigePeriode = current
            }
        }
    }

    private class Inntektsmeldinger {
        private val signaler = mutableMapOf<UUID, MutableList<Signal>>()

        fun håndtert(inntektsmeldingId: UUID) { signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.HÅNDTERT) }
        fun ikkeHåndtert(inntektsmeldingId: UUID) { signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.IKKE_HÅNDTERT) }
        fun førSøknad(inntektsmeldingId: UUID) { signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.FØR_SØKNAD) }
        fun korrigertInntekt(inntektsmeldingId: UUID) { signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.KORRIGERT_INNTEKT) }
        fun behandlingUtført() = signaler.clear()

        fun bekreftEntydighåndtering() {
            if (signaler.isEmpty()) return // En behandling uten håndtering av inntektsmeldinger 🤤
            signaler.forEach { (_, signaler) ->
                val unikeSignaler = signaler.toSet()

                if (Signal.IKKE_HÅNDTERT in signaler) check(unikeSignaler == setOf(Signal.IKKE_HÅNDTERT)) {
                    "Signalet om at inntektsmelding ikke er håndtert er sendt i kombinasjon med konflikterende signaler: $signaler"
                }

                if (Signal.FØR_SØKNAD in signaler) check(unikeSignaler == setOf(Signal.FØR_SØKNAD)) {
                    "Signalet om at inntektsmelding kom før søknad er sendt i kombinasjon med konflikterende signaler: $signaler"
                }
            }
        }

        private enum class Signal {
            HÅNDTERT,
            IKKE_HÅNDTERT,
            FØR_SØKNAD,
            KORRIGERT_INNTEKT,
        }
    }

    private enum class Behandlingstatus {
        ÅPEN, LUKKET, AVBRUTT, ANNULLERT, AVSLUTTET
    }
}