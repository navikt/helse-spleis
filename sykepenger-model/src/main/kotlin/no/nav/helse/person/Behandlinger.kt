package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.UtbetalingsavgjørelseHendelse
import no.nav.helse.hendelser.avvist
import no.nav.helse.hendelser.til
import no.nav.helse.person.Behandlinger.Behandling.Companion.berik
import no.nav.helse.person.Behandlinger.Behandling.Companion.dokumentsporing
import no.nav.helse.person.Behandlinger.Behandling.Companion.erUtbetaltPåForskjelligeUtbetalinger
import no.nav.helse.person.Behandlinger.Behandling.Companion.forrigeDokumentsporing
import no.nav.helse.person.Behandlinger.Behandling.Companion.grunnbeløpsregulert
import no.nav.helse.person.Behandlinger.Behandling.Companion.harGjenbrukbarInntekt
import no.nav.helse.person.Behandlinger.Behandling.Companion.lagreGjenbrukbarInntekt
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.eksterneIder
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.Companion.harUlikeGrunnbeløp
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.InntekterForBeregning
import no.nav.helse.person.inntekt.Inntektskilde
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.ArbeidsgiverHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.ForeldetSykedag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeForVedtaksperiode
import no.nav.helse.utbetalingstidslinje.BeregnetPeriode
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderVedtaksperiode

internal class Behandlinger private constructor(behandlinger: List<Behandling>) {
    internal constructor() : this(emptyList())

    companion object {
        internal fun Map<UUID, Behandlinger>.berik(builder: UtkastTilVedtakBuilder) = mapValues { (_, behandlinger) -> behandlinger.behandlinger.last() }.berik(builder)
        fun gjenopprett(dto: BehandlingerInnDto, grunnlagsdata: Map<UUID, VilkårsgrunnlagElement>, utbetalinger: Map<UUID, Utbetaling>) = Behandlinger(
            behandlinger = dto.behandlinger.map { Behandling.gjenopprett(it, grunnlagsdata, utbetalinger) }
        )
    }

    private val utbetalingene get() = behandlinger.mapNotNull(Behandling::utbetaling)
    private val behandlinger = behandlinger.toMutableList()
    private val siste get() = behandlinger.lastOrNull()?.utbetaling()

    private val observatører = mutableListOf<BehandlingObserver>()

    val sisteBehandlingId get() = behandlinger.last().id
    internal fun initiellBehandling(sykmeldingsperiode: Periode, sykdomstidslinje: Sykdomstidslinje, egenmeldingsdager: List<Periode>, inntektsendringer: Beløpstidslinje, dokumentsporing: Dokumentsporing, behandlingkilde: Behandlingkilde) {
        check(behandlinger.isEmpty())
        val behandling = Behandling.nyBehandling(this.observatører, sykdomstidslinje, egenmeldingsdager, inntektsendringer, dokumentsporing, sykmeldingsperiode, behandlingkilde)
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

    internal fun arbeidsgiverperiode() = ArbeidsgiverperiodeForVedtaksperiode(periode(), behandlinger.last().arbeidsgiverperiode)
    internal fun lagUtbetalingstidslinje(inntektstidslinje: Beløpstidslinje) =
        behandlinger.last().lagUtbetalingstidslinje(inntektstidslinje)

    internal val maksdato get() = behandlinger.last().maksdato
    internal val dagerNavOvertarAnsvar get() = behandlinger.last().dagerNavOvertarAnsvar
    internal fun utbetalingstidslinje() = behandlinger.last().utbetalingstidslinje()
    internal fun skjæringstidspunkt() = behandlinger.last().skjæringstidspunkt
    internal fun skjæringstidspunkter() = behandlinger.last().skjæringstidspunkter
    internal fun egenmeldingsdager() = behandlinger.last().egenmeldingsdager
    internal fun sykdomstidslinje() = behandlinger.last().sykdomstidslinje
    internal fun refusjonstidslinje() = behandlinger.last().refusjonstidslinje
    internal fun navOvertarAnsvar() = behandlinger.last().navOvertarAnsvar()
    internal fun trekkerTilbakePenger() = siste?.trekkerTilbakePenger() == true
    internal fun utbetales() = behandlinger.any { it.erInFlight() }
    internal fun erAvsluttet() = behandlinger.last().erAvsluttet()
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true
    internal fun kanForkastes(andreBehandlinger: List<Behandlinger>) = behandlinger.last().kanForkastes(andreBehandlinger.map { it.behandlinger.last() })

    internal fun harFlereSkjæringstidspunkt() = behandlinger.last().harFlereSkjæringstidspunkt()
    internal fun håndterUtbetalinghendelse(hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) = behandlinger.any { it.håndterUtbetalinghendelse(hendelse, aktivitetslogg) }
    internal fun behandlingVenter(builder: VedtaksperiodeVenter.Builder) {
        behandlinger.last().behandlingVenter(builder)
    }

    internal fun validerFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = behandlinger.last().validerFerdigBehandlet(meldingsreferanseId, aktivitetslogg)
    internal fun gjelderIkkeFor(hendelse: UtbetalingsavgjørelseHendelse) = siste?.gjelderFor(hendelse) != true

    internal fun overlapperMed(other: Behandlinger): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        siste!!.valider(simulering, aktivitetslogg)
    }

    internal fun erKlarForGodkjenning() = siste?.erKlarForGodkjenning() ?: false
    internal fun simuler(aktivitetslogg: IAktivitetslogg) = siste!!.simuler(aktivitetslogg)
    internal fun godkjenning(aktivitetslogg: IAktivitetslogg, builder: UtkastTilVedtakBuilder) {
        if (behandlinger.grunnbeløpsregulert()) builder.grunnbeløpsregulert()
        behandlinger.last().godkjenning(aktivitetslogg, builder)
    }

    internal fun håndterAnnullering(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, andreBehandlinger: List<Behandlinger>): Utbetaling? {
        val annullering = behandlinger.last().annuller(arbeidsgiver, hendelse, aktivitetslogg, this.behandlinger.toList()) ?: return null
        andreBehandlinger.forEach {
            it.kobleAnnulleringTilAndre(arbeidsgiver, behandlingkilde, aktivitetslogg, annullering)
        }
        return annullering
    }

    private fun kobleAnnulleringTilAndre(arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, annullering: Utbetaling) {
        leggTilNyBehandling(behandlinger.last().annuller(arbeidsgiver, behandlingkilde, aktivitetslogg, annullering, behandlinger.toList()))
    }

    internal fun nyUtbetaling(
        vedtaksperiodeSomLagerUtbetaling: UUID,
        arbeidsgiver: Arbeidsgiver,
        aktivitetslogg: IAktivitetslogg,
        beregning: BeregnetPeriode
    ) = behandlinger.last().utbetaling(vedtaksperiodeSomLagerUtbetaling, arbeidsgiver, aktivitetslogg, beregning)

    internal fun forkast(arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, automatiskBehandling: Boolean, aktivitetslogg: IAktivitetslogg) {
        leggTilNyBehandling(behandlinger.last().forkastVedtaksperiode(arbeidsgiver, behandlingkilde, aktivitetslogg))
        behandlinger.last().forkastetBehandling(automatiskBehandling)
    }

    internal fun forkastUtbetaling(aktivitetslogg: IAktivitetslogg) {
        behandlinger.last().forkastUtbetaling(aktivitetslogg)
    }

    internal fun harIkkeUtbetaling() = behandlinger.last().harIkkeUtbetaling()
    fun vedtakFattet(arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
        this.behandlinger.last().vedtakFattet(arbeidsgiver, utbetalingsavgjørelse, aktivitetslogg)
    }

