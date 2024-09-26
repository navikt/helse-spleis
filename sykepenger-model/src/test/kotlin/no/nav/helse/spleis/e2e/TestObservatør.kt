package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.FørsteFraværsdag
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import no.nav.helse.person.TilstandType
import no.nav.helse.spill_av_im.Forespørsel
import org.junit.jupiter.api.fail

internal typealias InntektsmeldingId = UUID
internal typealias VedtaksperiodeId = UUID

internal class TestObservatør(person: Person? = null) : PersonObserver {
    init {
        person?.addObserver(this)
    }
    internal val tilstandsendringer = person?.inspektør?.sisteVedtaksperiodeTilstander()?.mapValues { mutableListOf(it.value) }?.toMutableMap() ?: mutableMapOf()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val trengerArbeidsgiveropplysningerVedtaksperioder = mutableListOf<PersonObserver.TrengerArbeidsgiveropplysningerEvent>()
    val trengerIkkeArbeidsgiveropplysningerVedtaksperioder = mutableListOf<PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent>()
    val arbeidsgiveropplysningerKorrigert = mutableListOf<PersonObserver.ArbeidsgiveropplysningerKorrigertEvent>()
    val utbetalingUtenUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val utbetalingMedUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val feriepengerUtbetaltEventer = mutableListOf<PersonObserver.FeriepengerUtbetaltEvent>()
    val utbetaltEndretEventer = mutableListOf<PersonObserver.UtbetalingEndretEvent>()
    val avsluttetMedVedtakEvent = mutableMapOf<UUID, PersonObserver.AvsluttetMedVedtakEvent>()
    val avsluttetMedVedtakEventer = mutableMapOf<UUID, MutableList<PersonObserver.AvsluttetMedVedtakEvent>>()
    val behandlingOpprettetEventer = mutableListOf<PersonObserver.BehandlingOpprettetEvent>()
    val behandlingLukketEventer = mutableListOf<PersonObserver.BehandlingLukketEvent>()
    val behandlingForkastetEventer = mutableListOf<PersonObserver.BehandlingForkastetEvent>()
    val avsluttetUtenVedtakEventer = mutableMapOf<UUID, MutableList<PersonObserver.AvsluttetUtenVedtakEvent>>()
    val overstyringIgangsatt = mutableListOf<PersonObserver.OverstyringIgangsatt>()
    val vedtaksperiodeVenter = mutableListOf<PersonObserver.VedtaksperiodeVenterEvent>()
    val inntektsmeldingFørSøknad = mutableListOf<PersonObserver.InntektsmeldingFørSøknadEvent>()
    val inntektsmeldingIkkeHåndtert = mutableListOf<InntektsmeldingId>()
    val inntektsmeldingHåndtert = mutableListOf<Pair<InntektsmeldingId, VedtaksperiodeId>>()
    val skatteinntekterLagtTilGrunnEventer = mutableListOf<PersonObserver.SkatteinntekterLagtTilGrunnEvent>()
    val søknadHåndtert = mutableListOf<Pair<UUID, UUID>>()
    val vedtaksperiodeAnnullertEventer = mutableListOf<PersonObserver.VedtaksperiodeAnnullertEvent>()
    val vedtaksperiodeOpprettetEventer = mutableListOf<PersonObserver.VedtaksperiodeOpprettet>()
    val overlappendeInfotrygdperioder = mutableListOf<PersonObserver.OverlappendeInfotrygdperioder>()
    val utkastTilVedtakEventer = mutableListOf<PersonObserver.UtkastTilVedtakEvent>()
    val sykefraværstilfelleIkkeFunnet = mutableListOf<PersonObserver.SykefraværstilfelleIkkeFunnet>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = person?.inspektør?.vedtaksperioder()?.mapValues { (_, perioder) ->
        perioder.map { it.inspektør.id }.toMutableSet()
    }?.toMutableMap() ?: mutableMapOf()

    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    private val forkastedeEventer = mutableMapOf<UUID, PersonObserver.VedtaksperiodeForkastetEvent>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()
    val inntektsmeldingReplayEventer = mutableListOf<Forespørsel>()

    internal fun replayInntektsmeldinger(block: () -> Unit): Set<Forespørsel> {
        val replaysFør = inntektsmeldingReplayEventer.toSet()
        block()
        return inntektsmeldingReplayEventer.toSet() - replaysFør
    }

    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = IdInnhenter { orgnummer -> vedtaksperioder.getValue(orgnummer).last() }

    fun sisteVedtaksperiodeId(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()
    fun sisteVedtaksperiodeIdOrNull(orgnummer: String) = vedtaksperioder[orgnummer]?.last()
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]
    fun kvitterInntektsmeldingReplay(vedtaksperiodeId: UUID) {
        inntektsmeldingReplayEventer.removeAll { it.vedtaksperiodeId == vedtaksperiodeId }
    }

