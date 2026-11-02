package no.nav.helse.spleis.mediator

import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.*
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Personopplysninger
import no.nav.helse.spleis.meldinger.model.*
import java.time.LocalDate
import java.util.*

internal class TestHendelseMediator : IHendelseMediator {
    val lestNySøknad get() = lestNySøknadVerdi.get()
    val lestSendtSøknadArbeidsgiver get() = lestSendtSøknadArbeidsgiverVerdi.get()
    val lestSendtSøknad get() = lestSendtSøknadVerdi.get()
    val lestNavNoInntektsmelding get() = lestNavNoInntektsmeldingVerdi.get()
    val lestKorrigertNavNoInntektsmelding get() = lestKorrigertNavNoInntektsmeldingVerdi.get()
    val lestNavNoSelvbestemtInntektsmelding get() = lestNavNoSelvbestemtInntektsmeldingVerdi.get()
    val lestDødsmelding get() = lestDødsmeldingVerdi.get()
    val lestPåminnelse get() = lestPåminnelseVerdi.get()
    val lestPersonpåminnelse get() = lestPersonpåminnelseVerdi.get()
    val lestAnmodningOmForkasting get() = lestAnmodningOmForkastingVerdi.get()
    val lestUtbetalingshistorikk get() = lestUtbetalingshistorikkVerdi.get()
    val lestYtelser get() = lestYtelserVerdi.get()
    val lestVilkårsgrunnlag get() = lestVilkårsgrunnlagVerdi.get()
    val lestSimulering get() = lestSimuleringVerdi.get()
    val lestRettighetskategori get() = lestRettighetskategoriVerdi.get()
    val lestUtbetalingsgodkjenning get() = lestUtbetalingsgodkjenningVerdi.get()
    val lestUtbetaling get() = lestUtbetalingVerdi.get()
    val lestAnnullerUtbetaling get() = lestAnnullerUtbetalingVerdi.get()
    val lestAvstemming get() = lestAvstemmingVerdi.get()
    val lestMigrate get() = lestMigrateVerdi.get()
    val lestForkastSykmeldingsperioder get() = lestForkastSykmeldingsperioderMessageVerdi.get()
    val lestEndretVurderingPåSkjæringstidspunkt get() = lestEndretVurderingPåSkjæringstidspunktVerdi.get()
    val lestEndretGrunnlagForBeregning get() = lestEndretGrunnlagForBeregningVerdi.get()
    val lestAvbruttSøknad get() = lestAvbruttSøknadVerdi.get()

    private val lestNySøknadVerdi = ThreadLocal.withInitial { false }
    private val lestSendtSøknadArbeidsgiverVerdi = ThreadLocal.withInitial { false }
    private val lestSendtSøknadVerdi = ThreadLocal.withInitial { false }
    private val lestInntektsmeldingVerdi = ThreadLocal.withInitial { false }
    private val lestNavNoInntektsmeldingVerdi = ThreadLocal.withInitial { false }
    private val lestKorrigertNavNoInntektsmeldingVerdi = ThreadLocal.withInitial { false }
    private val lestNavNoSelvbestemtInntektsmeldingVerdi = ThreadLocal.withInitial { false }
    private val lestDødsmeldingVerdi = ThreadLocal.withInitial { false }
    private val lestPåminnelseVerdi = ThreadLocal.withInitial { false }
    private val lestPersonpåminnelseVerdi = ThreadLocal.withInitial { false }
    private val lestAnmodningOmForkastingVerdi = ThreadLocal.withInitial { false }
    private val lestUtbetalingshistorikkVerdi = ThreadLocal.withInitial { false }
    private val lestYtelserVerdi = ThreadLocal.withInitial { false }
    private val lestVilkårsgrunnlagVerdi = ThreadLocal.withInitial { false }
    private val lestSimuleringVerdi = ThreadLocal.withInitial { false }
    private val lestRettighetskategoriVerdi = ThreadLocal.withInitial { false }
    private val lestUtbetalingsgodkjenningVerdi = ThreadLocal.withInitial { false }
    private val lestUtbetalingVerdi = ThreadLocal.withInitial { false }
    private val lestAnnullerUtbetalingVerdi = ThreadLocal.withInitial { false }
    private val lestAvstemmingVerdi = ThreadLocal.withInitial { false }
    private val lestMigrateVerdi = ThreadLocal.withInitial { false }
    private val lestForkastSykmeldingsperioderMessageVerdi = ThreadLocal.withInitial { false }
    private val lestEndretVurderingPåSkjæringstidspunktVerdi: ThreadLocal<EndretVurderingPåSkjæringstidspunktData?> =
        ThreadLocal
            .withInitial {
                null
            }
    private val lestEndretGrunnlagForBeregningVerdi: ThreadLocal<EndretGrunnlagForBeregningData?> = ThreadLocal.withInitial { null }
    private val lestAvbruttSøknadVerdi: ThreadLocal<AvbruttSøknadData?> = ThreadLocal.withInitial { null }