    fun bekreftAvsluttetBehandlingMedVedtak(arbeidsgiver: Arbeidsgiver) {
        bekreftAvsluttetBehandling(arbeidsgiver)
        check(erFattetVedtak()) {
            "forventer at behandlingen skal ha fattet vedtak"
        }
    }

    private fun erFattetVedtak(): Boolean {
        return behandlinger.last().erFattetVedtak()
    }

    private fun bekreftAvsluttetBehandling(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.bekreftErLåst(periode())
        check(erAvsluttet()) {
            "forventer at utbetaling skal være avsluttet"
        }
    }

    fun avsluttUtenVedtak(arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: InntekterForBeregning) {
        check(behandlinger.last().utbetaling() == null) { "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. " }
        this.behandlinger.last().avsluttUtenVedtak(arbeidsgiver, aktivitetslogg, utbetalingstidslinje, inntekterForBeregning)
        bekreftAvsluttetBehandling(arbeidsgiver)
    }

    internal fun sykmeldingsperiode() = this.behandlinger.first().sykmeldingsperiode()
    internal fun periode() = this.behandlinger.last().periode()

    // sørger for ny behandling når vedtaksperioden går ut av Avsluttet/AUU,
    // men bare hvis det ikke er laget en ny allerede fra før
    fun sikreNyBehandling(arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>) {
        leggTilNyBehandling(behandlinger.last().sikreNyBehandling(arbeidsgiver, behandlingkilde, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode))
    }

    private fun leggTilNyBehandling(behandling: Behandling?) {
        if (behandling == null) return
        if (behandlinger.isNotEmpty())
            check(behandlinger.last().tillaterNyBehandling(behandling)) {
                "siste behandling ${behandlinger.last()} tillater ikke opprettelse av ny behandling $behandling"
            }
        this.behandlinger.add(behandling)
    }

    fun klarForUtbetaling(): Boolean {
        return behandlinger.last().klarForUtbetaling()
    }

