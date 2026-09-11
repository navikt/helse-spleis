package no.nav.helse.dsl

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Behandlinger.Behandling.Tilstand
import no.nav.helse.person.Person
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.perioderMedBeløp
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_UTBETALT
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat.Bestemmelse.IKKE_VURDERT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows

internal class UgyldigeSituasjonerObservatør(private val person: Person) : EventSubscription {

    private val arbeidsgivereMap = mutableMapOf<String, Yrkesaktivitet>()
    private val gjeldendeTilstander = mutableMapOf<UUID, TilstandType>()
    private val gjeldendeBehandlingstatus = mutableMapOf<UUID, MutableList<Pair<LocalDateTime, Behandlingstatus>>>()
    private val arbeidsgivere get() = arbeidsgivereMap.values
    private val IM = Inntektsmeldinger()
    private val søknader = mutableMapOf<UUID, UUID?>() // SøknadId -> VedtaksperiodeId

    private val behandlingOpprettetEventer = mutableListOf<EventSubscription.BehandlingOpprettetEvent>()
    private val behandlingLukketEventer = mutableListOf<EventSubscription.BehandlingLukketEvent>()
    private val behandlingForkastetEventer = mutableListOf<EventSubscription.BehandlingForkastetEvent>()

    private fun loggBehandlingstatus(vedtaksperiodeId: UUID, status: Behandlingstatus) {
        gjeldendeBehandlingstatus.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(0, LocalDateTime.now() to status)
    }

    private val kvittertUtArbeidsgiveropplysninger = mutableSetOf<UUID>()

