package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.feriepenger.Feriepengeberegner
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
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
import no.nav.helse.person.Arbeidsgiver.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.person.Arbeidsgiver.Companion.avventerSøknad
import no.nav.helse.person.Arbeidsgiver.Companion.beregnFeriepengerForAlleArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.beregnSkjæringstidspunkt
import no.nav.helse.person.Arbeidsgiver.Companion.beregnSkjæringstidspunkter
import no.nav.helse.person.Arbeidsgiver.Companion.finn
import no.nav.helse.person.Arbeidsgiver.Companion.fjernSykmeldingsperiode
import no.nav.helse.person.Arbeidsgiver.Companion.førsteAuuSomVilUtbetales
import no.nav.helse.person.Arbeidsgiver.Companion.gjenopptaBehandling
import no.nav.helse.person.Arbeidsgiver.Companion.håndter
import no.nav.helse.person.Arbeidsgiver.Companion.håndterOverstyringAvInntekt
import no.nav.helse.person.Arbeidsgiver.Companion.håndterOverstyringAvRefusjon
import no.nav.helse.person.Arbeidsgiver.Companion.igangsettOverstyring
import no.nav.helse.person.Arbeidsgiver.Companion.mursteinsperioder
import no.nav.helse.person.Arbeidsgiver.Companion.nestemann
import no.nav.helse.person.Arbeidsgiver.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.tidligsteDato
import no.nav.helse.person.Arbeidsgiver.Companion.validerTilstand
import no.nav.helse.person.Arbeidsgiver.Companion.vedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.venter
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.Yrkesaktivitet.Companion.tilYrkesaktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.view.PersonView
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker

class Person private constructor(
    personidentifikator: Personidentifikator,
    internal var alder: Alder,
    private val _arbeidsgivere: MutableList<Arbeidsgiver>,
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
            val arbeidsgivere = mutableListOf<Arbeidsgiver>()
            val grunnlagsdataMap = mutableMapOf<UUID, VilkårsgrunnlagElement>()
            val alder = Alder.gjenopprett(dto.alder)
            val person = Person(
                personidentifikator = Personidentifikator(dto.fødselsnummer),
                alder = alder,
                _arbeidsgivere = arbeidsgivere,
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
            arbeidsgivere.addAll(dto.arbeidsgivere.map {
                Arbeidsgiver.gjenopprett(person, it, regelverkslogg, grunnlagsdataMap)
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

    internal val arbeidsgivere: List<Arbeidsgiver> get() = _arbeidsgivere.toList()

    var personidentifikator: Personidentifikator = personidentifikator
        private set

    val fødselsnummer get() = personidentifikator.toString()

    private val observers = mutableListOf<PersonObserver>()
    internal fun view() = PersonView(
        arbeidsgivere = arbeidsgivere.map { it.view() },
        vilkårsgrunnlaghistorikk = vilkårsgrunnlagHistorikk.view()
    )

    fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler sykmelding")
        tidligereBehandlinger(sykmelding.behandlingsporing, aktivitetsloggMedPersonkontekst, sykmelding.periode())
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(sykmelding.behandlingsporing, aktivitetsloggMedPersonkontekst)
        arbeidsgiver.håndter(sykmelding, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(sykmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(avbruttSøknad: AvbruttSøknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler avbrutt søknad")
        val arbeidsgiver = finnArbeidsgiver(avbruttSøknad.behandlingsporing, aktivitetsloggMedPersonkontekst)
        arbeidsgiver.håndter(avbruttSøknad, aktivitetsloggMedPersonkontekst)
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(avbruttSøknad, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(forkastSykmeldingsperioder: ForkastSykmeldingsperioder, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler forkasting av sykmeldingsperioder")
        finnArbeidsgiver(forkastSykmeldingsperioder.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(forkastSykmeldingsperioder, aktivitetsloggMedPersonkontekst)
        gjenopptaBehandling(aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(forkastSykmeldingsperioder, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler anmodning om forkasting")
        finnArbeidsgiver(anmodningOmForkasting.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(anmodningOmForkasting, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(anmodningOmForkasting, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler søknad")
        tidligereBehandlinger(søknad.behandlingsporing, aktivitetsloggMedPersonkontekst, søknad.sykdomstidslinje.periode()!!)
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(søknad.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndter(søknad, aktivitetsloggMedPersonkontekst, arbeidsgivere.toList(), infotrygdhistorikk)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(søknad, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler arbeidsgiveropplysningene ${arbeidsgiveropplysninger.joinToString { "${it::class.simpleName}" }}")
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(arbeidsgiveropplysninger.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndter(arbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(arbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler de korrigerte arbeidsgiveropplysningene ${korrigerteArbeidsgiveropplysninger.joinToString { "${it::class.simpleName}" }}")
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(korrigerteArbeidsgiveropplysninger.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndter(korrigerteArbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(korrigerteArbeidsgiveropplysninger, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler inntektsmelding")
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(inntektsmelding.behandlingsporing, aktivitetsloggMedPersonkontekst)
        val revurderingseventyr = arbeidsgiver.håndter(inntektsmelding, aktivitetsloggMedPersonkontekst)
        arbeidsgiver.inntektsmeldingFerdigbehandlet(inntektsmelding, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(inntektsmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler replay av inntektsmeldinger")
        val revurderingseventyr = finnArbeidsgiver(replays.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(replays, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(replays, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(melding: MinimumSykdomsgradsvurderingMelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler minimum sykdomsgradvurdering")
        melding.oppdater(this.minimumSykdomsgradsvurdering)
        this.igangsettOverstyring(Revurderingseventyr.minimumSykdomsgradVurdert(melding, melding.periodeForEndring()), aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(melding, aktivitetsloggMedPersonkontekst)
    }

    private fun tidligereBehandlinger(behandlingsporing: Behandlingsporing.Arbeidsgiver, aktivitetslogg: IAktivitetslogg, periode: Periode) {
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

    private fun vedtaksperioderEtter(dato: LocalDate) = arbeidsgivere.flatMap { it.vedtaksperioderEtter(dato) }
    fun håndter(dødsmelding: Dødsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler dødsmelding")
        aktivitetsloggMedPersonkontekst.info("Registrerer dødsdato")
        alder = dødsmelding.dødsdato(alder)
        håndterGjenoppta(dødsmelding, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(identOpphørt: IdentOpphørt, aktivitetslogg: IAktivitetslogg, nyPersonidentifikator: Personidentifikator) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler ident opphørt")
        aktivitetsloggMedPersonkontekst.info("Person har byttet ident til $nyPersonidentifikator")
        this.personidentifikator = nyPersonidentifikator
        håndterGjenoppta(identOpphørt, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(infotrygdendring: Infotrygdendring, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler infotrygdendring")
        val tidligsteDato = arbeidsgivere.tidligsteDato()
        infotrygdhistorikk.oppfrisk(aktivitetsloggMedPersonkontekst, tidligsteDato)
        håndterGjenoppta(infotrygdendring, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(inntektsendringer: Inntektsendringer, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler inntektsendringer")
        igangsettOverstyring(Revurderingseventyr.inntektsendringer(inntektsendringer, inntektsendringer.inntektsendringFom), aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(inntektsendringer, aktivitetsloggMedPersonkontekst)
    }


    fun håndter(utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historikk fra infotrygd")
        håndterHistorikkFraInfotrygd(utbetalingshistorikkEtterInfotrygdendring, aktivitetsloggMedPersonkontekst, utbetalingshistorikkEtterInfotrygdendring.element)
    }

    fun håndter(utbetalingshistorikk: Utbetalingshistorikk, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historikk fra infotrygd")
        finnArbeidsgiver(utbetalingshistorikk.behandlingsporing, aktivitetsloggMedPersonkontekst)
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

    fun håndter(utbetalingshistorikk: UtbetalingshistorikkForFeriepenger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingshistorikk for feriepenger")

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            aktivitetsloggMedPersonkontekst.info("Starter beregning av feriepenger")
        }

        if (utbetalingshistorikk.skalBeregnesManuelt) {
            aktivitetsloggMedPersonkontekst.info("Person er markert for manuell beregning av feriepenger")
            return
        }

        // Hardkodet dato skal være datoen Infotrygd sist kjørte feriepenger
        val DATO_FOR_SISTE_FERIEPENGEKJØRING_I_INFOTRYGD = LocalDate.of(2025, 1, 18)

        val feriepengeberegner = Feriepengeberegner(
            alder = alder,
            opptjeningsår = utbetalingshistorikk.opptjeningsår,
            grunnlagFraInfotrygd = utbetalingshistorikk.grunnlagForFeriepenger(DATO_FOR_SISTE_FERIEPENGEKJØRING_I_INFOTRYGD),
            grunnlagFraSpleis = grunnlagForFeriepenger()
        )

        val feriepengepengebeløpPersonUtbetaltAvInfotrygd = utbetalingshistorikk.utbetalteFeriepengerTilPerson()
        val beregnetFeriepengebeløpPersonInfotrygd =
            feriepengeberegner.beregnFeriepengerForInfotrygdPerson().roundToInt()

        if (beregnetFeriepengebeløpPersonInfotrygd != 0 && beregnetFeriepengebeløpPersonInfotrygd !in feriepengepengebeløpPersonUtbetaltAvInfotrygd) {
            aktivitetsloggMedPersonkontekst.info(
                """
                Beregnet feriepengebeløp til person i IT samsvarer ikke med faktisk utbetalt beløp
                Faktisk utbetalt beløp: $feriepengepengebeløpPersonUtbetaltAvInfotrygd
                Beregnet beløp: $beregnetFeriepengebeløpPersonInfotrygd
                """.trimIndent()
            )
        }

        utbetalingshistorikk.sikreAtArbeidsgivereEksisterer {
            _arbeidsgivere.finnEllerOpprett(Yrkesaktivitet.Arbeidstaker(it), aktivitetsloggMedPersonkontekst)
        }
        arbeidsgivere.beregnFeriepengerForAlleArbeidsgivere(
            personidentifikator,
            feriepengeberegner,
            utbetalingshistorikk,
            aktivitetsloggMedPersonkontekst
        )

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            aktivitetsloggMedPersonkontekst.info("Feriepenger er utbetalt")
        }
    }

    fun håndter(ytelser: Ytelser, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler historiske utbetalinger og inntekter")
        finnArbeidsgiver(ytelser.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(ytelser, aktivitetsloggMedPersonkontekst, infotrygdhistorikk)
        håndterGjenoppta(ytelser, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingsgodkjenning")
        finnArbeidsgiver(utbetalingsgodkjenning.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(utbetalingsgodkjenning, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(utbetalingsgodkjenning, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(vedtakFattet: VedtakFattet, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler vedtak fattet")
        finnArbeidsgiver(vedtakFattet.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(vedtakFattet, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(vedtakFattet, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(kanIkkeBehandlesHer: KanIkkeBehandlesHer, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler kan ikke behandles her")
        finnArbeidsgiver(kanIkkeBehandlesHer.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(kanIkkeBehandlesHer, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(kanIkkeBehandlesHer, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler sykepengegrunnlag for arbeidsgiver")
        finnArbeidsgiver(sykepengegrunnlagForArbeidsgiver.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(vilkårsgrunnlag, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(vilkårsgrunnlag, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler simulering")
        finnArbeidsgiver(simulering.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(simulering, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(simulering, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(utbetaling: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetaling")
        finnArbeidsgiver(utbetaling.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(utbetaling, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(utbetaling, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(påminnelse: Utbetalingpåminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler utbetalingpåminnelse")
        finnArbeidsgiver(påminnelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(påminnelse, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(påminnelse: PersonPåminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler personpåminnelse")
        arbeidsgivere.beregnSkjæringstidspunkter(infotrygdhistorikk)
        arbeidsgivere.førsteAuuSomVilUtbetales()?.let {
            aktivitetsloggMedPersonkontekst.info("Igangsetter reberegning fra ${it.periode.start} på grunn av AUU som vil utbetales")
            igangsettOverstyring(Revurderingseventyr.reberegning(påminnelse, it.skjæringstidspunkt, it.periode), aktivitetsloggMedPersonkontekst)
        }
        håndterGjenoppta(påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler påminnelse")
        val revurderingseventyr = finnArbeidsgiver(påminnelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(påminnelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(påminnelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler Overstyr tidslinje")
        val revurderingseventyr = finnArbeidsgiver(hendelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(hendelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(hendelse: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler Overstyring av arbeidsgiveropplysninger")
        val inntektseventyr = arbeidsgivere.håndterOverstyringAvInntekt(hendelse, aktivitetsloggMedPersonkontekst)
        val refusjonseventyr = arbeidsgivere.håndterOverstyringAvRefusjon(hendelse, aktivitetsloggMedPersonkontekst)
        val tidligsteEventyr = tidligsteEventyr(inntektseventyr, refusjonseventyr)
        if (tidligsteEventyr == null) return aktivitetsloggMedPersonkontekst.info("Ingen vedtaksperioder håndterte overstyringen av arbeidsgiveropplysninger fordi overstyringen ikke har endret noe.")
        igangsettOverstyring(tidligsteEventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(hendelse: SkjønnsmessigFastsettelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler skjønnsmessig fastsettelse")
        val revurderingseventyr = arbeidsgivere.håndter(hendelse, aktivitetsloggMedPersonkontekst) ?: error("Ingen vedtaksperioder håndterte skjønnsmessig fastsettelse")
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler overstyring av arbeidsforhold")
        val revurderingseventyr = arbeidsgivere.håndter(overstyrArbeidsforhold, aktivitetsloggMedPersonkontekst) ?: error("Kan ikke overstyre arbeidsforhold fordi ingen vedtaksperioder håndterte hendelsen")
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(overstyrArbeidsforhold, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler annulleringforespørsel")
        val revurderingseventyr = finnArbeidsgiver(hendelse.behandlingsporing, aktivitetsloggMedPersonkontekst).håndter(hendelse, aktivitetsloggMedPersonkontekst)
        if (revurderingseventyr != null) igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun håndter(hendelse: Grunnbeløpsregulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedPersonkontekst = registrer(aktivitetslogg, "Behandler grunnbeløpsendring")
        if (vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(hendelse.skjæringstidspunkt) == null)
            return observers.forEach { hendelse.sykefraværstilfelleIkkeFunnet(it) }
        val revurderingseventyr = arbeidsgivere.håndter(hendelse, aktivitetsloggMedPersonkontekst) ?: return
        igangsettOverstyring(revurderingseventyr, aktivitetsloggMedPersonkontekst)
        håndterGjenoppta(hendelse, aktivitetsloggMedPersonkontekst)
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun annullert(event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
    }

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

    internal fun emitOverstyringIgangsattEvent(event: PersonObserver.OverstyringIgangsatt) {
        observers.forEach { it.overstyringIgangsatt(event) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent) {
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
    }

    internal fun grunnlagForFeriepenger() = arbeidsgivere.map { it.grunnlagForFeriepenger() }
    internal fun trengerHistorikkFraInfotrygd(aktivitetslogg: IAktivitetslogg) {
        infotrygdhistorikk.oppfriskNødvendig(aktivitetslogg, arbeidsgivere.tidligsteDato())
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Person", mapOf("fødselsnummer" to personidentifikator.toString()))
    }

    private fun registrer(aktivitetslogg: IAktivitetslogg, melding: String): IAktivitetslogg {
        return aktivitetslogg.kontekst(this).also {
            it.info(melding)
        }
    }

    private fun finnEllerOpprettArbeidsgiver(behandlingsporing: Behandlingsporing.Arbeidsgiver, aktivitetslogg: IAktivitetslogg) =
        finnEllerOpprettArbeidsgiver(behandlingsporing.organisasjonsnummer.tilYrkesaktivitet(), aktivitetslogg)

    private fun finnEllerOpprettArbeidsgiver(yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        _arbeidsgivere.finnEllerOpprett(yrkesaktivitet, aktivitetslogg)

    private fun finnArbeidsgiver(behandlingsporing: Behandlingsporing.Arbeidsgiver, aktivitetslogg: IAktivitetslogg) =
        behandlingsporing.organisasjonsnummer.tilYrkesaktivitet().let { yrkesaktivitet ->
            arbeidsgivere.finn(yrkesaktivitet) ?: aktivitetslogg.logiskFeil("Finner ikke arbeidsgiver")
        }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(yrkesaktivitet: Yrkesaktivitet, aktivitetslogg: IAktivitetslogg) =
        finn(yrkesaktivitet) ?: Arbeidsgiver(this@Person, yrkesaktivitet, regelverkslogg).also { arbeidsgiver ->
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", yrkesaktivitet)
            add(arbeidsgiver)
        }

    internal fun nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
        arbeidsgivere.nåværendeVedtaksperioder(filter)

    internal fun avventerSøknad(periode: Periode) = arbeidsgivere.avventerSøknad(periode)
    internal fun fjernSykmeldingsperiode(periode: Periode) = arbeidsgivere.fjernSykmeldingsperiode(periode)
    internal fun vedtaksperioder(filter: VedtaksperiodeFilter) = arbeidsgivere.vedtaksperioder(filter)
    internal fun mursteinsperioder(utgangspunkt: Vedtaksperiode) = arbeidsgivere.mursteinsperioder(utgangspunkt)

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun blitt6GBegrensetSidenSist(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlagHistorikk.blitt6GBegrensetSidenSist(skjæringstidspunkt)

    internal fun lagreVilkårsgrunnlag(vilkårsgrunnlag: VilkårsgrunnlagElement) {
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag)
    }

    internal fun beregnSkjæringstidspunkt() = arbeidsgivere.beregnSkjæringstidspunkt(infotrygdhistorikk)
    internal fun sykdomshistorikkEndret() {
        arbeidsgivere.beregnSkjæringstidspunkter(infotrygdhistorikk)
    }

    internal fun søppelbøtte(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, filter: VedtaksperiodeFilter) {
        infotrygdhistorikk.tøm()
        Arbeidsgiver.søppelbøtte(arbeidsgivere, hendelse, aktivitetslogg, filter)
        sykdomshistorikkEndret()
        ryddOppVilkårsgrunnlag(aktivitetslogg)
        gjenopptaBehandling(aktivitetslogg)
    }

    internal fun emitInntektsmeldingFørSøknadEvent(
        meldingsreferanseId: UUID,
        relevanteSykmeldingsperioder: List<Periode>,
        organisasjonsnummer: String
    ) {
        observers.forEach {
            it.inntektsmeldingFørSøknad(PersonObserver.InntektsmeldingFørSøknadEvent(meldingsreferanseId, relevanteSykmeldingsperioder, organisasjonsnummer))
        }
    }

    internal fun emitInntektsmeldingIkkeHåndtert(hendelse: Hendelse, organisasjonsnummer: String, harPeriodeInnenfor16Dager: Boolean) {
        observers.forEach {
            it.inntektsmeldingIkkeHåndtert(hendelse.metadata.meldingsreferanseId.id, organisasjonsnummer, harPeriodeInnenfor16Dager)
        }
    }

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
            arbeidsgivere.gjenopptaBehandling(hendelse, aktivitetslogg)
        }
        arbeidsgivere.validerTilstand(hendelse, aktivitetslogg)
        håndterVedtaksperiodeVenter(hendelse)
        behandlingUtført()
    }

    private fun håndterVedtaksperiodeVenter(hendelse: Hendelse) {
        when (hendelse) {
            is Sykmelding -> { /* Sykmelding fører ikke til endringer i tiltander, så sender ikke signal etter håndtering av den */
            }
            else -> {
                val eventer = arbeidsgivere.nestemann()?.let { nestemann ->
                    arbeidsgivere.venter(nestemann).map { it.event() }
                } ?: emptyList()
                observers.forEach { it.vedtaksperioderVenter(eventer) }
            }
        }
    }

    private fun behandlingUtført() {
        observers.forEach { it.behandlingUtført() }
    }

    private fun igangsettOverstyring(revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
        arbeidsgivere.igangsettOverstyring(revurdering, aktivitetslogg)
        revurdering.sendOverstyringIgangsattEvent(this)
        ryddOppVilkårsgrunnlag(aktivitetslogg)
    }

    private fun ryddOppVilkårsgrunnlag(aktivitetslogg: IAktivitetslogg) {
        val skjæringstidspunkter = arbeidsgivere.aktiveSkjæringstidspunkter()
        vilkårsgrunnlagHistorikk.oppdaterHistorikk(aktivitetslogg, skjæringstidspunkter)
    }

    internal fun nyVedtaksperiodeUtbetaling(organisasjonsnummer: String, utbetalingId: UUID, vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(organisasjonsnummer, utbetalingId, vedtaksperiodeId) }
    }

    internal fun vedtaksperiodeOpprettet(vedtaksperiodeId: UUID, organisasjonsnummer: String, periode: Periode, skjæringstidspunkt: LocalDate, opprettet: LocalDateTime) {
        val event = PersonObserver.VedtaksperiodeOpprettet(vedtaksperiodeId, organisasjonsnummer, periode, skjæringstidspunkt, opprettet)
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
        arbeidsgivere = arbeidsgivere.map { it.dto(arbeidsgivere.nestemann()) },
        opprettet = opprettet,
        infotrygdhistorikk = infotrygdhistorikk.dto(),
        vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.dto(),
        minimumSykdomsgradVurdering = minimumSykdomsgradsvurdering.dto()
    )
}
