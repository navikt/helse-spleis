package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig.somOrganisasjonsnummer
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
import no.nav.helse.hendelser.MeldingsreferanseId
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
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingpåminnelse
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
import no.nav.helse.person.Yrkesaktivitet.Companion.beregnSkjæringstidspunkter
import no.nav.helse.person.Yrkesaktivitet.Companion.finn
import no.nav.helse.person.Yrkesaktivitet.Companion.finnAnnulleringskandidater
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
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.view.PersonView
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker

class Person private constructor(
    personidentifikator: Personidentifikator,
    internal var alder: Alder,
    private val _yrkesaktiviteter: MutableList<Yrkesaktivitet>,
    private val opprettet: LocalDateTime,
    internal val infotrygdhistorikk: Infotrygdhistorikk,
    internal val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val regelverkslogg: Regelverkslogg,
    private val tidligereBehandlinger: List<Person> = emptyList(),
    internal val regler: ArbeidsgiverRegler = NormalArbeidstaker,
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
        regler: ArbeidsgiverRegler
    ) : this(
        personidentifikator,
        alder,
        mutableListOf(),
        LocalDateTime.now(),
        Infotrygdhistorikk(),
        VilkårsgrunnlagHistorikk(),
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

    private val observers = mutableListOf<PersonObserver>()
    internal fun view() = PersonView(
        arbeidsgivere = yrkesaktiviteter.map { it.view() },
        vilkårsgrunnlaghistorikk = vilkårsgrunnlagHistorikk.view()
    )

    fun håndterSykmelding(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler sykmelding")
        tidligereBehandlinger(sykmelding.behandlingsporing, aktivitetsloggMedPersonkontekst, sykmelding.periode())
        val yrkesaktivitet = finnEllerOpprettYrkesaktivitet(sykmelding.behandlingsporing, aktivitetsloggMedPersonkontekst)
        yrkesaktivitet.håndterSykmelding(sykmelding, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(sykmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndterAvbruttSøknad(avbruttSøknad: AvbruttSøknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler avbrutt søknad")
        val yrkesaktivitet = finnYrkesaktivitet(avbruttSøknad.behandlingsporing, aktivitetsloggMedPersonkontekst)
        yrkesaktivitet.håndterAvbruttSøknad(avbruttSøknad, aktivitetsloggMedPersonkontekst)
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(avbruttSøknad, aktivitetsloggMedPersonkontekst)
    }

    fun håndterForkastSykmeldingsperioder(forkastSykmeldingsperioder: ForkastSykmeldingsperioder, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler forkasting av sykmeldingsperioder")
        finnYrkesaktivitet(forkastSykmeldingsperioder.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterForkastSykmeldingsperioder(forkastSykmeldingsperioder, aktivitetsloggMedPersonkontekst)
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(forkastSykmeldingsperioder, aktivitetsloggMedPersonkontekst)
    }

    fun håndterAnmodningOmForkasting(anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler anmodning om forkasting")
        finnYrkesaktivitet(anmodningOmForkasting.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterAnmodningOmForkasting(anmodningOmForkasting, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(anmodningOmForkasting, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSøknad(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler søknad")
        tidligereBehandlinger(søknad.behandlingsporing, aktivitetsloggMedPersonkontekst, søknad.sykdomstidslinje.periode()!!)
        val yrkesaktivitet = finnEllerOpprettYrkesaktivitet(søknad.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = yrkesaktivitet.håndterSøknad(søknad, aktivitetsloggMedPersonkontekst, yrkesaktiviteter.toList(), infotrygdhistorikk)
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(søknad, aktivitetsloggMedPersonkontekst)
    }

    fun håndterArbeidsgiveropplysninger(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler arbeidsgiveropplysningene ${arbeidsgiveropplysninger.joinToString { "${it::class.simpleName}" }}")
        val arbeidsgiver = finnEllerOpprettYrkesaktivitet(arbeidsgiveropplysninger.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndterArbeidsgiveropplysninger(arbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(arbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
    }

    fun håndterKorrigerteArbeidsgiveropplysninger(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler de korrigerte arbeidsgiveropplysningene ${korrigerteArbeidsgiveropplysninger.joinToString { "${it::class.simpleName}" }}")
        val arbeidsgiver = finnEllerOpprettYrkesaktivitet(korrigerteArbeidsgiveropplysninger.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndterKorrigerteArbeidsgiveropplysninger(korrigerteArbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(korrigerteArbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInntektsmelding(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler inntektsmelding")
        val arbeidsgiver = finnEllerOpprettYrkesaktivitet(inntektsmelding.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndterInntektsmelding(inntektsmelding, aktivitetsloggMedPersonkontekst)
        arbeidsgiver.inntektsmeldingFerdigbehandlet(inntektsmelding, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(inntektsmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInntektsmeldingerReplay(replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler replay av inntektsmeldinger")
        val revurderingseventyr = finnYrkesaktivitet(replays.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterInntektsmeldingerReplay(replays, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(replays, aktivitetsloggMedPersonkontekst)
    }

    fun håndterMinimumSykdomsgradsvurderingMelding(melding: MinimumSykdomsgradsvurderingMelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler minimum sykdomsgradvurdering")
        melding.oppdater(this.minimumSykdomsgradsvurdering)
        this.igangsettOverstyring(Revurderingseventyr.minimumSykdomsgradVurdert(melding, melding.periodeForEndring()), aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(melding, aktivitetsloggMedPersonkontekst)
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
    fun håndterDødsmelding(dødsmelding: Dødsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler dødsmelding")
        aktivitetsloggMedPersonkontekst.info("Registrerer dødsdato")
        alder = dødsmelding.dødsdato(alder)
        håndterGjenoppta(dødsmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndterIdentOpphørt(identOpphørt: IdentOpphørt, aktivitetslogg: IAktivitetslogg, nyPersonidentifikator: Personidentifikator) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler ident opphørt")
        aktivitetsloggMedPersonkontekst.info("Person har byttet ident til $nyPersonidentifikator")
        this.personidentifikator = nyPersonidentifikator
        håndterGjenoppta(identOpphørt, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInfotrygdendringer(infotrygdendring: Infotrygdendring, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler infotrygdendring")
        val tidligsteDato = yrkesaktiviteter.tidligsteDato()
        infotrygdhistorikk.oppfrisk(aktivitetsloggMedPersonkontekst, tidligsteDato)
        håndterGjenoppta(infotrygdendring, aktivitetsloggMedPersonkontekst)
    }

    fun håndterInntektsendringer(inntektsendringer: Inntektsendringer, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler inntektsendringer")
        igangsettOverstyring(Revurderingseventyr.inntektsendringer(inntektsendringer, inntektsendringer.inntektsendringFom), aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(inntektsendringer, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historikk fra infotrygd")
        håndterHistorikkFraInfotrygd(utbetalingshistorikkEtterInfotrygdendring, aktivitetsloggMedPersonkontekst, utbetalingshistorikkEtterInfotrygdendring.element)
    }

    fun håndterUtbetalingshistorikk(utbetalingshistorikk: Utbetalingshistorikk, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historikk fra infotrygd")
        finnYrkesaktivitet(utbetalingshistorikk.behandlingsporing, aktivitetsloggMedPersonkontekst)
            .håndterHistorikkFraInfotrygd(utbetalingshistorikk, aktivitetsloggMedPersonkontekst)
        håndterHistorikkFraInfotrygd(utbetalingshistorikk, aktivitetsloggMedPersonkontekst, utbetalingshistorikk.element)
    }

    private fun håndterHistorikkFraInfotrygd(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, element: InfotrygdhistorikkElement) {
        aktivitetslogg.info("Oppdaterer Infotrygdhistorikk")
        val tidligsteDatoForEndring = infotrygdhistorikk.oppdaterHistorikk(element)
        val revurderingseventyr = if (tidligsteDatoForEndring == null) {
            aktivitetslogg.info("Oppfrisket Infotrygdhistorikk medførte ingen endringer")
            null
        } else {
            aktivitetslogg.info("Oppfrisket Infotrygdhistorikk ble lagret, starter revurdering fra tidligste endring $tidligsteDatoForEndring")
            Revurderingseventyr.infotrygdendring(hendelse, tidligsteDatoForEndring, tidligsteDatoForEndring.somPeriode())
        }
        sykdomshistorikkEndret()
        emitOverlappendeInfotrygdperioder()
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetslogg)
        håndterGjenoppta(hendelse, aktivitetslogg)
    }

    private fun emitOverlappendeInfotrygdperioder() {
        if (!infotrygdhistorikk.harHistorikk()) return
        val hendelseId = infotrygdhistorikk.siste.hendelseId
        val perioder = infotrygdhistorikk.siste.perioder
        val event = vedtaksperioder { true }.fold(PersonObserver.OverlappendeInfotrygdperioder(emptyList(), hendelseId.id)) { result, vedtaksperiode ->
            vedtaksperiode.overlappendeInfotrygdperioder(result, perioder)
        }
        observers.forEach { it.overlappendeInfotrygdperioder(event) }
    }

    fun håndterUtbetalingshistorikkForFeriepenger(utbetalingshistorikk: UtbetalingshistorikkForFeriepenger, aktivitetslogg: IAktivitetslogg) {
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

    fun håndterYtelser(ytelser: Ytelser, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historiske utbetalinger og inntekter")
        finnYrkesaktivitet(ytelser.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterYtelser(ytelser, aktivitetsloggMedPersonkontekst, infotrygdhistorikk)
        håndterGjenoppta(ytelser, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingsgodkjenning(utbetalingsgodkjenning: Utbetalingsgodkjenning, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingsgodkjenning")
        finnYrkesaktivitet(utbetalingsgodkjenning.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterBehandlingsavgjørelse(utbetalingsgodkjenning, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(utbetalingsgodkjenning, aktivitetsloggMedPersonkontekst)
    }

    fun håndterVedtakFattet(vedtakFattet: VedtakFattet, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler vedtak fattet")
        finnYrkesaktivitet(vedtakFattet.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterBehandlingsavgjørelse(vedtakFattet, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(vedtakFattet, aktivitetsloggMedPersonkontekst)
    }

    fun håndterKanIkkeBehandlesHer(kanIkkeBehandlesHer: KanIkkeBehandlesHer, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler kan ikke behandles her")
        finnYrkesaktivitet(kanIkkeBehandlesHer.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterBehandlingsavgjørelse(kanIkkeBehandlesHer, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(kanIkkeBehandlesHer, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSykepengegrunnlagForArbeidsgiver(sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler sykepengegrunnlag for arbeidsgiver")
        finnYrkesaktivitet(sykepengegrunnlagForArbeidsgiver.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterSykepengegrunnlagForArbeidsgiver(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedPersonkontekst)
    }

    fun håndterVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler vilkårsgrunnlag")
        finnYrkesaktivitet(vilkårsgrunnlag.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterVilkårsgrunnlag(vilkårsgrunnlag, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(vilkårsgrunnlag, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSimulering(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler simulering")
        finnYrkesaktivitet(simulering.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterSimulering(simulering, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(simulering, aktivitetsloggMedPersonkontekst)
    }

    fun håndterFeriepengeutbetalingHendelse(utbetaling: FeriepengeutbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetaling")
        finnYrkesaktivitet(utbetaling.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterFeriepengeutbetalingHendelse(utbetaling, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(utbetaling, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingHendelse(utbetaling: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetaling")
        finnYrkesaktivitet(utbetaling.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterUtbetalingHendelse(utbetaling, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(utbetaling, aktivitetsloggMedPersonkontekst)
    }

    fun håndterUtbetalingPåminnelse(påminnelse: Utbetalingpåminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingpåminnelse")
        finnYrkesaktivitet(påminnelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterUtbetalingpåminnelse(påminnelse, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterPersonPåminnelse(påminnelse: PersonPåminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler personpåminnelse")
        håndterGjenoppta(påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterGjenopptaBehandling(gjenopptaBehandling: GjenopptaBehandling, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler gjenoppta behandling")
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(gjenopptaBehandling, aktivitetsloggMedPersonkontekst)
    }

    fun håndterPåminnelse(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler påminnelse")
        val revurderingseventyr = finnYrkesaktivitet(påminnelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterPåminnelse(påminnelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterOverstyrTidslinje(overstyrTidslinjeHendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler Overstyr tidslinje")
        val revurderingseventyr = finnYrkesaktivitet(overstyrTidslinjeHendelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterOverstyrTidslinje(overstyrTidslinjeHendelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(overstyrTidslinjeHendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterOverstyrArbeidsgiveropplysninger(hendelse: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler Overstyring av arbeidsgiveropplysninger")
        val inntektseventyr = yrkesaktiviteter.håndterOverstyringAvInntekt(hendelse, aktivitetsloggMedPersonkontekst)
        val refusjonseventyr = yrkesaktiviteter.håndterOverstyringAvRefusjon(hendelse, aktivitetsloggMedPersonkontekst)
        val tidligsteEventyr = tidligsteEventyr(inntektseventyr, refusjonseventyr) ?: return aktivitetsloggMedPersonkontekst.info("Ingen vedtaksperioder håndterte overstyringen av arbeidsgiveropplysninger fordi overstyringen ikke har endret noe.")
        igangsettOverstyring(tidligsteEventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterSkjønnsmessigFastsettelse(skjønnsmessigFastsettelse: SkjønnsmessigFastsettelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler skjønnsmessig fastsettelse")
        val revurderingseventyr = yrkesaktiviteter.håndterOverstyrInntektsgrunnlag(skjønnsmessigFastsettelse, aktivitetsloggMedPersonkontekst) ?: error("Ingen vedtaksperioder håndterte skjønnsmessig fastsettelse")
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(skjønnsmessigFastsettelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterOverstyrArbeidsforhold(overstyrArbeidsforhold: OverstyrArbeidsforhold, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler overstyring av arbeidsforhold")
        val revurderingseventyr = yrkesaktiviteter.håndterOverstyrInntektsgrunnlag(overstyrArbeidsforhold, aktivitetsloggMedPersonkontekst) ?: error("Kan ikke overstyre arbeidsforhold fordi ingen vedtaksperioder håndterte hendelsen")
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(overstyrArbeidsforhold, aktivitetsloggMedPersonkontekst)
    }

    fun håndterAnnulerUtbetaling(hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler annulleringforespørsel")
        val revurderingseventyr = finnYrkesaktivitet(hendelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndterAnnullerUtbetaling(hendelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndterGrunnbeløpsregulering(hendelse: Grunnbeløpsregulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler grunnbeløpsendring")
        if (vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(hendelse.skjæringstidspunkt) == null)
            return observers.forEach { hendelse.sykefraværstilfelleIkkeFunnet(it) }
        val revurderingseventyr = yrkesaktiviteter.håndterOverstyrInntektsgrunnlag(hendelse, aktivitetsloggMedPersonkontekst) ?: return
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun annullert(event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
    }

    internal fun finnAnnulleringskandidater(vedtaksperiode: Vedtaksperiode) = yrkesaktiviteter.finnAnnulleringskandidater(vedtaksperiode)

    internal fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(vedtaksperiodeId, organisasjonsnummer, påminnelse) }
    }

    internal fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(vedtaksperiodeId, organisasjonsnummer, tilstandType) }
    }

    internal fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        observers.forEach { it.vedtaksperiodeForkastet(event) }
    }

    internal fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        observers.forEach { it.vedtaksperiodeEndret(event) }
    }

    internal fun inntektsmeldingReplay(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.inntektsmeldingReplay(event) }
    }

    internal fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerArbeidsgiveropplysninger(event) }
    }

    internal fun trengerIkkeArbeidsgiveropplysninger(event: PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerIkkeArbeidsgiveropplysninger(event) }
    }

    internal fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtbetalt(event) }
    }

    internal fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtenUtbetaling(event) }
    }

    internal fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(event) }
    }

    internal fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        observers.forEach { it.avsluttetUtenVedtak(event) }
    }

    internal fun avsluttetMedVedtak(avsluttetMedVedtakEvent: PersonObserver.AvsluttetMedVedtakEvent) {
        observers.forEach { it.avsluttetMedVedtak(avsluttetMedVedtakEvent) }
    }

    internal fun analytiskDatapakke(analytiskDatapakkeEvent: PersonObserver.AnalytiskDatapakkeEvent) {
        observers.forEach { it.analytiskDatapakke(analytiskDatapakkeEvent) }
    }

    internal fun behandlingLukket(behandlingLukketEvent: PersonObserver.BehandlingLukketEvent) {
        observers.forEach { it.behandlingLukket(behandlingLukketEvent) }
    }

    internal fun behandlingForkastet(behandlingForkastetEvent: PersonObserver.BehandlingForkastetEvent) {
        observers.forEach { it.behandlingForkastet(behandlingForkastetEvent) }
    }

    internal fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        observers.forEach { it.nyBehandling(event) }
    }

    internal fun utkastTilVedtak(event: PersonObserver.UtkastTilVedtakEvent) {
        observers.forEach { it.utkastTilVedtak(event) }
    }

    internal fun emitPlanlagtAnnullering(annulleringskandidater: Set<Vedtaksperiode>, hendelse: AnnullerUtbetaling) {
        val planlagtAnnullering = PersonObserver.PlanlagtAnnulleringEvent(
            yrkesaktivitet = hendelse.behandlingsporing.somOrganisasjonsnummer,
            vedtaksperioder = annulleringskandidater.map { it.id },
            fom = annulleringskandidater.minOf { it.periode.start },
            tom = annulleringskandidater.maxOf { it.periode.endInclusive },
            saksbehandlerIdent = hendelse.saksbehandlerIdent,
            årsaker = hendelse.årsaker,
            begrunnelse = hendelse.begrunnelse
        )
        observers.forEach { it.planlagtAnnullering(planlagtAnnullering) }
    }


    internal fun emitOverstyringIgangsattEvent(event: PersonObserver.OverstyringIgangsatt) {
        observers.forEach { it.overstyringIgangsatt(event) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent) {
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
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

    private fun finnYrkesaktivitet(behandlingsporing: Behandlingsporing.Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        yrkesaktiviteter.finn(behandlingsporing) ?: aktivitetslogg.logiskFeil("Finner ikke arbeidsgiver")

    private fun MutableList<Yrkesaktivitet>.finnEllerOpprett(behandlingsporing: Behandlingsporing.Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        finn(behandlingsporing) ?: Yrkesaktivitet(this@Person, behandlingsporing, regelverkslogg).also { yrkesaktivitet ->
            when (behandlingsporing) {
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> aktivitetslogg.info("Ny yrkesaktivitet som Arbeidsledig for denne personen")
                is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> aktivitetslogg.info("Ny yrkesaktivitet som Arbeidstaker med organisasjonsnummer ${behandlingsporing.organisasjonsnummer} for denne personen")
                Behandlingsporing.Yrkesaktivitet.Frilans -> aktivitetslogg.info("Ny yrkesaktivitet som Frilans for denne personen")
                Behandlingsporing.Yrkesaktivitet.Selvstendig -> aktivitetslogg.info("Ny yrkesaktivitet som Selvstendig for denne personen")
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

    internal fun blitt6GBegrensetSidenSist(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlagHistorikk.blitt6GBegrensetSidenSist(skjæringstidspunkt)

    internal fun lagreVilkårsgrunnlag(vilkårsgrunnlag: VilkårsgrunnlagElement) {
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag)
    }

    internal fun beregnSkjæringstidspunkt() = yrkesaktiviteter.beregnSkjæringstidspunkt(infotrygdhistorikk)
    internal fun sykdomshistorikkEndret() {
        yrkesaktiviteter.beregnSkjæringstidspunkter(infotrygdhistorikk)
    }

    internal fun søppelbøtte(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, vedtaksperioderSomSkalForkastes: List<Vedtaksperiode>) {
        aktivitetslogg.info("Forkaster ${vedtaksperioderSomSkalForkastes.size} vedtaksperioder")
        infotrygdhistorikk.tøm()
        Yrkesaktivitet.søppelbøtte(yrkesaktiviteter, hendelse, aktivitetslogg, vedtaksperioderSomSkalForkastes)
        sykdomshistorikkEndret()
        ryddOppVilkårsgrunnlag(aktivitetslogg)
        gjenopptaBehandling(aktivitetslogg)
    }

    internal fun emitInntektsmeldingFørSøknadEvent(
        meldingsreferanseId: UUID,
        yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
    ) {
        observers.forEach {
            it.inntektsmeldingFørSøknad(PersonObserver.InntektsmeldingFørSøknadEvent(meldingsreferanseId, yrkesaktivitetssporing))
        }
    }

    internal fun emitInntektsmeldingIkkeHåndtert(meldingsreferanseId: MeldingsreferanseId, organisasjonsnummer: String, speilrelatert: Boolean) {
        observers.forEach {
            it.inntektsmeldingIkkeHåndtert(meldingsreferanseId.id, organisasjonsnummer, speilrelatert)
        }
    }

    internal fun emitArbeidsgiveropplysningerIkkeHåndtert(meldingsreferanseId: MeldingsreferanseId, organisasjonsnummer: String) =
        emitInntektsmeldingIkkeHåndtert(meldingsreferanseId, organisasjonsnummer, true)

    internal fun emitInntektsmeldingHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        observers.forEach {
            it.inntektsmeldingHåndtert(meldingsreferanseId, vedtaksperiodeId, organisasjonsnummer)
        }
    }

    internal fun sendSkatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent: PersonObserver.SkatteinntekterLagtTilGrunnEvent) {
        observers.forEach {
            it.skatteinntekterLagtTilGrunn(skatteinntekterLagtTilGrunnEvent)
        }
    }

    internal fun emitSøknadHåndtert(meldingsreferanseId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        observers.forEach {
            it.søknadHåndtert(meldingsreferanseId, vedtaksperiodeId, organisasjonsnummer)
        }
    }

    private var gjenopptaBehandlingNy = false
    internal fun gjenopptaBehandling(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Forbereder gjenoppta behandling")
        gjenopptaBehandlingNy = true
    }

    private fun håndterGjenoppta(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        while (gjenopptaBehandlingNy) {
            gjenopptaBehandlingNy = false
            yrkesaktiviteter.gjenopptaBehandling(hendelse, aktivitetslogg)
        }
        yrkesaktiviteter.validerTilstand(hendelse, aktivitetslogg)
        håndterVedtaksperiodeVenter(hendelse)
        behandlingUtført()
    }

    private fun håndterVedtaksperiodeVenter(hendelse: Hendelse) {
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
                observers.forEach { it.vedtaksperioderVenter(eventer) }
            }
        }
    }

    private fun behandlingUtført() {
        observers.forEach { it.behandlingUtført() }
    }

    private fun igangsettOverstyring(revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
        yrkesaktiviteter.igangsettOverstyring(revurdering, aktivitetslogg)
        revurdering.sendOverstyringIgangsattEvent(this)
        ryddOppVilkårsgrunnlag(aktivitetslogg)
    }

    private fun ryddOppVilkårsgrunnlag(aktivitetslogg: IAktivitetslogg) {
        val skjæringstidspunkter = yrkesaktiviteter.aktiveSkjæringstidspunkter()
        vilkårsgrunnlagHistorikk.oppdaterHistorikk(aktivitetslogg, skjæringstidspunkter)
    }

    internal fun nyVedtaksperiodeUtbetaling(organisasjonsnummer: String, utbetalingId: UUID, vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(organisasjonsnummer, utbetalingId, vedtaksperiodeId) }
    }

    internal fun vedtaksperiodeOpprettet(vedtaksperiodeId: UUID, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, periode: Periode, skjæringstidspunkt: LocalDate, opprettet: LocalDateTime) {
        val event = PersonObserver.VedtaksperiodeOpprettet(vedtaksperiodeId, yrkesaktivitetssporing, periode, skjæringstidspunkt, opprettet)
        observers.forEach { it.vedtaksperiodeOpprettet(event) }
    }

    internal fun erBehandletIInfotrygd(vedtaksperiode: Periode): Boolean {
        return infotrygdhistorikk.harUtbetaltI(vedtaksperiode) || infotrygdhistorikk.harFerieI(vedtaksperiode)
    }

    internal fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        observers.forEach { it.vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent) }
    }

    fun dto() = PersonUtDto(
        fødselsnummer = personidentifikator.toString(),
        alder = alder.dto(),
        arbeidsgivere = yrkesaktiviteter.map { it.dto(yrkesaktiviteter.nestemann()) },
        opprettet = opprettet,
        infotrygdhistorikk = infotrygdhistorikk.dto(),
        vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.dto(),
        minimumSykdomsgradVurdering = minimumSykdomsgradsvurdering.dto()
    )
}