    override fun vedtaksperiodeVenter(event: PersonObserver.VedtaksperiodeVenterEvent) {
        vedtaksperiodeVenter.add(event)
    }

    fun forkastedePerioder() = forkastedeEventer.size
    fun forkastet(vedtaksperiodeId: UUID) = forkastedeEventer.getValue(vedtaksperiodeId)

    override fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingUtenUtbetalingEventer.add(event)
    }

    override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingMedUtbetalingEventer.add(event)
    }

    override fun feriepengerUtbetalt(event: PersonObserver.FeriepengerUtbetaltEvent) {
        feriepengerUtbetaltEventer.add(event)
    }

    override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        utbetaltEndretEventer.add(event)
    }

    override fun avsluttetMedVedtak(event: PersonObserver.AvsluttetMedVedtakEvent) {
        avsluttetMedVedtakEvent[event.vedtaksperiodeId] = event
        avsluttetMedVedtakEventer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        behandlingOpprettetEventer.add(event)
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        behandlingLukketEventer.add(event)
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        behandlingForkastetEventer.add(event)
    }

    override fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
       avsluttetUtenVedtakEventer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
    }

    override fun vedtaksperiodeOpprettet(event: PersonObserver.VedtaksperiodeOpprettet) {
        vedtaksperiodeOpprettetEventer.add(event)
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
        vedtaksperiodeendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
        vedtaksperioder.getOrPut(event.organisasjonsnummer) { mutableSetOf() }.add(sisteVedtaksperiode)
        tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event.forrigeTilstand) }.add(event.gjeldendeTilstand)
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }

    internal fun nullstillTilstandsendringer() {
        tilstandsendringer.replaceAll { _, value -> mutableListOf(value.last()) }
    }

    override fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        trengerArbeidsgiveropplysningerVedtaksperioder.add(event)
    }

    override fun trengerIkkeArbeidsgiveropplysninger(event: PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent) {
        trengerIkkeArbeidsgiveropplysningerVedtaksperioder.add(event)
    }

    override fun arbeidsgiveropplysningerKorrigert(event: PersonObserver.ArbeidsgiveropplysningerKorrigertEvent) {
        arbeidsgiveropplysningerKorrigert.add(event)
    }

    override fun inntektsmeldingReplay(
        personidentifikator: Personidentifikator,
        aktørId: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
        sykmeldingsperioder: List<Periode>,
        egenmeldingsperioder: List<Periode>,
        førsteFraværsdager: List<FørsteFraværsdag>,
        trengerArbeidsgiverperiode: Boolean,
        erPotensiellForespørsel: Boolean
    ) {
        inntektsmeldingReplayEventer.add(Forespørsel(
            fnr = personidentifikator.toString(),
            aktørId = aktørId,
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            førsteFraværsdager = førsteFraværsdager.map { no.nav.helse.spill_av_im.FørsteFraværsdag(it.organisasjonsnummer, it.førsteFraværsdag) },
            sykmeldingsperioder = sykmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
            egenmeldinger = egenmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
            harForespurtArbeidsgiverperiode = trengerArbeidsgiverperiode,
            erPotensiellForespørsel = erPotensiellForespørsel
        ))
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        forkastedeEventer[event.vedtaksperiodeId] = event
    }

    override fun overstyringIgangsatt(
        event: PersonObserver.OverstyringIgangsatt
    ) {
        overstyringIgangsatt.add(event)
    }

    override fun overlappendeInfotrygdperioder(event: PersonObserver.OverlappendeInfotrygdperioder) {
        overlappendeInfotrygdperioder.add(event)
    }

    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) {
        inntektsmeldingFørSøknad.add(event)
    }

    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, harPeriodeInnenfor16Dager: Boolean) {
        inntektsmeldingIkkeHåndtert.add(inntektsmeldingId)
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        inntektsmeldingHåndtert.add(inntektsmeldingId to vedtaksperiodeId)
    }

    override fun skatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent: PersonObserver.SkatteinntekterLagtTilGrunnEvent) {
        skatteinntekterLagtTilGrunnEventer.add(skatteinntekterLagtTilGrunnEvent)
    }

    override fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        søknadHåndtert.add(søknadId to vedtaksperiodeId)
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        vedtaksperiodeAnnullertEventer.add(vedtaksperiodeAnnullertEvent)
    }

    override fun utkastTilVedtak(event: PersonObserver.UtkastTilVedtakEvent) {
        utkastTilVedtakEventer.add(event)
    }

    override fun sykefraværstilfelleIkkeFunnet(event: PersonObserver.SykefraværstilfelleIkkeFunnet) {
        sykefraværstilfelleIkkeFunnet.add(event)
    }
}
