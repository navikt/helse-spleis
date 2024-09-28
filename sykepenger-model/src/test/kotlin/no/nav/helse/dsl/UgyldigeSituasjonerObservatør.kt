package no.nav.helse.dsl

import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.overlapper
import no.nav.helse.inspectors.BehandlingInspektør.Behandling
import no.nav.helse.inspectors.BehandlingInspektør.Behandling.Behandlingtilstand.TIL_INFOTRYGD
import no.nav.helse.inspectors.BehandlingInspektør.Behandling.Behandlingtilstand.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.inspectors.BehandlingInspektør.Behandling.Behandlingtilstand.VEDTAK_IVERKSATT
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Avsluttet
import no.nav.helse.person.Vedtaksperiode.AvsluttetUtenUtbetaling
import no.nav.helse.person.Vedtaksperiode.TilInfotrygd
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.AktivitetsloggObserver
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
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
            val perioderPerHendelse = arbeidsgiver.inspektør.sykdomshistorikk.perioderPerHendelse()
            perioderPerHendelse.forEach { (hendelseId, perioder) ->
                check(!perioder.overlapper()) {
                    "Sykdomshistorikk inneholder overlappende perioder fra hendelse $hendelseId"
                }
            }
        }
    }

    private fun validerSykdomstidslinjePåBehandlinger() {
        arbeidsgivere.forEach { it.accept(SprøSykdomstidslinjePåEndringer()) }
    }

    private fun validerTilstandPåSisteBehandlingForFerdigbehandledePerioder() {
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.accept(BekreftTilstandPåFerdigbehandlePerioder())
        }
    }

    private fun bekreftIngenOverlappende() {
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.accept(BekreftIngenOverlappendePerioder())
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

    private class SprøSykdomstidslinjePåEndringer : ArbeidsgiverVisitor {
        override fun visitBehandlingendring(
            id: UUID,
            tidsstempel: LocalDateTime,
            sykmeldingsperiode: Periode,
            periode: Periode,
            grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement?,
            utbetaling: Utbetaling?,
            dokumentsporing: Dokumentsporing,
            sykdomstidslinje: Sykdomstidslinje,
            skjæringstidspunkt: LocalDate,
            arbeidsgiverperiode: List<Periode>,
            utbetalingstidslinje: Utbetalingstidslinje,
            maksdatoresultat: Maksdatoresultat
        ) {
            val førsteIkkeUkjenteDag = sykdomstidslinje.firstOrNull { it !is UkjentDag }
            val førsteDag = sykdomstidslinje[periode.start]
            val normalSykdomstidslinje = førsteDag === førsteIkkeUkjenteDag
            if (normalSykdomstidslinje) return
            // Inntektsmeldingen driver selvfølgelig å lager noen ukjente dager i snuten når første fraværsdag blir SykedagNav 🫠
            val førsteIkkeUkjenteDagErSykedagNav = sykdomstidslinje.inspektør.dager[sykdomstidslinje.inspektør.førsteIkkeUkjenteDag] is Dag.SykedagNav
            if (førsteIkkeUkjenteDagErSykedagNav) return

            error("""
                - Nå har det skjedd noe sprøtt.. sykdomstidslinjen starter med UkjentDag.. er du helt sikker på at det er så lurt?
                Sykdomstidslinje: ${sykdomstidslinje.toShortString()}
                Periode på sykdomstidslinje: ${sykdomstidslinje.periode()}
                FørsteIkkeUkjenteDag=${sykdomstidslinje.inspektør.førsteIkkeUkjenteDag}
                Periode på endring: $periode
            """)
        }
    }

    private class BekreftTilstandPåFerdigbehandlePerioder : ArbeidsgiverVisitor {
        private var aktivePerioder = false
        private val perioderFordeltPåTilstand = mutableMapOf<Vedtaksperiode.Vedtaksperiodetilstand, MutableList<Behandling>>()
        private lateinit var forrigeBehandling: Behandling

        private fun Behandling.gyldigTilInfotrygd() = tilstand == TIL_INFOTRYGD && avsluttet != null && vedtakFattet == null
        private fun Behandling.gyldigAvsluttetUtenUtbetaling() = tilstand == AVSLUTTET_UTEN_VEDTAK && avsluttet != null && vedtakFattet == null
        private fun Behandling.gyldigAvsluttet() = tilstand == VEDTAK_IVERKSATT && avsluttet != null && vedtakFattet != null
        private val Behandling.nøkkelinfo get() = "tilstand=$tilstand, avsluttet=$avsluttet, vedtakFattet=$vedtakFattet"

        private fun validerTilstandPåSisteBehandlingForFerdigbehandledePerioder() {
            perioderFordeltPåTilstand.forEach { (tilstand, sisteBehandlinger) ->
                when (tilstand) {
                    TilInfotrygd -> sisteBehandlinger.filterNot { it.gyldigTilInfotrygd() }.let { check(it.isEmpty()) {
                        "Disse ${it.size} periodene i TilInfotrygd har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"}
                    }
                    AvsluttetUtenUtbetaling -> sisteBehandlinger.filterNot { it.gyldigAvsluttetUtenUtbetaling() }.let { check(it.isEmpty()) {
                        "Disse ${it.size} periodene i AvsluttetUtenUtbetaling har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"}
                    }
                    Avsluttet -> sisteBehandlinger.filterNot { it.gyldigAvsluttet() }.let { check(it.isEmpty()) {
                        "Disse ${it.size} periodene i Avsluttet har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"}
                    }
                    else -> error("Svært snedig at perioder i ${tilstand::class.simpleName} er ferdig behandlet")
                }
            }
        }

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            aktivePerioder = true
        }
        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            aktivePerioder = false
            validerTilstandPåSisteBehandlingForFerdigbehandledePerioder()
        }

        override fun postVisitBehandlinger(behandlinger: List<Behandlinger.Behandling>) {
            forrigeBehandling = behandlinger.last().inspektør.behandling
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            hendelseIder: Set<Dokumentsporing>
        ) {
            if (!tilstand.erFerdigBehandlet) return
            perioderFordeltPåTilstand.getOrPut(tilstand) { mutableListOf() }.add(forrigeBehandling)
        }
    }

    private class BekreftIngenOverlappendePerioder : ArbeidsgiverVisitor {
        private var aktivePerioder: Boolean = false

        private var orgnr: String? = null
        private var forrigePeriode: Pair<UUID, Periode>? = null

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String,
            sykdomshistorikk: Sykdomshistorikk
        ) {
            orgnr = organisasjonsnummer
        }

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            aktivePerioder = true
        }
        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            aktivePerioder = false
        }

        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            hendelseIder: Set<Dokumentsporing>,
            egenmeldingsperioder: List<Periode>
        ) {
            if (!aktivePerioder) return
            if (forrigePeriode?.second?.overlapperMed(periode) == true) {
                error("For Arbeidsgiver $orgnr overlapper Vedtaksperiode $id (${periode}) og Vedtaksperiode ${forrigePeriode?.first} (${forrigePeriode?.second}) med hverandre!")
            }
            forrigePeriode = id to periode
        }
    }
}