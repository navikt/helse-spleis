package no.nav.helse.dsl

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.BehandlingView
import no.nav.helse.person.BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.VedtaksperiodeView
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.perioderMedBeløp
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_A_ORDNINGEN
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_UTBETALT
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat.Bestemmelse.IKKE_VURDERT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows

internal class UgyldigeSituasjonerObservatør(private val person: Person) : PersonObserver {

    private val arbeidsgivereMap = mutableMapOf<String, Yrkesaktivitet>()
    private val gjeldendeTilstander = mutableMapOf<UUID, TilstandType>()
    private val gjeldendeBehandlingstatus = mutableMapOf<UUID, MutableList<Pair<LocalDateTime, Behandlingstatus>>>()
    private val arbeidsgivere get() = arbeidsgivereMap.values
    private val IM = Inntektsmeldinger()
    private val søknader = mutableMapOf<UUID, UUID?>() // SøknadId -> VedtaksperiodeId

    private val behandlingOpprettetEventer = mutableListOf<PersonObserver.BehandlingOpprettetEvent>()
    private val behandlingLukketEventer = mutableListOf<PersonObserver.BehandlingLukketEvent>()
    private val behandlingForkastetEventer = mutableListOf<PersonObserver.BehandlingForkastetEvent>()

    init {
        person.addObserver(this)
    }