    fun reset() {
        lestNySøknadVerdi.remove()
        lestSendtSøknadArbeidsgiverVerdi.remove()
        lestSendtSøknadVerdi.remove()
        lestInntektsmeldingVerdi.remove()
        lestNavNoInntektsmeldingVerdi.remove()
        lestNavNoSelvbestemtInntektsmeldingVerdi.remove()
        lestKorrigertNavNoInntektsmeldingVerdi.remove()
        lestDødsmeldingVerdi.remove()
        lestPåminnelseVerdi.remove()
        lestPersonpåminnelseVerdi.remove()
        lestUtbetalingshistorikkVerdi.remove()
        lestYtelserVerdi.remove()
        lestVilkårsgrunnlagVerdi.remove()
        lestSimuleringVerdi.remove()
        lestRettighetskategoriVerdi.remove()
        lestUtbetalingsgodkjenningVerdi.remove()
        lestUtbetalingVerdi.remove()
        lestAnnullerUtbetalingVerdi.remove()
        lestAvstemmingVerdi.remove()
        lestMigrateVerdi.remove()
        lestAnmodningOmForkastingVerdi.remove()
        lestForkastSykmeldingsperioderMessageVerdi.remove()
        lestEndretVurderingPåSkjæringstidspunktVerdi.remove()
        lestEndretGrunnlagForBeregningVerdi.remove()
        lestAvbruttSøknadVerdi.remove()
    }

