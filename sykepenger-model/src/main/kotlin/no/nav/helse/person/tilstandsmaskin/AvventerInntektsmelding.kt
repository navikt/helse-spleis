package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import java.time.Period
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig.somArbeidstakerOrThrow
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.MED_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Vedtaksperiode.Companion.egenmeldingsperioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal data object AvventerInntektsmelding : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_INNTEKTSMELDING
    override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        tilstandsendringstidspunkt.plusDays(180)

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype is Behandlingsporing.Yrkesaktivitet.Arbeidstaker) { "Forventer kun arbeidstakere her" }
        trengerInntektsmeldingReplay(vedtaksperiode, eventBus)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.behandlinger.harIkkeUtbetaling()) {
            "hæ?! vedtaksperiodens behandling er ikke uberegnet!"
        }
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg, påminnelse)) {
            aktivitetslogg.info("Gikk videre fra AvventerInntektsmelding til ${vedtaksperiode.tilstand::class.simpleName} som følge av en vanlig påminnelse.")
            return null
        }

        if (vurderOmInntektsmeldingAldriKommer(påminnelse)) {
            gåVidereMedInntekterFraAOrdningen(vedtaksperiode, aktivitetslogg, påminnelse, eventBus)
            return Revurderingseventyr.inntektsmeldingSomAldriKom(påminnelse, vedtaksperiode.periode)
        }

        trengerInntektsmeldingReplay(vedtaksperiode, eventBus)
        return null
    }

    private fun vurderOmInntektsmeldingAldriKommer(påminnelse: Påminnelse): Boolean {
        if (påminnelse.når(Påminnelse.Predikat.Flagg("ønskerInntektFraAOrdningen"))) return true
        val ventetMinst3Måneder = påminnelse.når(Påminnelse.Predikat.VentetMinst(Period.ofDays(90)))
        return ventetMinst3Måneder
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg, hendelse)
    }

    override fun replayUtført(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        if (vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg, hendelse)) return
        sendTrengerArbeidsgiveropplysninger(vedtaksperiode, eventBus)
    }

    override fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, eventBus, aktivitetslogg, hendelse)
    }

    private fun gåVidereMedInntekterFraAOrdningen(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg, hendelse: Hendelse, eventBus: EventBus) {
        if (vedtaksperiode.refusjonstidslinje.isEmpty() && vedtaksperiode.vilkårsgrunnlag != null) aktivitetslogg.varsel(Varselkode.RV_IV_10) // Burde dette være et eget varsel? Har jo bare brukt 0kr i refusjon 🤔
        vedtaksperiode.nullKronerRefusjonOmViManglerRefusjonsopplysninger(eventBus, hendelse.metadata, aktivitetslogg)
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
    }

    private fun vurderOmKanGåVidere(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg, hendelse: Hendelse): Boolean {
        vedtaksperiode.videreførEksisterendeRefusjonsopplysninger(eventBus, null, aktivitetslogg)
        vedtaksperiode.lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus, aktivitetslogg)

        if (!vedtaksperiode.skalArbeidstakerBehandlesISpeil()) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerAvsluttetUtenUtbetaling)
            return true
        }

        if (vedtaksperiode.harInntektOgRefusjon()) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
            return true
        }

        return false
    }


    private fun opplysningerViTrenger(vedtaksperiode: Vedtaksperiode): Set<EventSubscription.ForespurtOpplysning> {
        if (!vedtaksperiode.skalArbeidstakerBehandlesISpeil()) return emptySet() // perioden er AUU ✋

        if (vedtaksperiode.yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(vedtaksperiode).før.any { it.skalArbeidstakerBehandlesISpeil() }) return emptySet() // Da har en periode foran oss spurt for oss/ vi har det vi trenger ✋

        val opplysninger = mutableSetOf<EventSubscription.ForespurtOpplysning>().apply {
            if (!vedtaksperiode.harEksisterendeInntekt()) addAll(setOf(EventSubscription.Inntekt, EventSubscription.Refusjon)) // HAG støtter ikke skjema uten refusjon, så når vi først spør om inntekt _må_ vi også spørre om refusjon
            if (vedtaksperiode.refusjonstidslinje.isEmpty()) add(EventSubscription.Refusjon) // For de tilfellene vi faktiske trenger refusjon
        }
        if (opplysninger.isEmpty()) return emptySet() // Om vi har inntekt og refusjon så er saken biff 🥩

        if (vedtaksperiode.behandlinger.dagerNavOvertarAnsvar.isNotEmpty()) return opplysninger // Trenger hvert fall ikke opplysninger om arbeidsgiverperiode dersom Nav har overtatt ansvar for den ✋

        return opplysninger.apply {
            val sisteDelAvAgp = vedtaksperiode.behandlinger.ventedager().dagerUtenNavAnsvar.dager.lastOrNull()
            // Vi "trenger" jo aldri AGP, men spør om vi perioden overlapper/er rett etter beregnet AGP
            if (sisteDelAvAgp?.overlapperMed(vedtaksperiode.periode) == true || sisteDelAvAgp?.erRettFør(vedtaksperiode.periode) == true) {
                add(EventSubscription.Arbeidsgiverperiode)
            }
        }
    }

    internal fun sendTrengerArbeidsgiveropplysninger(vedtaksperiode: Vedtaksperiode, eventBus: EventBus) {
        val forespurteOpplysninger = opplysningerViTrenger(vedtaksperiode).takeUnless { it.isEmpty() } ?: return
        eventBus.trengerArbeidsgiveropplysninger(trengerArbeidsgiveropplysninger(vedtaksperiode, forespurteOpplysninger))

        // ved out-of-order gir vi beskjed om at vi ikke trenger arbeidsgiveropplysninger for den seneste perioden lenger
        vedtaksperiode.yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(vedtaksperiode).etter.firstOrNull()?.trengerIkkeArbeidsgiveropplysninger(eventBus)
    }

    private fun trengerArbeidsgiveropplysninger(
        vedtaksperiode: Vedtaksperiode,
        forespurteOpplysninger: Set<EventSubscription.ForespurtOpplysning>
    ): EventSubscription.TrengerArbeidsgiveropplysninger {
        val vedtaksperioder = when {
            // For å beregne riktig arbeidsgiverperiode/første fraværsdag
            EventSubscription.Arbeidsgiverperiode in forespurteOpplysninger -> vedtaksperioderIArbeidsgiverperiodeTilOgMedDenne(vedtaksperiode)
            // Dersom vi ikke trenger å beregne arbeidsgiverperiode/første fravarsdag trenger vi bare denne sykemeldingsperioden
            else -> listOf(vedtaksperiode)
        }
        return EventSubscription.TrengerArbeidsgiveropplysninger(
            personidentifikator = vedtaksperiode.person.personidentifikator,
            arbeidstaker = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype.somArbeidstakerOrThrow,
            vedtaksperiodeId = vedtaksperiode.id,
            skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
            sykmeldingsperioder = sykmeldingsperioder(vedtaksperioder),
            egenmeldingsperioder = vedtaksperioder.egenmeldingsperioder(),
            førsteFraværsdager = førsteFraværsdagerForForespørsel(vedtaksperiode),
            forespurteOpplysninger = forespurteOpplysninger
        )
    }

    private fun førsteFraværsdagerForForespørsel(vedtaksperiode: Vedtaksperiode): List<EventSubscription.FørsteFraværsdag> {
        val deAndre = vedtaksperiode.person.vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(vedtaksperiode.skjæringstidspunkt))
            .filterNot { it.yrkesaktivitet === vedtaksperiode.yrkesaktivitet }
            .filter { it.yrkesaktivitet.yrkesaktivitetstype is Behandlingsporing.Yrkesaktivitet.Arbeidstaker }
            .groupBy { it.yrkesaktivitet }
            .mapNotNull { (arbeidsgiver, perioder) ->
                val førsteFraværsdagForArbeidsgiver = perioder.asReversed().firstNotNullOfOrNull { it.førsteFraværsdag }
                førsteFraværsdagForArbeidsgiver?.let {
                    EventSubscription.FørsteFraværsdag(arbeidsgiver.yrkesaktivitetstype.somArbeidstakerOrThrow, it)
                }
            }
        val minEgen = vedtaksperiode.førsteFraværsdag?.let {
            EventSubscription.FørsteFraværsdag(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype.somArbeidstakerOrThrow, it)
        } ?: return deAndre
        return deAndre.plusElement(minEgen)
    }

    private fun vedtaksperioderIArbeidsgiverperiodeTilOgMedDenne(vedtaksperiode: Vedtaksperiode): List<Vedtaksperiode> {
        val arbeidsgiverperiode = vedtaksperiode.behandlinger.ventedager().dagerUtenNavAnsvar.periode ?: return listOf(vedtaksperiode)
        return vedtaksperiode.yrkesaktivitet.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).filter { it <= vedtaksperiode }
    }

    private fun Vedtaksperiode.trengerIkkeArbeidsgiveropplysninger(eventBus: EventBus) {
        eventBus.trengerIkkeArbeidsgiveropplysninger(
            EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent(
                arbeidstaker = yrkesaktivitet.yrkesaktivitetstype.somArbeidstakerOrThrow,
                vedtaksperiodeId = id
            )
        )
    }

    private fun trengerInntektsmeldingReplay(vedtaksperiode: Vedtaksperiode, eventBus: EventBus) {
        val erKortPeriode = !vedtaksperiode.skalArbeidstakerBehandlesISpeil()
        val opplysningerViTrenger = if (erKortPeriode)
            opplysningerViTrenger(vedtaksperiode) + EventSubscription.Arbeidsgiverperiode
        else
            opplysningerViTrenger(vedtaksperiode)

        eventBus.inntektsmeldingReplay(trengerArbeidsgiveropplysninger(vedtaksperiode, opplysningerViTrenger))
    }
}

private fun sykmeldingsperioder(vedtaksperioder: List<Vedtaksperiode>): List<Periode> {
    return vedtaksperioder.map { it.sykmeldingsperiode }
}
