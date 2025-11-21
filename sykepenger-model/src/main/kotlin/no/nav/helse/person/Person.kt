package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.feriepenger.Feriepengeberegner
import no.nav.helse.feriepenger.Feriepengegrunnlagstidslinje
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.GjenopptaBehandling
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.Inntektsendringer
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.KanIkkeBehandlesHer
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.Revurderingseventyr.Companion.tidligsteEventyr
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.VedtakFattet
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.Vedtaksperiode.Companion.SPEILRELATERT
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.Yrkesaktivitet.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.person.Yrkesaktivitet.Companion.avventerSøknad
import no.nav.helse.person.Yrkesaktivitet.Companion.beregnFeriepengerForAlleArbeidsgivere
import no.nav.helse.person.Yrkesaktivitet.Companion.beregnSkjæringstidspunkt
import no.nav.helse.person.Yrkesaktivitet.Companion.finn
import no.nav.helse.person.Yrkesaktivitet.Companion.fjernSykmeldingsperiode
import no.nav.helse.person.Yrkesaktivitet.Companion.gjenopptaBehandling
import no.nav.helse.person.Yrkesaktivitet.Companion.håndterOverstyrInntektsgrunnlag
import no.nav.helse.person.Yrkesaktivitet.Companion.håndterOverstyringAvInntekt
import no.nav.helse.person.Yrkesaktivitet.Companion.håndterOverstyringAvRefusjon
import no.nav.helse.person.Yrkesaktivitet.Companion.igangsettOverstyring
import no.nav.helse.person.Yrkesaktivitet.Companion.mursteinsperioder
import no.nav.helse.person.Yrkesaktivitet.Companion.nestemann
import no.nav.helse.person.Yrkesaktivitet.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.Yrkesaktivitet.Companion.tidligsteDato
import no.nav.helse.person.Yrkesaktivitet.Companion.validerTilstand
import no.nav.helse.person.Yrkesaktivitet.Companion.vedtaksperioder
import no.nav.helse.person.Yrkesaktivitet.Companion.venter
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.view.PersonView
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkter
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerregler
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerregler.Companion.NormalArbeidstaker

