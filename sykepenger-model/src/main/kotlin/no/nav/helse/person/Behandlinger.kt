package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.ArbeidssituasjonDto
import no.nav.helse.dto.BehandlingkildeDto
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.deserialisering.ArbeidstakerFaktaavklartInntektInnDto
import no.nav.helse.dto.deserialisering.BehandlingInnDto
import no.nav.helse.dto.deserialisering.BehandlingendringInnDto
import no.nav.helse.dto.deserialisering.BehandlingerInnDto
import no.nav.helse.dto.deserialisering.SelvstendigFaktaavklartInntektInnDto
import no.nav.helse.dto.serialisering.BehandlingUtDto
import no.nav.helse.dto.serialisering.BehandlingendringUtDto
import no.nav.helse.dto.serialisering.BehandlingerUtDto
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsavgjørelse
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.SelvstendigForsikring.Forsikringstype
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.vurdering
import no.nav.helse.person.Behandlinger.Behandling.Companion.berik
import no.nav.helse.person.Behandlinger.Behandling.Companion.dokumentsporing
import no.nav.helse.person.Behandlinger.Behandling.Companion.grunnbeløpsregulert
import no.nav.helse.person.Behandlinger.Behandling.Companion.harGjenbrukbarInntekt
import no.nav.helse.person.Behandlinger.Behandling.Companion.lagreGjenbrukbarInntekt
import no.nav.helse.person.Behandlinger.Behandling.Companion.vurderVarselForGjenbrukAvInntekt
import no.nav.helse.person.Behandlinger.Behandling.Endring.Arbeidssituasjon
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.bestemDagerUtenNavAnsvar
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.bestemSkjæringstidspunkt
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.eksterneIder
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.EventSubscription.AnalytiskDatapakkeEvent
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.Companion.harUlikeGrunnbeløp
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_24
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.FaktaavklartInntekt
import no.nav.helse.person.inntekt.FaktaavklartInntektView
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser
import no.nav.helse.sykdomstidslinje.Dag.ArbeidIkkeGjenopptattDag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.ArbeidsgiverHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.ForeldetSykedag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Permisjonsdag
import no.nav.helse.sykdomstidslinje.Dag.ProblemDag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkter
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingEventBus
import no.nav.helse.utbetalingslinjer.UtbetalingView
import no.nav.helse.utbetalingslinjer.Utbetalingkladd
import no.nav.helse.utbetalingslinjer.UtbetalingkladdBuilder
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.PeriodeUtenNavAnsvar
import no.nav.helse.utbetalingstidslinje.PeriodeUtenNavAnsvar.Companion.finn
import no.nav.helse.utbetalingstidslinje.SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.VentedagerForVedtaksperiode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class Behandlinger private constructor(behandlinger: List<Behandling>) : Aktivitetskontekst {
    internal constructor() : this(emptyList())

    companion object {
        internal fun Map<UUID, Behandlinger>.berik(builder: UtkastTilVedtakBuilder) = mapValues { (_, behandlinger) -> behandlinger.sisteBehandling }.berik(builder)
        fun gjenopprett(dto: BehandlingerInnDto, grunnlagsdata: Map<UUID, VilkårsgrunnlagElement>, utbetalinger: Map<UUID, Utbetaling>) = Behandlinger(
            behandlinger = dto.behandlinger.map { Behandling.gjenopprett(it, grunnlagsdata, utbetalinger) }
        )
    }

    // alle behandlinger for vedtaksperioden
    private val behandlinger = behandlinger.toMutableList()

    // den siste behandlingen uavhengig er tilstand
    private val sisteBehandling get() = behandlinger.last()
    // den siste behandlingen, hvis den er åpen for endring. dvs. ikke fattet vedtak eller annullert
    private val åpenBehandling get() = sisteBehandling.takeIf { it.erÅpenForEndring() }
    // alle tidligere behandlinger der en beslutning er tatt
    private val tidligereBehandlinger get() = åpenBehandling?.let { behandlinger.dropLast(1) } ?: behandlinger

    val sisteBehandlingId get() = sisteBehandling.id

    internal val maksdato get() = sisteBehandling.maksdato
    internal val dagerNavOvertarAnsvar get() = sisteBehandling.dagerNavOvertarAnsvar
    internal val dagerUtenNavAnsvar get() = behandlinger.last().dagerUtenNavAnsvar
    internal val faktaavklartInntekt get() = sisteBehandling.faktaavklartInntekt
    internal val arbeidssituasjon get() = sisteBehandling.arbeidssituasjon
    internal val utbetaling get() = sisteBehandling.utbetaling()

    internal fun sisteUtbetalteUtbetaling() = tidligereBehandlinger.lastOrNull()?.utbetaling()

    internal fun harFattetVedtak() = tidligereBehandlinger.isNotEmpty()

    internal fun åpenForEndring() = åpenBehandling != null

    internal fun initiellBehandling(
        behandlingEventBus: BehandlingEventBus,
        sykmeldingsperiode: Periode,
        sykdomstidslinje: Sykdomstidslinje,
        arbeidssituasjon: Arbeidssituasjon,
        egenmeldingsdager: List<Periode>,
        faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
        dokumentsporing: Dokumentsporing,
        behandlingkilde: Behandlingkilde,
    ) {
        check(behandlinger.isEmpty())
        val behandling = Behandling.initiellBehandling(
            sykdomstidslinje = sykdomstidslinje,
            arbeidssituasjon = arbeidssituasjon,
            egenmeldingsdager = egenmeldingsdager,
            faktaavklartInntekt = faktaavklartInntekt,
            dokumentsporing = dokumentsporing,
            sykmeldingsperiode = sykmeldingsperiode,
            behandlingkilde = behandlingkilde
        )
        leggTilNyBehandling(behandlingEventBus, behandling)
    }

    internal fun view() = BehandlingerView(
        behandlinger = behandlinger.map { it.view() },
        hendelser = hendelseIder()
    )

    internal fun ventedager() = VentedagerForVedtaksperiode(
        vedtaksperiode = periode(),
        dagerUtenNavAnsvar = sisteBehandling.dagerUtenNavAnsvar,
        dagerNavOvertarAnsvar = sisteBehandling.dagerNavOvertarAnsvar
    )

    internal fun utbetalingstidslinjeBuilderForArbeidstaker(): ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode {
        return ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
            arbeidsgiverperiode = sisteBehandling.dagerUtenNavAnsvar.dager,
            dagerNavOvertarAnsvar = sisteBehandling.dagerNavOvertarAnsvar,
            refusjonstidslinje = sisteBehandling.refusjonstidslinje
        )
    }

    internal fun utbetalingstidslinjeBuilderForSelvstendig(selvstendigForsikring: SelvstendigForsikring?): SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode {
        val dekningsgrad = when (val arbeidssituasjon = sisteBehandling.arbeidssituasjon) {
            Arbeidssituasjon.BARNEPASSER,
            Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE -> when (selvstendigForsikring?.type) {
                Forsikringstype.HundreProsentFraDagEn,
                Forsikringstype.HundreProsentFraDagSytten -> 100.prosent

                Forsikringstype.ÅttiProsentFraDagEn,
                null -> 80.prosent
            }

            Arbeidssituasjon.JORDBRUKER -> 100.prosent

            Arbeidssituasjon.ANNET,
            Arbeidssituasjon.FISKER,
            Arbeidssituasjon.ARBEIDSTAKER,
            Arbeidssituasjon.ARBEIDSLEDIG,
            Arbeidssituasjon.FRILANSER -> error("Har ikke implementert dekningsgrad for $arbeidssituasjon")
        }

        val dagerNavOvertarAnsvar = when (selvstendigForsikring?.type) {
            Forsikringstype.HundreProsentFraDagEn,
            Forsikringstype.ÅttiProsentFraDagEn -> behandlinger.last().dagerUtenNavAnsvar.dager

            Forsikringstype.HundreProsentFraDagSytten,
            null -> emptyList()
        }

        return SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(
            dekningsgrad = dekningsgrad,
            ventetid = sisteBehandling.dagerUtenNavAnsvar.periode,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar
        )
    }

    internal fun nyBehandling(
        behandlingEventBus: BehandlingEventBus,
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
    ): Behandling {
        check(åpenBehandling == null) { "Kan ikke opprette ny behandling når det finnes en åpen behandling" }
        val nyBehandling = tidligereBehandlinger.last().nyBehandling(yrkesaktivitet, behandlingkilde)
        leggTilNyBehandling(behandlingEventBus, nyBehandling)
        return nyBehandling
    }

    internal fun nyAnnulleringBehandling(
        behandlingEventBus: BehandlingEventBus,
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
    ): Behandling {
        check(åpenBehandling == null) { "Kan ikke opprette ny behandling når det finnes en åpen behandling" }
        val nyBehandling = tidligereBehandlinger.last().nyAnnulleringBehandling(yrkesaktivitet, behandlingkilde)
        leggTilNyBehandling(behandlingEventBus, nyBehandling)
        return nyBehandling
    }

    internal fun nyForkastetBehandling(
        behandlingEventBus: BehandlingEventBus,
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
        automatiskBehandling: Boolean
    ): Behandling {
        check(åpenBehandling == null) { "Kan ikke opprette ny behandling når det finnes en åpen behandling" }
        val nyBehandling = tidligereBehandlinger.last().nyForkastetBehandling(yrkesaktivitet, behandlingkilde)
        leggTilNyBehandling(behandlingEventBus, nyBehandling)
        behandlingEventBus.behandlingForkastet(nyBehandling.id, automatiskBehandling)
        return nyBehandling
    }

    internal fun analytiskDatapakke(yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, vedtaksperiodeId: UUID): AnalytiskDatapakkeEvent {
        val forrigeBehandling = behandlinger.dropLast(1).lastOrNull()
        return sisteBehandling.analytiskDatapakke(forrigeBehandling, yrkesaktivitetssporing, vedtaksperiodeId)
    }

    internal fun utbetalingstidslinjeFraForrigeVedtak() = behandlinger.lastOrNull { it.erFattetVedtak() }?.utbetalingstidslinje()
    internal fun utbetalingstidslinje() = sisteBehandling.utbetalingstidslinje()
    internal fun skjæringstidspunkt() = sisteBehandling.skjæringstidspunkt
    internal fun skjæringstidspunkter() = sisteBehandling.skjæringstidspunkter
    internal fun egenmeldingsdager() = sisteBehandling.egenmeldingsdager
    internal fun sykdomstidslinje() = sisteBehandling.sykdomstidslinje
    internal fun refusjonstidslinje() = sisteBehandling.refusjonstidslinje
    internal fun utbetales() = behandlinger.any { it.erInFlight() }
    internal fun erAvsluttet() = sisteBehandling.erAvsluttet()
    internal fun erAnnullert() = sisteBehandling.erAnnullert()
    internal fun erAvvist() = sisteBehandling.erAvvist()
    internal fun harUtbetalinger() = sisteBehandling.harOppdragMedUtbetalinger()
    internal fun kanForkastes(andreBehandlinger: List<Behandlinger>) = sisteBehandling.kanForkastes(andreBehandlinger.map { it.sisteBehandling })
    internal fun forventerUtbetaling(periodeSomBeregner: Periode, skjæringstidspunkt: LocalDate, skalBehandlesISpeil: Boolean) =
        sisteBehandling.forventerUtbetaling(periodeSomBeregner, skjæringstidspunkt, skalBehandlesISpeil)

    internal fun harFlereSkjæringstidspunkt() = sisteBehandling.harFlereSkjæringstidspunkt()
    internal fun børBrukeSkatteinntekterDirekte() = sisteBehandling.skjæringstidspunkter.isEmpty()

    internal fun håndterUtbetalinghendelseSisteInFlight(
        behandlingEventBus: BehandlingEventBus,
        utbetalingEventBus: UtbetalingEventBus,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ): Behandling? {
        return tidligereBehandlinger
            .lastOrNull { it.erInFlight() }
            ?.also { it.håndterUtbetalinghendelse(behandlingEventBus, utbetalingEventBus, hendelse, aktivitetslogg) }
    }

    internal fun håndterUtbetalinghendelseSisteBehandling(
        behandlingEventBus: BehandlingEventBus,
        utbetalingEventBus: UtbetalingEventBus,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ): Behandling {
        check(åpenBehandling == null) { "Kan ikke håndtere utbetalinghendelse på åpen behandling" }
        return tidligereBehandlinger.last().also { it.håndterUtbetalinghendelse(behandlingEventBus, utbetalingEventBus, hendelse, aktivitetslogg) }
    }

    internal fun validerFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = sisteBehandling.validerFerdigBehandlet(meldingsreferanseId, aktivitetslogg)
    internal fun validerIkkeFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = sisteBehandling.validerIkkeFerdigBehandlet(meldingsreferanseId, aktivitetslogg)

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Behandling", mapOf("behandlingId" to sisteBehandlingId.toString()))
    }

    internal fun byggUtkastTilVedtak(builder: UtkastTilVedtakBuilder, behandling: Behandling?): UtkastTilVedtakBuilder {
        if (behandlinger.grunnbeløpsregulert()) builder.grunnbeløpsregulert()
        builder.historiskeHendelseIder(eksterneIder())
        (behandling ?: sisteBehandling).byggUtkastTilVedtak(builder)
        return builder
    }

    internal fun håndterAnnullering(utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(åpenBehandling).håndterAnnullering(utbetalingEventBus, yrkesaktivitet, aktivitetslogg)
    }

    internal fun leggTilAnnullering(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, annullering: Utbetaling, vurdering: Utbetaling.Vurdering, aktivitetslogg: IAktivitetslogg) {
        val forrigeVedtak = tidligereBehandlinger.last()
        checkNotNull(åpenBehandling).leggTilAnnullering(behandlingEventBus, utbetalingEventBus, annullering, vurdering,  forrigeVedtak, aktivitetslogg)
    }

    internal fun beregnetBehandling(beregning: BeregnetBehandling, yrkesaktivitet: Behandlingsporing.Yrkesaktivitet) {
        checkNotNull(åpenBehandling).utbetaling(beregning, yrkesaktivitet)
    }

    internal fun lagUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        utbetalinger: List<Utbetaling>,
        mottakerRefusjon: String,
        mottakerBruker: String
    ) = checkNotNull(åpenBehandling).lagUtbetaling(aktivitetslogg, utbetalinger, mottakerRefusjon, mottakerBruker)

    internal fun forkastÅpenBehandling(
        eventBus: EventBus,
        behandlingEventBus: BehandlingEventBus,
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
        automatiskBehandling: Boolean,
        aktivitetslogg: IAktivitetslogg
    ) {
        checkNotNull(åpenBehandling).forkastBehandling(eventBus, behandlingEventBus, yrkesaktivitet, behandlingkilde, aktivitetslogg, automatiskBehandling)
    }

    internal fun forkastBeregning(utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(åpenBehandling).forkastBeregning(utbetalingEventBus, aktivitetslogg)
    }

    internal fun harIkkeUtbetaling() = sisteBehandling.harIkkeUtbetaling()

    fun vedtakFattet(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg): Behandling {
        return checkNotNull(åpenBehandling).also { it.vedtakFattet(behandlingEventBus, utbetalingEventBus, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg) }
    }

    fun vedtakAvvist(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg): Boolean {
        return checkNotNull(åpenBehandling).vedtakAvvist(behandlingEventBus, utbetalingEventBus, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)
    }

    fun bekreftAvsluttetBehandlingMedVedtak(yrkesaktivitet: Yrkesaktivitet) {
        bekreftAvsluttetBehandling(yrkesaktivitet)
        check(erFattetVedtak()) {
            "forventer at behandlingen skal ha fattet vedtak"
        }
    }

    private fun erFattetVedtak(): Boolean {
        return sisteBehandling.erFattetVedtak()
    }

    private fun bekreftAvsluttetBehandling(yrkesaktivitet: Yrkesaktivitet) {
        yrkesaktivitet.bekreftErLåst(periode())
        check(erAvsluttet()) {
            "forventer at utbetaling skal være avsluttet"
        }
    }

    fun avsluttUtenVedtak(behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>): Behandling {
        val behandlingen = checkNotNull(åpenBehandling)
        check(behandlingen.utbetaling() == null) {
            "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. "
        }
        return behandlingen.also {
            it.avsluttUtenVedtak(behandlingEventBus, yrkesaktivitet, utbetalingstidslinje, inntekterForBeregning)
            bekreftAvsluttetBehandling(yrkesaktivitet)
        }
    }

    internal fun sykmeldingsperiode() = this.behandlinger.first().sykmeldingsperiode()
    internal fun periode() = this.sisteBehandling.periode()

    private fun leggTilNyBehandling(behandlingEventBus: BehandlingEventBus, behandling: Behandling) {
        check(behandlinger.isEmpty() || åpenBehandling == null) { "Kan ikke opprette ny behandling når det finnes en åpen behandling" }
        this.behandlinger.add(behandling)
        behandling.behandlingOpprettet(behandlingEventBus)
    }

    fun bekreftÅpenBehandling(yrkesaktivitet: Yrkesaktivitet) {
        yrkesaktivitet.bekreftErÅpen(periode())
        check(sisteBehandling.harÅpenBehandling()) {
            "forventer at vedtaksperioden er uberegnet når den går ut av Avsluttet/AvsluttetUtenUtbetaling"
        }
    }

    internal fun subsumsjonslogg(regelverkslogg: Regelverkslogg, vedtaksperiodeId: UUID, fødselsnummer: String, organisasjonsnummer: String) =
        BehandlingSubsumsjonslogg(
            regelverkslogg = regelverkslogg,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = sisteBehandling.id
        )

    internal fun hendelseIder() = behandlinger.dokumentsporing
    internal fun eksterneIder() = behandlinger.dokumentsporing.eksterneIder()
    internal fun eksterneIderUUID() = eksterneIder().map { it.id }.toSet()
    internal fun søknadIder() = behandlinger.dokumentsporing.søknadIder()
    internal fun oppdaterDokumentsporing(dokument: Dokumentsporing) {
        return checkNotNull(åpenBehandling).oppdaterDokumentsporing(dokument)
    }

    fun dokumentHåndtert(dokumentsporing: Dokumentsporing) =
        behandlinger.any { it.dokumentHåndtert(dokumentsporing) }

    internal fun vurderVarselForGjenbrukAvInntekt(faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt, aktivitetslogg: IAktivitetslogg) = behandlinger.vurderVarselForGjenbrukAvInntekt(faktaavklartInntekt, aktivitetslogg)

    internal fun harGjenbrukbarInntekt(organisasjonsnummer: String) =
        behandlinger.harGjenbrukbarInntekt(organisasjonsnummer)

    internal fun lagreGjenbrukbarInntekt(skjæringstidspunkt: LocalDate, organisasjonsnummer: String, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        behandlinger.lagreGjenbrukbarInntekt(skjæringstidspunkt, organisasjonsnummer, yrkesaktivitet, aktivitetslogg)

    internal fun endretRefusjonstidslinje(refusjonstidslinje: Beløpstidslinje) =
        sisteBehandling.endretRefusjonstidslinje(refusjonstidslinje)

    internal fun håndterRefusjonstidslinje(
        eventBus: EventBus,
        yrkesaktivitet: Yrkesaktivitet,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg,
        benyttetRefusjonsopplysninger: Beløpstidslinje
    ) {
        checkNotNull(åpenBehandling).håndterRefusjonsopplysninger(eventBus, yrkesaktivitet, dokumentsporing, aktivitetslogg, benyttetRefusjonsopplysninger)
    }

    internal fun håndterFaktaavklartInntekt(eventBus: EventBus, arbeidstakerFaktaavklartInntekt: ArbeidstakerFaktaavklartInntekt, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(åpenBehandling).håndterFaktaavklartInntekt(eventBus, arbeidstakerFaktaavklartInntekt, yrkesaktivitet, aktivitetslogg)
    }

    internal fun håndterKorrigertInntekt(eventBus: EventBus, korrigertInntekt: Saksbehandler, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(åpenBehandling).håndterKorrigertInntekt(eventBus, korrigertInntekt, yrkesaktivitet, aktivitetslogg)
    }

    internal fun håndterSykdomstidslinje(
        eventBus: EventBus,
        yrkesaktivitet: Yrkesaktivitet,
        dokumentsporing: Dokumentsporing,
        hendelseSykdomstidslinje: Sykdomstidslinje,
        egenmeldingsdagerAndrePerioder: List<Periode>,
        dagerNavOvertarAnsvar: List<Periode>?,
        aktivitetslogg: IAktivitetslogg,
        validering: () -> Unit
    ) {
        checkNotNull(åpenBehandling).håndterSykdomstidslinje(
            eventBus = eventBus,
            yrkesaktivitet = yrkesaktivitet,
            dokumentsporing = dokumentsporing,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            egenmeldingsdagerAndrePerioder = egenmeldingsdagerAndrePerioder,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            aktivitetslogg = aktivitetslogg
        )
        validering()
    }

    internal fun nullstillEgenmeldingsdager(
        eventBus: EventBus,
        yrkesaktivitet: Yrkesaktivitet,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg
    ) {
        checkNotNull(åpenBehandling).nullstillEgenmeldingsdager(
            eventBus = eventBus,
            yrkesaktivitet = yrkesaktivitet,
            dokumentsporing = dokumentsporing,
            aktivitetslogg = aktivitetslogg
        )
    }

    fun oppdaterSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>) {
        checkNotNull(åpenBehandling).oppdaterSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetPerioderUtenNavAnsvar)
    }

    internal class Behandlingkilde(
        val meldingsreferanseId: MeldingsreferanseId,
        val innsendt: LocalDateTime,
        val registert: LocalDateTime,
        val avsender: Avsender
    ) {
        fun view() = BehandlingkildeView(meldingsreferanseId, innsendt, registert, avsender)
        internal fun dto() = BehandlingkildeDto(
            meldingsreferanseId = this.meldingsreferanseId.dto(),
            innsendt = this.innsendt,
            registert = this.registert,
            avsender = avsender.dto()
        )

        internal companion object {
            fun gjenopprett(dto: BehandlingkildeDto): Behandlingkilde {
                return Behandlingkilde(
                    meldingsreferanseId = MeldingsreferanseId.gjenopprett(dto.meldingsreferanseId),
                    innsendt = dto.innsendt,
                    registert = dto.registert,
                    avsender = Avsender.gjenopprett(dto.avsender)
                )
            }
        }
    }

    internal class Behandling private constructor(
        val id: UUID,
        private var tilstand: Tilstand,
        private val endringer: MutableList<Endring>,
        private var vedtakFattet: LocalDateTime?,
        avsluttet: LocalDateTime?,
        private val kilde: Behandlingkilde
    ) {
        var avsluttet: LocalDateTime? = avsluttet
            private set
        private val gjeldende get() = endringer.last()
        private val periode: Periode get() = gjeldende.periode
        private val dokumentsporing get() = endringer.dokumentsporing
        val dagerUtenNavAnsvar get() = gjeldende.dagerUtenNavAnsvar
        val skjæringstidspunkt get() = gjeldende.skjæringstidspunkt
        val egenmeldingsdager get() = gjeldende.egenmeldingsdager
        val skjæringstidspunkter get() = gjeldende.skjæringstidspunkter
        val maksdato get() = gjeldende.maksdatoresultat
        val dagerNavOvertarAnsvar get() = gjeldende.dagerNavOvertarAnsvar
        val sykdomstidslinje get() = endringer.last().sykdomstidslinje
        val refusjonstidslinje get() = endringer.last().refusjonstidslinje
        val arbeidssituasjon get() = endringer.last().arbeidssituasjon
        val utbetalingstidslinje get() = endringer.last().utbetalingstidslinje
        val faktaavklartInntekt get() = endringer.last().faktaavklartInntekt
        val korrigertInntekt get() = endringer.last().korrigertInntekt
        val inntektsjusteringer get() = endringer.last().inntektjusteringer

        constructor(tilstand: Tilstand, endringer: List<Endring>, avsluttet: LocalDateTime?, kilde: Behandlingkilde) : this(UUID.randomUUID(), tilstand, endringer.toMutableList(), null, avsluttet, kilde)

        init {
            check(endringer.isNotEmpty()) {
                "Må ha endringer for at det skal være vits med en behandling"
            }
        }

        fun behandlingOpprettet(behandlingEventBus: BehandlingEventBus) {
            val type = when (tilstand) {
                Tilstand.AnnullertPeriode -> EventSubscription.BehandlingOpprettetEvent.Type.Revurdering
                Tilstand.TilInfotrygd -> EventSubscription.BehandlingOpprettetEvent.Type.Omgjøring
                Tilstand.Uberegnet -> EventSubscription.BehandlingOpprettetEvent.Type.Søknad
                Tilstand.UberegnetAnnullering -> EventSubscription.BehandlingOpprettetEvent.Type.Revurdering
                Tilstand.UberegnetOmgjøring -> EventSubscription.BehandlingOpprettetEvent.Type.Omgjøring
                Tilstand.UberegnetRevurdering -> EventSubscription.BehandlingOpprettetEvent.Type.Revurdering
                Tilstand.AvsluttetUtenVedtak,
                Tilstand.Beregnet,
                Tilstand.BeregnetOmgjøring,
                Tilstand.BeregnetRevurdering,
                Tilstand.OverførtAnnullering,
                Tilstand.RevurdertVedtakAvvist,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt -> error("Kan ikke opprette ny behandling i tilstand $tilstand")
            }
            behandlingEventBus.nyBehandling(id, periode, kilde.meldingsreferanseId, kilde.innsendt, kilde.registert, kilde.avsender, type, endringer.dokumentsporing.søknadIder())
        }

        override fun toString() = "$periode - $tilstand"
        fun view() = BehandlingView(
            id = id,
            periode = periode,
            vedtakFattet = vedtakFattet,
            avsluttet = avsluttet,
            kilde = kilde.view(),
            tilstand = when (tilstand) {
                Tilstand.AnnullertPeriode -> BehandlingView.TilstandView.ANNULLERT_PERIODE
                Tilstand.AvsluttetUtenVedtak -> BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK
                Tilstand.Beregnet -> BehandlingView.TilstandView.BEREGNET
                Tilstand.BeregnetOmgjøring -> BehandlingView.TilstandView.BEREGNET_OMGJØRING
                Tilstand.BeregnetRevurdering -> BehandlingView.TilstandView.BEREGNET_REVURDERING
                Tilstand.RevurdertVedtakAvvist -> BehandlingView.TilstandView.REVURDERT_VEDTAK_AVVIST
                Tilstand.TilInfotrygd -> BehandlingView.TilstandView.TIL_INFOTRYGD
                Tilstand.Uberegnet -> BehandlingView.TilstandView.UBEREGNET
                Tilstand.UberegnetOmgjøring -> BehandlingView.TilstandView.UBEREGNET_OMGJØRING
                Tilstand.UberegnetRevurdering -> BehandlingView.TilstandView.UBEREGNET_REVURDERING
                Tilstand.VedtakFattet -> BehandlingView.TilstandView.VEDTAK_FATTET
                Tilstand.VedtakIverksatt -> BehandlingView.TilstandView.VEDTAK_IVERKSATT
                Tilstand.UberegnetAnnullering -> BehandlingView.TilstandView.UBEREGNET_ANNULLERING
                Tilstand.OverførtAnnullering -> BehandlingView.TilstandView.OVERFØRT_ANNULLERING
            },
            endringer = endringer.map { it.view() },
            faktaavklartInntekt = when (val fi = faktaavklartInntekt) {
                is SelvstendigFaktaavklartInntekt -> fi.view()
                is ArbeidstakerFaktaavklartInntekt -> fi.view()
                null -> null
            },
            korrigertInntekt = korrigertInntekt?.view()
        )

        fun sykmeldingsperiode() = endringer.first().sykmeldingsperiode
        fun periode() = periode

        fun analytiskDatapakke(
            forrigeBehandling: Behandling?,
            yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
            vedtaksperiodeId: UUID
        ): AnalytiskDatapakkeEvent {
            return AnalytiskDatapakkeEvent(
                yrkesaktivitetssporing = yrkesaktivitetssporing,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = this.id,
                skjæringstidspunkt = this.skjæringstidspunkt,
                beløpTilBruker = AnalytiskDatapakkeEvent.Pengeinformasjon(
                    totalBeløp = this.utbetalingstidslinje.totalbeløpPerson.daglig,
                    nettoBeløp = this.utbetalingstidslinje.totalbeløpPerson.daglig - (forrigeBehandling?.utbetalingstidslinje?.totalbeløpPerson?.daglig ?: 0.0),
                ),
                beløpTilArbeidsgiver = AnalytiskDatapakkeEvent.Pengeinformasjon(
                    totalBeløp = this.utbetalingstidslinje.totalbeløpRefusjon.daglig,
                    nettoBeløp = this.utbetalingstidslinje.totalbeløpRefusjon.daglig - (forrigeBehandling?.utbetalingstidslinje?.totalbeløpRefusjon?.daglig ?: 0.0),
                ),
                fom = this.periode.start,
                tom = this.periode.endInclusive,
                antallForbrukteSykedagerEtterPeriode = AnalytiskDatapakkeEvent.Daginformasjon(
                    antallDager = this.maksdato.antallForbrukteDager,
                    nettoDager = this.maksdato.antallForbrukteDager - (forrigeBehandling?.maksdato?.antallForbrukteDager ?: 0)
                ),
                antallGjenståendeSykedagerEtterPeriode = AnalytiskDatapakkeEvent.Daginformasjon(
                    antallDager = this.maksdato.gjenståendeDager,
                    nettoDager = this.maksdato.gjenståendeDager - (forrigeBehandling?.maksdato?.gjenståendeDager ?: 0)
                ),
                harAndreInntekterIBeregning = this.inntektsjusteringer.isNotEmpty()
            )
        }

        fun utbetalingstidslinje() = gjeldende.utbetalingstidslinje

        fun lagUtbetaling(
            aktivitetslogg: IAktivitetslogg,
            utbetalinger: List<Utbetaling>,
            mottakerRefusjon: String,
            mottakerBruker: String
        ) = when (tilstand) {
            Tilstand.Beregnet,
            Tilstand.BeregnetOmgjøring,
            Tilstand.BeregnetRevurdering -> {
                val utbetalingen = BeregnetBehandling(
                    maksdatoresultat = gjeldende.maksdatoresultat,
                    utbetalingstidslinje = gjeldende.utbetalingstidslinje,
                    grunnlagsdata = gjeldende.grunnlagsdata!!,
                    alleInntektjusteringer = gjeldende.inntektjusteringer
                ).lagUtbetaling(aktivitetslogg, gjeldende.periode, utbetalinger, mottakerRefusjon, mottakerBruker, tilstand is Tilstand.BeregnetRevurdering, gjeldende.arbeidssituasjon)

                this.nyEndring(gjeldende.kopierMedUtbetaling(utbetalingen))
                utbetalingen
            }

            Tilstand.AnnullertPeriode,
            Tilstand.AvsluttetUtenVedtak,
            Tilstand.OverførtAnnullering,
            Tilstand.RevurdertVedtakAvvist,
            Tilstand.TilInfotrygd,
            Tilstand.Uberegnet,
            Tilstand.UberegnetAnnullering,
            Tilstand.UberegnetOmgjøring,
            Tilstand.UberegnetRevurdering,
            Tilstand.VedtakFattet,
            Tilstand.VedtakIverksatt -> error("Forventer ikke å lage utbetaling i tilstand $tilstand")
        }

        internal fun håndterSykdomstidslinje(
            eventBus: EventBus,
            yrkesaktivitet: Yrkesaktivitet,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            egenmeldingsdagerAndrePerioder: List<Periode>,
            dagerNavOvertarAnsvar: List<Periode>?,
            aktivitetslogg: IAktivitetslogg
        ) {
            val hendelseSykdomstidslinjeFremTilOgMed = hendelseSykdomstidslinje.fremTilOgMed(periode.endInclusive)
            val hendelseperiode = hendelseSykdomstidslinjeFremTilOgMed.periode()

            val nyEndring = if (hendelseperiode == null) {
                gjeldende
                    .copy(
                        dokumentsporing = dokumentsporing,
                        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar ?: gjeldende.dagerNavOvertarAnsvar,
                    )
            } else {
                val oppdatertPeriode = periode.oppdaterFom(hendelseperiode)
                val (nySykdomstidslinje, nyeSkjæringstidspunkter, nyePerioderUtenNavAnsvar) = yrkesaktivitet.oppdaterSykdom(
                    meldingsreferanseId = dokumentsporing.id,
                    sykdomstidslinje = hendelseSykdomstidslinjeFremTilOgMed,
                    egenmeldingsperioder = egenmeldingsdagerAndrePerioder + gjeldende.egenmeldingsdager
                )
                val sykdomstidslinje = nySykdomstidslinje.subset(oppdatertPeriode)

                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = bestemSkjæringstidspunkt(nyeSkjæringstidspunkter, sykdomstidslinje, oppdatertPeriode)
                val dagerUtenNavAnsvar = bestemDagerUtenNavAnsvar(oppdatertPeriode, nyePerioderUtenNavAnsvar)

                gjeldende
                    .copy(
                        dokumentsporing = dokumentsporing,
                        skjæringstidspunkt = nyttSkjæringstidspunkt,
                        skjæringstidspunkter = alleSkjæringstidspunkter,
                        dagerUtenNavAnsvar = dagerUtenNavAnsvar,
                        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar ?: gjeldende.dagerNavOvertarAnsvar,
                        sykdomstidslinje = sykdomstidslinje,
                        periode = oppdatertPeriode,
                        refusjonstidslinje = gjeldende.refusjonstidslinje.fyll(oppdatertPeriode)
                    )
            }

            håndterNyFakta(
                eventBus = eventBus,
                nyEndring = nyEndring,
                yrkesaktivitet = yrkesaktivitet,
                aktivitetslogg = aktivitetslogg
            )
        }
        internal fun nullstillEgenmeldingsdager(
            eventBus: EventBus,
            yrkesaktivitet: Yrkesaktivitet,
            dokumentsporing: Dokumentsporing?,
            aktivitetslogg: IAktivitetslogg
        ) {
            // vi må oppdatere uansett om sykdomstidslinjen er tom, fordi egenmeldingsdager kan ha endret seg og dette påvirker agp
            val nyePerioderUtenNavAnsvar = yrkesaktivitet.beregnPerioderUtenNavAnsvar(egenmeldingsperioder = emptyList())
            val dagerUtenNavAnsvar = bestemDagerUtenNavAnsvar(periode, nyePerioderUtenNavAnsvar)

            val nyEndring = gjeldende
                .copy(
                    dokumentsporing = dokumentsporing ?: gjeldende.dokumentsporing,
                    dagerUtenNavAnsvar = dagerUtenNavAnsvar,
                    egenmeldingsdager = emptyList()
                )
            håndterNyFakta(
                eventBus = eventBus,
                nyEndring = nyEndring,
                yrkesaktivitet = yrkesaktivitet,
                aktivitetslogg = aktivitetslogg
            )
        }

        internal fun endretRefusjonstidslinje(nyRefusjonstidslinje: Beløpstidslinje): Beløpstidslinje? {
            val nyeRefusjonsopplysningerForPerioden = nyRefusjonstidslinje.subset(periode)
            val benyttetRefusjonsopplysninger = (gjeldende.refusjonstidslinje + nyeRefusjonsopplysningerForPerioden).fyll(periode)
            if (benyttetRefusjonsopplysninger == gjeldende.refusjonstidslinje) return null
            return benyttetRefusjonsopplysninger
        }

        internal fun håndterRefusjonsopplysninger(
            eventBus: EventBus,
            yrkesaktivitet: Yrkesaktivitet,
            dokumentsporing: Dokumentsporing?,
            aktivitetslogg: IAktivitetslogg,
            benyttetRefusjonsopplysninger: Beløpstidslinje
        ) {
            val nyEndring = gjeldende
                .copy(
                    dokumentsporing = dokumentsporing ?: gjeldende.dokumentsporing,
                    refusjonstidslinje = benyttetRefusjonsopplysninger
                )
            håndterNyFakta(
                eventBus = eventBus,
                nyEndring = nyEndring,
                yrkesaktivitet = yrkesaktivitet,
                aktivitetslogg = aktivitetslogg
            )
        }

        fun håndterFaktaavklartInntekt(
            eventBus: EventBus,
            arbeidstakerFaktaavklartInntekt: ArbeidstakerFaktaavklartInntekt,
            yrkesaktivitet: Yrkesaktivitet,
            aktivitetslogg: IAktivitetslogg
        ) {
            håndterNyFakta(
                eventBus = eventBus,
                nyEndring = gjeldende.copy(faktaavklartInntekt = arbeidstakerFaktaavklartInntekt),
                yrkesaktivitet = yrkesaktivitet,
                aktivitetslogg = aktivitetslogg
            )
        }

        fun håndterKorrigertInntekt(eventBus: EventBus, korrigertInntekt: Saksbehandler, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) {
            håndterNyFakta(
                eventBus = eventBus,
                nyEndring = gjeldende.copy(korrigertInntekt = korrigertInntekt),
                yrkesaktivitet = yrkesaktivitet,
                aktivitetslogg = aktivitetslogg
            )
        }

        private fun håndterNyFakta(
            eventBus: EventBus,
            nyEndring: Endring,
            yrkesaktivitet: Yrkesaktivitet,
            aktivitetslogg: IAktivitetslogg
        ) {
            // Forsikrer oss at ny endring er Uberegnet og får ny ID og tidsstempel
            val endringMedNyFakta = nyEndring.kopierUtenBeregning()

            val beregnetBehandling = { uberegnetTilstand: Tilstand ->
                gjeldende.forkastUtbetaling(with (yrkesaktivitet) {  eventBus.utbetalingEventBus }, aktivitetslogg)
                nyEndring(endringMedNyFakta)
                tilstand(uberegnetTilstand)
            }

            when (this.tilstand) {
                Tilstand.Uberegnet,
                Tilstand.UberegnetOmgjøring,
                Tilstand.UberegnetRevurdering -> nyEndring(endringMedNyFakta)

                Tilstand.Beregnet -> beregnetBehandling(Tilstand.Uberegnet)
                Tilstand.BeregnetRevurdering -> beregnetBehandling(Tilstand.UberegnetRevurdering)
                Tilstand.BeregnetOmgjøring -> beregnetBehandling(Tilstand.UberegnetOmgjøring)

                Tilstand.AvsluttetUtenVedtak,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt,
                Tilstand.UberegnetAnnullering,
                Tilstand.OverførtAnnullering,
                Tilstand.RevurdertVedtakAvvist,
                Tilstand.AnnullertPeriode,
                Tilstand.TilInfotrygd -> error("Forventet ikke å håndtere ny fakta i tilstand ${tilstand::class.simpleName}")
            }
        }

        data class Endring(
            val id: UUID,
            val tidsstempel: LocalDateTime,
            val sykmeldingsperiode: Periode,
            val periode: Periode,
            val arbeidssituasjon: Arbeidssituasjon,
            val grunnlagsdata: VilkårsgrunnlagElement?,
            val utbetaling: Utbetaling?,
            val dokumentsporing: Dokumentsporing,
            val sykdomstidslinje: Sykdomstidslinje,
            val utbetalingstidslinje: Utbetalingstidslinje,
            val refusjonstidslinje: Beløpstidslinje,
            val skjæringstidspunkt: LocalDate,
            val skjæringstidspunkter: List<LocalDate>,
            val egenmeldingsdager: List<Periode>,
            val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaring,
            val dagerNavOvertarAnsvar: List<Periode>,
            val maksdatoresultat: Maksdatoresultat,
            val inntektjusteringer: Map<Inntektskilde, Beløpstidslinje>,
            val faktaavklartInntekt: FaktaavklartInntekt?,
            val korrigertInntekt: Saksbehandler?
        ) {

            fun view() = BehandlingendringView(
                id = id,
                sykmeldingsperiode = sykmeldingsperiode,
                periode = periode,
                sykdomstidslinje = sykdomstidslinje,
                grunnlagsdata = grunnlagsdata,
                utbetaling = utbetaling?.view,
                dokumentsporing = dokumentsporing,
                utbetalingstidslinje = utbetalingstidslinje,
                refusjonstidslinje = refusjonstidslinje,
                skjæringstidspunkt = skjæringstidspunkt,
                skjæringstidspunkter = skjæringstidspunkter,
                dagerUtenNavAnsvar = dagerUtenNavAnsvar,
                egenmeldingsdager = egenmeldingsdager,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                maksdatoresultat = maksdatoresultat
            )

            companion object {
                val IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT = LocalDate.MIN
                val List<Endring>.dokumentsporing get() = map { it.dokumentsporing }.toSet()

                fun bestemDagerUtenNavAnsvar(periode: Periode, beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>): DagerUtenNavAnsvaravklaring {
                    return beregnetPerioderUtenNavAnsvar.finn(periode)?.let {
                        DagerUtenNavAnsvaravklaring(
                            ferdigAvklart = it.ferdigAvklart,
                            dager = it.dagerUtenAnsvar.grupperSammenhengendePerioder()
                        )
                    } ?: DagerUtenNavAnsvaravklaring(false, emptyList())
                }

                fun bestemSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, sykdomstidslinje: Sykdomstidslinje, periode: Periode): Pair<LocalDate, List<LocalDate>> {
                    val sisteSykedag = sykdomstidslinje.lastOrNull {
                        // uttømmende when-blokk (uten else) med hensikt, fordi om nye det lages nye
                        // dagtyper så vil det bli compile error og vi blir tvunget til å måtte ta stilling til den
                        when (it) {
                            is ArbeidsgiverHelgedag,
                            is Arbeidsgiverdag,
                            is ForeldetSykedag,
                            is SykHelgedag,
                            is Sykedag -> true

                            is AndreYtelser,
                            is ArbeidIkkeGjenopptattDag,
                            is Arbeidsdag,
                            is Feriedag,
                            is FriskHelgedag,
                            is Permisjonsdag,
                            is ProblemDag,
                            is UkjentDag -> false
                        }
                    }?.dato

                    // trimmer friskmelding/ferie i halen bort
                    val søkeperiode = sisteSykedag?.let { periode.start til sisteSykedag } ?: periode
                    val skjæringstidspunkter = beregnetSkjæringstidspunkter
                        .alle(søkeperiode)
                    val fastsattSkjæringstidspunkt = skjæringstidspunkter.maxOrNull() ?: periode.start
                    return fastsattSkjæringstidspunkt to skjæringstidspunkter
                }

                fun gjenopprett(dto: BehandlingendringInnDto, grunnlagsdata: Map<UUID, VilkårsgrunnlagElement>, utbetalinger: Map<UUID, Utbetaling>): Endring {
                    val periode = Periode.gjenopprett(dto.periode)
                    val utbetaling = dto.utbetalingId?.let { utbetalinger.getValue(it) }
                    return Endring(
                        id = dto.id,
                        tidsstempel = dto.tidsstempel,
                        sykmeldingsperiode = Periode.gjenopprett(dto.sykmeldingsperiode),
                        periode = periode,
                        arbeidssituasjon = when (dto.arbeidssituasjon) {
                            ArbeidssituasjonDto.ARBEIDSTAKER -> Arbeidssituasjon.ARBEIDSTAKER
                            ArbeidssituasjonDto.ARBEIDSLEDIG -> Arbeidssituasjon.ARBEIDSLEDIG
                            ArbeidssituasjonDto.SELVSTENDIG_NÆRINGSDRIVENDE -> Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE
                            ArbeidssituasjonDto.BARNEPASSER -> Arbeidssituasjon.BARNEPASSER
                            ArbeidssituasjonDto.FRILANSER -> Arbeidssituasjon.FRILANSER
                            ArbeidssituasjonDto.JORDBRUKER -> Arbeidssituasjon.JORDBRUKER
                            ArbeidssituasjonDto.FISKER -> Arbeidssituasjon.FISKER
                            ArbeidssituasjonDto.ANNET -> Arbeidssituasjon.ANNET
                        },
                        grunnlagsdata = dto.vilkårsgrunnlagId?.let { grunnlagsdata.getValue(it) },
                        utbetaling = utbetaling,
                        dokumentsporing = Dokumentsporing.gjenopprett(dto.dokumentsporing),
                        sykdomstidslinje = Sykdomstidslinje.gjenopprett(dto.sykdomstidslinje),
                        utbetalingstidslinje = Utbetalingstidslinje.gjenopprett(dto.utbetalingstidslinje),
                        refusjonstidslinje = Beløpstidslinje.gjenopprett(dto.refusjonstidslinje),
                        skjæringstidspunkt = dto.skjæringstidspunkt,
                        skjæringstidspunkter = dto.skjæringstidspunkter,
                        dagerUtenNavAnsvar = DagerUtenNavAnsvaravklaring.gjenopprett(dto.dagerUtenNavAnsvar),
                        egenmeldingsdager = dto.egenmeldingsdager.map { Periode.gjenopprett(it) },
                        dagerNavOvertarAnsvar = dto.dagerNavOvertarAnsvar.map { Periode.gjenopprett(it) },
                        maksdatoresultat = dto.maksdatoresultat.let { Maksdatoresultat.gjenopprett(it) },
                        inntektjusteringer = dto.inntektjusteringer.map { (inntektskildeDto, beløpstidslinjeDto) ->
                            Inntektskilde.gjenopprett(inntektskildeDto) to Beløpstidslinje.gjenopprett(beløpstidslinjeDto)
                        }.toMap(),
                        faktaavklartInntekt = dto.faktaavklartInntekt?.let {
                            when (it) {
                                is SelvstendigFaktaavklartInntektInnDto -> SelvstendigFaktaavklartInntekt.gjenopprett(it)
                                is ArbeidstakerFaktaavklartInntektInnDto -> ArbeidstakerFaktaavklartInntekt.gjenopprett(it)
                            }
                        },
                        korrigertInntekt = dto.korrigertInntekt?.let { Saksbehandler.gjenopprett(it) }
                    )
                }
            }

            internal fun arbeidsgiverperiodeEndret(other: Endring) =
                this.dagerUtenNavAnsvar != other.dagerUtenNavAnsvar

            internal fun erRettFør(neste: Endring): Boolean {
                return this.sykdomstidslinje.erRettFør(neste.sykdomstidslinje)
            }

            override fun toString() = "$periode - $dokumentsporing - ${sykdomstidslinje.toShortString()}${utbetaling?.let { " - $it" } ?: ""}"

            /* kopierer dataklassen og lager ny, men sørger for at den nye endringen får ny id og tidsstempel (!!) */
            private fun kopierMed(
                sykmeldingsperiode: Periode = this.sykmeldingsperiode,
                periode: Periode = this.periode,
                grunnlagsdata: VilkårsgrunnlagElement? = this.grunnlagsdata,
                utbetaling: Utbetaling? = this.utbetaling,
                dokumentsporing: Dokumentsporing = this.dokumentsporing,
                sykdomstidslinje: Sykdomstidslinje = this.sykdomstidslinje,
                utbetalingstidslinje: Utbetalingstidslinje = this.utbetalingstidslinje,
                refusjonstidslinje: Beløpstidslinje = this.refusjonstidslinje,
                skjæringstidspunkt: LocalDate = this.skjæringstidspunkt,
                skjæringstidspunkter: List<LocalDate> = this.skjæringstidspunkter,
                dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaring = this.dagerUtenNavAnsvar,
                dagerNavOvertarAnsvar: List<Periode> = this.dagerNavOvertarAnsvar,
                egenmeldingsdager: List<Periode> = this.egenmeldingsdager,
                maksdatoresultat: Maksdatoresultat = this.maksdatoresultat,
                inntektjusteringer: Map<Inntektskilde, Beløpstidslinje> = this.inntektjusteringer,
                faktaavklartInntekt: FaktaavklartInntekt? = this.faktaavklartInntekt,
                korrigertInntekt: Saksbehandler? = this.korrigertInntekt
            ) = copy(
                id = UUID.randomUUID(),
                tidsstempel = LocalDateTime.now(),
                sykmeldingsperiode = sykmeldingsperiode,
                periode = periode,
                grunnlagsdata = grunnlagsdata,
                utbetaling = utbetaling,
                dokumentsporing = dokumentsporing,
                sykdomstidslinje = sykdomstidslinje,
                utbetalingstidslinje = utbetalingstidslinje,
                refusjonstidslinje = refusjonstidslinje,
                skjæringstidspunkt = skjæringstidspunkt,
                skjæringstidspunkter = skjæringstidspunkter,
                dagerUtenNavAnsvar = dagerUtenNavAnsvar,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                egenmeldingsdager = egenmeldingsdager,
                maksdatoresultat = maksdatoresultat,
                inntektjusteringer = inntektjusteringer,
                faktaavklartInntekt = faktaavklartInntekt,
                korrigertInntekt = korrigertInntekt
            )

            internal fun kopierMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>): Endring? {
                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = bestemSkjæringstidspunkt(beregnetSkjæringstidspunkter, sykdomstidslinje, periode)
                val dagerUtenNavAnsvar = bestemDagerUtenNavAnsvar(periode, beregnetPerioderUtenNavAnsvar)
                if (nyttSkjæringstidspunkt == this.skjæringstidspunkt && dagerUtenNavAnsvar == this.dagerUtenNavAnsvar) return null
                return kopierMed(
                    skjæringstidspunkt = nyttSkjæringstidspunkt,
                    skjæringstidspunkter = alleSkjæringstidspunkter,
                    dagerUtenNavAnsvar = dagerUtenNavAnsvar
                )
            }

            internal fun kopierUtenBeregning(): Endring {
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    inntektjusteringer = emptyMap()
                )
            }

            internal fun kopierMedBeregning(beregning: BeregnetBehandling, dagerNavOvertarAnsvar: List<Periode>) = kopierMed(
                grunnlagsdata = beregning.grunnlagsdata,
                utbetalingstidslinje = beregning.utbetalingstidslinje.subset(this.periode),
                maksdatoresultat = beregning.maksdatoresultat,
                inntektjusteringer = beregning.alleInntektjusteringer,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar
            )

            internal fun kopierMedUtbetaling(utbetaling: Utbetaling) = kopierMed(utbetaling = utbetaling)

            internal fun kopierMedAnnullering(grunnlagsdata: VilkårsgrunnlagElement, annullering: Utbetaling) = kopierMed(
                grunnlagsdata = grunnlagsdata,
                utbetaling = annullering,
                utbetalingstidslinje = Utbetalingstidslinje(),
                maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                inntektjusteringer = emptyMap()
            )

            internal fun kopierDokument(dokument: Dokumentsporing) = kopierMed(dokumentsporing = dokument)
            internal fun kopierMedUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) = kopierMed(
                utbetalingstidslinje = utbetalingstidslinje.subset(this.periode),
                inntektjusteringer = inntekterForBeregning
            )

            fun forkastUtbetaling(utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
                utbetaling?.forkast(utbetalingEventBus, aktivitetslogg)
            }

            fun byggUtkastTilVedtak(utkastTilVedtakBuilder: UtkastTilVedtakBuilder) {
                checkNotNull(utbetaling) { "Forventet ikke manglende utbetaling ved godkjenningsbehov" }
                checkNotNull(grunnlagsdata) { "Forventet ikke manglende vilkårsgrunnlag ved godkjenningsbehov" }
                utkastTilVedtakBuilder
                    .periode(dagerUtenNavAnsvar.dager, dagerUtenNavAnsvar.ferdigAvklart, periode)
                    .skjæringstidspunkt(skjæringstidspunkt)
                    .utbetalingsinformasjon(utbetaling, utbetalingstidslinje, sykdomstidslinje, refusjonstidslinje)
                    .sykepengerettighet(maksdatoresultat.antallForbrukteDager, maksdatoresultat.gjenståendeDager, maksdatoresultat.maksdato)
                    .apply { (faktaavklartInntekt as? SelvstendigFaktaavklartInntekt)?.also { pensjonsgivendeInntekter(it.pensjonsgivendeInntekter) } }
                grunnlagsdata.berik(utkastTilVedtakBuilder)
            }

            internal fun dto(): BehandlingendringUtDto {
                val vilkårsgrunnlagUtDto = this.grunnlagsdata?.dto()
                val utbetalingUtDto = this.utbetaling?.dto()
                return BehandlingendringUtDto(
                    id = this.id,
                    tidsstempel = this.tidsstempel,
                    sykmeldingsperiode = this.sykmeldingsperiode.dto(),
                    periode = this.periode.dto(),
                    arbeidssituasjon = when (this.arbeidssituasjon) {
                        Arbeidssituasjon.ARBEIDSTAKER -> ArbeidssituasjonDto.ARBEIDSTAKER
                        Arbeidssituasjon.ARBEIDSLEDIG -> ArbeidssituasjonDto.ARBEIDSLEDIG
                        Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE -> ArbeidssituasjonDto.SELVSTENDIG_NÆRINGSDRIVENDE
                        Arbeidssituasjon.BARNEPASSER -> ArbeidssituasjonDto.BARNEPASSER
                        Arbeidssituasjon.FRILANSER -> ArbeidssituasjonDto.FRILANSER
                        Arbeidssituasjon.JORDBRUKER -> ArbeidssituasjonDto.JORDBRUKER
                        Arbeidssituasjon.FISKER -> ArbeidssituasjonDto.FISKER
                        Arbeidssituasjon.ANNET -> ArbeidssituasjonDto.ANNET
                    },
                    vilkårsgrunnlagId = vilkårsgrunnlagUtDto?.vilkårsgrunnlagId,
                    skjæringstidspunkt = this.skjæringstidspunkt,
                    skjæringstidspunkter = this.skjæringstidspunkter,
                    utbetalingId = utbetalingUtDto?.id,
                    utbetalingstatus = utbetalingUtDto?.tilstand,
                    dokumentsporing = this.dokumentsporing.dto(),
                    sykdomstidslinje = this.sykdomstidslinje.dto(),
                    utbetalingstidslinje = this.utbetalingstidslinje.dto(),
                    refusjonstidslinje = this.refusjonstidslinje.dto(),
                    dagerUtenNavAnsvar = this.dagerUtenNavAnsvar.dto(),
                    dagerNavOvertarAnsvar = this.dagerNavOvertarAnsvar.map { it.dto() },
                    egenmeldingsdager = this.egenmeldingsdager.map { it.dto() },
                    maksdatoresultat = this.maksdatoresultat.dto(),
                    inntektjusteringer = this.inntektjusteringer.map { (inntektskilde, beløpstidslinje) ->
                        inntektskilde.dto() to beløpstidslinje.dto()
                    }.toMap(),
                    faktaavklartInntekt = when (val fi = faktaavklartInntekt) {
                        is SelvstendigFaktaavklartInntekt -> fi.dto()
                        is ArbeidstakerFaktaavklartInntekt -> fi.dto()
                        null -> null
                    },
                    korrigertInntekt = korrigertInntekt?.dto()
                )
            }

            enum class Arbeidssituasjon {
                ARBEIDSTAKER,
                ARBEIDSLEDIG,
                FRILANSER,
                SELVSTENDIG_NÆRINGSDRIVENDE,
                BARNEPASSER,
                FISKER,
                JORDBRUKER,
                ANNET
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Behandling) return false
            return this.tilstand == other.tilstand && this.dokumentsporing == other.dokumentsporing
        }

        override fun hashCode(): Int {
            return this.dokumentsporing.hashCode()
        }

        internal fun forventerUtbetaling(periodeSomBeregner: Periode, skjæringstidspunkt: LocalDate, skalBehandlesISpeil: Boolean): Boolean {
            val skjæringstidspunktetErLikt = this.skjæringstidspunkt == skjæringstidspunkt
            val overlapperMedDenBeregnedePerioden = this.periode.overlapperMed(periodeSomBeregner)
            val relevantForBeregning = skjæringstidspunktetErLikt && overlapperMedDenBeregnedePerioden

            if (!relevantForBeregning) return false
            return when (this.tilstand) {
                Tilstand.UberegnetRevurdering -> true

                Tilstand.Uberegnet,
                Tilstand.UberegnetOmgjøring -> skalBehandlesISpeil

                Tilstand.AnnullertPeriode,
                Tilstand.UberegnetAnnullering,
                Tilstand.AvsluttetUtenVedtak,
                Tilstand.Beregnet,
                Tilstand.BeregnetOmgjøring,
                Tilstand.BeregnetRevurdering,
                Tilstand.OverførtAnnullering,
                Tilstand.RevurdertVedtakAvvist,
                Tilstand.TilInfotrygd,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt -> false

            }
        }

        internal fun erÅpenForEndring() = when (tilstand) {
            Tilstand.Beregnet,
            Tilstand.BeregnetOmgjøring,
            Tilstand.BeregnetRevurdering,
            Tilstand.Uberegnet,
            Tilstand.UberegnetOmgjøring,
            Tilstand.UberegnetRevurdering,
            Tilstand.UberegnetAnnullering -> true

            Tilstand.AnnullertPeriode,
            Tilstand.AvsluttetUtenVedtak,
            Tilstand.OverførtAnnullering,
            Tilstand.RevurdertVedtakAvvist,
            Tilstand.TilInfotrygd,
            Tilstand.VedtakFattet,
            Tilstand.VedtakIverksatt -> false
        }

        internal fun erFattetVedtak() = vedtakFattet != null
        internal fun erInFlight() = erFattetVedtak() && !erAvsluttet()
        internal fun erAvsluttet() = avsluttet != null
        internal fun erAnnullert() = tilstand == Tilstand.AnnullertPeriode
        internal fun erAvvist() = tilstand == Tilstand.RevurdertVedtakAvvist
        internal fun harÅpenBehandling() = this.tilstand in setOf(Tilstand.UberegnetRevurdering, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd, Tilstand.UberegnetAnnullering)
        internal fun harIkkeUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd, Tilstand.UberegnetAnnullering)

        internal fun harOppdragMedUtbetalinger() = gjeldende.utbetaling?.harOppdragMedUtbetalinger() == true

        internal fun vedtakFattet(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
            when (tilstand) {
                Tilstand.Beregnet,
                Tilstand.BeregnetOmgjøring,
                Tilstand.BeregnetRevurdering -> tilstand.vedtakFattet(this@Behandling, behandlingEventBus, utbetalingEventBus, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)

                Tilstand.AnnullertPeriode,
                Tilstand.AvsluttetUtenVedtak,
                Tilstand.OverførtAnnullering,
                Tilstand.RevurdertVedtakAvvist,
                Tilstand.TilInfotrygd,
                Tilstand.Uberegnet,
                Tilstand.UberegnetAnnullering,
                Tilstand.UberegnetOmgjøring,
                Tilstand.UberegnetRevurdering,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt -> error("Forventer ikke å få utbetalingsavgjørelse i tilstand $tilstand")
            }
        }

        internal fun vedtakAvvist(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg): Boolean {
            gjeldende.utbetaling?.ikkeGodkjent(utbetalingEventBus, aktivitetslogg, utbetalingsavgjørelse.vurdering)
            when (tilstand) {
                Tilstand.Beregnet,
                Tilstand.BeregnetOmgjøring -> {
                    tilstand.vedtakAvvist(this@Behandling, behandlingEventBus, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)
                    return true
                }
                Tilstand.BeregnetRevurdering -> {
                    aktivitetslogg.varsel(RV_UT_24)
                    tilstand.vedtakAvvist(this@Behandling, behandlingEventBus, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)
                    return false
                }

                Tilstand.AnnullertPeriode,
                Tilstand.AvsluttetUtenVedtak,
                Tilstand.OverførtAnnullering,
                Tilstand.RevurdertVedtakAvvist,
                Tilstand.TilInfotrygd,
                Tilstand.Uberegnet,
                Tilstand.UberegnetAnnullering,
                Tilstand.UberegnetOmgjøring,
                Tilstand.UberegnetRevurdering,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt -> error("Forventer ikke å få utbetalingsavgjørelse i tilstand $tilstand")
            }
        }

        internal fun avsluttUtenVedtak(behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
            tilstand.avsluttUtenVedtak(this, behandlingEventBus, yrkesaktivitet, utbetalingstidslinje, inntekterForBeregning)
        }

        internal fun forkastBehandling(
            eventBus: EventBus,
            behandlingEventBus: BehandlingEventBus,
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            aktivitetslogg: IAktivitetslogg,
            automatiskBehandling: Boolean
        ) {
            tilstand.forkastBehandling(this, eventBus, behandlingEventBus, yrkesaktivitet, behandlingkilde, aktivitetslogg, automatiskBehandling)
        }

        private fun tilstand(nyTilstand: Tilstand) {
            tilstand.leaving(this)
            tilstand = nyTilstand
            tilstand.entering(this)
        }

        fun forkastBeregning(utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            tilstand.utenBeregning(this, utbetalingEventBus, aktivitetslogg)
        }

        fun utbetaling() = gjeldende.utbetaling
        fun utbetaling(
            beregning: BeregnetBehandling,
            yrkesaktivitet: Behandlingsporing.Yrkesaktivitet
        ) = tilstand.beregning(this, beregning, yrkesaktivitet)

        internal fun håndterAnnullering(utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg): Behandling? {
            return this.tilstand.håndterAnnullering(this, utbetalingEventBus, yrkesaktivitet, aktivitetslogg)
        }

        internal fun leggTilAnnullering(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, annullering: Utbetaling, vurdering: Utbetaling.Vurdering, forrigeVedtak: Behandling, aktivitetslogg: IAktivitetslogg) {
            tilstand.leggTilAnnullering(this, behandlingEventBus, utbetalingEventBus, annullering, vurdering, forrigeVedtak.gjeldende.grunnlagsdata!!, aktivitetslogg)
        }

        fun dokumentHåndtert(dokumentsporing: Dokumentsporing) =
            dokumentsporing in this.dokumentsporing

        internal fun oppdaterDokumentsporing(dokument: Dokumentsporing) {
            return tilstand.oppdaterDokumentsporing(this, dokument)
        }

        private fun kopierMedDokument(dokument: Dokumentsporing) {
            if (gjeldende.dokumentsporing == dokument) return
            nyEndring(gjeldende.kopierDokument(dokument))
        }

        private fun kopierMedUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
            nyEndring(gjeldende.kopierMedUtbetalingstidslinje(utbetalingstidslinje, inntekterForBeregning))
        }

        private fun utenBeregning(utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
            gjeldende.utbetaling!!.forkast(utbetalingEventBus, aktivitetslogg)
            nyEndring(gjeldende.kopierUtenBeregning())
        }

        private fun nyEndring(endring: Endring?) {
            if (endring == null) return
            check(endringer.none { it.id == endring.id }) { "Endringer må ha unik ID" }
            check(endringer.none { it.tidsstempel == endring.tidsstempel }) { "Endringer må ha unik tidsstempel" }
            check(endringer.none { it.tidsstempel > endring.tidsstempel }) { "Endringer må ha nyere tidsstempel" }
            endringer.add(endring)
        }

        fun oppdaterSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>) {
            tilstand.oppdaterSkjæringstidspunkt(this, beregnetSkjæringstidspunkter, beregnetPerioderUtenNavAnsvar)
        }

        private fun oppdaterMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>) {
            val endring = endringer.last().kopierMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetPerioderUtenNavAnsvar) ?: return
            nyEndring(endring)
        }

        internal fun nyAnnulleringBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
        ): Behandling {
            return nyBehandling(yrkesaktivitet, behandlingkilde, Tilstand.UberegnetAnnullering)
        }

        internal fun nyForkastetBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde
        ): Behandling {
            return nyBehandling(yrkesaktivitet, behandlingkilde, Tilstand.TilInfotrygd, LocalDateTime.now())
        }

        internal fun nyBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
        ): Behandling {
            val starttilstand = when (tilstand) {
                Tilstand.AvsluttetUtenVedtak -> Tilstand.UberegnetOmgjøring
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt,
                Tilstand.RevurdertVedtakAvvist -> Tilstand.UberegnetRevurdering

                Tilstand.AnnullertPeriode,
                Tilstand.Beregnet,
                Tilstand.BeregnetOmgjøring,
                Tilstand.BeregnetRevurdering,
                Tilstand.OverførtAnnullering,
                Tilstand.TilInfotrygd,
                Tilstand.Uberegnet,
                Tilstand.UberegnetAnnullering,
                Tilstand.UberegnetOmgjøring,
                Tilstand.UberegnetRevurdering -> error("Forventer ikke ny behandling fra tilstand $tilstand")
            }
            return nyBehandling(yrkesaktivitet, behandlingkilde, starttilstand)
        }

        private fun nyBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            starttilstand: Tilstand,
            avsluttet: LocalDateTime? = null
        ): Behandling {
            yrkesaktivitet.låsOpp(periode)
            return Behandling(
                tilstand = starttilstand,
                endringer = listOf(endringer.last().kopierUtenBeregning()),
                avsluttet = avsluttet,
                kilde = behandlingkilde
            )
        }

        fun håndterUtbetalinghendelse(behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
            tilstand.håndterUtbetalinghendelse(this, behandlingEventBus, utbetalingEventBus, hendelse, aktivitetslogg)
        }

        fun kanForkastes(andreBehandlinger: List<Behandling>): Boolean {
            return kanForkastesBasertPåTilstand {
                kanForkastingAvKortPeriodeTillates(andreBehandlinger)
            }
        }

        private fun kanForkastesBasertPåTilstand(hvisAUU: () -> Boolean = { true }): Boolean {
            return when (tilstand) {
                Tilstand.TilInfotrygd,
                Tilstand.AnnullertPeriode,
                Tilstand.Beregnet,
                Tilstand.Uberegnet -> true

                Tilstand.RevurdertVedtakAvvist,
                Tilstand.BeregnetRevurdering,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt,
                Tilstand.UberegnetRevurdering,
                Tilstand.OverførtAnnullering,
                Tilstand.UberegnetAnnullering -> false

                Tilstand.AvsluttetUtenVedtak,
                Tilstand.BeregnetOmgjøring,
                Tilstand.UberegnetOmgjøring -> hvisAUU()

            }
        }

        fun harFlereSkjæringstidspunkt(): Boolean {
            // ett eller færre skjæringstidspunkter er ok
            if (skjæringstidspunkter.size <= 1) return false
            val arbeidsgiverperioden = dagerUtenNavAnsvar.periode
            if (arbeidsgiverperioden == null) return false
            val skjæringstidspunkterEtterAgp = skjæringstidspunkter.filter {
                it > arbeidsgiverperioden.endInclusive
            }

            // hvis perioden har flere skjæringstidspunkter, men ingen er utenfor agp så er det greit
            if (skjæringstidspunkterEtterAgp.isEmpty()) return false
            // hvis perioden har flere skjæringstidspunkter etter agp så er det noe muffins
            if (skjæringstidspunkterEtterAgp.size > 1) return true

            // hvis perioden har ett skjæringstidspunkt utenfor agp må vi sjekke
            // om det er sykedager mellom agp og skjæringstidspunktet. Perioden kan jo
            // ha blitt strukket tilbake for å hensynta agp-dagene, og da vil perioden ha
            // skjæringstidspunkt utenfor agp (men det er helt ok!)
            val ekstraSkjæringstidspunkt = skjæringstidspunkterEtterAgp.single()
            if (ekstraSkjæringstidspunkt == periode.start) return false
            val mellomliggendePeriode = arbeidsgiverperioden
                .periodeMellom(ekstraSkjæringstidspunkt)
                ?.subset(periode)
                ?: return false

            // sjekker om det finnes en dagtype som gir rett på utbetaling mellom agp og det ekstra skjæringstidspunktet
            val harSykdomMellomAGPOgSkjæringstidspunktet = sykdomstidslinje
                .subset(mellomliggendePeriode)
                .any { dag -> dag is Sykedag }

            return harSykdomMellomAGPOgSkjæringstidspunktet
        }

        private fun behandlingLukket(behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet) {
            yrkesaktivitet.lås(periode)
            behandlingEventBus.behandlingLukket(id)
        }

        internal fun byggUtkastTilVedtak(builder: UtkastTilVedtakBuilder): UtkastTilVedtakBuilder {
            builder
                .behandlingId(id)
                .arbeidssituasjon(arbeidssituasjon)
                .apply { vedtakFattet?.also { vedtakFattet(it) } }
                .hendelseIder(dokumentsporing.ider())
            gjeldende.byggUtkastTilVedtak(builder)
            return builder
        }

        /* hvorvidt en AUU- (eller har vært-auu)-periode kan forkastes */
        private fun kanForkastingAvKortPeriodeTillates(andreBehandlinger: List<Behandling>): Boolean {
            val overlappendeBehandlinger = andreBehandlinger.filter { it.dagerUtenNavAnsvar.dager.any { it.overlapperMed(this.periode) } }
            return overlappendeBehandlinger.all { it.kanForkastesBasertPåTilstand() }
        }

        internal fun validerFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = tilstand.validerFerdigBehandlet(this, meldingsreferanseId, aktivitetslogg)
        internal fun validerIkkeFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = tilstand.validerIkkeFerdigBehandlet(this, meldingsreferanseId, aktivitetslogg)
        private fun valideringFeilet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg, feil: String) {
            // Om de er hendelsen vi håndterer nå som har skapt situasjonen feiler vi fremfor å gå videre.
            if (kilde.meldingsreferanseId == meldingsreferanseId) error(feil)
            // Om det er krøll fra tidligere logger vi bare
            else aktivitetslogg.info("Eksisterende ugyldig behandling på en ferdig behandlet vedtaksperiode: $feil")
        }

        internal companion object {
            val List<Behandling>.sykmeldingsperiode get() = first().periode
            val List<Behandling>.dokumentsporing get() = map { it.dokumentsporing }.takeUnless { it.isEmpty() }?.reduce(Set<Dokumentsporing>::plus) ?: emptySet()
            fun initiellBehandling(
                sykdomstidslinje: Sykdomstidslinje,
                arbeidssituasjon: Arbeidssituasjon,
                egenmeldingsdager: List<Periode>,
                faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
                dokumentsporing: Dokumentsporing,
                sykmeldingsperiode: Periode,
                behandlingkilde: Behandlingkilde
            ) =
                Behandling(
                    tilstand = Tilstand.Uberegnet,
                    endringer = listOf(
                        Endring(
                            id = UUID.randomUUID(),
                            tidsstempel = LocalDateTime.now(),
                            grunnlagsdata = null,
                            utbetaling = null,
                            arbeidssituasjon = arbeidssituasjon,
                            dokumentsporing = dokumentsporing,
                            sykdomstidslinje = sykdomstidslinje,
                            sykmeldingsperiode = sykmeldingsperiode,
                            utbetalingstidslinje = Utbetalingstidslinje(),
                            refusjonstidslinje = Beløpstidslinje(),
                            periode = checkNotNull(sykdomstidslinje.periode()) { "kan ikke opprette behandling på tom sykdomstidslinje" },
                            skjæringstidspunkt = IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT,
                            skjæringstidspunkter = emptyList(),
                            dagerUtenNavAnsvar = DagerUtenNavAnsvaravklaring(false, emptyList()),
                            egenmeldingsdager = egenmeldingsdager,
                            dagerNavOvertarAnsvar = emptyList(),
                            maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                            inntektjusteringer = emptyMap(),
                            faktaavklartInntekt = faktaavklartInntekt,
                            korrigertInntekt = null
                        )
                    ),
                    avsluttet = null,
                    kilde = behandlingkilde
                )

            internal fun List<Behandling>.vurderVarselForGjenbrukAvInntekt(faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt, aktivitetslogg: IAktivitetslogg) {
                val førsteEndringMedInntekten = firstNotNullOf { behandling -> behandling.endringer.firstOrNull { endring -> endring.faktaavklartInntekt == faktaavklartInntekt } }

                faktaavklartInntekt.medInnteksdato(gjeldendeEndring().skjæringstidspunkt).vurderVarselForGjenbrukAvInntekt(
                    forrigeDato = førsteEndringMedInntekten.skjæringstidspunkt,
                    harNyArbeidsgiverperiode = gjeldendeEndring().dagerUtenNavAnsvar != førsteEndringMedInntekten.dagerUtenNavAnsvar,
                    aktivitetslogg = aktivitetslogg
                )
            }

            internal fun List<Behandling>.harGjenbrukbarInntekt(organisasjonsnummer: String) = forrigeEndringMedGjenbrukbarInntekt(organisasjonsnummer) != null
            internal fun List<Behandling>.lagreGjenbrukbarInntekt(skjæringstidspunkt: LocalDate, organisasjonsnummer: String, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) {
                val (forrigeEndring, vilkårsgrunnlag) = forrigeEndringMedGjenbrukbarInntekt(organisasjonsnummer) ?: return
                val nyArbeidsgiverperiode = forrigeEndring.arbeidsgiverperiodeEndret(gjeldendeEndring())
                // Herfra bruker vi "gammel" løype - kanskje noe kan skrus på fra det punktet her om en skulle skru på dette
                vilkårsgrunnlag.lagreTidsnæreInntekter(skjæringstidspunkt, yrkesaktivitet, aktivitetslogg, nyArbeidsgiverperiode)
            }

            private fun List<Behandling>.forrigeEndringMedGjenbrukbarInntekt(organisasjonsnummer: String): Pair<Endring, VilkårsgrunnlagElement>? =
                forrigeEndringMed { it.grunnlagsdata?.harGjenbrukbarInntekt(organisasjonsnummer) == true }?.let { it to it.grunnlagsdata!! }

            private fun List<Behandling>.gjeldendeEndring() = this.last().gjeldende
            private fun List<Behandling>.forrigeEndringMed(predikat: (endring: Endring) -> Boolean) =
                this.asReversed().firstNotNullOfOrNull { behandling ->
                    behandling.endringer.asReversed().firstNotNullOfOrNull { endring ->
                        endring.takeIf { predikat(it) }
                    }
                }

            internal fun List<Behandling>.grunnbeløpsregulert(): Boolean {
                val gjeldende = gjeldendeEndring().takeIf { it.grunnlagsdata != null } ?: return false
                val forrige = forrigeEndringMed { it.tidsstempel < gjeldende.tidsstempel && it.grunnlagsdata != null && it.utbetaling != null } ?: return false
                if (forrige.skjæringstidspunkt != gjeldende.skjæringstidspunkt) return false
                return listOf(forrige.grunnlagsdata!!, gjeldende.grunnlagsdata!!).harUlikeGrunnbeløp()
            }

            internal fun Map<UUID, Behandling>.berik(builder: UtkastTilVedtakBuilder) = forEach { (vedtaksperiodeId, behandling) ->
                builder.relevantPeriode(vedtaksperiodeId, behandling.id, behandling.skjæringstidspunkt, behandling.periode)
            }

            internal fun gjenopprett(dto: BehandlingInnDto, grunnlagsdata: Map<UUID, VilkårsgrunnlagElement>, utbetalinger: Map<UUID, Utbetaling>): Behandling {
                return Behandling(
                    id = dto.id,
                    tilstand = when (dto.tilstand) {
                        BehandlingtilstandDto.ANNULLERT_PERIODE -> Tilstand.AnnullertPeriode
                        BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK -> Tilstand.AvsluttetUtenVedtak
                        BehandlingtilstandDto.BEREGNET -> Tilstand.Beregnet
                        BehandlingtilstandDto.BEREGNET_OMGJØRING -> Tilstand.BeregnetOmgjøring
                        BehandlingtilstandDto.BEREGNET_REVURDERING -> Tilstand.BeregnetRevurdering
                        BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST -> Tilstand.RevurdertVedtakAvvist
                        BehandlingtilstandDto.TIL_INFOTRYGD -> Tilstand.TilInfotrygd
                        BehandlingtilstandDto.UBEREGNET -> Tilstand.Uberegnet
                        BehandlingtilstandDto.UBEREGNET_OMGJØRING -> Tilstand.UberegnetOmgjøring
                        BehandlingtilstandDto.UBEREGNET_REVURDERING -> Tilstand.UberegnetRevurdering
                        BehandlingtilstandDto.VEDTAK_FATTET -> Tilstand.VedtakFattet
                        BehandlingtilstandDto.VEDTAK_IVERKSATT -> Tilstand.VedtakIverksatt
                        BehandlingtilstandDto.UBEREGNET_ANNULLERING -> Tilstand.UberegnetAnnullering
                        BehandlingtilstandDto.OVERFØRT_ANNULLERING -> Tilstand.OverførtAnnullering
                    },
                    endringer = dto.endringer.map { Endring.gjenopprett(it, grunnlagsdata, utbetalinger) }.toMutableList(),
                    vedtakFattet = dto.vedtakFattet,
                    avsluttet = dto.avsluttet,
                    kilde = Behandlingkilde.gjenopprett(dto.kilde)
                )
            }
        }

        internal sealed interface Tilstand {
            fun entering(behandling: Behandling) {}
            fun leaving(behandling: Behandling) {}

            fun forkastBehandling(
                behandling: Behandling,
                eventBus: EventBus,
                behandlingEventBus: BehandlingEventBus,
                yrkesaktivitet: Yrkesaktivitet,
                behandlingkilde: Behandlingkilde,
                aktivitetslogg: IAktivitetslogg,
                automatiskBehandling: Boolean
            ) {
                error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
            }

            fun oppdaterSkjæringstidspunkt(behandling: Behandling, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>) {}

            fun håndterAnnullering(
                behandling: Behandling,
                utbetalingEventBus: UtbetalingEventBus,
                yrkesaktivitet: Yrkesaktivitet,
                aktivitetslogg: IAktivitetslogg
            ): Behandling? {
                error("Har ikke implementert håndtering av annullering i $this")
            }

            fun leggTilAnnullering(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, annullering: Utbetaling, vurdering: Utbetaling.Vurdering, grunnlagsdata: VilkårsgrunnlagElement, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke legge til annullering i $this")
            }

            fun vedtakAvvist(
                behandling: Behandling,
                behandlingEventBus: BehandlingEventBus,
                yrkesaktivitet: Yrkesaktivitet,
                utbetalingsavgjørelse: Behandlingsavgjørelse,
                aktivitetslogg: IAktivitetslogg
            ) {
                error("Kan ikke avvise vedtak for behandling i $this")
            }

            fun vedtakFattet(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke fatte vedtak for behandling i $this")
            }

            fun avsluttUtenVedtak(behandling: Behandling, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
                error("Kan ikke avslutte uten vedtak for behandling i $this")
            }

            fun utenBeregning(behandling: Behandling, utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
                error("Støtter ikke å forkaste utbetaling utbetaling i $this")
            }

            fun beregning(
                behandling: Behandling,
                beregning: BeregnetBehandling,
                yrkesaktivitet: Behandlingsporing.Yrkesaktivitet
            ) {
                error("Støtter ikke å beregne behandlingen i $this")
            }

            fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) {}

            fun håndterUtbetalinghendelse(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
                error("forventer ikke å håndtere utbetalinghendelse i tilstand ${this.javaClass.simpleName}")
            }
            fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} burde vært ferdig behandlet, men står i tilstand ${behandling.tilstand::class.simpleName}")
            }

            fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                if (behandling.avsluttet == null) return
                behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} burde ikke ha avsluttet tidspunkt, fordi den ikke er ferdig behandlet")
            }

            data object Uberegnet : Tilstand {
                override fun entering(behandling: Behandling) {
                    check(behandling.utbetaling() == null) { "skal ikke ha utbetaling og være uberegnet samtidig" }
                }

                override fun forkastBehandling(behandling: Behandling, eventBus: EventBus, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, automatiskBehandling: Boolean) {
                    behandling.tilstand(TilInfotrygd)
                    behandlingEventBus.behandlingForkastet(behandling.id, automatiskBehandling)
                }

                override fun oppdaterSkjæringstidspunkt(
                    behandling: Behandling,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>
                ) {
                    behandling.oppdaterMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetPerioderUtenNavAnsvar)
                }

                override fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) =
                    behandling.kopierMedDokument(dokument)

                override fun utenBeregning(behandling: Behandling, utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {}
                override fun beregning(
                    behandling: Behandling,
                    beregning: BeregnetBehandling,
                    yrkesaktivitet: Behandlingsporing.Yrkesaktivitet
                ) {
                    val dagerNavOvertarAnsvar = when (yrkesaktivitet) {
                        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                        is Arbeidstaker,
                        Behandlingsporing.Yrkesaktivitet.Frilans -> null

                        Behandlingsporing.Yrkesaktivitet.Selvstendig -> when (beregning.selvstendigForsikring?.type) {
                            Forsikringstype.HundreProsentFraDagEn,
                            Forsikringstype.ÅttiProsentFraDagEn -> behandling.dagerUtenNavAnsvar.dager

                            Forsikringstype.HundreProsentFraDagSytten -> emptyList()
                            null -> null
                        }
                    } ?: behandling.dagerNavOvertarAnsvar

                    behandling.nyEndring(
                        behandling.gjeldende.kopierMedBeregning(
                            beregning,
                            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar
                        )
                    )
                    behandling.tilstand(Beregnet)
                }

                override fun avsluttUtenVedtak(behandling: Behandling, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
                    behandling.behandlingLukket(behandlingEventBus, yrkesaktivitet)
                    behandling.kopierMedUtbetalingstidslinje(utbetalingstidslinje, inntekterForBeregning)
                    behandling.tilstand(AvsluttetUtenVedtak)
                }
            }

            data object UberegnetOmgjøring : Tilstand by (Uberegnet) {
                override fun beregning(
                    behandling: Behandling,
                    beregning: BeregnetBehandling,
                    yrkesaktivitet: Behandlingsporing.Yrkesaktivitet
                ) {
                    val dagerNavOvertarAnsvar = when (yrkesaktivitet) {
                        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                        is Arbeidstaker,
                        Behandlingsporing.Yrkesaktivitet.Frilans -> null

                        Behandlingsporing.Yrkesaktivitet.Selvstendig -> when (beregning.selvstendigForsikring?.type) {
                            Forsikringstype.HundreProsentFraDagEn,
                            Forsikringstype.ÅttiProsentFraDagEn -> behandling.dagerUtenNavAnsvar.dager

                            Forsikringstype.HundreProsentFraDagSytten -> emptyList()
                            null -> null
                        }
                    } ?: behandling.dagerNavOvertarAnsvar

                    behandling.nyEndring(behandling.gjeldende.kopierMedBeregning(beregning, dagerNavOvertarAnsvar))
                    behandling.tilstand(BeregnetOmgjøring)
                }
            }

            data object UberegnetRevurdering : Tilstand by (Uberegnet) {
                override fun forkastBehandling(behandling: Behandling, eventBus: EventBus, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, automatiskBehandling: Boolean) {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun beregning(
                    behandling: Behandling,
                    beregning: BeregnetBehandling,
                    yrkesaktivitet: Behandlingsporing.Yrkesaktivitet
                ) {
                    val dagerNavOvertarAnsvar = when (yrkesaktivitet) {
                        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                        is Arbeidstaker,
                        Behandlingsporing.Yrkesaktivitet.Frilans -> null

                        Behandlingsporing.Yrkesaktivitet.Selvstendig -> when (beregning.selvstendigForsikring?.type) {
                            Forsikringstype.HundreProsentFraDagEn,
                            Forsikringstype.ÅttiProsentFraDagEn -> behandling.dagerUtenNavAnsvar.dager

                            Forsikringstype.HundreProsentFraDagSytten -> emptyList()
                            null -> null
                        }
                    } ?: behandling.dagerNavOvertarAnsvar

                    behandling.nyEndring(behandling.gjeldende.kopierMedBeregning(beregning, dagerNavOvertarAnsvar))
                    behandling.tilstand(BeregnetRevurdering)
                }

                override fun håndterAnnullering(
                    behandling: Behandling,
                    utbetalingEventBus: UtbetalingEventBus,
                    yrkesaktivitet: Yrkesaktivitet,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling? {
                    behandling.nyEndring(behandling.endringer.last().kopierUtenBeregning())
                    behandling.tilstand(UberegnetAnnullering)
                    return null
                }
            }

            data object Beregnet : Tilstand {
                override fun entering(behandling: Behandling) {
                    checkNotNull(behandling.gjeldende.grunnlagsdata)
                }

                override fun forkastBehandling(behandling: Behandling, eventBus: EventBus, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, automatiskBehandling: Boolean) {
                    behandling.gjeldende.forkastUtbetaling(with (yrkesaktivitet) { eventBus.utbetalingEventBus }, aktivitetslogg)
                    behandling.tilstand(TilInfotrygd)
                    behandlingEventBus.behandlingForkastet(behandling.id, automatiskBehandling)
                }

                override fun oppdaterSkjæringstidspunkt(
                    behandling: Behandling,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetPerioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>
                ) {
                    behandling.oppdaterMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetPerioderUtenNavAnsvar)
                }

                override fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) =
                    behandling.kopierMedDokument(dokument)

                override fun utenBeregning(behandling: Behandling, utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenBeregning(utbetalingEventBus, aktivitetslogg)
                    behandling.tilstand(Uberegnet)
                }

                override fun vedtakAvvist(behandling: Behandling, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
                    // perioden kommer til å bli kastet til infotrygd, gjør ikke tilstandsendring her
                }

                override fun vedtakFattet(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
                    behandling.gjeldende.utbetaling!!.godkjent(utbetalingEventBus, aktivitetslogg, utbetalingsavgjørelse.vurdering)
                    behandling.vedtakFattet = utbetalingsavgjørelse.avgjørelsestidspunkt
                    behandling.behandlingLukket(behandlingEventBus, yrkesaktivitet)
                    behandling.tilstand(if (behandling.gjeldende.utbetaling?.harOppdragMedUtbetalinger() == true) VedtakFattet else VedtakIverksatt)
                }
            }

            data object BeregnetOmgjøring : Tilstand by (Beregnet) {
                override fun utenBeregning(behandling: Behandling, utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenBeregning(utbetalingEventBus, aktivitetslogg)
                    behandling.tilstand(UberegnetOmgjøring)
                }
            }

            data object BeregnetRevurdering : Tilstand by (Beregnet) {
                override fun forkastBehandling(behandling: Behandling, eventBus: EventBus, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, automatiskBehandling: Boolean) {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun utenBeregning(behandling: Behandling, utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenBeregning(utbetalingEventBus, aktivitetslogg)
                    behandling.tilstand(UberegnetRevurdering)
                }

                override fun vedtakAvvist(behandling: Behandling, behandlingEventBus: BehandlingEventBus, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
                    behandling.behandlingLukket(behandlingEventBus, yrkesaktivitet)
                    behandling.tilstand(RevurdertVedtakAvvist)
                }

                override fun håndterAnnullering(
                    behandling: Behandling,
                    utbetalingEventBus: UtbetalingEventBus,
                    yrkesaktivitet: Yrkesaktivitet,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(utbetalingEventBus, aktivitetslogg)
                    behandling.nyEndring(behandling.endringer.last().kopierUtenBeregning())
                    behandling.tilstand(UberegnetAnnullering)
                    return null
                }
            }

            data object RevurdertVedtakAvvist : Tilstand

            data object VedtakFattet : Tilstand {
                override fun entering(behandling: Behandling) {
                    checkNotNull(behandling.gjeldende.utbetaling)
                    checkNotNull(behandling.gjeldende.grunnlagsdata)
                }

                override fun håndterUtbetalinghendelse(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
                    val utbetaling = checkNotNull(behandling.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return
                    utbetaling.håndterUtbetalingmodulHendelse(utbetalingEventBus, hendelse, aktivitetslogg)
                    if (!utbetaling.erAvsluttet()) return
                    behandling.tilstand(VedtakIverksatt)
                }

                override fun utenBeregning(behandling: Behandling, utbetalingEventBus: UtbetalingEventBus, aktivitetslogg: IAktivitetslogg) {}

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}
            }

            data object AvsluttetUtenVedtak : Tilstand {
                override fun entering(behandling: Behandling) {
                    behandling.vedtakFattet = null // det fattes ikke vedtak i AUU
                    behandling.avsluttet = LocalDateTime.now()
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}

                override fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                    if (behandling.avsluttet != null && behandling.vedtakFattet == null) return
                    behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} er ferdig behandlet i tilstand AvsluttetUtenVedtak, men med uventede tidsstempler.")
                }
            }

            data object VedtakIverksatt : Tilstand {
                override fun entering(behandling: Behandling) {
                    behandling.avsluttet = LocalDateTime.now()
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}

                override fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                    if (behandling.avsluttet != null && behandling.vedtakFattet != null) return
                    behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} er ferdig behandlet i tilstand VedtakIverksatt, men med uventede tidsstempler.")
                }
            }

            data object AnnullertPeriode : Tilstand {
                override fun entering(behandling: Behandling) {
                    behandling.avsluttet = LocalDateTime.now()
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}
            }

            data object UberegnetAnnullering : Tilstand {
                override fun leggTilAnnullering(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, annullering: Utbetaling, vurdering: Utbetaling.Vurdering, grunnlagsdata: VilkårsgrunnlagElement, aktivitetslogg: IAktivitetslogg) {
                    behandling.nyEndring(behandling.gjeldende.kopierMedAnnullering(grunnlagsdata, annullering))

                    annullering.godkjent(utbetalingEventBus, aktivitetslogg, vurdering)

                    if (annullering.erAvsluttet()) {
                        behandling.tilstand(AnnullertPeriode)
                        behandlingEventBus.behandlingForkastet(behandling.id, false)
                    }
                    else behandling.tilstand(OverførtAnnullering)
                }
            }

            data object OverførtAnnullering : Tilstand {
                override fun håndterUtbetalinghendelse(behandling: Behandling, behandlingEventBus: BehandlingEventBus, utbetalingEventBus: UtbetalingEventBus, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
                    val utbetaling = checkNotNull(behandling.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return
                    utbetaling.håndterUtbetalingmodulHendelse(utbetalingEventBus, hendelse, aktivitetslogg)
                    if (!utbetaling.erAvsluttet()) return
                    behandling.tilstand(AnnullertPeriode)
                    behandlingEventBus.behandlingForkastet(behandling.id, false)
                }
            }

            data object TilInfotrygd : Tilstand {
                override fun entering(behandling: Behandling) {
                    behandling.avsluttet = LocalDateTime.now()
                }

                override fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                    if (behandling.avsluttet != null && behandling.vedtakFattet == null) return
                    behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} er ferdig behandlet i tiltand TilInfotrygd, men med uventede tidsstempler.")
                }
            }
        }

        internal fun dto() = BehandlingUtDto(
            id = this.id,
            tilstand = when (tilstand) {
                Tilstand.AnnullertPeriode -> BehandlingtilstandDto.ANNULLERT_PERIODE
                Tilstand.AvsluttetUtenVedtak -> BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK
                Tilstand.Beregnet -> BehandlingtilstandDto.BEREGNET
                Tilstand.BeregnetOmgjøring -> BehandlingtilstandDto.BEREGNET_OMGJØRING
                Tilstand.BeregnetRevurdering -> BehandlingtilstandDto.BEREGNET_REVURDERING
                Tilstand.RevurdertVedtakAvvist -> BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST
                Tilstand.TilInfotrygd -> BehandlingtilstandDto.TIL_INFOTRYGD
                Tilstand.Uberegnet -> BehandlingtilstandDto.UBEREGNET
                Tilstand.UberegnetOmgjøring -> BehandlingtilstandDto.UBEREGNET_OMGJØRING
                Tilstand.UberegnetRevurdering -> BehandlingtilstandDto.UBEREGNET_REVURDERING
                Tilstand.VedtakFattet -> BehandlingtilstandDto.VEDTAK_FATTET
                Tilstand.VedtakIverksatt -> BehandlingtilstandDto.VEDTAK_IVERKSATT
                Tilstand.UberegnetAnnullering -> BehandlingtilstandDto.UBEREGNET_ANNULLERING
                Tilstand.OverførtAnnullering -> BehandlingtilstandDto.OVERFØRT_ANNULLERING
            },
            endringer = this.endringer.map { it.dto() },
            vedtakFattet = this.vedtakFattet,
            avsluttet = this.avsluttet,
            kilde = this.kilde.dto(),
        )
    }

    internal fun dto() = BehandlingerUtDto(behandlinger = this.behandlinger.map { it.dto() })

    internal fun harSammeUtbetalingSom(annenVedtaksperiode: Vedtaksperiode): Boolean {
        val sisteVedtak = behandlinger.lastOrNull { it.erFattetVedtak() } ?: return false
        return annenVedtaksperiode.behandlinger.utbetaling?.let { sisteVedtak.utbetaling()?.hørerSammen(it) } ?: false
    }
}