    private fun loggBehandlingstatus(vedtaksperiodeId: UUID, status: Behandlingstatus) {
        gjeldendeBehandlingstatus.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(0, LocalDateTime.now() to status)
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        check(behandlingOpprettetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut opprettet event"
        }
        behandlingOpprettetEventer.add(event)
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.ÅPEN)
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingLukketEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut lukket event"
        }
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.LUKKET)
    }

    override fun avsluttetMedVedtak(event: PersonObserver.AvsluttetMedVedtakEvent) {
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.AVSLUTTET)
    }

    override fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.AVSLUTTET)
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        loggBehandlingstatus(vedtaksperiodeAnnullertEvent.vedtaksperiodeId, Behandlingstatus.ANNULLERT)
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingForkastetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut forkastet event"
        }
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.AVBRUTT)
    }

    private fun bekreftAtBehandlingFinnes(behandlingId: UUID) {
        val behandlingVarsletOmFør = { id: UUID ->
            behandlingOpprettetEventer.singleOrNull { it.behandlingId == id } != null
        }
        // gjelder tester som tar utgangspunkt i en serialisert personjson
        val behandlingFinnesHosArbeidsgiver = { _: UUID ->
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
        arbeidsgivereMap.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { person.arbeidsgiver(event.yrkesaktivitetssporing.somOrganisasjonsnummer) }
        gjeldendeTilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
    }

    override fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        søknader[søknadId] = null
    }

    override fun vedtaksperioderVenter(eventer: List<PersonObserver.VedtaksperiodeVenterEvent>) = sjekk {
        eventer.forEach { event ->
            sjekkUgyldigeVentesituasjoner(event)
            sjekkSøknadIdEierskap(event.vedtaksperiodeId, event.hendelser)
        }
    }

    private fun sjekkUgyldigeVentesituasjoner(event: PersonObserver.VedtaksperiodeVenterEvent) {
        if (event.venterPå.venteårsak.hva != "HJELP") return // Om vi venter på noe annet enn hjelp er det OK 👍
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
    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, speilrelatert: Boolean) = IM.ikkeHåndtert(inntektsmeldingId)
    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) = IM.førSøknad(event.inntektsmeldingId)
    override fun overstyringIgangsatt(event: PersonObserver.OverstyringIgangsatt) {
        check(event.berørtePerioder.isNotEmpty()) { "Forventet ikke en igangsatt overstyring uten berørte perioder." }
        if (event.årsak == "KORRIGERT_INNTEKTSMELDING") IM.korrigertInntekt(event.meldingsreferanseId)
    }

    private fun PersonObserver.VedtaksperiodeVenterEvent.tilstander() = when (vedtaksperiodeId == venterPå.vedtaksperiodeId) {
        true -> "En vedtaksperiode i ${gjeldendeTilstander[vedtaksperiodeId]} trenger hjelp${venterPå.venteårsak.hvorfor?.let { " fordi $it" } ?: ""}! 😱"
        false -> "En vedtaksperiode i ${gjeldendeTilstander[vedtaksperiodeId]} venter på en annen vedtaksperiode i ${gjeldendeTilstander[venterPå.vedtaksperiodeId]} som trenger${venterPå.venteårsak.hvorfor?.let { " fordi $it" } ?: ""}! 😱"
    }

    override fun behandlingUtført() {
        bekreftIngenUgyldigeSituasjoner()
        IM.behandlingUtført()
    }

    private fun sjekk(block: () -> Unit) {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is UgyldigSituasjonException) throw throwable
            throw UgyldigSituasjonException(throwable)
        }
    }

    private fun bekreftIngenUgyldigeSituasjoner() = sjekk {
        bekreftIngenOverlappende()
        validerSykdomshistorikk()
        validerSykdomstidslinjePåBehandlinger()
        validerTilstandPåSisteBehandlingForFerdigbehandledePerioder()
        validerRefusjonsopplysningerPåBehandlinger()
        validerUtbetalingOgVilkårsgrunnlagPåBehandlinger()
        IM.bekreftEntydighåndtering()
    }

    internal fun bekreftVarselHarKnytningTilVedtaksperiode(varsler: List<Aktivitet.Varsel>) {
        varsler.forEach { aktivitet ->
            // disse opprettes utenfor en vedtaksperiode/eller på en lukket vedtaksperiode 💀
            if (aktivitet.kode in setOf(Varselkode.RV_RV_7)) return@forEach

            val vedtaksperiodekontekst = checkNotNull(aktivitet.kontekster.firstOrNull { it.kontekstType == "Vedtaksperiode" }) {
                "Det er opprettet et varsel utenom Vedtaksperiode:\n${aktivitet}"
            }
            val vedtaksperiodeId = UUID.fromString(vedtaksperiodekontekst.kontekstMap.getValue("vedtaksperiodeId"))
            val behandlingstatusPåTidspunkt = gjeldendeBehandlingstatus
                .getValue(vedtaksperiodeId)
                .firstOrNull { (tidspunkt, _) -> tidspunkt < aktivitet.tidsstempel }?.second
                ?: error("Finner ikke behandling forut før varselstidspunktet (vedtaksperiode $vedtaksperiodeId)")
            check(behandlingstatusPåTidspunkt == Behandlingstatus.ÅPEN) {
                "Det er opprettet et varsel (${aktivitet.melding}) utenom en åpen behandling (status = $behandlingstatusPåTidspunkt)"
            }
        }
    }

    private fun validerSykdomshistorikk() {
        arbeidsgivere.forEach { arbeidsgiver ->
            val perioderPerHendelse = arbeidsgiver.view().sykdomshistorikk.inspektør.perioderPerHendelse()
            perioderPerHendelse.forEach { (_, sykdomstidslinjer) ->
                check(sykdomstidslinjer.none { sykdomstidslinje ->
                    sykdomstidslinjer.filterNot { it === sykdomstidslinje }.any { it == sykdomstidslinje }
                }) {
                    "Samme hendelse er blitt lagt til flere ganger med lik sykdomstidslinje"
                }
            }
        }
    }

    private fun validerSykdomstidslinjePåBehandlinger() {
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.view().aktiveVedtaksperioder.forEach { aktivVedtaksperiode ->
                aktivVedtaksperiode.behandlinger.behandlinger.forEach { behandling ->
                    behandling.endringer.forEach {
                        val førsteIkkeUkjenteDag = it.sykdomstidslinje.firstOrNull { dag -> dag !is UkjentDag }
                        val førsteDag = it.sykdomstidslinje[it.periode.start]
                        val normalSykdomstidslinje = førsteDag === førsteIkkeUkjenteDag
                        if (normalSykdomstidslinje) return

                        error(
                            """
                - Nå har det skjedd noe sprøtt.. sykdomstidslinjen starter med UkjentDag.. er du helt sikker på at det er så lurt?
                Sykdomstidslinje: ${it.sykdomstidslinje.toShortString()}
                Periode på sykdomstidslinje: ${it.sykdomstidslinje.periode()}
                FørsteIkkeUkjenteDag=${it.sykdomstidslinje.inspektør.førsteIkkeUkjenteDag}
                Periode på endring: ${it.periode}
            """
                        )
                    }
                }
            }
        }
    }

    private fun validerRefusjonsopplysningerPåBehandlinger() {
        arbeidsgivere.map { it.view() }.forEach { arbeidsgiver ->
            if (arbeidsgiver.yrkesaktivitetssporing !is Behandlingsporing.Yrkesaktivitet.Arbeidstaker) return@forEach
            arbeidsgiver.aktiveVedtaksperioder.forEach { vedtaksperiode ->
                vedtaksperiode.behandlinger.behandlinger.forEach behandling@{ behandling ->
                    behandling.endringer.last().let { endring ->
                        if (endring.refusjonstidslinje.isEmpty()) {
                            if (behandling.tilstand == AVSLUTTET_UTEN_VEDTAK) return@behandling // Ikke noe refusjonsopplysning på AUU er OK
                            if (vedtaksperiode.tilstand == AVVENTER_BLOKKERENDE_PERIODE && !vedtaksperiode.skalBehandlesISpeil) return@behandling // Dette kan være AUU'er som skal tilbake til AUU, de må ikke ha refusjonsopplysninger.
                            if (vedtaksperiode.tilstand in setOf(AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_A_ORDNINGEN)) return@behandling// Ikke fått refusjonsopplysninger enda da
                            error("Burde ikke ha tom refusjonstidslinje i tilstand ${vedtaksperiode.tilstand}")
                        }
                        val perioder = endring.refusjonstidslinje.perioderMedBeløp
                        check(perioder.size == 1) { "Burde ikke være noen hull i refusjonstidslinjen." }
                        check(perioder.single() == endring.periode) { "Refusjonstidslinjen skal dekke hele perioden. Perioden er ${endring.periode}, refusjonsopplysninger for $perioder" }
                    }
                }
            }
        }
    }

    private fun validerUtbetalingOgVilkårsgrunnlagPåBehandlinger() {
        arbeidsgivere.map { it.view() }.forEach { arbeidsgiver ->
            arbeidsgiver.aktiveVedtaksperioder.forEach { vedtaksperiode ->
                vedtaksperiode.behandlinger.behandlinger.forEach { behandling ->
                    behandling.endringer.last().let { endring ->
                        when (behandling.tilstand) {
                            BehandlingView.TilstandView.BEREGNET,
                            BehandlingView.TilstandView.BEREGNET_OMGJØRING,
                            BehandlingView.TilstandView.BEREGNET_ANNULLERING,
                            BehandlingView.TilstandView.BEREGNET_REVURDERING -> {
                                assertNotNull(endring.utbetaling) { "forventer utbetaling i ${behandling.tilstand}" }
                                assertNotNull(endring.grunnlagsdata) { "forventer vilkårsgrunnlag i ${behandling.tilstand}" }
                                assertEquals(IKKE_UTBETALT, endring.utbetaling!!.inspektør.tilstand) { "forventer at utbetaling i behandlingstilstand ${behandling.tilstand} skal være IKKE_UTBETALT, men var ${endring.utbetaling.inspektør.tilstand}" }
                            }

                            BehandlingView.TilstandView.REVURDERT_VEDTAK_AVVIST,
                            BehandlingView.TilstandView.VEDTAK_FATTET,
                            BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                            BehandlingView.TilstandView.VEDTAK_IVERKSATT,
                            BehandlingView.TilstandView.ANNULLERT_PERIODE -> {
                                assertNotNull(endring.utbetaling) { "forventer utbetaling i ${behandling.tilstand}" }
                                assertNotNull(endring.grunnlagsdata) { "forventer vilkårsgrunnlag i ${behandling.tilstand}" }
                            }

                            BehandlingView.TilstandView.TIL_INFOTRYGD,
                            BehandlingView.TilstandView.UBEREGNET,
                            BehandlingView.TilstandView.UBEREGNET_OMGJØRING,
                            BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                            BehandlingView.TilstandView.UBEREGNET_REVURDERING -> {
                                assertNull(endring.utbetaling) { "forventer ingen utbetaling i ${behandling.tilstand}" }
                                assertNull(endring.grunnlagsdata) { "forventer inget vilkårsgrunnlag i ${behandling.tilstand}" }
                                assertEquals(IKKE_VURDERT, endring.maksdatoresultat.bestemmelse) { "forventer maksdatoresultat IKKE_VURDERT i ${behandling.tilstand}" }
                                assertTrue(endring.utbetalingstidslinje.isEmpty()) { "forventer tom utbetalingstidslinje i ${behandling.tilstand}" }
                            }

                            BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK -> {
                                assertNull(endring.utbetaling) { "forventer ingen utbetaling i ${behandling.tilstand}" }
                                assertNull(endring.grunnlagsdata) { "forventer inget vilkårsgrunnlag i ${behandling.tilstand}" }
                                assertEquals(IKKE_VURDERT, endring.maksdatoresultat.bestemmelse) { "forventer maksdatoresultat IKKE_VURDERT i ${behandling.tilstand}" }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun BehandlingView.gyldigTilInfotrygd() = tilstand == BehandlingView.TilstandView.TIL_INFOTRYGD && avsluttet != null && vedtakFattet == null
    private fun BehandlingView.gyldigAvsluttetUtenUtbetaling() = tilstand == AVSLUTTET_UTEN_VEDTAK && avsluttet != null && vedtakFattet == null
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
                        TilstandType.TIL_INFOTRYGD -> sisteBehandlinger.filterNot { it.gyldigTilInfotrygd() }.let {
                            check(it.isEmpty()) {
                                "Disse ${it.size} periodene i TilInfotrygd har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"
                            }
                        }

                        TilstandType.AVSLUTTET_UTEN_UTBETALING -> sisteBehandlinger.filterNot { it.gyldigAvsluttetUtenUtbetaling() }.let {
                            check(it.isEmpty()) {
                                "Disse ${it.size} periodene i AvsluttetUtenUtbetaling har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"
                            }
                        }

                        TilstandType.AVSLUTTET -> sisteBehandlinger.filterNot { it.gyldigAvsluttet() }.let {
                            check(it.isEmpty()) {
                                "Disse ${it.size} periodene i Avsluttet har sine siste behandlinger i snedige tilstander: ${it.map { behandling -> behandling.nøkkelinfo }}}"
                            }
                        }

                        else -> error("Svært snedig at perioder i ${tilstand::class.simpleName} er ferdig behandlet")
                    }
                }
        }
    }

    private fun bekreftIngenOverlappende() {
        arbeidsgivere.forEach { arbeidsgiver ->
            var kanskjeForrigePeriode: VedtaksperiodeView? = null
            val view = arbeidsgiver.view()
            view.aktiveVedtaksperioder.forEach { current ->
                kanskjeForrigePeriode?.also { forrigePeriode ->
                    if (forrigePeriode.periode.overlapperMed(current.periode)) {
                        error("For Arbeidsgiver ${view.organisasjonsnummer} overlapper Vedtaksperiode ${current.id} (${current.periode}) og Vedtaksperiode ${forrigePeriode.id} (${forrigePeriode.periode}) med hverandre!")
                    }
                }
                kanskjeForrigePeriode = current
            }
        }
    }

    private class Inntektsmeldinger {
        private val signaler = mutableMapOf<UUID, MutableList<Signal>>()
        fun håndtert(inntektsmeldingId: UUID) {
            signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.HÅNDTERT)
        }

        fun ikkeHåndtert(inntektsmeldingId: UUID) {
            signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.IKKE_HÅNDTERT)
        }

        fun førSøknad(inntektsmeldingId: UUID) {
            signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.FØR_SØKNAD)
        }

        fun korrigertInntekt(inntektsmeldingId: UUID) {
            signaler.getOrPut(inntektsmeldingId) { mutableListOf() }.add(Signal.KORRIGERT_INNTEKT)
        }

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

    internal companion object {
        internal class UgyldigSituasjonException(cause: Throwable) : Throwable(cause.message, cause)

        internal fun assertUgyldigSituasjon(forventetUgyldigSituasjon: String, block: () -> Unit) {
            val ugyldigSituasjon = assertThrows<UgyldigSituasjonException> { block() }.message
            assertTrue(ugyldigSituasjon?.contains(forventetUgyldigSituasjon) == true) {
                "Forventet ugyldig situasjon '$forventetUgyldigSituasjon', men var '$ugyldigSituasjon'"
            }
        }
    }
}
