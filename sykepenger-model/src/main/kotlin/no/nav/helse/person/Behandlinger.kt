package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.ArbeidssituasjonDto
import no.nav.helse.dto.BehandlingkildeDto
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.deserialisering.BehandlingInnDto
import no.nav.helse.dto.deserialisering.BehandlingendringInnDto
import no.nav.helse.dto.deserialisering.BehandlingerInnDto
import no.nav.helse.dto.serialisering.BehandlingUtDto
import no.nav.helse.dto.serialisering.BehandlingendringUtDto
import no.nav.helse.dto.serialisering.BehandlingerUtDto
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.UtbetalingsavgjørelseHendelse
import no.nav.helse.hendelser.avvist
import no.nav.helse.hendelser.til
import no.nav.helse.person.Behandlinger.Behandling.Companion.berik
import no.nav.helse.person.Behandlinger.Behandling.Companion.dokumentsporing
import no.nav.helse.person.Behandlinger.Behandling.Companion.forrigeDokumentsporing
import no.nav.helse.person.Behandlinger.Behandling.Companion.grunnbeløpsregulert
import no.nav.helse.person.Behandlinger.Behandling.Companion.harGjenbrukbarInntekt
import no.nav.helse.person.Behandlinger.Behandling.Companion.lagreGjenbrukbarInntekt
import no.nav.helse.person.Behandlinger.Behandling.Endring.Arbeidssituasjon
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.eksterneIder
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.PersonObserver.AnalytiskDatapakkeEvent
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.Companion.harUlikeGrunnbeløp
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView
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
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingView
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeForVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperioderesultat
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperioderesultat.Companion.finn
import no.nav.helse.utbetalingstidslinje.ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class Behandlinger private constructor(behandlinger: List<Behandling>) : Aktivitetskontekst {
    internal constructor() : this(emptyList())

    companion object {
        internal fun Map<UUID, Behandlinger>.berik(builder: UtkastTilVedtakBuilder) = mapValues { (_, behandlinger) -> behandlinger.behandlinger.last() }.berik(builder)
        fun gjenopprett(dto: BehandlingerInnDto, grunnlagsdata: Map<UUID, VilkårsgrunnlagElement>, utbetalinger: Map<UUID, Utbetaling>) = Behandlinger(
            behandlinger = dto.behandlinger.map { Behandling.gjenopprett(it, grunnlagsdata, utbetalinger) }
        )
    }

    private val behandlinger = behandlinger.toMutableList()
    private val siste get() = behandlinger.lastOrNull()?.utbetaling()

    internal fun sisteUtbetalteUtbetaling() = behandlinger.lastOrNull { it.erFattetVedtak() }?.utbetaling()
    internal fun harNoenBehandlingUtbetaling(utbetalingsId: UUID) = behandlinger.any { it.utbetaling()?.id == utbetalingsId }

    internal fun harFattetVedtak() = behandlinger.any { it.erFattetVedtak() }

    private val observatører = mutableListOf<BehandlingObserver>()

    val sisteBehandlingId get() = behandlinger.last().id

    internal fun initiellBehandling(
        sykmeldingsperiode: Periode,
        sykdomstidslinje: Sykdomstidslinje,
        arbeidssituasjon: Arbeidssituasjon,
        egenmeldingsdager: List<Periode>,
        faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
        inntektsendringer: Beløpstidslinje,
        ventetid: Periode?,
        dokumentsporing: Dokumentsporing,
        behandlingkilde: Behandlingkilde,
        selvstendigForsikring: Boolean?
    ) {
        check(behandlinger.isEmpty())

        // Hvis bruker har oppgitt å ha forsikring så sier vi at
        // nav overtar ansvar for dagene i ventetiden
        val dagerNavOvertarAnsvar = when (selvstendigForsikring) {
            null,
            false -> emptyList()
            true -> ventetid ?.let { listOf(ventetid) } ?: emptyList()
        }

        val behandling = Behandling.nyBehandling(
            observatører = this.observatører,
            sykdomstidslinje = sykdomstidslinje,
            arbeidssituasjon = arbeidssituasjon,
            egenmeldingsdager = egenmeldingsdager,
            faktaavklartInntekt = faktaavklartInntekt,
            inntektsendringer = inntektsendringer,
            dokumentsporing = dokumentsporing,
            sykmeldingsperiode = sykmeldingsperiode,
            ventetid = ventetid,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            behandlingkilde = behandlingkilde
        )
        leggTilNyBehandling(behandling)
    }

    internal fun addObserver(observatør: BehandlingObserver) {
        observatører.add(observatør)
        behandlinger.forEach { it.addObserver(observatør) }
    }

    internal fun view() = BehandlingerView(
        behandlinger = behandlinger.map { it.view() },
        hendelser = hendelseIder()
    )

    internal fun arbeidsgiverperiode() = ArbeidsgiverperiodeForVedtaksperiode(
        vedtaksperiode = periode(),
        arbeidsgiverperiode = behandlinger.last().arbeidsgiverperiode,
        dagerNavOvertarAnsvar = behandlinger.last().dagerNavOvertarAnsvar
    )

    internal fun utbetalingstidslinjeBuilderForArbeidstaker(): ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode {
        return ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
            arbeidsgiverperiode = behandlinger.last().arbeidsgiverperiode.dager,
            dagerNavOvertarAnsvar = behandlinger.last().dagerNavOvertarAnsvar,
            refusjonstidslinje = behandlinger.last().refusjonstidslinje
        )
    }

    internal fun utbetalingstidslinjeBuilderForSelvstendig(): SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode {
        val dekningsgrad = when (val arbeidssituasjon = behandlinger.last().arbeidssituasjon) {
            Arbeidssituasjon.BARNEPASSER,
            Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE -> 80.prosent

            Arbeidssituasjon.JORDBRUKER -> 100.prosent

            Arbeidssituasjon.ANNET,
            Arbeidssituasjon.FISKER,
            Arbeidssituasjon.ARBEIDSTAKER,
            Arbeidssituasjon.ARBEIDSLEDIG,
            Arbeidssituasjon.FRILANSER -> error("Har ikke implementert dekningsgrad for $arbeidssituasjon")
        }

        return SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(dekningsgrad, behandlinger.last().ventetid!!, behandlinger.last().dagerNavOvertarAnsvar)
    }

    internal val maksdato get() = behandlinger.last().maksdato
    internal val dagerNavOvertarAnsvar get() = behandlinger.last().dagerNavOvertarAnsvar
    internal val faktaavklartInntekt get() = behandlinger.last().faktaavklartInntekt
    internal val arbeidssituasjon get() = behandlinger.last().arbeidssituasjon

    internal fun analytiskDatapakke(yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, vedtaksperiodeId: UUID): AnalytiskDatapakkeEvent {
        val forrigeBehandling = behandlinger.dropLast(1).lastOrNull()
        return behandlinger.last().analytiskDatapakke(forrigeBehandling, yrkesaktivitetssporing, vedtaksperiodeId)
    }

    internal fun utbetalingstidslinjeFraForrigeVedtak() = behandlinger.lastOrNull { it.erFattetVedtak() }?.utbetalingstidslinje()
    internal fun utbetalingstidslinje() = behandlinger.last().utbetalingstidslinje()
    internal fun skjæringstidspunkt() = behandlinger.last().skjæringstidspunkt
    internal fun skjæringstidspunkter() = behandlinger.last().skjæringstidspunkter
    internal fun egenmeldingsdager() = behandlinger.last().egenmeldingsdager
    internal fun sykdomstidslinje() = behandlinger.last().sykdomstidslinje
    internal fun refusjonstidslinje() = behandlinger.last().refusjonstidslinje
    internal fun utbetales() = behandlinger.any { it.erInFlight() }
    internal fun erAvsluttet() = behandlinger.last().erAvsluttet()
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true
    internal fun kanForkastes(andreBehandlinger: List<Behandlinger>) = behandlinger.last().kanForkastes(andreBehandlinger.map { it.behandlinger.last() })
    internal fun forventerUtbetaling(periodeSomBeregner: Periode, skjæringstidspunkt: LocalDate, skalBehandlesISpeil: Boolean) =
        behandlinger.last().forventerUtbetaling(periodeSomBeregner, skjæringstidspunkt, skalBehandlesISpeil)

    internal fun harFlereSkjæringstidspunkt() = behandlinger.last().harFlereSkjæringstidspunkt()
    internal fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) = behandlinger.any { it.håndterUtbetalinghendelse(hendelse, aktivitetslogg) }

    internal fun validerFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = behandlinger.last().validerFerdigBehandlet(meldingsreferanseId, aktivitetslogg)
    internal fun validerIkkeFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = behandlinger.last().validerIkkeFerdigBehandlet(meldingsreferanseId, aktivitetslogg)
    internal fun gjelderIkkeFor(hendelse: UtbetalingsavgjørelseHendelse) = siste?.gjelderFor(hendelse) != true

    internal fun overlapperMed(other: Behandlinger): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        siste!!.valider(simulering, aktivitetslogg)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Behandling", mapOf("behandlingId" to sisteBehandlingId.toString()))
    }

    internal fun erKlarForGodkjenning() = siste?.erKlarForGodkjenning() ?: false
    internal fun simuler(aktivitetslogg: IAktivitetslogg) = siste!!.simuler(aktivitetslogg)
    internal fun godkjenning(aktivitetslogg: IAktivitetslogg, builder: UtkastTilVedtakBuilder) {
        if (behandlinger.grunnbeløpsregulert()) builder.grunnbeløpsregulert()
        behandlinger.last().godkjenning(aktivitetslogg, builder)
    }

    internal fun håndterAnnullering(yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg) {
        leggTilNyBehandling(
            behandlinger.last().håndterAnnullering(
                yrkesaktivitet,
                behandlingkilde,
                aktivitetslogg
            )
        )
    }

    internal fun leggTilAnnullering(annullering: Utbetaling, aktivitetslogg: IAktivitetslogg) {
        val forrigeVedtak = behandlinger.last { it.erFattetVedtak() }
        behandlinger.last().leggTilAnnullering(annullering, forrigeVedtak, aktivitetslogg)
    }

    internal fun sisteUtbetalingSkalOverføres(): Boolean {
        val sisteUtbetaling = siste
        checkNotNull(sisteUtbetaling) { "Finner ikke utbetaling i TIL_ANNULLERING, og det er ganske rart. BehandlingId ${behandlinger.last().id}" }
        return sisteUtbetaling.harOppdragMedUtbetalinger()
    }

    internal fun overførSisteUtbetaling(aktivitetslogg: IAktivitetslogg) {
        behandlinger.last().overførAnnullering(aktivitetslogg)
    }

    internal fun avsluttTomAnnullering(aktivitetslogg: IAktivitetslogg) {
        behandlinger.last().avsluttTomAnnullering(aktivitetslogg)
    }

    internal fun beregnetBehandling(
        aktivitetslogg: IAktivitetslogg,
        beregning: BeregnetBehandling
    ) {
        behandlinger.last().utbetaling(aktivitetslogg, beregning)
    }

    internal fun forkast(yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, automatiskBehandling: Boolean, aktivitetslogg: IAktivitetslogg) {
        leggTilNyBehandling(behandlinger.last().forkastVedtaksperiode(yrkesaktivitet, behandlingkilde, aktivitetslogg))
        behandlinger.last().forkastetBehandling(automatiskBehandling)
    }

    internal fun forkastUtbetaling(aktivitetslogg: IAktivitetslogg) {
        behandlinger.last().forkastUtbetaling(aktivitetslogg)
    }

    internal fun harIkkeUtbetaling() = behandlinger.last().harIkkeUtbetaling()
    fun vedtakFattet(yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
        this.behandlinger.last().vedtakFattet(yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)
    }

    fun bekreftAvsluttetBehandlingMedVedtak(yrkesaktivitet: Yrkesaktivitet) {
        bekreftAvsluttetBehandling(yrkesaktivitet)
        check(erFattetVedtak()) {
            "forventer at behandlingen skal ha fattet vedtak"
        }
    }

    private fun erFattetVedtak(): Boolean {
        return behandlinger.last().erFattetVedtak()
    }

    private fun bekreftAvsluttetBehandling(yrkesaktivitet: Yrkesaktivitet) {
        yrkesaktivitet.bekreftErLåst(periode())
        check(erAvsluttet()) {
            "forventer at utbetaling skal være avsluttet"
        }
    }

    fun avsluttUtenVedtak(yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
        check(behandlinger.last().utbetaling() == null) {
            "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. "
        }
        this.behandlinger.last().avsluttUtenVedtak(yrkesaktivitet, aktivitetslogg, utbetalingstidslinje, inntekterForBeregning)
        bekreftAvsluttetBehandling(yrkesaktivitet)
    }

    internal fun sykmeldingsperiode() = this.behandlinger.first().sykmeldingsperiode()
    internal fun periode() = this.behandlinger.last().periode()

    // sørger for ny behandling når vedtaksperioden går ut av Avsluttet/AUU,
    // men bare hvis det ikke er laget en ny allerede fra før
    fun sikreNyBehandling(yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>) {
        leggTilNyBehandling(behandlinger.last().sikreNyBehandling(yrkesaktivitet, behandlingkilde, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder))
    }

    private fun leggTilNyBehandling(behandling: Behandling?) {
        if (behandling == null) return
        if (behandlinger.isNotEmpty())
            check(behandlinger.last().tillaterNyBehandling(behandling)) {
                "siste behandling ${behandlinger.last()} tillater ikke opprettelse av ny behandling $behandling"
            }
        this.behandlinger.add(behandling)
    }

    fun bekreftÅpenBehandling(yrkesaktivitet: Yrkesaktivitet) {
        yrkesaktivitet.bekreftErÅpen(periode())
        check(behandlinger.last().harÅpenBehandling()) {
            "forventer at vedtaksperioden er uberegnet når den går ut av Avsluttet/AvsluttetUtenUtbetaling"
        }
    }

    internal fun subsumsjonslogg(regelverkslogg: Regelverkslogg, vedtaksperiodeId: UUID, fødselsnummer: String, organisasjonsnummer: String) =
        BehandlingSubsumsjonslogg(
            regelverkslogg = regelverkslogg,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlinger.last().id
        )

    internal fun hendelseIder() = behandlinger.dokumentsporing
    internal fun eksterneIder() = behandlinger.dokumentsporing.eksterneIder()
    internal fun søknadIder() = behandlinger.dokumentsporing.søknadIder()
    internal fun oppdaterDokumentsporing(dokument: Dokumentsporing) {
        return behandlinger.last().oppdaterDokumentsporing(dokument)
    }

    fun dokumentHåndtert(dokumentsporing: Dokumentsporing) =
        behandlinger.any { it.dokumentHåndtert(dokumentsporing) }

    internal fun harGjenbrukbarInntekt(organisasjonsnummer: String) =
        behandlinger.harGjenbrukbarInntekt(organisasjonsnummer)

    internal fun lagreGjenbrukbarInntekt(skjæringstidspunkt: LocalDate, organisasjonsnummer: String, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        behandlinger.lagreGjenbrukbarInntekt(skjæringstidspunkt, organisasjonsnummer, yrkesaktivitet, aktivitetslogg)

    internal fun håndterRefusjonstidslinje(
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg,
        beregnetSkjæringstidspunkter: Skjæringstidspunkter,
        beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
        refusjonstidslinje: Beløpstidslinje
    ): Boolean {
        val refusjonstidslinjeFør = behandlinger.last().refusjonstidslinje
        behandlinger.last().håndterRefusjonsopplysninger(yrkesaktivitet, behandlingkilde, dokumentsporing ?: behandlinger.forrigeDokumentsporing, aktivitetslogg, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder, refusjonstidslinje)?.also {
            leggTilNyBehandling(it)
        }
        val refusjonstidslinjeEtter = behandlinger.last().refusjonstidslinje
        val endret = refusjonstidslinjeFør != refusjonstidslinjeEtter
        return endret
    }

    internal fun håndterEgenmeldingsdager(
        person: Person,
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg,
        egenmeldingsdager: List<Periode>,
        egenmeldingsdagerAndrePerioder: List<Periode>
    ): Boolean {
        if (egenmeldingsdager == egenmeldingsdager()) return false

        håndterEndring(
            person = person,
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = behandlingkilde,
            dokumentsporing = dokumentsporing ?: behandlinger.forrigeDokumentsporing,
            hendelseSykdomstidslinje = Sykdomstidslinje(),
            egenmeldingsdagerAndrePerioder = egenmeldingsdagerAndrePerioder,
            aktivitetslogg = aktivitetslogg,
            egenmeldingsdager = egenmeldingsdager,
            dagerNavOvertarAnsvar = null,
            validering = { /* nei takk */ }
        )
        return true
    }

    fun håndterEndring(
        person: Person,
        yrkesaktivitet: Yrkesaktivitet,
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing,
        hendelseSykdomstidslinje: Sykdomstidslinje,
        egenmeldingsdagerAndrePerioder: List<Periode>,
        dagerNavOvertarAnsvar: List<Periode>?,
        egenmeldingsdager: List<Periode>?,
        aktivitetslogg: IAktivitetslogg,
        validering: () -> Unit
    ) {
        behandlinger.last().håndterEndring(
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = behandlingkilde,
            dokumentsporing = dokumentsporing,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            egenmeldingsdagerAndrePerioder = egenmeldingsdagerAndrePerioder,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            egenmeldingsdager = egenmeldingsdager,
            aktivitetslogg = aktivitetslogg
        )?.also {
            leggTilNyBehandling(it)
        }
        person.sykdomshistorikkEndret()
        validering()
    }

    fun oppdaterSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>) {
        behandlinger.last().oppdaterSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
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
        private var avsluttet: LocalDateTime?,
        private val kilde: Behandlingkilde,
        observatører: List<BehandlingObserver>
    ) {
        private val observatører = observatører.toMutableList()
        private val gjeldende get() = endringer.last()
        private val periode: Periode get() = gjeldende.periode
        private val tidsstempel = endringer.first().tidsstempel
        private val dokumentsporing get() = endringer.dokumentsporing
        val arbeidsgiverperiode get() = gjeldende.arbeidsgiverperiode
        val skjæringstidspunkt get() = gjeldende.skjæringstidspunkt
        val egenmeldingsdager get() = gjeldende.egenmeldingsdager
        val skjæringstidspunkter get() = gjeldende.skjæringstidspunkter
        val maksdato get() = gjeldende.maksdatoresultat
        val dagerNavOvertarAnsvar get() = gjeldende.dagerNavOvertarAnsvar
        val sykdomstidslinje get() = endringer.last().sykdomstidslinje
        val refusjonstidslinje get() = endringer.last().refusjonstidslinje
        val arbeidssituasjon get() = endringer.last().arbeidssituasjon
        val ventetid get() = endringer.last().ventetid
        val utbetalingstidslinje get() = endringer.last().utbetalingstidslinje
        val faktaavklartInntekt get() = endringer.last().faktaavklartInntekt
        val inntektsendringer get() = endringer.last().inntektsendringer

        constructor(observatører: List<BehandlingObserver>, tilstand: Tilstand, endringer: List<Endring>, avsluttet: LocalDateTime?, kilde: Behandlingkilde) : this(UUID.randomUUID(), tilstand, endringer.toMutableList(), null, avsluttet, kilde, observatører) {
            check(observatører.isNotEmpty()) {
                "må ha minst én observatør for å registrere en behandling"
            }
            tilstand.behandlingOpprettet(this)
        }

        init {
            check(endringer.isNotEmpty()) {
                "Må ha endringer for at det skal være vits med en behandling"
            }
        }

        internal fun addObserver(observatør: BehandlingObserver) {
            check(observatører.none { it === observatør }) { "observatør finnes fra før" }
            observatører.add(observatør)
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
                Tilstand.BeregnetAnnullering -> BehandlingView.TilstandView.BEREGNET_ANNULLERING
                Tilstand.OverførtAnnullering -> BehandlingView.TilstandView.OVERFØRT_ANNULLERING
            },
            endringer = endringer.map { it.view() },
            faktaavklartInntekt = faktaavklartInntekt?.view()
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
                harAndreInntekterIBeregning = this.inntektsendringer.isNotEmpty()
            )
        }

        fun utbetalingstidslinje() = gjeldende.utbetalingstidslinje

        internal fun håndterRefusjonsopplysninger(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            aktivitetslogg: IAktivitetslogg,
            beregnetSkjæringstidspunkter: Skjæringstidspunkter,
            beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
            nyRefusjonstidslinje: Beløpstidslinje
        ): Behandling? {
            val nyeRefusjonsopplysningerForPerioden = nyRefusjonstidslinje.subset(periode)
            val benyttetRefusjonsopplysninger = (gjeldende.refusjonstidslinje + nyeRefusjonsopplysningerForPerioden).fyll(periode)
            if (benyttetRefusjonsopplysninger == gjeldende.refusjonstidslinje) return null // Ingen endring
            return this.tilstand.håndterRefusjonsopplysninger(yrkesaktivitet, this, behandlingkilde, dokumentsporing, aktivitetslogg, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder, benyttetRefusjonsopplysninger)
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
            val inntektsendringer: Beløpstidslinje,
            val skjæringstidspunkt: LocalDate,
            val skjæringstidspunkter: List<LocalDate>,
            val egenmeldingsdager: List<Periode>,
            val arbeidsgiverperiode: Arbeidsgiverperiodeavklaring,
            val ventetid: Periode?,
            val dagerNavOvertarAnsvar: List<Periode>,
            val maksdatoresultat: Maksdatoresultat,
            val inntektjusteringer: Map<Inntektskilde, Beløpstidslinje>,
            val faktaavklartInntekt: SelvstendigFaktaavklartInntekt?
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
                inntektsendringer = inntektsendringer,
                skjæringstidspunkt = skjæringstidspunkt,
                skjæringstidspunkter = skjæringstidspunkter,
                arbeidsgiverperiode = arbeidsgiverperiode,
                egenmeldingsdager = egenmeldingsdager,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                maksdatoresultat = maksdatoresultat,
                ventetid = ventetid
            )

            private fun arbeidsgiverperiode(beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Arbeidsgiverperiodeavklaring {
                return beregnetArbeidsgiverperioder.finn(this.periode)?.let {
                    Arbeidsgiverperiodeavklaring(
                        ferdigAvklart = it.ferdigAvklart,
                        dager = it.arbeidsgiverperiode.grupperSammenhengendePerioder()
                    )
                } ?: Arbeidsgiverperiodeavklaring(false, emptyList())
            }
            private fun skjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, sykdomstidslinje: Sykdomstidslinje = this.sykdomstidslinje, periode: Periode = this.periode): Pair<LocalDate, List<LocalDate>> {
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

            companion object {
                val IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT = LocalDate.MIN
                val List<Endring>.dokumentsporing get() = map { it.dokumentsporing }.toSet()
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
                        inntektsendringer = Beløpstidslinje.gjenopprett(dto.inntektsendringer),
                        skjæringstidspunkt = dto.skjæringstidspunkt,
                        skjæringstidspunkter = dto.skjæringstidspunkter,
                        arbeidsgiverperiode = Arbeidsgiverperiodeavklaring.gjenopprett(dto.arbeidsgiverperiode),
                        ventetid = dto.ventetid?.let { Periode.gjenopprett(it) },
                        egenmeldingsdager = dto.egenmeldingsdager.map { Periode.gjenopprett(it) },
                        dagerNavOvertarAnsvar = dto.dagerNavOvertarAnsvar.map { Periode.gjenopprett(it) },
                        maksdatoresultat = dto.maksdatoresultat.let { Maksdatoresultat.gjenopprett(it) },
                        inntektjusteringer = dto.inntektjusteringer.map { (inntektskildeDto, beløpstidslinjeDto) ->
                            Inntektskilde.gjenopprett(inntektskildeDto) to Beløpstidslinje.gjenopprett(beløpstidslinjeDto)
                        }.toMap(),
                        faktaavklartInntekt = dto.faktaavklartInntekt?.let { SelvstendigFaktaavklartInntekt.gjenopprett(it) }
                    )
                }
            }

            internal fun arbeidsgiverperiodeEndret(other: Endring) =
                this.arbeidsgiverperiode != other.arbeidsgiverperiode

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
                inntektsendringer: Beløpstidslinje = this.inntektsendringer,
                skjæringstidspunkt: LocalDate = this.skjæringstidspunkt,
                skjæringstidspunkter: List<LocalDate> = this.skjæringstidspunkter,
                arbeidsgiverperiode: Arbeidsgiverperiodeavklaring = this.arbeidsgiverperiode,
                dagerNavOvertarAnsvar: List<Periode> = this.dagerNavOvertarAnsvar,
                egenmeldingsdager: List<Periode> = this.egenmeldingsdager,
                maksdatoresultat: Maksdatoresultat = this.maksdatoresultat,
                inntektjusteringer: Map<Inntektskilde, Beløpstidslinje> = this.inntektjusteringer,
                faktaavklartInntekt: SelvstendigFaktaavklartInntekt? = this.faktaavklartInntekt
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
                inntektsendringer = inntektsendringer,
                skjæringstidspunkt = skjæringstidspunkt,
                skjæringstidspunkter = skjæringstidspunkter,
                arbeidsgiverperiode = arbeidsgiverperiode,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                egenmeldingsdager = egenmeldingsdager,
                maksdatoresultat = maksdatoresultat,
                inntektjusteringer = inntektjusteringer,
                faktaavklartInntekt = faktaavklartInntekt
            )

            internal fun kopierMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Endring? {
                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = skjæringstidspunkt(beregnetSkjæringstidspunkter)
                val arbeidsgiverperiode = arbeidsgiverperiode(beregnetArbeidsgiverperioder)
                if (nyttSkjæringstidspunkt == this.skjæringstidspunkt && arbeidsgiverperiode == this.arbeidsgiverperiode) return null
                return kopierMed(
                    skjæringstidspunkt = nyttSkjæringstidspunkt,
                    skjæringstidspunkter = alleSkjæringstidspunkter,
                    arbeidsgiverperiode = arbeidsgiverperiode
                )
            }

            internal fun kopierMedEndring(
                periode: Periode,
                dokument: Dokumentsporing,
                sykdomstidslinje: Sykdomstidslinje,
                dagerNavOvertarAnsvar: List<Periode>?,
                egenmeldingsdager: List<Periode>?,
                beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>
            ): Endring {
                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = skjæringstidspunkt(beregnetSkjæringstidspunkter, sykdomstidslinje, periode)
                val arbeidsgiverperiodeavklaring = arbeidsgiverperiode(beregnetArbeidsgiverperioder)
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    dokumentsporing = dokument,
                    sykdomstidslinje = sykdomstidslinje,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    refusjonstidslinje = this.refusjonstidslinje.fyll(periode),
                    periode = periode,
                    skjæringstidspunkt = nyttSkjæringstidspunkt,
                    skjæringstidspunkter = alleSkjæringstidspunkter,
                    arbeidsgiverperiode = arbeidsgiverperiodeavklaring,
                    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar ?: this.dagerNavOvertarAnsvar,
                    egenmeldingsdager = egenmeldingsdager ?: this.egenmeldingsdager,
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    inntektjusteringer = emptyMap()
                )
            }

            internal fun kopierUtenUtbetaling(
                beregnSkjæringstidspunkt: Skjæringstidspunkter? = null,
                beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>? = null
            ): Endring {
                val beregnetSkjæringstidspunkt = beregnSkjæringstidspunkt?.let { skjæringstidspunkt(beregnSkjæringstidspunkt) }
                val arbeidsgiverperiode = beregnetArbeidsgiverperioder?.let { arbeidsgiverperiode(it) }
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    skjæringstidspunkt = beregnetSkjæringstidspunkt?.first ?: this.skjæringstidspunkt,
                    skjæringstidspunkter = beregnetSkjæringstidspunkt?.second ?: this.skjæringstidspunkter,
                    arbeidsgiverperiode = arbeidsgiverperiode ?: this.arbeidsgiverperiode,
                    inntektjusteringer = emptyMap()
                )
            }

            internal fun kopierMedRefusjonstidslinje(
                dokument: Dokumentsporing,
                refusjonstidslinje: Beløpstidslinje,
                beregnSkjæringstidspunkt: Skjæringstidspunkter? = null,
                beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>? = null
            ): Endring {
                val beregnetSkjæringstidspunkt = beregnSkjæringstidspunkt?.let { skjæringstidspunkt(beregnSkjæringstidspunkt) }
                val arbeidsgiverperiode = beregnetArbeidsgiverperioder?.let { arbeidsgiverperiode(it) }
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    dokumentsporing = dokument,
                    refusjonstidslinje = refusjonstidslinje,
                    skjæringstidspunkt = beregnetSkjæringstidspunkt?.first ?: this.skjæringstidspunkt,
                    skjæringstidspunkter = beregnetSkjæringstidspunkt?.second ?: this.skjæringstidspunkter,
                    arbeidsgiverperiode = arbeidsgiverperiode ?: this.arbeidsgiverperiode,
                    inntektjusteringer = emptyMap()
                )
            }

            internal fun kopierMedUtbetaling(beregning: BeregnetBehandling) = kopierMed(
                grunnlagsdata = beregning.grunnlagsdata,
                utbetaling = beregning.utbetaling,
                utbetalingstidslinje = beregning.utbetalingstidslinje.subset(this.periode),
                maksdatoresultat = beregning.maksdatoresultat,
                inntektjusteringer = beregning.alleInntektjusteringer
            )

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

            fun forkastUtbetaling(aktivitetslogg: IAktivitetslogg) {
                utbetaling?.forkast(aktivitetslogg)
            }

            fun godkjenning(
                aktivitetslogg: IAktivitetslogg,
                behandling: Behandling,
                utkastTilVedtakBuilder: UtkastTilVedtakBuilder
            ) {
                checkNotNull(utbetaling) { "Forventet ikke manglende utbetaling ved godkjenningsbehov" }
                checkNotNull(grunnlagsdata) { "Forventet ikke manglende vilkårsgrunnlag ved godkjenningsbehov" }
                val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(utbetaling)
                utkastTilVedtakBuilder
                    .utbetalingsinformasjon(utbetaling, utbetalingstidslinje, sykdomstidslinje, refusjonstidslinje)
                    .sykepengerettighet(maksdatoresultat.antallForbrukteDager, maksdatoresultat.gjenståendeDager, maksdatoresultat.maksdato)
                faktaavklartInntekt?.let {
                    utkastTilVedtakBuilder.pensjonsgivendeInntekter(it.pensjonsgivendeInntekter)
                }
                grunnlagsdata.berik(utkastTilVedtakBuilder)
                behandling.observatører.forEach { it.utkastTilVedtak(utkastTilVedtakBuilder.buildUtkastTilVedtak()) }
                Aktivitet.Behov.godkjenning(aktivitetsloggMedUtbetalingkontekst, utkastTilVedtakBuilder.buildGodkjenningsbehov())
            }

            internal fun berik(builder: UtkastTilVedtakBuilder) {
                checkNotNull(utbetaling) { "Forventet ikke manglende utbetaling ved utkast til vedtak" }
                checkNotNull(grunnlagsdata) { "Forventet ikke manglende vilkårsgrunnlag ved utkast til vedtak" }
                builder
                    .utbetalingsinformasjon(utbetaling, utbetalingstidslinje, sykdomstidslinje, refusjonstidslinje)
                    .sykepengerettighet(maksdatoresultat.antallForbrukteDager, maksdatoresultat.gjenståendeDager, maksdatoresultat.maksdato)
                grunnlagsdata.berik(builder)
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
                    inntektsendringer = this.inntektsendringer.dto(),
                    arbeidsgiverperiode = this.arbeidsgiverperiode.dto(),
                    dagerNavOvertarAnsvar = this.dagerNavOvertarAnsvar.map { it.dto() },
                    egenmeldingsdager = this.egenmeldingsdager.map { it.dto() },
                    maksdatoresultat = this.maksdatoresultat.dto(),
                    inntektjusteringer = this.inntektjusteringer.map { (inntektskilde, beløpstidslinje) ->
                        inntektskilde.dto() to beløpstidslinje.dto()
                    }.toMap(),
                    faktaavklartInntekt = faktaavklartInntekt?.dto(),
                    ventetid = ventetid?.dto()
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
                Tilstand.BeregnetAnnullering,
                Tilstand.OverførtAnnullering,
                Tilstand.RevurdertVedtakAvvist,
                Tilstand.TilInfotrygd,
                Tilstand.VedtakFattet,
                Tilstand.VedtakIverksatt -> false

            }
        }

        internal fun erFattetVedtak() = vedtakFattet != null
        internal fun erInFlight() = erFattetVedtak() && !erAvsluttet()
        internal fun erAvsluttet() = avsluttet != null
        internal fun harÅpenBehandling() = this.tilstand in setOf(Tilstand.UberegnetRevurdering, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd, Tilstand.UberegnetAnnullering)
        internal fun harIkkeUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd, Tilstand.UberegnetAnnullering)
        internal fun vedtakFattet(yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
            if (utbetalingsavgjørelse.avvist) return tilstand.vedtakAvvist(this, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)
            tilstand.vedtakFattet(this, yrkesaktivitet, utbetalingsavgjørelse, aktivitetslogg)
        }

        internal fun avsluttUtenVedtak(yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
            tilstand.avsluttUtenVedtak(this, yrkesaktivitet, aktivitetslogg, utbetalingstidslinje, inntekterForBeregning)
        }

        internal fun forkastVedtaksperiode(yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
            return tilstand.forkastVedtaksperiode(this, yrkesaktivitet, behandlingkilde, aktivitetslogg)
        }

        private fun tilstand(nyTilstand: Tilstand, aktivitetslogg: IAktivitetslogg) {
            tilstand.leaving(this)
            tilstand = nyTilstand
            tilstand.entering(this, aktivitetslogg)
        }

        fun forkastUtbetaling(aktivitetslogg: IAktivitetslogg) {
            tilstand.utenUtbetaling(this, aktivitetslogg)
        }

        fun utbetaling() = gjeldende.utbetaling
        fun utbetaling(
            aktivitetslogg: IAktivitetslogg,
            beregning: BeregnetBehandling
        ) = tilstand.utbetaling(this, aktivitetslogg, beregning)

        internal fun håndterAnnullering(yrkesaktivitet: Yrkesaktivitet, behandlingskilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
            return this.tilstand.håndterAnnullering(this, yrkesaktivitet, behandlingskilde, aktivitetslogg)
        }

        internal fun leggTilAnnullering(annullering: Utbetaling, forrigeVedtak: Behandling, aktivitetslogg: IAktivitetslogg) {
            tilstand.leggTilAnnullering(this, annullering, forrigeVedtak.gjeldende.grunnlagsdata!!, aktivitetslogg)
        }

        internal fun overførAnnullering(aktivitetslogg: IAktivitetslogg) {
            tilstand.overførAnnullering(this, aktivitetslogg)
        }

        internal fun avsluttTomAnnullering(aktivitetslogg: IAktivitetslogg) {
            tilstand.avsluttTomAnnullering(this, aktivitetslogg)
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

        private fun utenUtbetaling(aktivitetslogg: IAktivitetslogg) {
            gjeldende.utbetaling!!.forkast(aktivitetslogg)
            nyEndring(gjeldende.kopierUtenUtbetaling())
        }

        private fun nyEndring(endring: Endring?) {
            if (endring == null) return
            check(endringer.none { it.id == endring.id }) { "Endringer må ha unik ID" }
            check(endringer.none { it.tidsstempel == endring.tidsstempel }) { "Endringer må ha unik tidsstempel" }
            endringer.add(endring)
        }

        fun oppdaterSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>) {
            tilstand.oppdaterSkjæringstidspunkt(this, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
        }

        fun håndterEndring(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            egenmeldingsdagerAndrePerioder: List<Periode>,
            dagerNavOvertarAnsvar: List<Periode>?,
            egenmeldingsdager: List<Periode>?,
            aktivitetslogg: IAktivitetslogg
        ): Behandling? {
            return tilstand.håndterEndring(
                behandling = this,
                yrkesaktivitet = yrkesaktivitet,
                behandlingkilde = behandlingkilde,
                dokumentsporing = dokumentsporing,
                hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                egenmeldingsdagerAndrePerioder = egenmeldingsdagerAndrePerioder,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                egenmeldingsdager = egenmeldingsdager,
                aktivitetslogg = aktivitetslogg
            )
        }

        private fun håndtereEndring(
            yrkesaktivitet: Yrkesaktivitet,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            egenmeldingsdagerAndrePerioder: List<Periode>,
            dagerNavOvertarAnsvar: List<Periode>? = null,
            egenmeldingsdager: List<Periode>? = null
        ): Endring {
            val hendelseSykdomstidslinjeFremTilOgMed = hendelseSykdomstidslinje
                .fremTilOgMed(periode.endInclusive)
            val hendelseperiode = hendelseSykdomstidslinjeFremTilOgMed.periode()
            val oppdatertPeriode = hendelseperiode?.let { this.periode.oppdaterFom(hendelseperiode) } ?: this.periode
            // vi må oppdatere uansett om sykdomstidslinjen er tom, fordi egenmeldingsdager kan ha endret seg og dette påvirker agp
            val egenmeldingsperioder = egenmeldingsdagerAndrePerioder + (egenmeldingsdager ?: this.egenmeldingsdager)
            val (nySykdomstidslinje, nyeSkjæringstidspunkter, nyeArbeidsgiverperioder) = yrkesaktivitet.oppdaterSykdom(
                meldingsreferanseId = dokumentsporing.id,
                sykdomstidslinje = hendelseSykdomstidslinjeFremTilOgMed.takeIf { hendelseperiode != null },
                egenmeldingsperioder = egenmeldingsperioder
            )
            val sykdomstidslinje = nySykdomstidslinje.subset(oppdatertPeriode)
            return endringer.last().kopierMedEndring(oppdatertPeriode, dokumentsporing, sykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, nyeSkjæringstidspunkter, nyeArbeidsgiverperioder)
        }

        private fun oppdaterMedRefusjonstidslinje(dokumentsporing: Dokumentsporing, nyeRefusjonsopplysninger: Beløpstidslinje) {
            val endring = endringer.last().kopierMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
            nyEndring(endring)
        }

        private fun oppdaterMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>) {
            val endring = endringer.last().kopierMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder) ?: return
            nyEndring(endring)
        }

        // oppdaterer seg selv med endringen
        private fun oppdaterMedEndring(
            yrkesaktivitet: Yrkesaktivitet,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            egenmeldingsdagerAndrePerioder: List<Periode>,
            dagerNavOvertarAnsvar: List<Periode>?,
            egenmeldingsdager: List<Periode>?
        ) {
            val endring = håndtereEndring(
                yrkesaktivitet = yrkesaktivitet,
                dokumentsporing = dokumentsporing,
                hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                egenmeldingsdagerAndrePerioder = egenmeldingsdagerAndrePerioder,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                egenmeldingsdager = egenmeldingsdager
            )
            if (endring == gjeldende) return
            nyEndring(endring)
        }

        private fun nyBehandlingMedEndring(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            egenmeldingsdagerAndrePerioder: List<Periode>,
            dagerNavOvertarAnsvar: List<Periode>?,
            egenmeldingsdager: List<Periode>?,
            starttilstand: Tilstand = Tilstand.Uberegnet
        ): Behandling {
            yrkesaktivitet.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(
                    håndtereEndring(
                        yrkesaktivitet = yrkesaktivitet,
                        dokumentsporing = dokumentsporing,
                        hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                        egenmeldingsdagerAndrePerioder = egenmeldingsdagerAndrePerioder,
                        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                        egenmeldingsdager = egenmeldingsdager
                    )
                ),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        private fun nyBehandlingMedRefusjonstidslinje(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            beregnetSkjæringstidspunkter: Skjæringstidspunkter,
            beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
            nyRefusjonstidslinje: Beløpstidslinje,
            starttilstand: Tilstand = Tilstand.Uberegnet
        ): Behandling {
            yrkesaktivitet.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(
                    endringer.last().kopierMedRefusjonstidslinje(
                        dokumentsporing,
                        nyRefusjonstidslinje,
                        beregnetSkjæringstidspunkter,
                        beregnetArbeidsgiverperioder
                    )
                ),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        private fun sikreNyBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            starttilstand: Tilstand,
            behandlingkilde: Behandlingkilde,
            beregnetSkjæringstidspunkter: Skjæringstidspunkter,
            beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>
        ): Behandling {
            yrkesaktivitet.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(endringer.last().kopierUtenUtbetaling(beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        private fun nyAnnulleringBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
        ): Behandling {
            yrkesaktivitet.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = Tilstand.UberegnetAnnullering,
                endringer = listOf(endringer.last().kopierUtenUtbetaling()),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        fun sikreNyBehandling(
            yrkesaktivitet: Yrkesaktivitet,
            behandlingkilde: Behandlingkilde,
            beregnetSkjæringstidspunkter: Skjæringstidspunkter,
            beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>
        ): Behandling? {
            return tilstand.sikreNyBehandling(this, yrkesaktivitet, behandlingkilde, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
        }

        fun tillaterNyBehandling(other: Behandling): Boolean {
            return tilstand.tillaterNyBehandling(this, other)
        }

        fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg): Boolean {
            return tilstand.håndterUtbetalinghendelse(this, hendelse, aktivitetslogg)
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
                Tilstand.BeregnetAnnullering,
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
            val arbeidsgiverperioden = arbeidsgiverperiode.periode
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

        private fun behandlingLukket(yrkesaktivitet: Yrkesaktivitet) {
            yrkesaktivitet.lås(periode)
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            observatører.forEach { it.behandlingLukket(id) }
        }

        private fun vedtakIverksatt(aktivitetslogg: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            observatører.forEach { it.vedtakIverksatt(aktivitetslogg, vedtakFattet!!, this) }
        }

        private fun avsluttetUtenVedtak(aktivitetslogg: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            val dekkesAvArbeidsgiverperioden = arbeidsgiverperiode.periode?.inneholder(periode) != false
            observatører.forEach { it.avsluttetUtenVedtak(aktivitetslogg, id, avsluttet!!, periode, dekkesAvArbeidsgiverperioden, dokumentsporing.ider()) }
        }

        private fun emitNyBehandlingOpprettet(type: PersonObserver.BehandlingOpprettetEvent.Type) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            observatører.forEach { it.nyBehandling(id, periode, kilde.meldingsreferanseId, kilde.innsendt, kilde.registert, kilde.avsender, type, endringer.dokumentsporing.søknadIder()) }
        }

        internal fun forkastetBehandling(automatiskBehandling: Boolean) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            check(this.tilstand in setOf(Tilstand.TilInfotrygd, Tilstand.AnnullertPeriode))
            observatører.forEach { it.behandlingForkastet(id, automatiskBehandling) }
        }

        internal fun vedtakAnnullert(aktivitetslogg: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            check(this.tilstand === Tilstand.AnnullertPeriode)
            observatører.forEach { it.vedtakAnnullert(aktivitetslogg, id) }
        }

        internal fun godkjenning(aktivitetslogg: IAktivitetslogg, builder: UtkastTilVedtakBuilder) {
            builder.behandlingId(id).periode(arbeidsgiverperiode.dager, arbeidsgiverperiode.ferdigAvklart, periode).hendelseIder(dokumentsporing.ider()).skjæringstidspunkt(skjæringstidspunkt)
            gjeldende.godkjenning(aktivitetslogg, this, builder)
        }

        internal fun berik(builder: UtkastTilVedtakBuilder) {
            builder.behandlingId(id).periode(arbeidsgiverperiode.dager, arbeidsgiverperiode.ferdigAvklart, periode).hendelseIder(dokumentsporing.ider()).skjæringstidspunkt(skjæringstidspunkt)
            gjeldende.berik(builder)
        }

        /* hvorvidt en AUU- (eller har vært-auu)-periode kan forkastes */
        private fun kanForkastingAvKortPeriodeTillates(andreBehandlinger: List<Behandling>): Boolean {
            val overlappendeBehandlinger = andreBehandlinger.filter { it.arbeidsgiverperiode.dager.any { it.overlapperMed(this.periode) } }
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
            val List<Behandling>.forrigeDokumentsporing get() = last().gjeldende.dokumentsporing
            fun nyBehandling(
                observatører: List<BehandlingObserver>,
                sykdomstidslinje: Sykdomstidslinje,
                arbeidssituasjon: Endring.Arbeidssituasjon,
                egenmeldingsdager: List<Periode>,
                faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
                inntektsendringer: Beløpstidslinje,
                dokumentsporing: Dokumentsporing,
                sykmeldingsperiode: Periode,
                ventetid: Periode?,
                dagerNavOvertarAnsvar: List<Periode>,
                behandlingkilde: Behandlingkilde
            ) =
                Behandling(
                    observatører = observatører,
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
                            inntektsendringer = inntektsendringer,
                            periode = checkNotNull(sykdomstidslinje.periode()) { "kan ikke opprette behandling på tom sykdomstidslinje" },
                            skjæringstidspunkt = IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT,
                            skjæringstidspunkter = emptyList(),
                            arbeidsgiverperiode = Arbeidsgiverperiodeavklaring(false, emptyList()),
                            ventetid = ventetid,
                            egenmeldingsdager = egenmeldingsdager,
                            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                            maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                            inntektjusteringer = emptyMap(),
                            faktaavklartInntekt = faktaavklartInntekt
                        )
                    ),
                    avsluttet = null,
                    kilde = behandlingkilde
                )

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
                val forrige = forrigeEndringMed { it.tidsstempel < gjeldende.tidsstempel && it.grunnlagsdata != null } ?: return false
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
                        BehandlingtilstandDto.BEREGNET_ANNULLERING -> Tilstand.BeregnetAnnullering
                        BehandlingtilstandDto.OVERFØRT_ANNULLERING -> Tilstand.OverførtAnnullering
                    },
                    endringer = dto.endringer.map { Endring.gjenopprett(it, grunnlagsdata, utbetalinger) }.toMutableList(),
                    vedtakFattet = dto.vedtakFattet,
                    avsluttet = dto.avsluttet,
                    kilde = Behandlingkilde.gjenopprett(dto.kilde),
                    observatører = emptyList()
                )
            }
        }

        internal sealed interface Tilstand {
            fun behandlingOpprettet(behandling: Behandling) {
                error("Forventer ikke å opprette behandling i tilstand ${this.javaClass.simpleName}")
            }

            fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {}
            fun leaving(behandling: Behandling) {}

            fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                behandling.tilstand(TilInfotrygd, aktivitetslogg)
                return null
            }

            fun oppdaterSkjæringstidspunkt(behandling: Behandling, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>) {}
            fun håndterRefusjonsopplysninger(
                yrkesaktivitet: Yrkesaktivitet,
                behandling: Behandling,
                behandlingkilde: Behandlingkilde,
                dokumentsporing: Dokumentsporing,
                aktivitetslogg: IAktivitetslogg,
                beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                nyeRefusjonsopplysninger: Beløpstidslinje
            ): Behandling? {
                error("Har ikke implementert håndtering av refusjonsopplysninger i $this")
            }

            fun håndterEndring(
                behandling: Behandling,
                yrkesaktivitet: Yrkesaktivitet,
                behandlingkilde: Behandlingkilde,
                dokumentsporing: Dokumentsporing,
                hendelseSykdomstidslinje: Sykdomstidslinje,
                egenmeldingsdagerAndrePerioder: List<Periode>,
                dagerNavOvertarAnsvar: List<Periode>?,
                egenmeldingsdager: List<Periode>?,
                aktivitetslogg: IAktivitetslogg
            ): Behandling? {
                error("Har ikke implementert håndtering av endring i $this")
            }

            fun håndterAnnullering(
                behandling: Behandling,
                yrkesaktivitet: Yrkesaktivitet,
                behandlingkilde: Behandlingkilde,
                aktivitetslogg: IAktivitetslogg
            ): Behandling? {
                error("Har ikke implementert håndtering av annullering i $this")
            }

            fun leggTilAnnullering(behandling: Behandling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke legge til annullering i $this")
            }

            fun overførAnnullering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke overføre annullering i $this")
            }

            fun avsluttTomAnnullering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke avslutte tom annullering i $this")
            }

            fun vedtakAvvist(
                behandling: Behandling,
                yrkesaktivitet: Yrkesaktivitet,
                utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse,
                aktivitetslogg: IAktivitetslogg
            ) {
                error("Kan ikke avvise vedtak for behandling i $this")
            }

            fun vedtakFattet(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke fatte vedtak for behandling i $this")
            }

            fun avsluttUtenVedtak(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
                error("Kan ikke avslutte uten vedtak for behandling i $this")
            }

            fun avsluttMedVedtak(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke avslutte behandling i $this")
            }

            fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                error("Støtter ikke å forkaste utbetaling utbetaling i $this")
            }

            fun utbetaling(
                behandling: Behandling,
                aktivitetslogg: IAktivitetslogg,
                beregning: BeregnetBehandling
            ) {
                error("Støtter ikke å opprette utbetaling i $this")
            }

            fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) {}

            fun sikreNyBehandling(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Behandling? {
                return null
            }

            fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean = false
            fun håndterUtbetalinghendelse(behandling: Behandling, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) = false
            fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} burde vært ferdig behandlet, men står i tilstand ${behandling.tilstand::class.simpleName}")
            }

            fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                if (behandling.avsluttet == null) return
                behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} burde ikke ha avsluttet tidspunkt, fordi den ikke er ferdig behandlet")
            }

            data object Uberegnet : Tilstand {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Søknad)
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    check(behandling.utbetaling() == null) { "skal ikke ha utbetaling og være uberegnet samtidig" }
                }

                override fun oppdaterSkjæringstidspunkt(
                    behandling: Behandling,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>
                ) {
                    behandling.oppdaterMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
                }

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    return null
                }

                override fun håndterEndring(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, dokumentsporing: Dokumentsporing, hendelseSykdomstidslinje: Sykdomstidslinje, egenmeldingsdagerAndrePerioder: List<Periode>, dagerNavOvertarAnsvar: List<Periode>?, egenmeldingsdager: List<Periode>?, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.oppdaterMedEndring(yrkesaktivitet, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager)
                    return null
                }

                override fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) =
                    behandling.kopierMedDokument(dokument)

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {}
                override fun utbetaling(
                    behandling: Behandling,
                    aktivitetslogg: IAktivitetslogg,
                    beregning: BeregnetBehandling
                ) {
                    behandling.nyEndring(behandling.gjeldende.kopierMedUtbetaling(beregning))
                    behandling.tilstand(Beregnet, aktivitetslogg)
                }

                override fun avsluttUtenVedtak(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>) {
                    behandling.behandlingLukket(yrkesaktivitet)
                    behandling.kopierMedUtbetalingstidslinje(utbetalingstidslinje, inntekterForBeregning)
                    behandling.tilstand(AvsluttetUtenVedtak, aktivitetslogg)
                }
            }

            data object UberegnetOmgjøring : Tilstand by (Uberegnet) {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Omgjøring)
                override fun utbetaling(
                    behandling: Behandling,
                    aktivitetslogg: IAktivitetslogg,
                    beregning: BeregnetBehandling,
                ) {
                    behandling.nyEndring(behandling.gjeldende.kopierMedUtbetaling(beregning))
                    behandling.tilstand(BeregnetOmgjøring, aktivitetslogg)
                }
            }

            data object UberegnetRevurdering : Tilstand by (Uberegnet) {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering)

                override fun utbetaling(
                    behandling: Behandling,
                    aktivitetslogg: IAktivitetslogg,
                    beregning: BeregnetBehandling
                ) {
                    behandling.nyEndring(behandling.gjeldende.kopierMedUtbetaling(beregning))
                    behandling.tilstand(BeregnetRevurdering, aktivitetslogg)
                }

                override fun håndterAnnullering(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling? {
                    behandling.nyEndring(behandling.endringer.last().kopierUtenUtbetaling())
                    behandling.tilstand(UberegnetAnnullering, aktivitetslogg)
                    return null
                }
            }

            data object Beregnet : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    checkNotNull(behandling.gjeldende.utbetaling)
                    checkNotNull(behandling.gjeldende.grunnlagsdata)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    return super.forkastVedtaksperiode(behandling, yrkesaktivitet, behandlingkilde, aktivitetslogg)
                }

                override fun oppdaterSkjæringstidspunkt(
                    behandling: Behandling,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>
                ) {
                    behandling.oppdaterMedNyttSkjæringstidspunkt(beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
                }

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    behandling.tilstand(Uberegnet, aktivitetslogg)
                    return null
                }

                override fun håndterEndring(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, dokumentsporing: Dokumentsporing, hendelseSykdomstidslinje: Sykdomstidslinje, egenmeldingsdagerAndrePerioder: List<Periode>, dagerNavOvertarAnsvar: List<Periode>?, egenmeldingsdager: List<Periode>?, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedEndring(yrkesaktivitet, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager)
                    behandling.tilstand(Uberegnet, aktivitetslogg)
                    return null
                }

                override fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) =
                    behandling.kopierMedDokument(dokument)

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenUtbetaling(aktivitetslogg)
                    behandling.tilstand(Uberegnet, aktivitetslogg)
                }

                override fun vedtakAvvist(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                    // perioden kommer til å bli kastet til infotrygd
                }

                override fun vedtakFattet(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                    behandling.vedtakFattet = utbetalingsavgjørelse.avgjørelsestidspunkt
                    behandling.behandlingLukket(yrkesaktivitet)
                    behandling.tilstand(if (behandling.gjeldende.utbetaling?.erAvsluttet() == true) VedtakIverksatt else VedtakFattet, aktivitetslogg)
                }
            }

            data object BeregnetOmgjøring : Tilstand by (Beregnet) {
                override fun håndterEndring(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, dokumentsporing: Dokumentsporing, hendelseSykdomstidslinje: Sykdomstidslinje, egenmeldingsdagerAndrePerioder: List<Periode>, dagerNavOvertarAnsvar: List<Periode>?, egenmeldingsdager: List<Periode>?, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedEndring(yrkesaktivitet, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager)
                    behandling.tilstand(UberegnetOmgjøring, aktivitetslogg)
                    return null
                }

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    behandling.tilstand(UberegnetOmgjøring, aktivitetslogg)
                    return null
                }

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenUtbetaling(aktivitetslogg)
                    behandling.tilstand(UberegnetOmgjøring, aktivitetslogg)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    return super.forkastVedtaksperiode(behandling, yrkesaktivitet, behandlingkilde, aktivitetslogg)
                }
            }

            data object BeregnetRevurdering : Tilstand by (Beregnet) {
                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    return super.forkastVedtaksperiode(behandling, yrkesaktivitet, behandlingkilde, aktivitetslogg)
                }

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenUtbetaling(aktivitetslogg)
                    behandling.tilstand(UberegnetRevurdering, aktivitetslogg)
                }

                override fun vedtakAvvist(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                    behandling.behandlingLukket(yrkesaktivitet)
                    behandling.tilstand(RevurdertVedtakAvvist, aktivitetslogg)
                }

                override fun håndterEndring(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    egenmeldingsdagerAndrePerioder: List<Periode>,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedEndring(yrkesaktivitet, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager)
                    behandling.tilstand(UberegnetRevurdering, aktivitetslogg)
                    return null
                }

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    behandling.tilstand(UberegnetRevurdering, aktivitetslogg)
                    return null
                }

                override fun håndterAnnullering(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.nyEndring(behandling.endringer.last().kopierUtenUtbetaling())
                    behandling.tilstand(UberegnetAnnullering, aktivitetslogg)
                    return null
                }
            }

            data object RevurdertVedtakAvvist : Tilstand {
                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun sikreNyBehandling(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Behandling {
                    return behandling.sikreNyBehandling(yrkesaktivitet, UberegnetRevurdering, behandlingkilde, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun håndterAnnullering(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling {
                    return behandling.nyAnnulleringBehandling(
                        yrkesaktivitet = yrkesaktivitet,
                        behandlingkilde = behandlingkilde
                    )
                }
            }

            data object VedtakFattet : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    checkNotNull(behandling.gjeldende.utbetaling)
                    checkNotNull(behandling.gjeldende.grunnlagsdata)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun sikreNyBehandling(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Behandling {
                    return behandling.sikreNyBehandling(yrkesaktivitet, UberegnetRevurdering, behandlingkilde, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
                }

                override fun håndterUtbetalinghendelse(behandling: Behandling, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg): Boolean {
                    val utbetaling = checkNotNull(behandling.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return false
                    if (utbetaling.erAvsluttet()) avsluttMedVedtak(behandling, aktivitetslogg)
                    return true
                }

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {}

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ) = behandling.nyBehandlingMedRefusjonstidslinje(yrkesaktivitet, behandlingkilde, dokumentsporing, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder, nyeRefusjonsopplysninger, UberegnetRevurdering)

                override fun håndterEndring(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    egenmeldingsdagerAndrePerioder: List<Periode>,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg
                ) = behandling.nyBehandlingMedEndring(yrkesaktivitet, behandlingkilde, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager, UberegnetRevurdering)

                override fun avsluttMedVedtak(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.tilstand(VedtakIverksatt, aktivitetslogg)
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}

                override fun håndterAnnullering(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling {
                    return behandling.nyAnnulleringBehandling(
                        yrkesaktivitet = yrkesaktivitet,
                        behandlingkilde = behandlingkilde
                    )
                }
            }

            data object AvsluttetUtenVedtak : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.vedtakFattet = null // det fattes ikke vedtak i AUU
                    behandling.avsluttet = LocalDateTime.now()
                    behandling.avsluttetUtenVedtak(aktivitetslogg)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    yrkesaktivitet.låsOpp(behandling.periode)
                    return Behandling(
                        observatører = behandling.observatører,
                        tilstand = TilInfotrygd,
                        endringer = listOf(behandling.gjeldende.kopierUtenUtbetaling()),
                        avsluttet = LocalDateTime.now(),
                        kilde = behandlingkilde
                    )
                }

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling {
                    return behandling.nyBehandlingMedRefusjonstidslinje(yrkesaktivitet, behandlingkilde, dokumentsporing, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder, nyeRefusjonsopplysninger, UberegnetOmgjøring)
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun sikreNyBehandling(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Behandling {
                    return behandling.sikreNyBehandling(yrkesaktivitet, UberegnetOmgjøring, behandlingkilde, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}

                override fun håndterEndring(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    egenmeldingsdagerAndrePerioder: List<Periode>,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling {
                    return behandling.nyBehandlingMedEndring(yrkesaktivitet, behandlingkilde, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager, UberegnetOmgjøring)
                }

                override fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                    if (behandling.avsluttet != null && behandling.vedtakFattet == null) return
                    behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} er ferdig behandlet i tilstand AvsluttetUtenVedtak, men med uventede tidsstempler.")
                }
            }

            data object VedtakIverksatt : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.avsluttet = LocalDateTime.now()
                    behandling.vedtakIverksatt(aktivitetslogg)
                }

                override fun håndterAnnullering(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg
                ): Behandling {
                    return behandling.nyAnnulleringBehandling(
                        yrkesaktivitet = yrkesaktivitet,
                        behandlingkilde = behandlingkilde
                    )
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}

                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun sikreNyBehandling(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, beregnetSkjæringstidspunkter: Skjæringstidspunkter, beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>): Behandling {
                    return behandling.sikreNyBehandling(yrkesaktivitet, UberegnetRevurdering, behandlingkilde, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder)
                }

                override fun håndterRefusjonsopplysninger(
                    yrkesaktivitet: Yrkesaktivitet,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnetSkjæringstidspunkter: Skjæringstidspunkter,
                    beregnetArbeidsgiverperioder: List<Arbeidsgiverperioderesultat>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ) = behandling.nyBehandlingMedRefusjonstidslinje(yrkesaktivitet, behandlingkilde, dokumentsporing, beregnetSkjæringstidspunkter, beregnetArbeidsgiverperioder, nyeRefusjonsopplysninger, UberegnetRevurdering)

                override fun håndterEndring(
                    behandling: Behandling,
                    yrkesaktivitet: Yrkesaktivitet,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    egenmeldingsdagerAndrePerioder: List<Periode>,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg
                ) = behandling.nyBehandlingMedEndring(yrkesaktivitet, behandlingkilde, dokumentsporing, hendelseSykdomstidslinje, egenmeldingsdagerAndrePerioder, dagerNavOvertarAnsvar, egenmeldingsdager, UberegnetRevurdering)

                override fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                    if (behandling.avsluttet != null && behandling.vedtakFattet != null) return
                    behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} er ferdig behandlet i tilstand VedtakIverksatt, men med uventede tidsstempler.")
                }
            }

            data object AnnullertPeriode : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.avsluttet = LocalDateTime.now()
                }

                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering)
                override fun forkastVedtaksperiode(behandling: Behandling, yrkesaktivitet: Yrkesaktivitet, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.vedtakAnnullert(aktivitetslogg)
                    return null
                }

                override fun validerIkkeFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {}
            }

            data object UberegnetAnnullering : Tilstand {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering)

                override fun leggTilAnnullering(behandling: Behandling, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement, aktivitetslogg: IAktivitetslogg) {
                    behandling.nyEndring(behandling.gjeldende.kopierMedAnnullering(grunnlagsdata, annullering))
                    behandling.tilstand(BeregnetAnnullering, aktivitetslogg)
                }
            }

            data object BeregnetAnnullering : Tilstand {
                override fun overførAnnullering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    val annullering = behandling.gjeldende.utbetaling
                    checkNotNull(annullering) { "Finner ikke utbetaling i BEREGNET_ANNULLERING, og det er ganske rart. BehandlingId ${behandling.id}" }
                    annullering.overfør(aktivitetslogg)
                    behandling.tilstand(OverførtAnnullering, aktivitetslogg)
                }

                override fun avsluttTomAnnullering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    val annullering = behandling.gjeldende.utbetaling
                    checkNotNull(annullering) { "Finner ikke utbetaling i BEREGNET_ANNULLERING, og det er ganske rart. BehandlingId ${behandling.id}" }
                    check(!annullering.harOppdragMedUtbetalinger()) { "Forventet tom annullering for behandlingId ${behandling.id}" }
                    annullering.avsluttTomAnnullering(aktivitetslogg)
                    behandling.tilstand(AnnullertPeriode, aktivitetslogg)
                }
            }

            data object OverførtAnnullering : Tilstand {
                override fun håndterUtbetalinghendelse(behandling: Behandling, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg): Boolean {
                    val utbetaling = checkNotNull(behandling.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return false
                    if (utbetaling.erAvsluttet()) behandling.tilstand(AnnullertPeriode, aktivitetslogg)
                    return true
                }
            }

            data object TilInfotrygd : Tilstand {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Omgjøring)
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
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
                Tilstand.BeregnetAnnullering -> BehandlingtilstandDto.BEREGNET_ANNULLERING
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
        return annenVedtaksperiode.behandlinger.siste?.let { sisteVedtak.utbetaling()?.hørerSammen(it) } ?: false
    }
}