class Person private constructor(
    personidentifikator: Personidentifikator,
    internal var alder: Alder,
    private val _yrkesaktiviteter: MutableList<Yrkesaktivitet>,
    private val opprettet: LocalDateTime,
    internal val infotrygdhistorikk: Infotrygdhistorikk,
    internal val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    skjæringstidspunkter: Skjæringstidspunkter,
    private val regelverkslogg: Regelverkslogg,
    private val tidligereBehandlinger: List<Person> = emptyList(),
    internal val regler: MaksimumSykepengedagerregler = NormalArbeidstaker,
    internal val minimumSykdomsgradsvurdering: MinimumSykdomsgradsvurdering = MinimumSykdomsgradsvurdering()
) : Aktivitetskontekst {
    companion object {
        fun gjenopprett(
            regelverkslogg: Regelverkslogg,
            dto: PersonInnDto,
            tidligereBehandlinger: List<Person> = emptyList()
        ): Person {
            val yrkesaktiviteter = mutableListOf<Yrkesaktivitet>()
            val grunnlagsdataMap = mutableMapOf<UUID, VilkårsgrunnlagElement>()
            val alder = Alder.gjenopprett(dto.alder)
            val person = Person(
                personidentifikator = Personidentifikator(dto.fødselsnummer),
                alder = alder,
                _yrkesaktiviteter = yrkesaktiviteter,
                opprettet = dto.opprettet,
                infotrygdhistorikk = Infotrygdhistorikk.gjenopprett(dto.infotrygdhistorikk),
                vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk.gjenopprett(
                    dto.vilkårsgrunnlagHistorikk,
                    grunnlagsdataMap
                ),
                skjæringstidspunkter = Skjæringstidspunkter.gjenopprett(dto.skjæringstidspunkter),
                minimumSykdomsgradsvurdering = MinimumSykdomsgradsvurdering.gjenopprett(dto.minimumSykdomsgradVurdering),
                regelverkslogg = regelverkslogg,
                tidligereBehandlinger = tidligereBehandlinger
            )
            yrkesaktiviteter.addAll(dto.arbeidsgivere.map {
                Yrkesaktivitet.gjenopprett(person, it, regelverkslogg, grunnlagsdataMap)
            })
            return person
        }
    }

    internal constructor(
        personidentifikator: Personidentifikator,
        alder: Alder,
        regelverkslogg: Regelverkslogg,
        regler: MaksimumSykepengedagerregler
    ) : this(
        personidentifikator,
        alder,
        mutableListOf(),
        LocalDateTime.now(),
        Infotrygdhistorikk(),
        VilkårsgrunnlagHistorikk(),
        Skjæringstidspunkter(emptyList()),
        regelverkslogg,
        emptyList<Person>(),
        regler = regler
    )

    constructor(
        personidentifikator: Personidentifikator,
        alder: Alder,
        regelverkslogg: Regelverkslogg
    ) : this(personidentifikator, alder, regelverkslogg, NormalArbeidstaker)

    internal val yrkesaktiviteter: List<Yrkesaktivitet> get() = _yrkesaktiviteter.toList()

    var personidentifikator: Personidentifikator = personidentifikator
        private set

    val fødselsnummer get() = personidentifikator.toString()

    internal var skjæringstidspunkter: Skjæringstidspunkter = skjæringstidspunkter
        private set

    internal fun view() = PersonView(
        arbeidsgivere = yrkesaktiviteter.map { it.view() },
        vilkårsgrunnlaghistorikk = vilkårsgrunnlagHistorikk.view()
    )

    fun håndterSykmelding(eventBus: EventBus, sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler sykmelding")
        tidligereBehandlinger(sykmelding.behandlingsporing, aktivitetsloggMedPersonkontekst, sykmelding.periode())
        val yrkesaktivitet = finnEllerOpprettYrkesaktivitet(sykmelding.behandlingsporing, aktivitetsloggMedPersonkontekst)
        yrkesaktivitet.håndterSykmelding(sykmelding, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, sykmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndterAvbruttSøknad(eventBus: EventBus, avbruttSøknad: AvbruttSøknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler avbrutt søknad")
        val yrkesaktivitet = finnYrkesaktivitet(avbruttSøknad.behandlingsporing)
        yrkesaktivitet.håndterAvbruttSøknad(avbruttSøknad, aktivitetsloggMedPersonkontekst)
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, avbruttSøknad, aktivitetsloggMedPersonkontekst)
    }

    fun håndterForkastSykmeldingsperioder(eventBus: EventBus, forkastSykmeldingsperioder: ForkastSykmeldingsperioder, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler forkasting av sykmeldingsperioder")
        finnYrkesaktivitet(forkastSykmeldingsperioder.behandlingsporing).håndterForkastSykmeldingsperioder(forkastSykmeldingsperioder, aktivitetsloggMedPersonkontekst)
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, forkastSykmeldingsperioder, aktivitetsloggMedPersonkontekst)
    }

    fun håndterAnmodningOmForkasting(eventBus: EventBus, anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler anmodning om forkasting")
        val revurderingseventyr = finnYrkesaktivitet(anmodningOmForkasting.behandlingsporing).håndterAnmodningOmForkasting(eventBus, anmodningOmForkasting, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, anmodningOmForkasting, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSøknad(eventBus: EventBus, søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler søknad")
        tidligereBehandlinger(søknad.behandlingsporing, aktivitetsloggMedPersonkontekst, søknad.sykdomstidslinje.periode()!!)
        val yrkesaktivitet = finnEllerOpprettYrkesaktivitet(søknad.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = yrkesaktivitet.håndterSøknad(eventBus, søknad, aktivitetsloggMedPersonkontekst, yrkesaktiviteter.toList(), infotrygdhistorikk)
        igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, søknad, aktivitetsloggMedPersonkontekst)
    }

    fun håndterArbeidsgiveropplysninger(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler arbeidsgiveropplysningene ${arbeidsgiveropplysninger.joinToString { "${it::class.simpleName}" }}")
        val arbeidsgiver = finnEllerOpprettYrkesaktivitet(arbeidsgiveropplysninger.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
    }

    fun håndterKorrigerteArbeidsgiveropplysninger(eventBus: EventBus, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler de korrigerte arbeidsgiveropplysningene ${korrigerteArbeidsgiveropplysninger.joinToString { "${it::class.simpleName}" }}")
        val arbeidsgiver = finnEllerOpprettYrkesaktivitet(korrigerteArbeidsgiveropplysninger.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndterKorrigerteArbeidsgiveropplysninger(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInntektsmelding(eventBus: EventBus, inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler inntektsmelding")
        val arbeidsgiver = finnEllerOpprettYrkesaktivitet(inntektsmelding.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndterInntektsmelding(eventBus, inntektsmelding, aktivitetsloggMedPersonkontekst)
        arbeidsgiver.inntektsmeldingFerdigbehandlet(eventBus, inntektsmelding, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, inntektsmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInntektsmeldingerReplay(eventBus: EventBus, replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler replay av inntektsmeldinger")
        val revurderingseventyr = finnYrkesaktivitet(replays.behandlingsporing).håndterInntektsmeldingerReplay(eventBus, replays, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, replays, aktivitetsloggMedPersonkontekst)
    }

    fun håndterMinimumSykdomsgradsvurderingMelding(eventBus: EventBus, melding: MinimumSykdomsgradsvurderingMelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler minimum sykdomsgradvurdering")
        melding.oppdater(this.minimumSykdomsgradsvurdering)
        this.igangsettOverstyring(eventBus, Revurderingseventyr.minimumSykdomsgradVurdert(melding, melding.periodeForEndring()), aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, melding, aktivitetsloggMedPersonkontekst)
    }

    private fun tidligereBehandlinger(behandlingsporing: Behandlingsporing.Yrkesaktivitet, aktivitetslogg: IAktivitetslogg, periode: Periode) {
        val cutoff = periode.start.minusMonths(6)
        val andreBehandledeVedtaksperioder = tidligereBehandlinger.flatMap { it.vedtaksperioderEtter(cutoff) }
        if (andreBehandledeVedtaksperioder.isNotEmpty()) {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_AN_5)
            val msg = andreBehandledeVedtaksperioder.map {
                "vedtaksperiode(${it.periode})"
            }
            aktivitetslogg.info(
                """hendelse: ${behandlingsporing::class.java.simpleName} ($periode) kaster ut personen 
                | tidligere behandlede identer: ${tidligereBehandlinger.map { it.personidentifikator }}
                | tidligere behandlede perioder: ${msg.joinToString { it }}
                | cutoff: $cutoff""".trimMargin()
            )
        }
    }

    private fun vedtaksperioderEtter(dato: LocalDate) = yrkesaktiviteter.flatMap { it.vedtaksperioderEtter(dato) }

    fun håndterDødsmelding(eventBus: EventBus, dødsmelding: Dødsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler dødsmelding")
        aktivitetsloggMedPersonkontekst.info("Registrerer dødsdato")
        alder = dødsmelding.dødsdato(alder)
        håndterGjenoppta(eventBus, dødsmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndterIdentOpphørt(eventBus: EventBus, identOpphørt: IdentOpphørt, aktivitetslogg: IAktivitetslogg, nyPersonidentifikator: Personidentifikator) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler ident opphørt")
        aktivitetsloggMedPersonkontekst.info("Person har byttet ident til $nyPersonidentifikator")
        this.personidentifikator = nyPersonidentifikator
        håndterGjenoppta(eventBus, identOpphørt, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInfotrygdendringer(eventBus: EventBus, infotrygdendring: Infotrygdendring, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler infotrygdendring")
        val tidligsteDato = yrkesaktiviteter.tidligsteDato()
        infotrygdhistorikk.oppfrisk(aktivitetsloggMedPersonkontekst, tidligsteDato)
        håndterGjenoppta(eventBus, infotrygdendring, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInntektsendringer(eventBus: EventBus, inntektsendringer: Inntektsendringer, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler inntektsendringer")
        igangsettOverstyring(eventBus, Revurderingseventyr.inntektsendringer(inntektsendringer, inntektsendringer.inntektsendringFom), aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, inntektsendringer, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingshistorikkEtterInfotrygdendring(eventBus: EventBus, utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historikk fra infotrygd")
        håndterHistorikkFraInfotrygd(eventBus, utbetalingshistorikkEtterInfotrygdendring, aktivitetsloggMedPersonkontekst, utbetalingshistorikkEtterInfotrygdendring.element)
    }

    fun håndterUtbetalingshistorikk(eventBus: EventBus, utbetalingshistorikk: Utbetalingshistorikk, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historikk fra infotrygd")
        finnYrkesaktivitet(utbetalingshistorikk.behandlingsporing)
            .håndterHistorikkFraInfotrygd(eventBus, utbetalingshistorikk, aktivitetsloggMedPersonkontekst)
        håndterHistorikkFraInfotrygd(eventBus, utbetalingshistorikk, aktivitetsloggMedPersonkontekst, utbetalingshistorikk.element)
    }

    private fun håndterHistorikkFraInfotrygd(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, element: InfotrygdhistorikkElement) {
        aktivitetslogg.info("Oppdaterer Infotrygdhistorikk")
        val tidligsteDatoForEndring = infotrygdhistorikk.oppdaterHistorikk(element)
        val revurderingseventyr = if (tidligsteDatoForEndring == null) {
            aktivitetslogg.info("Oppfrisket Infotrygdhistorikk medførte ingen endringer")
            null
        } else {
            aktivitetslogg.info("Oppfrisket Infotrygdhistorikk ble lagret, starter revurdering fra tidligste endring $tidligsteDatoForEndring")
            Revurderingseventyr.infotrygdendring(hendelse, tidligsteDatoForEndring, tidligsteDatoForEndring.somPeriode())
        }
        beregnSkjæringstidspunkter()
        beregnArbeidsgiverperioder()
        emitOverlappendeInfotrygdperioder(eventBus)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetslogg)
        håndterGjenoppta(eventBus, hendelse, aktivitetslogg)
    }

    private fun emitOverlappendeInfotrygdperioder(eventBus: EventBus) {
        if (!infotrygdhistorikk.harHistorikk()) return
        val hendelseId = infotrygdhistorikk.siste.hendelseId
        val perioder = infotrygdhistorikk.siste.perioder
        val event = vedtaksperioder { true }.fold(EventSubscription.OverlappendeInfotrygdperioder(emptyList(), hendelseId.id)) { result, vedtaksperiode ->
            vedtaksperiode.overlappendeInfotrygdperioder(result, perioder)
        }
        eventBus.overlappendeInfotrygdperioder(event)
    }

    fun håndterUtbetalingshistorikkForFeriepenger(eventBus: EventBus, utbetalingshistorikk: UtbetalingshistorikkForFeriepenger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingshistorikk for feriepenger")

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            aktivitetsloggMedPersonkontekst.info("Starter beregning av feriepenger")
        }

        if (utbetalingshistorikk.skalBeregnesManuelt) {
            aktivitetsloggMedPersonkontekst.info("Person er markert for manuell beregning av feriepenger")
            return
        }

        val feriepengeberegner = Feriepengeberegner(
            alder = alder,
            opptjeningsår = utbetalingshistorikk.opptjeningsår,
            grunnlagFraInfotrygd = utbetalingshistorikk.grunnlagForFeriepenger(utbetalingshistorikk.datoForSisteFeriepengekjøringIInfotrygd),
            grunnlagFraSpleis = grunnlagForFeriepenger()
        )

        utbetalingshistorikk.sikreAtArbeidsgivereEksisterer {
            _yrkesaktiviteter.finnEllerOpprett(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(it), aktivitetsloggMedPersonkontekst)
        }
        yrkesaktiviteter.beregnFeriepengerForAlleArbeidsgivere(
            personidentifikator,
            feriepengeberegner,
            utbetalingshistorikk,
            aktivitetsloggMedPersonkontekst
        )

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            aktivitetsloggMedPersonkontekst.info("Feriepenger er utbetalt")
        }
    }

    fun håndterYtelser(eventBus: EventBus, ytelser: Ytelser, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historiske utbetalinger og inntekter")
        finnYrkesaktivitet(ytelser.behandlingsporing).håndterYtelser(eventBus, ytelser, aktivitetsloggMedPersonkontekst, infotrygdhistorikk)
        håndterGjenoppta(eventBus, ytelser, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingsgodkjenning(eventBus: EventBus, utbetalingsgodkjenning: Utbetalingsgodkjenning, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingsgodkjenning")
        val revurderingseventyr = finnYrkesaktivitet(utbetalingsgodkjenning.behandlingsporing).håndterBehandlingsavgjørelse(eventBus, utbetalingsgodkjenning, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, utbetalingsgodkjenning, aktivitetsloggMedPersonkontekst)
    }

    fun håndterVedtakFattet(eventBus: EventBus, vedtakFattet: VedtakFattet, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler vedtak fattet")
        finnYrkesaktivitet(vedtakFattet.behandlingsporing).håndterBehandlingsavgjørelse(eventBus, vedtakFattet, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, vedtakFattet, aktivitetsloggMedPersonkontekst)
    }

    fun håndterKanIkkeBehandlesHer(eventBus: EventBus, kanIkkeBehandlesHer: KanIkkeBehandlesHer, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler kan ikke behandles her")
        finnYrkesaktivitet(kanIkkeBehandlesHer.behandlingsporing).håndterBehandlingsavgjørelse(eventBus, kanIkkeBehandlesHer, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, kanIkkeBehandlesHer, aktivitetsloggMedPersonkontekst)
    }

    fun håndterVilkårsgrunnlag(eventBus: EventBus, vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler vilkårsgrunnlag")
        finnYrkesaktivitet(vilkårsgrunnlag.behandlingsporing).håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, vilkårsgrunnlag, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSimulering(eventBus: EventBus, simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler simulering")
        finnYrkesaktivitet(simulering.behandlingsporing).håndterSimulering(eventBus, simulering, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, simulering, aktivitetsloggMedPersonkontekst)
    }

    fun håndterFeriepengeutbetalingHendelse(eventBus: EventBus, utbetaling: FeriepengeutbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetaling")
        finnYrkesaktivitet(utbetaling.behandlingsporing).håndterFeriepengeutbetalingHendelse(eventBus, utbetaling, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, utbetaling, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingHendelse(eventBus: EventBus, utbetaling: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetaling")
        finnYrkesaktivitet(utbetaling.behandlingsporing).håndterUtbetalingHendelse(eventBus, utbetaling, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, utbetaling, aktivitetsloggMedPersonkontekst)
    }

    fun håndterPersonPåminnelse(eventBus: EventBus, påminnelse: PersonPåminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler personpåminnelse")
        håndterGjenoppta(eventBus, påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterGjenopptaBehandling(eventBus: EventBus, gjenopptaBehandling: GjenopptaBehandling, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler gjenoppta behandling")
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, gjenopptaBehandling, aktivitetsloggMedPersonkontekst)
    }

    fun håndterPåminnelse(eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler påminnelse")
        val revurderingseventyr = finnYrkesaktivitet(påminnelse.behandlingsporing).håndterPåminnelse(eventBus, påminnelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterOverstyrTidslinje(eventBus: EventBus, overstyrTidslinjeHendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler Overstyr tidslinje")
        val revurderingseventyr = finnYrkesaktivitet(overstyrTidslinjeHendelse.behandlingsporing).håndterOverstyrTidslinje(eventBus, overstyrTidslinjeHendelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, overstyrTidslinjeHendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterOverstyrArbeidsgiveropplysninger(eventBus: EventBus, hendelse: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler Overstyring av arbeidsgiveropplysninger")
        val inntektseventyr = yrkesaktiviteter.håndterOverstyringAvInntekt(eventBus, hendelse, aktivitetsloggMedPersonkontekst)
        val refusjonseventyr = yrkesaktiviteter.håndterOverstyringAvRefusjon(eventBus, hendelse, aktivitetsloggMedPersonkontekst)
        val tidligsteEventyr = tidligsteEventyr(inntektseventyr, refusjonseventyr) ?: return aktivitetsloggMedPersonkontekst.info("Ingen vedtaksperioder håndterte overstyringen av arbeidsgiveropplysninger fordi overstyringen ikke har endret noe.")
        igangsettOverstyring(eventBus, tidligsteEventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSkjønnsmessigFastsettelse(eventBus: EventBus, skjønnsmessigFastsettelse: SkjønnsmessigFastsettelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler skjønnsmessig fastsettelse")
        val revurderingseventyr = yrkesaktiviteter.håndterOverstyrInntektsgrunnlag(skjønnsmessigFastsettelse, aktivitetsloggMedPersonkontekst) ?: error("Ingen vedtaksperioder håndterte skjønnsmessig fastsettelse")
        igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, skjønnsmessigFastsettelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterOverstyrArbeidsforhold(eventBus: EventBus, overstyrArbeidsforhold: OverstyrArbeidsforhold, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler overstyring av arbeidsforhold")
        val revurderingseventyr = yrkesaktiviteter.håndterOverstyrInntektsgrunnlag(overstyrArbeidsforhold, aktivitetsloggMedPersonkontekst) ?: error("Kan ikke overstyre arbeidsforhold fordi ingen vedtaksperioder håndterte hendelsen")
        igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, overstyrArbeidsforhold, aktivitetsloggMedPersonkontekst)
    }

    fun håndterAnnulerUtbetaling(eventBus: EventBus, hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler annulleringforespørsel")
        val revurderingseventyr = finnYrkesaktivitet(hendelse.behandlingsporing).håndterAnnullerUtbetaling(eventBus, hendelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterGrunnbeløpsregulering(eventBus: EventBus, hendelse: Grunnbeløpsregulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler grunnbeløpsendring")
        if (vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(hendelse.skjæringstidspunkt) == null) return eventBus.sykefraværstilfelleIkkeFunnet(hendelse.skjæringstidspunkt)
        val revurderingseventyr = yrkesaktiviteter.håndterOverstyrInntektsgrunnlag(hendelse, aktivitetsloggMedPersonkontekst) ?: return
        igangsettOverstyring(eventBus, revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(eventBus, hendelse, aktivitetsloggMedPersonkontekst)
    }

    internal fun grunnlagForFeriepenger() = yrkesaktiviteter
        .map { it.grunnlagForFeriepenger() }
        .fold(Feriepengegrunnlagstidslinje(emptyList()), Feriepengegrunnlagstidslinje::plus)

    internal fun trengerHistorikkFraInfotrygd(aktivitetslogg: IAktivitetslogg) {
        infotrygdhistorikk.oppfriskNødvendig(aktivitetslogg, yrkesaktiviteter.tidligsteDato())
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Person", mapOf("fødselsnummer" to personidentifikator.toString()))
    }

    private fun registrer(aktivitetslogg: IAktivitetslogg, melding: String): IAktivitetslogg {
        return aktivitetslogg.kontekst(this).also {
            it.info(melding)
        }
    }

    private fun finnEllerOpprettYrkesaktivitet(yrkesaktivitet: Behandlingsporing.Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        _yrkesaktiviteter.finnEllerOpprett(yrkesaktivitet, aktivitetslogg)

    private fun finnYrkesaktivitet(behandlingsporing: Behandlingsporing.Yrkesaktivitet) =
        yrkesaktiviteter.finn(behandlingsporing) ?: error("Finner ikke arbeidsgiver med $behandlingsporing")

    private fun MutableList<Yrkesaktivitet>.finnEllerOpprett(behandlingsporing: Behandlingsporing.Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        finn(behandlingsporing) ?: Yrkesaktivitet(this@Person, behandlingsporing, regelverkslogg).also { yrkesaktivitet ->
            when (behandlingsporing) {
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> aktivitetslogg.info("Ny yrkesaktivitet som Arbeidsledig for denne personen")
                is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> aktivitetslogg.info("Ny yrkesaktivitet som Arbeidstaker med organisasjonsnummer ${behandlingsporing.organisasjonsnummer} for denne personen")
                Behandlingsporing.Yrkesaktivitet.Frilans -> aktivitetslogg.info("Ny yrkesaktivitet som Frilans for denne personen")
                Behandlingsporing.Yrkesaktivitet.Selvstendig -> aktivitetslogg.info("Ny yrkesaktivitet som Selvstendig for denne personen")
                Behandlingsporing.Yrkesaktivitet.Jordbruker -> aktivitetslogg.info("Ny yrkesaktivitet som Jordbruker for denne personen")
            }
            add(yrkesaktivitet)
        }

    internal fun nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
        yrkesaktiviteter.nåværendeVedtaksperioder(filter)

    internal fun speilrelatert(vararg perioder: Periode) = yrkesaktiviteter.nåværendeVedtaksperioder(SPEILRELATERT(*perioder)).isNotEmpty()
    internal fun avventerSøknad(periode: Periode) = yrkesaktiviteter.avventerSøknad(periode)
    internal fun fjernSykmeldingsperiode(periode: Periode) = yrkesaktiviteter.fjernSykmeldingsperiode(periode)
    internal fun vedtaksperioder(filter: VedtaksperiodeFilter) = yrkesaktiviteter.vedtaksperioder(filter)
    internal fun mursteinsperioder(utgangspunkt: Vedtaksperiode) = yrkesaktiviteter.mursteinsperioder(utgangspunkt)

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun lagreVilkårsgrunnlag(vilkårsgrunnlag: VilkårsgrunnlagElement) {
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag)
    }

    private fun beregnArbeidsgiverperioder() {
        yrkesaktiviteter.forEach { it.beregnPerioderUtenNavAnsvar() }
    }

    internal fun beregnSkjæringstidspunkter(): Skjæringstidspunkter {
        skjæringstidspunkter = yrkesaktiviteter.beregnSkjæringstidspunkt(infotrygdhistorikk)
        return skjæringstidspunkter
    }

    internal fun søppelbøtte(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, vedtaksperioderSomSkalForkastes: List<Vedtaksperiode>) {
        aktivitetslogg.info("Forkaster ${vedtaksperioderSomSkalForkastes.size} vedtaksperioder")
        infotrygdhistorikk.tøm()
        Yrkesaktivitet.søppelbøtte(eventBus, yrkesaktiviteter, hendelse, aktivitetslogg, vedtaksperioderSomSkalForkastes)
        beregnSkjæringstidspunkter()
        ryddOppVilkårsgrunnlag(aktivitetslogg)
        gjenopptaBehandling(aktivitetslogg)
    }

    private var gjenopptaBehandlingNy = false
    internal fun gjenopptaBehandling(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Forbereder gjenoppta behandling")
        gjenopptaBehandlingNy = true
    }

    private fun håndterGjenoppta(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        while (gjenopptaBehandlingNy) {
            gjenopptaBehandlingNy = false
            yrkesaktiviteter.gjenopptaBehandling(eventBus, hendelse, aktivitetslogg)
        }
        yrkesaktiviteter.validerTilstand(hendelse, aktivitetslogg)
        håndterVedtaksperiodeVenter(eventBus, hendelse)
        eventBus.behandlingUtført()
    }

    private fun håndterVedtaksperiodeVenter(eventBus: EventBus, hendelse: Hendelse) {
        when (hendelse) {
            is Sykmelding -> {
                /* Sykmelding fører ikke til endringer i tiltander, så sender ikke signal etter håndtering av den */
            }
            else -> {
                val eventer = yrkesaktiviteter
                    .nestemann()
                    ?.vedtaksperiodeVenter
                    ?.let { nestemannVenter ->
                        yrkesaktiviteter
                            .venter()
                            .mapNotNull { it.event(nestemannVenter) }
                    } ?: emptyList()
                eventBus.vedtaksperiodeVenter(eventer)
            }
        }
    }

    private fun igangsettOverstyring(eventBus: EventBus, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
        yrkesaktiviteter.igangsettOverstyring(eventBus, revurdering, aktivitetslogg)
        revurdering.sendOverstyringIgangsattEvent(eventBus)
        ryddOppVilkårsgrunnlag(aktivitetslogg)
    }

    private fun ryddOppVilkårsgrunnlag(aktivitetslogg: IAktivitetslogg) {
        val skjæringstidspunkter = yrkesaktiviteter.aktiveSkjæringstidspunkter()
        vilkårsgrunnlagHistorikk.oppdaterHistorikk(aktivitetslogg, skjæringstidspunkter)
    }

    internal fun erBehandletIInfotrygd(vedtaksperiode: Periode): Boolean {
        return infotrygdhistorikk.harUtbetaltI(vedtaksperiode) || infotrygdhistorikk.harFerieI(vedtaksperiode)
    }

    fun dto() = PersonUtDto(
        fødselsnummer = personidentifikator.toString(),
        alder = alder.dto(),
        arbeidsgivere = yrkesaktiviteter.map { it.dto(yrkesaktiviteter.nestemann()) },
        opprettet = opprettet,
        infotrygdhistorikk = infotrygdhistorikk.dto(),
        vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.dto(),
        skjæringstidspunkter = skjæringstidspunkter.dto(),
        minimumSykdomsgradVurdering = minimumSykdomsgradsvurdering.dto()
    )
}