    fun bekreftÅpenBehandling(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.bekreftErÅpen(periode())
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

    internal fun lagreGjenbrukbarInntekt(skjæringstidspunkt: LocalDate, organisasjonsnummer: String, arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg) =
        behandlinger.lagreGjenbrukbarInntekt(skjæringstidspunkt, organisasjonsnummer, arbeidsgiver, aktivitetslogg)

    internal fun håndterRefusjonstidslinje(
        arbeidsgiver: Arbeidsgiver,
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg,
        beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
        beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
        refusjonstidslinje: Beløpstidslinje
    ): Boolean {
        val refusjonstidslinjeFør = behandlinger.last().refusjonstidslinje
        behandlinger.last().håndterRefusjonsopplysninger(arbeidsgiver, behandlingkilde, dokumentsporing ?: behandlinger.forrigeDokumentsporing, aktivitetslogg, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, refusjonstidslinje)?.also {
            leggTilNyBehandling(it)
        }
        val refusjonstidslinjeEtter = behandlinger.last().refusjonstidslinje
        val endret = refusjonstidslinjeFør != refusjonstidslinjeEtter
        return endret
    }

    internal fun håndterEgenmeldingsdager(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg,
        beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
        beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
        egenmeldingsdager: List<Periode>
    ): Boolean {
        if (egenmeldingsdager == egenmeldingsdager()) return false

        håndterEndring(
            person = person,
            arbeidsgiver = arbeidsgiver,
            behandlingkilde = behandlingkilde,
            dokumentsporing = dokumentsporing ?: behandlinger.forrigeDokumentsporing,
            hendelseSykdomstidslinje = Sykdomstidslinje(),
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = beregnSkjæringstidspunkt,
            beregnArbeidsgiverperiode = beregnArbeidsgiverperiode,
            egenmeldingsdager = egenmeldingsdager,
            dagerNavOvertarAnsvar = null,
            validering = { /* nei takk */ }
        )
        return true
    }

    fun håndterEndring(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing,
        hendelseSykdomstidslinje: Sykdomstidslinje,
        dagerNavOvertarAnsvar: List<Periode>?,
        egenmeldingsdager: List<Periode>?,
        aktivitetslogg: IAktivitetslogg,
        beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
        beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
        validering: () -> Unit
    ) {
        behandlinger.last().håndterEndring(
            arbeidsgiver = arbeidsgiver,
            behandlingkilde = behandlingkilde,
            dokumentsporing = dokumentsporing,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            egenmeldingsdager = egenmeldingsdager,
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = beregnSkjæringstidspunkt,
            beregnArbeidsgiverperiode = beregnArbeidsgiverperiode
        )?.also {
            leggTilNyBehandling(it)
        }
        person.sykdomshistorikkEndret()
        validering()
    }

    fun beregnSkjæringstidspunkt(beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>) {
        behandlinger.last().beregnSkjæringstidspunkt(beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
    }

    fun erUtbetaltPåForskjelligeUtbetalinger(other: Behandlinger): Boolean {
        return this.behandlinger.erUtbetaltPåForskjelligeUtbetalinger(other.behandlinger)
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
            },
            endringer = endringer.map { it.view() },
        )

        fun sykmeldingsperiode() = endringer.first().sykmeldingsperiode
        fun periode() = periode
        fun behandlingVenter(builder: VedtaksperiodeVenter.Builder) {
            builder.behandlingVenter(id)
        }

        fun utbetalingstidslinje() = gjeldende.utbetalingstidslinje
        fun lagUtbetalingstidslinje(inntektstidslinje: Beløpstidslinje): Utbetalingstidslinje {
            val builder = UtbetalingstidslinjeBuilderVedtaksperiode(
                regler = ArbeidsgiverRegler.Companion.NormalArbeidstaker,
                arbeidsgiverperiode = arbeidsgiverperiode,
                dagerNavOvertarAnsvar = gjeldende.dagerNavOvertarAnsvar,
                refusjonstidslinje = refusjonstidslinje,
                inntektstidslinje = inntektstidslinje
            )
            return builder.result(sykdomstidslinje)
        }

        internal fun håndterRefusjonsopplysninger(
            arbeidsgiver: Arbeidsgiver,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            aktivitetslogg: IAktivitetslogg,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
            nyRefusjonstidslinje: Beløpstidslinje
        ): Behandling? {
            val nyeRefusjonsopplysningerForPerioden = nyRefusjonstidslinje.subset(periode)
            val benyttetRefusjonsopplysninger = (gjeldende.refusjonstidslinje + nyeRefusjonsopplysningerForPerioden).fyll(periode)
            if (benyttetRefusjonsopplysninger == gjeldende.refusjonstidslinje) return null // Ingen endring
            return this.tilstand.håndterRefusjonsopplysninger(arbeidsgiver, this, behandlingkilde, dokumentsporing, aktivitetslogg, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, benyttetRefusjonsopplysninger)
        }

        data class Endring(
            val id: UUID,
            val tidsstempel: LocalDateTime,
            val sykmeldingsperiode: Periode,
            val periode: Periode,
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
            val arbeidsgiverperiode: List<Periode>,
            val dagerNavOvertarAnsvar: List<Periode>,
            val maksdatoresultat: Maksdatoresultat,
            val inntekter: Map<Inntektskilde, Beløpstidslinje>
        ) {

            fun view() = BehandlingendringView(
                id = id,
                sykmeldingsperiode = sykmeldingsperiode,
                periode = periode,
                sykdomstidslinje = sykdomstidslinje,
                grunnlagsdata = grunnlagsdata,
                utbetaling = utbetaling,
                dokumentsporing = dokumentsporing,
                utbetalingstidslinje = utbetalingstidslinje,
                refusjonstidslinje = refusjonstidslinje,
                inntektsendringer = inntektsendringer,
                skjæringstidspunkt = skjæringstidspunkt,
                skjæringstidspunkter = skjæringstidspunkter,
                arbeidsgiverperiode = arbeidsgiverperiode,
                egenmeldingsdager = egenmeldingsdager,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                maksdatoresultat = maksdatoresultat
            )

            private fun skjæringstidspunkt(beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, sykdomstidslinje: Sykdomstidslinje = this.sykdomstidslinje, periode: Periode = this.periode): Pair<LocalDate, List<LocalDate>> {
                val sisteSykedag = sykdomstidslinje.lastOrNull {
                    // uttømmende when-blokk (uten else) med hensikt, fordi om nye det lages nye
                    // dagtyper så vil det bli compile error og vi blir tvunget til å måtte ta stilling til den
                    when (it) {
                        is ArbeidsgiverHelgedag,
                        is Arbeidsgiverdag,
                        is ForeldetSykedag,
                        is SykHelgedag,
                        is Sykedag -> true

                        is Dag.AndreYtelser,
                        is Dag.ArbeidIkkeGjenopptattDag,
                        is Dag.Arbeidsdag,
                        is Dag.Feriedag,
                        is Dag.FriskHelgedag,
                        is Dag.Permisjonsdag,
                        is Dag.ProblemDag,
                        is Dag.UkjentDag -> false
                    }
                }?.dato

                // trimmer friskmelding/ferie i halen bort
                val søkeperiode = sisteSykedag?.let { periode.start til sisteSykedag } ?: periode
                val skjæringstidspunkter = beregnSkjæringstidspunkt()
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
                        grunnlagsdata = dto.vilkårsgrunnlagId?.let { grunnlagsdata.getValue(it) },
                        utbetaling = utbetaling,
                        dokumentsporing = Dokumentsporing.gjenopprett(dto.dokumentsporing),
                        sykdomstidslinje = Sykdomstidslinje.gjenopprett(dto.sykdomstidslinje),
                        utbetalingstidslinje = Utbetalingstidslinje.gjenopprett(dto.utbetalingstidslinje),
                        refusjonstidslinje = Beløpstidslinje.gjenopprett(dto.refusjonstidslinje),
                        inntektsendringer = Beløpstidslinje.gjenopprett(dto.inntektsendringer),
                        skjæringstidspunkt = dto.skjæringstidspunkt,
                        skjæringstidspunkter = dto.skjæringstidspunkter,
                        arbeidsgiverperiode = dto.arbeidsgiverperiode.map { Periode.gjenopprett(it) },
                        egenmeldingsdager = dto.egenmeldingsdager.map { Periode.gjenopprett(it) },
                        dagerNavOvertarAnsvar = dto.dagerNavOvertarAnsvar.map { Periode.gjenopprett(it) },
                        maksdatoresultat = dto.maksdatoresultat.let { Maksdatoresultat.gjenopprett(it) },
                        inntekter = dto.inntekter.map { (inntektskildeDto, beløpstidslinjeDto) ->
                            Inntektskilde.gjenopprett(inntektskildeDto) to Beløpstidslinje.gjenopprett(beløpstidslinjeDto)
                        }.toMap()
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
                arbeidsgiverperiode: List<Periode> = this.arbeidsgiverperiode,
                dagerNavOvertarAnsvar: List<Periode> = this.dagerNavOvertarAnsvar,
                egenmeldingsdager: List<Periode> = this.egenmeldingsdager,
                maksdatoresultat: Maksdatoresultat = this.maksdatoresultat,
                inntekter: Map<Inntektskilde, Beløpstidslinje> = this.inntekter
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
                inntekter = inntekter
            )

            internal fun kopierMedNyttSkjæringstidspunkt(beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Endring? {
                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = skjæringstidspunkt(beregnSkjæringstidspunkt)
                val arbeidsgiverperiode = beregnArbeidsgiverperiode(this.periode)
                if (nyttSkjæringstidspunkt == this.skjæringstidspunkt && arbeidsgiverperiode == this.arbeidsgiverperiode) return null
                return kopierMed(
                    skjæringstidspunkt = nyttSkjæringstidspunkt,
                    skjæringstidspunkter = alleSkjæringstidspunkter,
                    arbeidsgiverperiode = arbeidsgiverperiode
                )
            }

            internal fun kopierUtenEndring(
                dokument: Dokumentsporing,
                dagerNavOvertarAnsvar: List<Periode>?,
                egenmeldingsdager: List<Periode>?,
                beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                beregnArbeidsgiverperiode: (Periode) -> List<Periode>
            ): Endring {
                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = skjæringstidspunkt(beregnSkjæringstidspunkt, sykdomstidslinje, periode)
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    dokumentsporing = dokument,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    skjæringstidspunkt = nyttSkjæringstidspunkt,
                    skjæringstidspunkter = alleSkjæringstidspunkter,
                    arbeidsgiverperiode = beregnArbeidsgiverperiode(this.periode),
                    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar ?: this.dagerNavOvertarAnsvar,
                    egenmeldingsdager = egenmeldingsdager ?: this.egenmeldingsdager,
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    inntekter = emptyMap()
                )
            }

            internal fun kopierMedEndring(
                periode: Periode,
                dokument: Dokumentsporing,
                sykdomstidslinje: Sykdomstidslinje,
                dagerNavOvertarAnsvar: List<Periode>?,
                egenmeldingsdager: List<Periode>?,
                beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                beregnArbeidsgiverperiode: (Periode) -> List<Periode>
            ): Endring {
                val (nyttSkjæringstidspunkt, alleSkjæringstidspunkter) = skjæringstidspunkt(beregnSkjæringstidspunkt, sykdomstidslinje, periode)
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
                    arbeidsgiverperiode = beregnArbeidsgiverperiode(this.periode),
                    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar ?: this.dagerNavOvertarAnsvar,
                    egenmeldingsdager = egenmeldingsdager ?: this.egenmeldingsdager,
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    inntekter = emptyMap()
                )
            }

            internal fun kopierUtenUtbetaling(
                beregnSkjæringstidspunkt: (() -> Skjæringstidspunkt)? = null,
                beregnArbeidsgiverperiode: (Periode) -> List<Periode> = { this.arbeidsgiverperiode }
            ): Endring {
                val beregnetSkjæringstidspunkt = beregnSkjæringstidspunkt?.let { skjæringstidspunkt(beregnSkjæringstidspunkt) }
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    skjæringstidspunkt = beregnetSkjæringstidspunkt?.first ?: this.skjæringstidspunkt,
                    skjæringstidspunkter = beregnetSkjæringstidspunkt?.second ?: this.skjæringstidspunkter,
                    arbeidsgiverperiode = beregnArbeidsgiverperiode(this.periode),
                    inntekter = emptyMap()
                )
            }

            internal fun kopierMedRefusjonstidslinje(
                dokument: Dokumentsporing,
                refusjonstidslinje: Beløpstidslinje,
                beregnSkjæringstidspunkt: (() -> Skjæringstidspunkt)? = null,
                beregnArbeidsgiverperiode: (Periode) -> List<Periode> = { this.arbeidsgiverperiode }
            ): Endring {
                val beregnetSkjæringstidspunkt = beregnSkjæringstidspunkt?.let { skjæringstidspunkt(beregnSkjæringstidspunkt) }
                return kopierMed(
                    grunnlagsdata = null,
                    utbetaling = null,
                    utbetalingstidslinje = Utbetalingstidslinje(),
                    maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                    dokumentsporing = dokument,
                    refusjonstidslinje = refusjonstidslinje,
                    skjæringstidspunkt = beregnetSkjæringstidspunkt?.first ?: this.skjæringstidspunkt,
                    skjæringstidspunkter = beregnetSkjæringstidspunkt?.second ?: this.skjæringstidspunkter,
                    arbeidsgiverperiode = beregnArbeidsgiverperiode(this.periode),
                    inntekter = emptyMap()
                )
            }