    override fun nyBehandling(event: EventSubscription.BehandlingOpprettetEvent) {
        check(behandlingOpprettetEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut opprettet event"
        }
        behandlingOpprettetEventer.add(event)
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.ÅPEN)
    }

    override fun behandlingLukket(event: EventSubscription.BehandlingLukketEvent) {
        bekreftAtBehandlingFinnes(event.behandlingId)
        check(behandlingLukketEventer.none { it.behandlingId == event.behandlingId }) {
            "behandling ${event.behandlingId} har allerede sendt ut lukket event"
        }
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.LUKKET)
    }

    override fun avsluttetMedVedtak(event: EventSubscription.AvsluttetMedVedtakEvent) {
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.AVSLUTTET)
    }

    override fun avsluttetUtenVedtak(event: EventSubscription.AvsluttetUtenVedtakEvent) {
        loggBehandlingstatus(event.vedtaksperiodeId, Behandlingstatus.AVSLUTTET)
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: EventSubscription.VedtaksperiodeAnnullertEvent) {
        loggBehandlingstatus(vedtaksperiodeAnnullertEvent.vedtaksperiodeId, Behandlingstatus.ANNULLERT)
    }

    override fun behandlingForkastet(event: EventSubscription.BehandlingForkastetEvent) {
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
        event: EventSubscription.VedtaksperiodeEndretEvent
    ) {
        arbeidsgivereMap.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { person.arbeidsgiver(event.yrkesaktivitetssporing.somOrganisasjonsnummer) }
        gjeldendeTilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
    }

    override fun søknadHåndtert(event: EventSubscription.SøknadHåndtertEvent) {
        søknader[event.meldingsreferanseId] = null
    }

    override fun vedtaksperioderVenter(event: EventSubscription.VedtaksperioderVenterEvent) = sjekk {
        event.vedtaksperioder.forEach { event ->
            sjekkUgyldigeVentesituasjoner(event)
            sjekkSøknadIdEierskap(event.vedtaksperiodeId, event.hendelser)
        }
    }

    private fun sjekkUgyldigeVentesituasjoner(event: EventSubscription.VedtaksperiodeVenterEvent) {
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

    override fun trengerArbeidsgiveropplysninger(event: EventSubscription.TrengerArbeidsgiveropplysningerEvent) {
        if (event.opplysninger.vedtaksperiodeId in kvittertUtArbeidsgiveropplysninger) {
            throw UgyldigSituasjonException(IllegalStateException("Vedtaksperioden har allerede kvittert ut arbeidsgiveropplysninger! Hvorfor blir det forespurt på ny?\n\t${event}"))
        }
    }

    override fun trengerIkkeArbeidsgiveropplysninger(event: EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent) {
        kvittertUtArbeidsgiveropplysninger.add(event.vedtaksperiodeId)
    }

    override fun inntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent) {
        kvittertUtArbeidsgiveropplysninger.add(event.vedtaksperiodeId)
        IM.håndtert(event.meldingsreferanseId)
    }
    override fun inntektsmeldingIkkeHåndtert(event: EventSubscription.InntektsmeldingIkkeHåndtertEvent) = IM.ikkeHåndtert(event.meldingsreferanseId)
    override fun inntektsmeldingFørSøknad(event: EventSubscription.InntektsmeldingFørSøknadEvent) = IM.førSøknad(event.inntektsmeldingId)
    override fun overstyringIgangsatt(event: EventSubscription.OverstyringIgangsatt) {
        check(event.berørtePerioder.isNotEmpty()) { "Forventet ikke en igangsatt overstyring uten berørte perioder." }
        if (event.årsak == "KORRIGERT_INNTEKTSMELDING") IM.korrigertInntekt(event.meldingsreferanseId)
    }

    private fun EventSubscription.VedtaksperiodeVenterEvent.tilstander() = when (vedtaksperiodeId == venterPå.vedtaksperiodeId) {
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
        bekreftTilstandPåSisteBehandlingForForkastedePerioder()
        validerRefusjonsopplysningerPåBehandlinger()
        validerUtbetalingOgVilkårsgrunnlagPåBehandlinger()
        validerBeregningIder()
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
            val perioderPerHendelse = arbeidsgiver.sykdomshistorikk.inspektør.perioderPerHendelse()
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
            arbeidsgiver.vedtaksperioder.forEach { aktivVedtaksperiode ->
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
        arbeidsgivere.forEach { arbeidsgiver ->
            if (arbeidsgiver.yrkesaktivitetstype !is Behandlingsporing.Yrkesaktivitet.Arbeidstaker) return@forEach
            arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                vedtaksperiode.behandlinger.behandlinger.forEach behandling@{ behandling ->
                    behandling.endringer.last().let { endring ->
                        if (endring.refusjonstidslinje.isEmpty()) {
                            if (behandling.tilstand == Tilstand.AvsluttetUtenVedtak) return@behandling // Ikke noe refusjonsopplysning på AUU er OK
                            if (vedtaksperiode.tilstand.type == AVVENTER_AVSLUTTET_UTEN_UTBETALING) return@behandling // Dette ER være AUU'er som skal tilbake til AUU, de må ikke ha refusjonsopplysninger.
                            if (vedtaksperiode.tilstand.type in setOf(AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)) return@behandling// Ikke fått refusjonsopplysninger enda da
                            error("Burde ikke ha tom refusjonstidslinje i tilstand ${vedtaksperiode.tilstand.type}")
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
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                vedtaksperiode.behandlinger.behandlinger.forEach { behandling ->
                    behandling.endringer.last().let { endring ->
                        when (behandling.tilstand) {
                            Tilstand.Beregnet,
                            Tilstand.BeregnetOmgjøring,
                            Tilstand.BeregnetRevurdering -> {
                                assertNotNull(endring.utbetaling) { "forventer utbetaling i ${behandling.tilstand}" }
                                assertNotNull(endring.grunnlagsdata) { "forventer vilkårsgrunnlag i ${behandling.tilstand}" }
                                assertEquals(IKKE_UTBETALT, endring.utbetaling!!.inspektør.tilstand) { "forventer at utbetaling i behandlingstilstand ${behandling.tilstand} skal være IKKE_UTBETALT, men var ${endring.utbetaling.inspektør.tilstand}" }
                            }

                            Tilstand.RevurdertVedtakAvvist,
                            Tilstand.VedtakFattet,
                            Tilstand.OverførtAnnullering,
                            Tilstand.VedtakIverksatt,
                            Tilstand.AnnullertPeriode -> {
                                assertNotNull(endring.utbetaling) { "forventer utbetaling i ${behandling.tilstand}" }
                                assertNotNull(endring.grunnlagsdata) { "forventer vilkårsgrunnlag i ${behandling.tilstand}" }
                            }

                            Tilstand.TilInfotrygd,
                            Tilstand.Uberegnet,
                            Tilstand.UberegnetOmgjøring,
                            Tilstand.UberegnetAnnullering,
                            Tilstand.UberegnetRevurdering -> {
                                assertNull(endring.utbetaling) { "forventer ingen utbetaling i ${behandling.tilstand}" }
                                assertNull(endring.grunnlagsdata) { "forventer inget vilkårsgrunnlag i ${behandling.tilstand}" }
                                assertEquals(IKKE_VURDERT, endring.maksdatoresultat.bestemmelse) { "forventer maksdatoresultat IKKE_VURDERT i ${behandling.tilstand}" }
                                assertTrue(endring.utbetalingstidslinje.isEmpty()) { "forventer tom utbetalingstidslinje i ${behandling.tilstand}" }
                            }

                            Tilstand.AvsluttetUtenVedtak -> {
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

    private fun validerBeregningIder() {
        val oppbrukteBeregningIder = mutableSetOf<UUID>()
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                vedtaksperiode.behandlinger.behandlinger.forEach { behandling ->
                    var forrigeBeregningId: UUID? = null
                    var forrigeUtbetalingId: UUID? = null
                    behandling.endringer.forEach { endring ->
                        if (forrigeBeregningId == null) { // første endring
                            forrigeBeregningId = endring.beregningId
                            return@forEach
                        }

                        check(endring.beregningId !in oppbrukteBeregningIder) { "Forventer ikke en gjenoppstått beregningId" }

                        if (endring.utbetaling != null) {
                            if (endring.utbetaling.id != forrigeUtbetalingId && forrigeUtbetalingId != null) {
                                check(endring.beregningId != forrigeBeregningId) { "Forventer ulike beregningIder ved ulike utbetalingIder for arbeidsgiver ${arbeidsgiver.organisasjonsnummer} for periode ${endring.periode}" }
                                oppbrukteBeregningIder.add(forrigeBeregningId)
                            } else {
                                check(endring.beregningId == forrigeBeregningId) { "Forventer like beregningIder ved like utbetalingIder for arbeidsgiver ${arbeidsgiver.organisasjonsnummer} for periode ${endring.periode}" }
                            }
                        } else {
                            if (forrigeUtbetalingId != null) {
                                check(endring.beregningId != forrigeBeregningId) { "Forventer ulike beregningIder når forrige endring hadde utbetaling for arbeidsgiver ${arbeidsgiver.organisasjonsnummer} for periode ${endring.periode}" }
                                oppbrukteBeregningIder.add(forrigeBeregningId)
                            } else {
                                check(endring.beregningId == forrigeBeregningId) { "Forventer like beregningIder når forrige endring ikke hadde utbetaling og gjeldende ikke har utbetaling for arbeidsgiver ${arbeidsgiver.organisasjonsnummer} for periode ${endring.periode}" }
                            }
                        }
                        forrigeBeregningId = endring.beregningId
                        forrigeUtbetalingId = endring.utbetaling?.id
                    }
                }
            }
        }
    }

    private fun Behandlinger.Behandling.gyldigTilInfotrygd() = tilstand == Tilstand.TilInfotrygd && avsluttet != null && vedtakFattet == null
    private fun Behandlinger.Behandling.gyldigAvsluttetUtenUtbetaling() = tilstand == Tilstand.AvsluttetUtenVedtak && avsluttet != null && vedtakFattet == null
    private fun Behandlinger.Behandling.gyldigAvsluttet() = tilstand == Tilstand.VedtakIverksatt && avsluttet != null && vedtakFattet != null
    private val Behandlinger.Behandling.nøkkelinfo get() = "tilstand=$tilstand, avsluttet=$avsluttet, vedtakFattet=$vedtakFattet"
    private fun validerTilstandPåSisteBehandlingForFerdigbehandledePerioder() {
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder
                .filter { it.tilstand.type in setOf(TilstandType.AVSLUTTET, TilstandType.AVSLUTTET_UTEN_UTBETALING, TilstandType.TIL_INFOTRYGD) }
                .groupBy(keySelector = { it.tilstand.type }) {
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

    private fun bekreftTilstandPåSisteBehandlingForForkastedePerioder() {
        arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.forkastede.map { it.vedtaksperiode }.forEach { forkastetPeriode ->
                check(forkastetPeriode.tilstand.type == TilstandType.TIL_INFOTRYGD) {
                    "Forventet at forkastet vedtaksperiode ${forkastetPeriode.id} er i tilstand TIL_INFOTRYGD"
                }
                forkastetPeriode.behandlinger.behandlinger.last().also { behandling ->
                    val utbetalingstatus = behandling.endringer.asReversed().firstNotNullOfOrNull { it.utbetaling }?.inspektør?.tilstand
                    check(utbetalingstatus == null || utbetalingstatus in setOf(Utbetalingstatus.FORKASTET, Utbetalingstatus.IKKE_GODKJENT, Utbetalingstatus.ANNULLERT)) {
                        "Utbetalingstatus for forkastet behandling er ikke FORKASTET / IKKE_GODKJENT / ANNULLERT, men $utbetalingstatus"
                    }
                    check(behandling.tilstand in setOf(Tilstand.TilInfotrygd, Tilstand.AnnullertPeriode)) {
                        "Forventet at siste behandling på forkastet vedtaksperiode ${forkastetPeriode.id} er i tilstand TIL_INFOTRYGD eller ANNULLERT_PERIODE"
                    }
                }
            }
        }
    }

    private fun bekreftIngenOverlappende() {
        arbeidsgivere.forEach { arbeidsgiver ->
            var kanskjeForrigePeriode: no.nav.helse.person.Vedtaksperiode? = null
            arbeidsgiver.vedtaksperioder.forEach { current ->
                kanskjeForrigePeriode?.also { forrigePeriode ->
                    if (forrigePeriode.periode.overlapperMed(current.periode)) {
                        error("For Arbeidsgiver ${arbeidsgiver.organisasjonsnummer()} overlapper Vedtaksperiode ${current.id} (${current.periode}) og Vedtaksperiode ${forrigePeriode.id} (${forrigePeriode.periode}) med hverandre!")
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