internal data class BeregnetBehandling(
    val maksdatoresultat: Maksdatoresultat,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val grunnlagsdata: VilkårsgrunnlagElement,
    val alleInntektjusteringer: Map<Inntektskilde, Beløpstidslinje>,
    val selvstendigForsikring: SelvstendigForsikring? = null
) {
    fun lagUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode,
        utbetalinger: List<Utbetaling>,
        mottakerRefusjon: String,
        mottakerBruker: String,
        harFattetVedtak: Boolean,
        arbeidssituasjon: Arbeidssituasjon
    ): Utbetaling {
        val utbetalingtype = if (harFattetVedtak)
            Utbetalingtype.REVURDERING
        else
            Utbetalingtype.UTBETALING

        val klassekodeBruker = when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSLEDIG,
            Arbeidssituasjon.ARBEIDSTAKER -> Klassekode.SykepengerArbeidstakerOrdinær

            Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE -> Klassekode.SelvstendigNæringsdrivendeOppgavepliktig
            Arbeidssituasjon.BARNEPASSER -> Klassekode.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig
            Arbeidssituasjon.JORDBRUKER -> Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk
            Arbeidssituasjon.FRILANSER,
            Arbeidssituasjon.FISKER,
            Arbeidssituasjon.ANNET -> TODO("har ikke klassekode for $arbeidssituasjon")
        }

        val utbetalingen = Utbetaling.lagUtbetaling(
            utbetalinger = utbetalinger,
            vedtaksperiodekladd = lagUtbetalingkladd(utbetalingstidslinje, mottakerRefusjon, mottakerBruker, klassekodeBruker),
            utbetalingstidslinje = utbetalingstidslinje,
            periode = periode,
            aktivitetslogg = aktivitetslogg,
            maksdato = maksdatoresultat.maksdato,
            forbrukteSykedager = maksdatoresultat.antallForbrukteDager,
            gjenståendeSykedager = maksdatoresultat.gjenståendeDager,
            type = utbetalingtype
        )

        return utbetalingen
    }

    private fun lagUtbetalingkladd(
        utbetalingstidslinje: Utbetalingstidslinje,
        mottakerRefusjon: String,
        mottakerBruker: String,
        klassekodeBruker: Klassekode
    ): Utbetalingkladd {
        return UtbetalingkladdBuilder(
            tidslinje = utbetalingstidslinje,
            mottakerRefusjon = mottakerRefusjon,
            mottakerBruker = mottakerBruker,
            klassekodeBruker = klassekodeBruker
        ).build()
    }
}