    override fun behandle(
        message: HendelseMessage,
        context: BehandlingContext,
    ) {
        message.behandle(this, context)
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NySøknadMessage,
        sykmelding: Sykmelding,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {
        lestNySøknadVerdi.set(true)
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NyFrilansSøknadMessage,
        sykmelding: Sykmelding,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NySelvstendigSøknadMessage,
        sykmelding: Sykmelding,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NyArbeidsledigSøknadMessage,
        sykmelding: Sykmelding,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NyArbeidsledigTidligereArbeidstakerSøknadMessage,
        sykmelding: Sykmelding,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadFrilansMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadSelvstendigMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadFiskerMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadAnnetMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsledigMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsledigTidligereArbeidstakerMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {}

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsgiverMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {
        lestSendtSøknadArbeidsgiverVerdi.set(true)
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadNavMessage,
        søknad: Søknad,
        context: BehandlingContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
    ) {
        lestSendtSøknadVerdi.set(true)
    }

    override fun behandle(
        message: InntektsopplysningerFraLagretInntektsmeldingMessage,
        inntektsmeldingMeldingsreferanseId: MeldingsreferanseId,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: NavNoSelvbestemtInntektsmeldingMessage,
        selvbestemteArbeidsgiveropplysninger: SelvbestemteArbeidsgiveropplysninger,
        context: BehandlingContext,
    ) {
        lestNavNoSelvbestemtInntektsmeldingVerdi.set(true)
    }

    override fun behandle(
        message: NavNoInntektsmeldingMessage,
        arbeidsgiveropplysninger: Arbeidsgiveropplysninger,
        context: BehandlingContext,
    ) {
        lestNavNoInntektsmeldingVerdi.set(true)
    }

    override fun behandle(
        message: NavNoKorrigertInntektsmeldingMessage,
        korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger,
        context: BehandlingContext,
    ) {
        lestKorrigertNavNoInntektsmeldingVerdi.set(true)
    }

    override fun behandle(
        message: InntektsmeldingerReplayMessage,
        replays: InntektsmeldingerReplay,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: DødsmeldingMessage,
        dødsmelding: Dødsmelding,
        context: BehandlingContext,
    ) {
        lestDødsmeldingVerdi.set(true)
    }

    override fun behandle(
        nyPersonidentifikator: Personidentifikator,
        message: IdentOpphørtMessage,
        identOpphørt: IdentOpphørt,
        gamleIdenter: Set<Personidentifikator>,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: PåminnelseMessage,
        påminnelse: Påminnelse,
        context: BehandlingContext,
    ) {
        lestPåminnelseVerdi.set(true)
    }

    override fun behandle(
        message: PersonPåminnelseMessage,
        påminnelse: PersonPåminnelse,
        context: BehandlingContext,
    ) {
        lestPersonpåminnelseVerdi.set(true)
    }

    override fun behandle(
        message: GjenopptaBehandlingMessage,
        gjenopptaBehandling: GjenopptaBehandling,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: AnmodningOmForkastingMessage,
        anmodning: AnmodningOmForkasting,
        context: BehandlingContext,
    ) {
        lestAnmodningOmForkastingVerdi.set(true)
    }

    override fun behandle(
        message: UtbetalingshistorikkMessage,
        utbetalingshistorikk: Utbetalingshistorikk,
        context: BehandlingContext,
    ) {
        lestUtbetalingshistorikkVerdi.set(true)
    }

    override fun behandle(
        message: UtbetalingshistorikkForFeriepengerMessage,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: YtelserMessage,
        ytelser: Ytelser,
        context: BehandlingContext,
    ) {
        lestYtelserVerdi.set(true)
    }

    override fun behandle(
        message: VilkårsgrunnlagMessage,
        vilkårsgrunnlag: Vilkårsgrunnlag,
        context: BehandlingContext,
    ) {
        lestVilkårsgrunnlagVerdi.set(true)
    }

    override fun behandle(
        message: UtbetalingsgodkjenningMessage,
        utbetalingsgodkjenning: Utbetalingsgodkjenning,
        context: BehandlingContext,
    ) {
        lestUtbetalingsgodkjenningVerdi.set(true)
    }

    override fun behandle(
        message: FeriepengeutbetalingMessage,
        utbetaling: FeriepengeutbetalingHendelse,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: UtbetalingMessage,
        utbetaling: UtbetalingHendelse,
        context: BehandlingContext,
    ) {
        lestUtbetalingVerdi.set(true)
    }

    override fun behandle(
        message: SimuleringMessage,
        simulering: Simulering,
        context: BehandlingContext,
    ) {
        lestSimuleringVerdi.set(true)
    }

    override fun behandle(
        message: RettighetskategoriMessage,
        rettighetskategori: Rettighetskategori,
        context: BehandlingContext,
    ) {
        lestRettighetskategoriVerdi.set(true)
    }

    override fun behandle(
        message: AnnulleringMessage,
        annullerUtbetaling: AnnullerUtbetaling,
        context: BehandlingContext,
    ) {
        lestAnnullerUtbetalingVerdi.set(true)
    }

    override fun behandle(
        message: AvstemmingMessage,
        personidentifikator: Personidentifikator,
        context: BehandlingContext,
    ) {
        lestAvstemmingVerdi.set(true)
    }

    override fun behandle(
        message: MigrateMessage,
        migrate: Migrate,
        context: BehandlingContext,
    ) {
        lestMigrateVerdi.set(true)
    }

    override fun behandle(
        message: OverstyrTidslinjeMessage,
        overstyrTidslinje: OverstyrTidslinje,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: OverstyrArbeidsgiveropplysningerMessage,
        overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: OverstyrArbeidsforholdMessage,
        overstyrArbeidsforhold: OverstyrArbeidsforhold,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: GrunnbeløpsreguleringMessage,
        grunnbeløpsregulering: Grunnbeløpsregulering,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: InfotrygdendringMessage,
        infotrygdEndring: Infotrygdendring,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: UtbetalingshistorikkEtterInfotrygdendringMessage,
        utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: ForkastSykmeldingsperioderMessage,
        forkastSykmeldingsperioder: ForkastSykmeldingsperioder,
        context: BehandlingContext,
    ) {
        lestForkastSykmeldingsperioderMessageVerdi.set(true)
    }

    data class AvbruttSøknadData(
        val fom: LocalDate,
        val tom: LocalDate,
        val behandlingsporing: Behandlingsporing,
    )

    override fun behandle(
        message: AvbruttSøknadMessage,
        avbruttSøknad: AvbruttSøknad,
        context: BehandlingContext,
    ) {
        lestAvbruttSøknadVerdi.set(
            AvbruttSøknadData(
                fom = message.periode.start,
                tom = message.periode.endInclusive,
                behandlingsporing = message.behandlingsporing,
            ),
        )
    }

    override fun behandle(
        message: SkjønnsmessigFastsettelseMessage,
        skjønnsmessigFastsettelse: SkjønnsmessigFastsettelse,
        context: BehandlingContext,
    ) {}

    override fun behandle(
        message: MinimumSykdomsgradVurdertMessage,
        minimumSykdomsgradsvurdering: MinimumSykdomsgradsvurderingMelding,
        context: BehandlingContext,
    ) {}

    data class EndretVurderingPåSkjæringstidspunktData(
        val skjæringstidspunkt: LocalDate,
        val vurderingId: UUID,
        val type: String,
        val manuellVurdering: Boolean,
    )

    override fun behandle(
        message: EndretVurderingPåSkjæringstidspunktMessage,
        endretVurderingPåSkjæringstidspunkt: EndretVurderingPåSkjæringstidspunkt,
        context: BehandlingContext,
    ) {
        lestEndretVurderingPåSkjæringstidspunktVerdi.set(
            EndretVurderingPåSkjæringstidspunktData(
                skjæringstidspunkt = message.skjæringstidspunkt,
                vurderingId = message.vurdering.id,
                type = message.vurdering::class.simpleName!!,
                manuellVurdering = message.avsender == Avsender.SAKSBEHANDLER,
            ),
        )
    }

    data class EndretGrunnlagForBeregningData(
        val type: String,
        val fom: LocalDate,
    )

    override fun behandle(
        message: EndretGrunnlagForBeregningMessage,
        endretGrunnlagForBeregning: EndretGrunnlagForBeregning,
        context: BehandlingContext,
    ) {
        lestEndretGrunnlagForBeregningVerdi.set(
            EndretGrunnlagForBeregningData(
                type = message.endretGrunnlag::class.simpleName!!,
                fom = message.fom,
            ),
        )
    }
}