internal data class BeregnetBehandling(
    val utbetaling: Utbetaling,
    val maksdatoresultat: Maksdatoresultat,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val grunnlagsdata: VilkårsgrunnlagElement,
    val alleInntektjusteringer: Map<Inntektskilde, Beløpstidslinje>
)

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
    val faktaavklartInntekt: SelvstendigFaktaavklartInntektView?
) {
    enum class TilstandView {
        ANNULLERT_PERIODE, AVSLUTTET_UTEN_VEDTAK,
        BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING,
        REVURDERT_VEDTAK_AVVIST,
        TIL_INFOTRYGD, UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING,
        VEDTAK_FATTET, VEDTAK_IVERKSATT, UBEREGNET_ANNULLERING, BEREGNET_ANNULLERING, OVERFØRT_ANNULLERING
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
    val inntektsendringer: Beløpstidslinje,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val dagerNavOvertarAnsvar: List<Periode>,
    val arbeidsgiverperiode: Arbeidsgiverperiodeavklaring,
    val ventetid: Periode?,
    val egenmeldingsdager: List<Periode>,
    val maksdatoresultat: Maksdatoresultat
)

internal data class BehandlingkildeView(
    val meldingsreferanseId: MeldingsreferanseId,
    val innsendt: LocalDateTime,
    val registert: LocalDateTime,
    val avsender: Avsender
)