            internal fun kopierMedUtbetaling(beregning: BeregnetPeriode, utbetaling: Utbetaling) = kopierMed(
                grunnlagsdata = beregning.grunnlagsdata,
                utbetaling = utbetaling,
                utbetalingstidslinje = beregning.utbetalingstidslinje.subset(this.periode),
                maksdatoresultat = beregning.maksdatovurdering.resultat,
                inntekter = beregning.inntekterForBeregning.forPeriode(this.periode)
            )

            internal fun kopierMedAnnullering(grunnlagsdata: VilkårsgrunnlagElement, annullering: Utbetaling) = kopierMed(
                grunnlagsdata = grunnlagsdata,
                utbetaling = annullering,
                utbetalingstidslinje = Utbetalingstidslinje(),
                maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                inntekter = emptyMap()
            )

            internal fun kopierDokument(dokument: Dokumentsporing) = kopierMed(dokumentsporing = dokument)
            internal fun kopierMedUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: InntekterForBeregning) = kopierMed(
                utbetalingstidslinje = utbetalingstidslinje.subset(this.periode),
                inntekter = inntekterForBeregning.forPeriode(this.periode)
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
                    .utbetalingstidslinje(utbetalingstidslinje)
                    .utbetaling(utbetaling)
                    .sykdomstidslinje(sykdomstidslinje)
                    .refusjonstidslinje(refusjonstidslinje)
                grunnlagsdata.berik(utkastTilVedtakBuilder)
                behandling.observatører.forEach { it.utkastTilVedtak(utkastTilVedtakBuilder.buildUtkastTilVedtak()) }
                Aktivitet.Behov.godkjenning(aktivitetsloggMedUtbetalingkontekst, utkastTilVedtakBuilder.buildGodkjenningsbehov())
            }

            internal fun berik(builder: UtkastTilVedtakBuilder) {
                checkNotNull(utbetaling) { "Forventet ikke manglende utbetaling ved utkast til vedtak" }
                checkNotNull(grunnlagsdata) { "Forventet ikke manglende vilkårsgrunnlag ved utkast til vedtak" }
                builder.utbetalingstidslinje(utbetalingstidslinje).utbetaling(utbetaling).sykdomstidslinje(sykdomstidslinje).refusjonstidslinje(refusjonstidslinje)
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
                    arbeidsgiverperioder = this.arbeidsgiverperiode.map { it.dto() },
                    dagerNavOvertarAnsvar = this.dagerNavOvertarAnsvar.map { it.dto() },
                    egenmeldingsdager = this.egenmeldingsdager.map { it.dto() },
                    maksdatoresultat = this.maksdatoresultat.dto(),
                    inntekter = this.inntekter.map { (inntektskilde, beløpstidslinje) ->
                        inntektskilde.dto() to beløpstidslinje.dto()
                    }.toMap()
                )
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

        internal fun navOvertarAnsvar() = gjeldende.dagerNavOvertarAnsvar.isNotEmpty()
        internal fun erFattetVedtak() = vedtakFattet != null
        internal fun erInFlight() = erFattetVedtak() && !erAvsluttet()
        internal fun erAvsluttet() = avsluttet != null
        internal fun klarForUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.UberegnetRevurdering)
        internal fun harÅpenBehandling() = this.tilstand in setOf(Tilstand.UberegnetRevurdering, Tilstand.UberegnetOmgjøring, Tilstand.AnnullertPeriode, Tilstand.TilInfotrygd)
        internal fun harIkkeUtbetaling() = this.tilstand in setOf(Tilstand.Uberegnet, Tilstand.UberegnetOmgjøring, Tilstand.TilInfotrygd)
        internal fun vedtakFattet(arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
            if (utbetalingsavgjørelse.avvist) return tilstand.vedtakAvvist(this, arbeidsgiver, utbetalingsavgjørelse, aktivitetslogg)
            tilstand.vedtakFattet(this, arbeidsgiver, utbetalingsavgjørelse, aktivitetslogg)
        }

        internal fun avsluttUtenVedtak(arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: InntekterForBeregning) {
            tilstand.avsluttUtenVedtak(this, arbeidsgiver, aktivitetslogg, utbetalingstidslinje, inntekterForBeregning)
        }

        internal fun forkastVedtaksperiode(arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
            return tilstand.forkastVedtaksperiode(this, arbeidsgiver, behandlingkilde, aktivitetslogg)
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
            vedtaksperiodeSomLagerUtbetaling: UUID,
            arbeidsgiver: Arbeidsgiver,
            aktivitetslogg: IAktivitetslogg,
            beregning: BeregnetPeriode
        ) = tilstand.utbetaling(this, vedtaksperiodeSomLagerUtbetaling, arbeidsgiver, aktivitetslogg, beregning)

        private fun håndterAnnullering(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg): Utbetaling? {
            val utbetaling = checkNotNull(gjeldende.utbetaling) { "forventer å ha en tidligere utbetaling" }
            return arbeidsgiver.nyAnnullering(hendelse, aktivitetslogg, utbetaling)
        }

        private fun lagOmgjøring(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            arbeidsgiver: Arbeidsgiver,
            aktivitetslogg: IAktivitetslogg,
            beregning: BeregnetPeriode
        ) {
            val strategi = Arbeidsgiver::lagUtbetaling
            lagUtbetaling(
                vedtaksperiodeSomLagerUtbetaling,
                arbeidsgiver,
                aktivitetslogg,
                beregning,
                strategi,
                Tilstand.BeregnetOmgjøring
            )
        }

        private fun lagUtbetaling(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            arbeidsgiver: Arbeidsgiver,
            aktivitetslogg: IAktivitetslogg,
            beregning: BeregnetPeriode
        ) {
            val strategi = Arbeidsgiver::lagUtbetaling
            // TODO: bør sende med beregnet AGP slik at utbetalingskoden vet hvilket oppdrag som skal bygges videre på
            return lagUtbetaling(
                vedtaksperiodeSomLagerUtbetaling,
                arbeidsgiver,
                aktivitetslogg,
                beregning,
                strategi,
                Tilstand.Beregnet
            )
        }

        private fun lagRevurdering(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            arbeidsgiver: Arbeidsgiver,
            aktivitetslogg: IAktivitetslogg,
            beregning: BeregnetPeriode
        ) {
            val strategi = Arbeidsgiver::lagRevurdering
            lagUtbetaling(
                vedtaksperiodeSomLagerUtbetaling,
                arbeidsgiver,
                aktivitetslogg,
                beregning,
                strategi,
                Tilstand.BeregnetRevurdering
            )
        }

        private fun lagUtbetaling(
            vedtaksperiodeSomLagerUtbetaling: UUID,
            arbeidsgiver: Arbeidsgiver,
            aktivitetslogg: IAktivitetslogg,
            beregning: BeregnetPeriode,
            strategi: (Arbeidsgiver, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, maksdato: LocalDate, forbrukteSykedager: Int, gjenståendeSykedager: Int, periode: Periode) -> Utbetaling,
            nyTilstand: Tilstand
        ) {
            val denNyeUtbetalingen = strategi(arbeidsgiver, aktivitetslogg, beregning.utbetalingstidslinje, beregning.maksdatovurdering.resultat.maksdato, beregning.maksdatovurdering.resultat.antallForbrukteDager, beregning.maksdatovurdering.resultat.gjenståendeDager, periode)
            denNyeUtbetalingen.nyVedtaksperiodeUtbetaling(vedtaksperiodeSomLagerUtbetaling)
            nyEndring(gjeldende.kopierMedUtbetaling(beregning, denNyeUtbetalingen))
            tilstand(nyTilstand, aktivitetslogg)
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

        private fun kopierMedUtbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: InntekterForBeregning): Boolean {
            nyEndring(gjeldende.kopierMedUtbetalingstidslinje(utbetalingstidslinje, inntekterForBeregning))
            return true
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

        fun beregnSkjæringstidspunkt(beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>) {
            tilstand.beregnSkjæringstidspunkt(this, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
        }

        fun håndterEndring(
            arbeidsgiver: Arbeidsgiver,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            dagerNavOvertarAnsvar: List<Periode>?,
            egenmeldingsdager: List<Periode>?,
            aktivitetslogg: IAktivitetslogg,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>
        ): Behandling? {
            return tilstand.håndterEndring(
                behandling = this,
                arbeidsgiver = arbeidsgiver,
                behandlingkilde = behandlingkilde,
                dokumentsporing = dokumentsporing,
                hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                egenmeldingsdager = egenmeldingsdager,
                aktivitetslogg = aktivitetslogg,
                beregnSkjæringstidspunkt = beregnSkjæringstidspunkt,
                beregnArbeidsgiverperiode = beregnArbeidsgiverperiode
            )
        }

        private fun håndtereEndring(
            arbeidsgiver: Arbeidsgiver,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            dagerNavOvertarAnsvar: List<Periode>? = null,
            egenmeldingsdager: List<Periode>? = null,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>
        ): Endring {
            val hendelseSykdomstidslinjeFremTilOgMed = hendelseSykdomstidslinje.fremTilOgMed(periode.endInclusive)
            val hendelseperiode = hendelseSykdomstidslinjeFremTilOgMed.periode() ?: return endringer.last().kopierUtenEndring(dokumentsporing, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
            val oppdatertPeriode = this.periode.oppdaterFom(hendelseperiode)
            val sykdomstidslinje = arbeidsgiver.oppdaterSykdom(dokumentsporing.id, hendelseSykdomstidslinjeFremTilOgMed).subset(oppdatertPeriode)
            return endringer.last().kopierMedEndring(oppdatertPeriode, dokumentsporing, sykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
        }

        private fun oppdaterMedRefusjonstidslinje(dokumentsporing: Dokumentsporing, nyeRefusjonsopplysninger: Beløpstidslinje) {
            val endring = endringer.last().kopierMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
            nyEndring(endring)
        }

        private fun oppdaterMedNyttSkjæringstidspunkt(beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>) {
            val endring = endringer.last().kopierMedNyttSkjæringstidspunkt(beregnSkjæringstidspunkt, beregnArbeidsgiverperiode) ?: return
            nyEndring(endring)
        }

        // oppdaterer seg selv med endringen
        private fun oppdaterMedEndring(
            arbeidsgiver: Arbeidsgiver,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            dagerNavOvertarAnsvar: List<Periode>?,
            egenmeldingsdager: List<Periode>?,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            arbeidsgiverperioder: (Periode) -> List<Periode>
        ) {
            val endring = håndtereEndring(
                arbeidsgiver = arbeidsgiver,
                dokumentsporing = dokumentsporing,
                hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                egenmeldingsdager = egenmeldingsdager,
                beregnSkjæringstidspunkt = beregnSkjæringstidspunkt,
                beregnArbeidsgiverperiode = arbeidsgiverperioder
            )
            if (endring == gjeldende) return
            nyEndring(endring)
        }

        private fun nyBehandlingMedEndring(
            arbeidsgiver: Arbeidsgiver,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            hendelseSykdomstidslinje: Sykdomstidslinje,
            dagerNavOvertarAnsvar: List<Periode>?,
            egenmeldingsdager: List<Periode>?,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
            starttilstand: Tilstand = Tilstand.Uberegnet
        ): Behandling {
            arbeidsgiver.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(håndtereEndring(
                    arbeidsgiver = arbeidsgiver,
                    dokumentsporing = dokumentsporing,
                    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                    egenmeldingsdager = egenmeldingsdager,
                    beregnSkjæringstidspunkt = beregnSkjæringstidspunkt,
                    beregnArbeidsgiverperiode = beregnArbeidsgiverperiode
                )),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        private fun nyBehandlingMedRefusjonstidslinje(
            arbeidsgiver: Arbeidsgiver,
            behandlingkilde: Behandlingkilde,
            dokumentsporing: Dokumentsporing,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
            nyRefusjonstidslinje: Beløpstidslinje,
            starttilstand: Tilstand = Tilstand.Uberegnet
        ): Behandling {
            arbeidsgiver.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(
                    endringer.last().kopierMedRefusjonstidslinje(
                        dokumentsporing,
                        nyRefusjonstidslinje,
                        beregnSkjæringstidspunkt,
                        beregnArbeidsgiverperiode
                    )
                ),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        private fun sikreNyBehandling(
            arbeidsgiver: Arbeidsgiver,
            starttilstand: Tilstand,
            behandlingkilde: Behandlingkilde,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>
        ): Behandling {
            arbeidsgiver.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = starttilstand,
                endringer = listOf(endringer.last().kopierUtenUtbetaling(beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)),
                avsluttet = null,
                kilde = behandlingkilde
            )
        }

        private fun nyAnnullertBehandling(arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Behandling {
            arbeidsgiver.låsOpp(periode)
            return Behandling(
                observatører = this.observatører,
                tilstand = Tilstand.AnnullertPeriode,
                endringer = listOf(this.gjeldende.kopierMedAnnullering(grunnlagsdata, annullering)),
                avsluttet = LocalDateTime.now(),
                kilde = behandlingkilde
            )
        }

        fun sikreNyBehandling(
            arbeidsgiver: Arbeidsgiver,
            behandlingkilde: Behandlingkilde,
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>
        ): Behandling? {
            return tilstand.sikreNyBehandling(this, arbeidsgiver, behandlingkilde, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
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
                Tilstand.UberegnetRevurdering -> false

                Tilstand.AvsluttetUtenVedtak,
                Tilstand.BeregnetOmgjøring,
                Tilstand.UberegnetOmgjøring -> hvisAUU()
            }
        }

        fun harFlereSkjæringstidspunkt(): Boolean {
            // ett eller færre skjæringstidspunkter er ok
            if (skjæringstidspunkter.size <= 1) return false
            val arbeidsgiverperioden = arbeidsgiverperiode.periode()
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

        private fun behandlingLukket(arbeidsgiver: Arbeidsgiver) {
            arbeidsgiver.lås(periode)
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            observatører.forEach { it.behandlingLukket(id) }
        }

        private fun vedtakIverksatt(aktivitetslogg: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            observatører.forEach { it.vedtakIverksatt(aktivitetslogg, vedtakFattet!!, this) }
        }

        private fun avsluttetUtenVedtak(aktivitetslogg: IAktivitetslogg) {
            check(observatører.isNotEmpty()) { "behandlingen har ingen registrert observatør" }
            val dekkesAvArbeidsgiverperioden = arbeidsgiverperiode.periode()?.inneholder(periode) != false
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
            builder.behandlingId(id).periode(arbeidsgiverperiode, periode).hendelseIder(dokumentsporing.ider()).skjæringstidspunkt(skjæringstidspunkt)
            gjeldende.godkjenning(aktivitetslogg, this, builder)
        }

        internal fun berik(builder: UtkastTilVedtakBuilder) {
            builder.behandlingId(id).periode(arbeidsgiverperiode, periode).hendelseIder(dokumentsporing.ider()).skjæringstidspunkt(skjæringstidspunkt)
            gjeldende.berik(builder)
        }

        fun annuller(arbeidsgiver: Arbeidsgiver, hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg, behandlinger: List<Behandling>): Utbetaling? {
            val sisteVedtak = behandlinger.lastOrNull { it.erFattetVedtak() } ?: return null
            return sisteVedtak.håndterAnnullering(arbeidsgiver, hendelse, aktivitetslogg)
        }

        fun annuller(arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, annullering: Utbetaling, andreBehandlinger: List<Behandling>): Behandling? {
            val sisteVedtak = andreBehandlinger.lastOrNull { behandlingen -> behandlingen.erFattetVedtak() } ?: return null
            if (true != sisteVedtak.utbetaling()?.hørerSammen(annullering)) return null
            return tilstand.annuller(this, arbeidsgiver, behandlingkilde, aktivitetslogg, annullering, checkNotNull(sisteVedtak.gjeldende.grunnlagsdata))
        }

        /* hvorvidt en AUU- (eller har vært-auu)-periode kan forkastes */
        private fun kanForkastingAvKortPeriodeTillates(andreBehandlinger: List<Behandling>): Boolean {
            val overlappendeBehandlinger = andreBehandlinger.filter { it.arbeidsgiverperiode.any { it.overlapperMed(this.periode) } }
            return overlappendeBehandlinger.all { it.kanForkastesBasertPåTilstand() }
        }

        internal fun validerFerdigBehandlet(meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) = tilstand.validerFerdigBehandlet(this, meldingsreferanseId, aktivitetslogg)
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
            fun nyBehandling(observatører: List<BehandlingObserver>, sykdomstidslinje: Sykdomstidslinje, egenmeldingsdager: List<Periode>, inntektsendringer: Beløpstidslinje, dokumentsporing: Dokumentsporing, sykmeldingsperiode: Periode, behandlingkilde: Behandlingkilde) =
                Behandling(
                    observatører = observatører,
                    tilstand = Tilstand.Uberegnet,
                    endringer = listOf(
                        Endring(
                            id = UUID.randomUUID(),
                            tidsstempel = LocalDateTime.now(),
                            grunnlagsdata = null,
                            utbetaling = null,
                            dokumentsporing = dokumentsporing,
                            sykdomstidslinje = sykdomstidslinje,
                            sykmeldingsperiode = sykmeldingsperiode,
                            utbetalingstidslinje = Utbetalingstidslinje(),
                            refusjonstidslinje = Beløpstidslinje(),
                            inntektsendringer = inntektsendringer,
                            periode = checkNotNull(sykdomstidslinje.periode()) { "kan ikke opprette behandling på tom sykdomstidslinje" },
                            skjæringstidspunkt = IKKE_FASTSATT_SKJÆRINGSTIDSPUNKT,
                            skjæringstidspunkter = emptyList(),
                            arbeidsgiverperiode = emptyList(),
                            egenmeldingsdager = egenmeldingsdager,
                            dagerNavOvertarAnsvar = emptyList(),
                            maksdatoresultat = Maksdatoresultat.IkkeVurdert,
                            inntekter = emptyMap()
                        )
                    ),
                    avsluttet = null,
                    kilde = behandlingkilde
                )

            internal fun List<Behandling>.harGjenbrukbarInntekt(organisasjonsnummer: String) = forrigeEndringMedGjenbrukbarInntekt(organisasjonsnummer) != null
            internal fun List<Behandling>.lagreGjenbrukbarInntekt(skjæringstidspunkt: LocalDate, organisasjonsnummer: String, arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg) {
                val (forrigeEndring, vilkårsgrunnlag) = forrigeEndringMedGjenbrukbarInntekt(organisasjonsnummer) ?: return
                val nyArbeidsgiverperiode = forrigeEndring.arbeidsgiverperiodeEndret(gjeldendeEndring())
                // Herfra bruker vi "gammel" løype - kanskje noe kan skrus på fra det punktet her om en skulle skru på dette
                vilkårsgrunnlag.lagreTidsnæreInntekter(skjæringstidspunkt, arbeidsgiver, aktivitetslogg, nyArbeidsgiverperiode)
            }

            private fun List<Behandling>.forrigeEndringMedGjenbrukbarInntekt(organisasjonsnummer: String): Pair<Endring, VilkårsgrunnlagElement>? =
                forrigeEndringMed { it.grunnlagsdata?.harGjenbrukbarInntekt(organisasjonsnummer) == true }?.let { it to it.grunnlagsdata!! }

            // hvorvidt man delte samme utbetaling før
            fun List<Behandling>.erUtbetaltPåForskjelligeUtbetalinger(other: List<Behandling>): Boolean {
                val forrigeIverksatteThis = forrigeIverksatte ?: return true
                val forrigeIverksatteOther = other.forrigeIverksatte ?: return true
                // hvis forrige iverksatte på *this* har ulik korrelasjonsId som siste iverksatte på *other* -> return true
                val utbetalingThis = checkNotNull(forrigeIverksatteThis.utbetaling()) {
                    "forventer at det skal være en utbetaling på en behandling som er iverksatt"
                }
                val utbetalingOther = forrigeIverksatteOther.utbetaling() ?: return true // forrige periode kan være AUU
                return !utbetalingOther.hørerSammen(utbetalingThis)
            }

            private val List<Behandling>.forrigeIverksatte get() = lastOrNull { it.vedtakFattet != null }
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
            fun annuller(
                behandling: Behandling,
                arbeidsgiver: Arbeidsgiver,
                behandlingkilde: Behandlingkilde,
                aktivitetslogg: IAktivitetslogg,
                annullering: Utbetaling,
                grunnlagsdata: VilkårsgrunnlagElement
            ): Behandling? {
                return null
            }

            fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                behandling.tilstand(TilInfotrygd, aktivitetslogg)
                return null
            }

            fun beregnSkjæringstidspunkt(behandling: Behandling, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>) {}
            fun håndterRefusjonsopplysninger(
                arbeidsgiver: Arbeidsgiver,
                behandling: Behandling,
                behandlingkilde: Behandlingkilde,
                dokumentsporing: Dokumentsporing,
                aktivitetslogg: IAktivitetslogg,
                beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                nyeRefusjonsopplysninger: Beløpstidslinje
            ): Behandling? {
                error("Har ikke implementert håndtering av refusjonsopplysninger i $this")
            }

            fun håndterEndring(
                behandling: Behandling,
                arbeidsgiver: Arbeidsgiver,
                behandlingkilde: Behandlingkilde,
                dokumentsporing: Dokumentsporing,
                hendelseSykdomstidslinje: Sykdomstidslinje,
                dagerNavOvertarAnsvar: List<Periode>?,
                egenmeldingsdager: List<Periode>?,
                aktivitetslogg: IAktivitetslogg,
                beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                beregnArbeidsgiverperiode: (Periode) -> List<Periode>
            ): Behandling? {
                error("Har ikke implementert håndtering av endring i $this")
            }

            fun vedtakAvvist(
                behandling: Behandling,
                arbeidsgiver: Arbeidsgiver,
                utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse,
                aktivitetslogg: IAktivitetslogg
            ) {
                error("Kan ikke avvise vedtak for behandling i $this")
            }

            fun vedtakFattet(behandling: Behandling, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                error("Kan ikke fatte vedtak for behandling i $this")
            }

            fun avsluttUtenVedtak(behandling: Behandling, arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: InntekterForBeregning) {
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
                vedtaksperiodeSomLagerUtbetaling: UUID,
                arbeidsgiver: Arbeidsgiver,
                aktivitetslogg: IAktivitetslogg,
                beregning: BeregnetPeriode
            ) {
                error("Støtter ikke å opprette utbetaling i $this")
            }

            fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) {}

            fun sikreNyBehandling(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling? {
                return null
            }

            fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean = false
            fun håndterUtbetalinghendelse(behandling: Behandling, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) = false
            fun validerFerdigBehandlet(behandling: Behandling, meldingsreferanseId: MeldingsreferanseId, aktivitetslogg: IAktivitetslogg) {
                behandling.valideringFeilet(meldingsreferanseId, aktivitetslogg, "Behandling ${behandling.id} burde vært ferdig behandlet, men står i tilstand ${behandling.tilstand::class.simpleName}")
            }

            data object Uberegnet : Tilstand {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Søknad)
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    check(behandling.utbetaling() == null) { "skal ikke ha utbetaling og være uberegnet samtidig" }
                }

                override fun beregnSkjæringstidspunkt(
                    behandling: Behandling,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>
                ) {
                    behandling.oppdaterMedNyttSkjæringstidspunkt(beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                }

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    return null
                }

                override fun håndterEndring(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, dokumentsporing: Dokumentsporing, hendelseSykdomstidslinje: Sykdomstidslinje, dagerNavOvertarAnsvar: List<Periode>?, egenmeldingsdager: List<Periode>?, aktivitetslogg: IAktivitetslogg, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling? {
                    behandling.oppdaterMedEndring(arbeidsgiver, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                    return null
                }

                override fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) =
                    behandling.kopierMedDokument(dokument)

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {}
                override fun utbetaling(
                    behandling: Behandling,
                    vedtaksperiodeSomLagerUtbetaling: UUID,
                    arbeidsgiver: Arbeidsgiver,
                    aktivitetslogg: IAktivitetslogg,
                    beregning: BeregnetPeriode
                ) = behandling.lagUtbetaling(vedtaksperiodeSomLagerUtbetaling, arbeidsgiver, aktivitetslogg, beregning)

                override fun avsluttUtenVedtak(behandling: Behandling, arbeidsgiver: Arbeidsgiver, aktivitetslogg: IAktivitetslogg, utbetalingstidslinje: Utbetalingstidslinje, inntekterForBeregning: InntekterForBeregning) {
                    behandling.behandlingLukket(arbeidsgiver)
                    behandling.kopierMedUtbetalingstidslinje(utbetalingstidslinje, inntekterForBeregning)
                    behandling.tilstand(AvsluttetUtenVedtak, aktivitetslogg)
                }
            }

            data object UberegnetOmgjøring : Tilstand by (Uberegnet) {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Omgjøring)
                override fun utbetaling(
                    behandling: Behandling,
                    vedtaksperiodeSomLagerUtbetaling: UUID,
                    arbeidsgiver: Arbeidsgiver,
                    aktivitetslogg: IAktivitetslogg,
                    beregning: BeregnetPeriode
                ) = behandling.lagOmgjøring(vedtaksperiodeSomLagerUtbetaling, arbeidsgiver, aktivitetslogg, beregning)
            }

            data object UberegnetRevurdering : Tilstand by (Uberegnet) {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering)
                override fun annuller(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg,
                    annullering: Utbetaling,
                    grunnlagsdata: VilkårsgrunnlagElement
                ): Behandling? {
                    behandling.nyEndring(behandling.gjeldende.kopierMedAnnullering(grunnlagsdata, annullering))
                    behandling.tilstand(AnnullertPeriode, aktivitetslogg)
                    return null
                }

                override fun utbetaling(
                    behandling: Behandling,
                    vedtaksperiodeSomLagerUtbetaling: UUID,
                    arbeidsgiver: Arbeidsgiver,
                    aktivitetslogg: IAktivitetslogg,
                    beregning: BeregnetPeriode
                ) = behandling.lagRevurdering(vedtaksperiodeSomLagerUtbetaling, arbeidsgiver, aktivitetslogg, beregning)
            }

            data object Beregnet : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    checkNotNull(behandling.gjeldende.utbetaling)
                    checkNotNull(behandling.gjeldende.grunnlagsdata)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    return super.forkastVedtaksperiode(behandling, arbeidsgiver, behandlingkilde, aktivitetslogg)
                }

                override fun beregnSkjæringstidspunkt(
                    behandling: Behandling,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>
                ) {
                    behandling.oppdaterMedNyttSkjæringstidspunkt(beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                }

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    behandling.tilstand(Uberegnet, aktivitetslogg)
                    return null
                }

                override fun håndterEndring(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, dokumentsporing: Dokumentsporing, hendelseSykdomstidslinje: Sykdomstidslinje, dagerNavOvertarAnsvar: List<Periode>?, egenmeldingsdager: List<Periode>?, aktivitetslogg: IAktivitetslogg, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedEndring(arbeidsgiver, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                    behandling.tilstand(Uberegnet, aktivitetslogg)
                    return null
                }

                override fun oppdaterDokumentsporing(behandling: Behandling, dokument: Dokumentsporing) =
                    behandling.kopierMedDokument(dokument)

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenUtbetaling(aktivitetslogg)
                    behandling.tilstand(Uberegnet, aktivitetslogg)
                }

                override fun vedtakAvvist(behandling: Behandling, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                    // perioden kommer til å bli kastet til infotrygd
                }

                override fun vedtakFattet(behandling: Behandling, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                    behandling.vedtakFattet = utbetalingsavgjørelse.avgjørelsestidspunkt
                    behandling.behandlingLukket(arbeidsgiver)
                    behandling.tilstand(if (behandling.gjeldende.utbetaling?.erAvsluttet() == true) VedtakIverksatt else VedtakFattet, aktivitetslogg)
                }
            }

            data object BeregnetOmgjøring : Tilstand by (Beregnet) {
                override fun håndterEndring(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, dokumentsporing: Dokumentsporing, hendelseSykdomstidslinje: Sykdomstidslinje, dagerNavOvertarAnsvar: List<Periode>?, egenmeldingsdager: List<Periode>?, aktivitetslogg: IAktivitetslogg, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedEndring(arbeidsgiver, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                    behandling.tilstand(UberegnetOmgjøring, aktivitetslogg)
                    return null
                }

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
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
            }

            data object BeregnetRevurdering : Tilstand by (Beregnet) {
                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    return super.forkastVedtaksperiode(behandling, arbeidsgiver, behandlingkilde, aktivitetslogg)
                }

                override fun annuller(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Behandling? {
                    behandling.gjeldende.utbetaling!!.forkast(aktivitetslogg)
                    behandling.nyEndring(behandling.gjeldende.kopierMedAnnullering(grunnlagsdata, annullering))
                    behandling.tilstand(AnnullertPeriode, aktivitetslogg)
                    return null
                }

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.utenUtbetaling(aktivitetslogg)
                    behandling.tilstand(UberegnetRevurdering, aktivitetslogg)
                }

                override fun vedtakAvvist(behandling: Behandling, arbeidsgiver: Arbeidsgiver, utbetalingsavgjørelse: UtbetalingsavgjørelseHendelse, aktivitetslogg: IAktivitetslogg) {
                    behandling.behandlingLukket(arbeidsgiver)
                    behandling.tilstand(RevurdertVedtakAvvist, aktivitetslogg)
                }

                override fun håndterEndring(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedEndring(arbeidsgiver, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                    behandling.tilstand(UberegnetRevurdering, aktivitetslogg)
                    return null
                }

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling? {
                    behandling.gjeldende.forkastUtbetaling(aktivitetslogg)
                    behandling.oppdaterMedRefusjonstidslinje(dokumentsporing, nyeRefusjonsopplysninger)
                    behandling.tilstand(UberegnetRevurdering, aktivitetslogg)
                    return null
                }
            }

            data object RevurdertVedtakAvvist : Tilstand {
                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun sikreNyBehandling(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling {
                    return behandling.sikreNyBehandling(arbeidsgiver, UberegnetRevurdering, behandlingkilde, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                }

                override fun annuller(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg, annullering: Utbetaling, grunnlagsdata: VilkårsgrunnlagElement): Behandling {
                    return behandling.nyAnnullertBehandling(arbeidsgiver, behandlingkilde, annullering, grunnlagsdata)
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }
            }

            data object VedtakFattet : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    checkNotNull(behandling.gjeldende.utbetaling)
                    checkNotNull(behandling.gjeldende.grunnlagsdata)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun sikreNyBehandling(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling {
                    return behandling.sikreNyBehandling(arbeidsgiver, UberegnetRevurdering, behandlingkilde, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                }

                override fun håndterUtbetalinghendelse(behandling: Behandling, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg): Boolean {
                    val utbetaling = checkNotNull(behandling.gjeldende.utbetaling) { "forventer utbetaling" }
                    if (!utbetaling.gjelderFor(hendelse)) return false
                    if (utbetaling.erAvsluttet()) avsluttMedVedtak(behandling, aktivitetslogg)
                    return true
                }

                override fun utenUtbetaling(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {}

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ) = behandling.nyBehandlingMedRefusjonstidslinje(arbeidsgiver, behandlingkilde, dokumentsporing, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, nyeRefusjonsopplysninger, UberegnetRevurdering)

                override fun håndterEndring(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>
                ) = behandling.nyBehandlingMedEndring(arbeidsgiver, behandlingkilde, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, UberegnetRevurdering)

                override fun avsluttMedVedtak(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.tilstand(VedtakIverksatt, aktivitetslogg)
                }
            }

            data object AvsluttetUtenVedtak : Tilstand {
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.vedtakFattet = null // det fattes ikke vedtak i AUU
                    behandling.avsluttet = LocalDateTime.now()
                    behandling.avsluttetUtenVedtak(aktivitetslogg)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    arbeidsgiver.låsOpp(behandling.periode)
                    return Behandling(
                        observatører = behandling.observatører,
                        tilstand = TilInfotrygd,
                        endringer = listOf(behandling.gjeldende.kopierUtenUtbetaling()),
                        avsluttet = LocalDateTime.now(),
                        kilde = behandlingkilde
                    )
                }

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ): Behandling {
                    return behandling.nyBehandlingMedRefusjonstidslinje(arbeidsgiver, behandlingkilde, dokumentsporing, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, nyeRefusjonsopplysninger, UberegnetOmgjøring)
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun sikreNyBehandling(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling {
                    return behandling.sikreNyBehandling(arbeidsgiver, UberegnetOmgjøring, behandlingkilde, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                }

                override fun håndterEndring(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>
                ): Behandling {
                    return behandling.nyBehandlingMedEndring(arbeidsgiver, behandlingkilde, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, UberegnetOmgjøring)
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

                override fun annuller(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg,
                    annullering: Utbetaling,
                    grunnlagsdata: VilkårsgrunnlagElement
                ): Behandling {
                    return behandling.nyAnnullertBehandling(arbeidsgiver, behandlingkilde, annullering, grunnlagsdata)
                }

                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling {
                    error("Kan ikke forkaste i tilstand ${this.javaClass.simpleName}")
                }

                override fun tillaterNyBehandling(behandling: Behandling, other: Behandling): Boolean {
                    return true
                }

                override fun sikreNyBehandling(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, beregnSkjæringstidspunkt: () -> Skjæringstidspunkt, beregnArbeidsgiverperiode: (Periode) -> List<Periode>): Behandling {
                    return behandling.sikreNyBehandling(arbeidsgiver, UberegnetRevurdering, behandlingkilde, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode)
                }

                override fun håndterRefusjonsopplysninger(
                    arbeidsgiver: Arbeidsgiver,
                    behandling: Behandling,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>,
                    nyeRefusjonsopplysninger: Beløpstidslinje
                ) = behandling.nyBehandlingMedRefusjonstidslinje(arbeidsgiver, behandlingkilde, dokumentsporing, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, nyeRefusjonsopplysninger, UberegnetRevurdering)

                override fun håndterEndring(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    dokumentsporing: Dokumentsporing,
                    hendelseSykdomstidslinje: Sykdomstidslinje,
                    dagerNavOvertarAnsvar: List<Periode>?,
                    egenmeldingsdager: List<Periode>?,
                    aktivitetslogg: IAktivitetslogg,
                    beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
                    beregnArbeidsgiverperiode: (Periode) -> List<Periode>
                ) = behandling.nyBehandlingMedEndring(arbeidsgiver, behandlingkilde, dokumentsporing, hendelseSykdomstidslinje, dagerNavOvertarAnsvar, egenmeldingsdager, beregnSkjæringstidspunkt, beregnArbeidsgiverperiode, UberegnetRevurdering)

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
                override fun forkastVedtaksperiode(behandling: Behandling, arbeidsgiver: Arbeidsgiver, behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg): Behandling? {
                    behandling.vedtakAnnullert(aktivitetslogg)
                    return null
                }

                override fun annuller(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg,
                    annullering: Utbetaling,
                    grunnlagsdata: VilkårsgrunnlagElement
                ): Behandling? {
                    error("forventer ikke å annullere i $this")
                }
            }

            data object TilInfotrygd : Tilstand {
                override fun behandlingOpprettet(behandling: Behandling) = behandling.emitNyBehandlingOpprettet(PersonObserver.BehandlingOpprettetEvent.Type.Omgjøring)
                override fun entering(behandling: Behandling, aktivitetslogg: IAktivitetslogg) {
                    behandling.avsluttet = LocalDateTime.now()
                }

                override fun annuller(
                    behandling: Behandling,
                    arbeidsgiver: Arbeidsgiver,
                    behandlingkilde: Behandlingkilde,
                    aktivitetslogg: IAktivitetslogg,
                    annullering: Utbetaling,
                    grunnlagsdata: VilkårsgrunnlagElement
                ): Behandling? {
                    error("forventer ikke å annullere i $this")
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
            },
            endringer = this.endringer.map { it.dto() },
            vedtakFattet = this.vedtakFattet,
            avsluttet = this.avsluttet,
            kilde = this.kilde.dto(),
        )
    }

    internal fun dto() = BehandlingerUtDto(behandlinger = this.behandlinger.map { it.dto() })
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
    val endringer: List<BehandlingendringView>
) {
    enum class TilstandView {
        ANNULLERT_PERIODE, AVSLUTTET_UTEN_VEDTAK,
        BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING,
        REVURDERT_VEDTAK_AVVIST,
        TIL_INFOTRYGD, UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING,
        VEDTAK_FATTET, VEDTAK_IVERKSATT
    }
}

internal data class BehandlingendringView(
    val id: UUID,
    val sykmeldingsperiode: Periode,
    val periode: Periode,
    val sykdomstidslinje: Sykdomstidslinje,
    val grunnlagsdata: VilkårsgrunnlagElement?,
    val utbetaling: Utbetaling?,
    val dokumentsporing: Dokumentsporing,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val refusjonstidslinje: Beløpstidslinje,
    val inntektsendringer: Beløpstidslinje,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val dagerNavOvertarAnsvar: List<Periode>,
    val arbeidsgiverperiode: List<Periode>,
    val egenmeldingsdager: List<Periode>,
    val maksdatoresultat: Maksdatoresultat
)

internal data class BehandlingkildeView(
    val meldingsreferanseId: MeldingsreferanseId,
    val innsendt: LocalDateTime,
    val registert: LocalDateTime,
    val avsender: Avsender
)