internal data class BehandlingerView(
    val behandlinger: List<BehandlingView>,
    val hendelser: Set<Dokumentsporing>
)

internal data class BehandlingView(
    val id: UUID,
    val periode: Periode,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: BehandlingkildeView,
    val tilstand: TilstandView,
    val endringer: List<BehandlingendringView>,
    val faktaavklartInntekt: FaktaavklartInntektView?,
    val korrigertInntekt: Saksbehandler.SaksbehandlerView?
) {
    enum class TilstandView {
        ANNULLERT_PERIODE, AVSLUTTET_UTEN_VEDTAK,
        BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING,
        REVURDERT_VEDTAK_AVVIST,
        TIL_INFOTRYGD, UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING,
        VEDTAK_FATTET, VEDTAK_IVERKSATT, UBEREGNET_ANNULLERING, OVERFØRT_ANNULLERING
    }
}

internal data class BehandlingendringView(
    val id: UUID,
    val sykmeldingsperiode: Periode,
    val periode: Periode,
    val sykdomstidslinje: Sykdomstidslinje,
    val grunnlagsdata: VilkårsgrunnlagElement?,
    val utbetaling: UtbetalingView?,
    val dokumentsporing: Dokumentsporing,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val refusjonstidslinje: Beløpstidslinje,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val dagerNavOvertarAnsvar: List<Periode>,
    val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaring,
    val egenmeldingsdager: List<Periode>,
    val maksdatoresultat: Maksdatoresultat
)

internal data class BehandlingkildeView(
    val meldingsreferanseId: MeldingsreferanseId,
    val innsendt: LocalDateTime,
    val registert: LocalDateTime,
    val avsender: Avsender
)
