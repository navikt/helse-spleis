package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.erHelg
import no.nav.helse.erRettFør
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Inntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.Periode.Companion.periodeRettFør
import no.nav.helse.nesteDag
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.ukedager
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.slf4j.LoggerFactory

internal class DagerFraInntektsmelding(
    private val arbeidsgiverperioder: List<Periode>,
    private val førsteFraværsdag: LocalDate?,
    mottatt: LocalDateTime,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: BegrunnelseForReduksjonEllerIkkeUtbetalt?,
    private val harFlereInntektsmeldinger: Boolean,
    private val opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse>,
    val hendelse: Hendelse
) {
    // TODO: kilden må være av en type som arver SykdomshistorikkHendelse; altså BitAvInntektsmelding
    // krever nok at vi json-migrerer alle "Inntektsmelding" til "BitAvInntektsmelding" først
    internal val kilde = Hendelseskilde("Inntektsmelding", hendelse.metadata.meldingsreferanseId, mottatt)
    private val dokumentsporing = Dokumentsporing.inntektsmeldingDager(hendelse.metadata.meldingsreferanseId)
    private val arbeidsgiverperiode = arbeidsgiverperioder.periode()
    private val overlappsperiode = when {
        // første fraværsdag er oppgitt etter arbeidsgiverperioden
        arbeidsgiverperiode == null || førsteFraværsdag != null && førsteFraværsdag > arbeidsgiverperiode.endInclusive.nesteDag -> førsteFraværsdag?.somPeriode()
        // kant-i-kant
        førsteFraværsdag?.forrigeDag == arbeidsgiverperiode.endInclusive -> arbeidsgiverperiode.oppdaterTom(arbeidsgiverperiode.endInclusive.nesteDag)
        else -> arbeidsgiverperiode
    }

    private val dagerNavOvertarAnsvar: List<Periode> = dagerNavOvertarAnsvar()
    private val sykdomstidslinje = tidslinjeForArbeidsgiverperioden()
    private val opprinneligPeriode = sykdomstidslinje.periode()

    private val arbeidsdager = mutableSetOf<LocalDate>()
    private val _gjenståendeDager = opprinneligPeriode?.toMutableSet() ?: mutableSetOf()
    val gjenståendeDager get() = _gjenståendeDager.toSet()

    private val håndterteDager = mutableSetOf<LocalDate>()
    private var harValidert: Periode? = null

    private fun dagerNavOvertarAnsvar(): List<Periode> {
        if (begrunnelseForReduksjonEllerIkkeUtbetalt == null) return emptyList()
        return (arbeidsgiverperioder + listOfNotNull(førsteFraværsdag?.somPeriode())).flatten().grupperSammenhengendePerioder()
    }

    private fun tidslinjeForArbeidsgiverperioden(): Sykdomstidslinje {
        if (arbeidsgiverperiodenKanIkkeTolkes()) return Sykdomstidslinje()
        val arbeidsdager = arbeidsgiverperiode?.let { Sykdomstidslinje.arbeidsdager(arbeidsgiverperiode, kilde) } ?: Sykdomstidslinje()
        val friskHelg = friskHelgMellomFørsteFraværsdagOgArbeidsgiverperioden()
        return arbeidsdager.merge(lagArbeidsgivertidslinje(), Dag.replace).merge(friskHelg)
    }

    private fun arbeidsgiverperiodenKanIkkeTolkes(): Boolean {
        if (førsteFraværsdag == null) return false
        val periodeMellom = arbeidsgiverperiode?.periodeMellom(førsteFraværsdag)
        return periodeMellom != null && periodeMellom.count() >= MAKS_ANTALL_DAGER_MELLOM_FØRSTE_FRAVÆRSDAG_OG_AGP_FOR_HÅNDTERING_AV_DAGER
    }

    private fun friskHelgMellomFørsteFraværsdagOgArbeidsgiverperioden(): Sykdomstidslinje {
        if (førsteFraværsdag == null) return Sykdomstidslinje()
        if (arbeidsgiverperiode?.erRettFør(førsteFraværsdag) != true) return Sykdomstidslinje()
        val helgen = arbeidsgiverperiode.periodeMellom(førsteFraværsdag) ?: return Sykdomstidslinje()
        return Sykdomstidslinje.arbeidsdager(helgen, kilde)
    }

    private fun lagArbeidsgivertidslinje(): Sykdomstidslinje {
        return arbeidsgiverperioder.map(::arbeidsgiverdager).merge()
    }

    private fun arbeidsgiverdager(periode: Periode) = Sykdomstidslinje.arbeidsgiverdager(periode.start, periode.endInclusive, 100.prosent, kilde)

    internal fun alleredeHåndtert(behandlinger: Behandlinger) = behandlinger.dokumentHåndtert(dokumentsporing)

    internal fun vurdertTilOgMed(dato: LocalDate) {
        _gjenståendeDager.removeAll { gjenstående -> gjenstående <= dato }
    }

    internal fun leggTilArbeidsdagerFør(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return
        if (opprinneligPeriode == null) return
        val oppdatertPeriode = opprinneligPeriode.oppdaterFom(dato)
        val arbeidsdagerFør = oppdatertPeriode.uten(opprinneligPeriode).flatten()
        if (!arbeidsdager.addAll(arbeidsdagerFør)) return
        _gjenståendeDager.addAll(arbeidsdagerFør)
    }

    private fun overlappendeDager(periode: Periode) = periode.intersect(_gjenståendeDager)

    private fun periodeRettFør(periode: Periode) = _gjenståendeDager.periodeRettFør(periode.start)

    private fun skalHåndtere(periode: Periode): Boolean {
        val overlapperMedVedtaksperiode = overlappendeDager(periode).isNotEmpty()
        val periodeRettFør = periodeRettFør(periode) != null
        return overlapperMedVedtaksperiode || periodeRettFør
    }

    internal fun skalHåndteresAv(periode: Periode): Boolean {
        val vedtaksperiodeRettFør = _gjenståendeDager.isNotEmpty() && periode.endInclusive.erRettFør(_gjenståendeDager.first())
        return skalHåndtere(periode) || vedtaksperiodeRettFør || tomSykdomstidslinjeMenSkalValidere(periode) || egenmeldingerIForkantAvPerioden(periode)
    }

    private fun tomSykdomstidslinjeMenSkalValidere(periode: Periode) =
        (opprinneligPeriode == null && skalValideresAv(periode)).also {
            if (it) {
                harValidert = harValidert?.oppdaterFom(periode) ?: periode
            }
        }

    // om vedtaksperioden ikke direkte overlapper med gjenståendeDager, men gjenståendeDager er ikke tom, og vedtaksperioden overlapper
    // med første fraværsdag, betyr det at inntektsmeldingen informerer om egenmeldinger vi ikke har søknad for.
    // Om vi hadde en vedtaksperiode, ville dagene bli 'spist opp' via vurdertTilOgMed()
    private fun egenmeldingerIForkantAvPerioden(periode: Periode) =
        (overlappsperiode != null && overlappsperiode.overlapperMed(periode) && _gjenståendeDager.isNotEmpty())

    internal fun skalHåndteresAvRevurdering(periode: Periode, sammenhengende: Periode, arbeidsgiverperiode: List<Periode>?): Boolean {
        if (skalHåndtere(periode)) return true
        // vedtaksperiodene før dagene skal bare håndtere dagene om de nye opplyste dagene er nærmere enn 10 dager fra forrige AGP-beregning
        if (opprinneligPeriode == null || arbeidsgiverperiode == null) return false
        val periodeMellomForrigeAgpOgNyAgp = arbeidsgiverperiode.periode()?.periodeMellom(opprinneligPeriode.start) ?: return false
        return periodeMellomForrigeAgpOgNyAgp.count() <= MAKS_ANTALL_DAGER_MELLOM_TIDLIGERE_OG_NY_AGP_FOR_HÅNDTERING_AV_DAGER && sammenhengende.contains(periodeMellomForrigeAgpOgNyAgp)
    }

    internal fun harBlittHåndtertAv(periode: Periode) = håndterteDager.any { it in periode }

    internal fun bitAvInntektsmelding(aktivitetslogg: IAktivitetslogg, vedtaksperiode: Periode): BitAvArbeidsgiverperiode? {
        val dagerNavOvertarAnsvar = dagerNavOvertarAnsvar
            .filter { it.overlapperMed(vedtaksperiode) }
            .map { it.subset(vedtaksperiode) }
        val sykdomstidslinje = håndterDager(aktivitetslogg, vedtaksperiode)
        if (sykdomstidslinje == null && dagerNavOvertarAnsvar.isEmpty()) return null
        return BitAvArbeidsgiverperiode(hendelse.metadata, sykdomstidslinje ?: Sykdomstidslinje(), dagerNavOvertarAnsvar)
    }

    internal fun tomBitAvInntektsmelding(aktivitetslogg: IAktivitetslogg, vedtaksperiode: Periode): BitAvArbeidsgiverperiode {
        håndterDager(aktivitetslogg, vedtaksperiode)
        return BitAvArbeidsgiverperiode(hendelse.metadata, Sykdomstidslinje(), emptyList())
    }

    private fun håndterDager(aktivitetslogg: IAktivitetslogg, vedtaksperiode: Periode): Sykdomstidslinje? {
        val periode = håndterDagerFør(vedtaksperiode) ?: return null
        if (periode.start != vedtaksperiode.start) aktivitetslogg.info("Perioden ble strukket tilbake fra ${vedtaksperiode.start} til ${periode.start} (${ChronoUnit.DAYS.between(periode.start, vedtaksperiode.start)} dager)")
        val sykdomstidslinje = samletSykdomstidslinje(periode)

        håndterteDager.addAll(periode.toList())
        _gjenståendeDager.removeAll(periode)
        return sykdomstidslinje
    }

    private fun samletSykdomstidslinje(periode: Periode) =
        (arbeidsdagertidslinje() + sykdomstidslinje).subset(periode)

    private fun arbeidsdagertidslinje() =
        arbeidsdager.map { Sykdomstidslinje.arbeidsdager(it, it, kilde) }.merge()

    private fun håndterDagerFør(vedtaksperiode: Periode): Periode? {
        leggTilArbeidsdagerFør(vedtaksperiode.start)
        val gjenståendePeriode = _gjenståendeDager.omsluttendePeriode ?: return null
        val periode = vedtaksperiode.oppdaterFom(gjenståendePeriode)
        return periode
    }

    private fun skalValideresAv(periode: Periode) = overlappsperiode?.overlapperMed(periode) == true || ikkeUtbetaltAGPOgAGPOverlapper(periode)
    private fun ikkeUtbetaltAGPOgAGPOverlapper(periode: Periode): Boolean {
        if (begrunnelseForReduksjonEllerIkkeUtbetalt == null) return false
        if (arbeidsgiverperiode == null) return false
        if (førsteFraværsdag != null && førsteFraværsdag > arbeidsgiverperiode.endInclusive.nesteDag) return false
        return arbeidsgiverperiode.overlapperMed(periode)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, gammelAgp: List<Periode>? = null, vedtaksperiodeId: UUID) {
        if (opphørAvNaturalytelser.isNotEmpty()) {
            if (opphørAvNaturalytelser.any { it.fom != førsteFraværsdag }) {
                sikkerLogg.info(
                    "Vi har mottatt en inntektsmelding med naturalytelser som opphører fra en annen dato enn første fraværsdag:  {}, {}",
                    keyValue("vedtaksperiode", vedtaksperiodeId),
                    keyValue("naturalytelser", opphørAvNaturalytelser)
                )
            }
            aktivitetslogg.funksjonellFeil(Varselkode.RV_IM_7)
        }
        if (harFlereInntektsmeldinger) aktivitetslogg.varsel(Varselkode.RV_IM_22)
        begrunnelseForReduksjonEllerIkkeUtbetalt?.valider(aktivitetslogg, hulleteArbeidsgiverperiode())
        validerOverstigerMaksimaltTillatAvstandMellomTidligereAGP(aktivitetslogg, gammelAgp)
    }

    private fun hulleteArbeidsgiverperiode(): Boolean {
        return arbeidsgiverperioder.size > 1 && (førsteFraværsdag == null || førsteFraværsdag in arbeidsgiverperiode!!)
    }

    private fun validerArbeidsgiverperiodeVedGjenståendeDager(aktivitetslogg: IAktivitetslogg, vedtaksperiode: Periode, beregnetArbeidsgiverperiode: List<Periode>?) {
        val sisteDagAgp = beregnetArbeidsgiverperiode?.periode()?.endInclusive ?: return
        // Om det er én eller fler ukedager mellom beregnet AGP og vedtaksperioden som overlapper med dager fra inntektsmeldingen
        // tyder det på at arbeidsgiver tror det er ny arbeidsgiverperiode, men vi har beregnet at det _ikke_ er ny arbeidsgiverperiode.
        if (ukedager(sisteDagAgp, vedtaksperiode.start) > 0) aktivitetslogg.varsel(RV_IM_3)
    }

    internal fun validerArbeidsgiverperiode(aktivitetslogg: IAktivitetslogg, periode: Periode, beregnetArbeidsgiverperiode: List<Periode>?) {
        if (!skalValideresAv(periode)) return
        if (_gjenståendeDager.isNotEmpty()) return validerArbeidsgiverperiodeVedGjenståendeDager(aktivitetslogg, periode, beregnetArbeidsgiverperiode)
        if (beregnetArbeidsgiverperiode != null) validerArbeidsgiverperiode(aktivitetslogg, beregnetArbeidsgiverperiode)
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.info("Inntektsmeldingen mangler arbeidsgiverperiode. Vurder om vilkårene for sykepenger er oppfylt, og om det skal være arbeidsgiverperiode")
    }

    private fun validerArbeidsgiverperiode(aktivitetslogg: IAktivitetslogg, arbeidsgiverperiode: List<Periode>) {
        if (this.arbeidsgiverperioder.isEmpty()) return
        if (starterUtbetalingSamtidig(arbeidsgiverperiode)) return
        aktivitetslogg.varsel(RV_IM_3)
    }

    private fun starterUtbetalingSamtidig(beregnetArbeidsgiverperiode: List<Periode>): Boolean {
        val thisSiste = this.arbeidsgiverperiode?.endInclusive ?: return false
        val otherSiste = beregnetArbeidsgiverperiode.periode()?.endInclusive ?: return false
        return otherSiste == thisSiste || (thisSiste.erHelg() && otherSiste.erRettFør(thisSiste)) || (otherSiste.erHelg() && thisSiste.erRettFør(otherSiste))
    }

    private fun validerOverstigerMaksimaltTillatAvstandMellomTidligereAGP(aktivitetslogg: IAktivitetslogg, gammelAgp: List<Periode>?) {
        if (!overstigerMaksimaltTillatAvstandMellomTidligereAGP(gammelAgp)) return
        aktivitetslogg.varsel(Varselkode.RV_IM_24, "Ignorerer dager fra inntektsmelding fordi perioden mellom gammel agp og opplyst agp er mer enn 10 dager")
    }

    private fun overstigerMaksimaltTillatAvstandMellomTidligereAGP(gammelAgp: List<Periode>?): Boolean {
        if (opprinneligPeriode == null) return false
        val periodeMellom = gammelAgp?.periode()?.periodeMellom(opprinneligPeriode.start) ?: return false
        return periodeMellom.count() > MAKS_ANTALL_DAGER_MELLOM_TIDLIGERE_OG_NY_AGP_FOR_HÅNDTERING_AV_DAGER
    }

    internal fun erKorrigeringForGammel(aktivitetslogg: IAktivitetslogg, gammelAgp: List<Periode>?): Boolean {
        if (opprinneligPeriode == null) return true
        if (gammelAgp == null) return false
        if (overstigerMaksimaltTillatAvstandMellomTidligereAGP(gammelAgp)) return true
        aktivitetslogg.info("Håndterer dager fordi perioden mellom gammel agp og opplyst agp er mindre enn 10 dager")
        return false
    }

    internal fun revurderingseventyr(): Revurderingseventyr? {
        val dagene = håndterteDager.omsluttendePeriode ?: harValidert ?: return null
        return Revurderingseventyr.arbeidsgiverperiode(hendelse, dagene.start, dagene)
    }

    internal fun førsteOverlappendeVedtaksperiode(vedtaksperioder: List<Vedtaksperiode>): Vedtaksperiode? {
        return vedtaksperioder.firstOrNull { it.periode.overlapperMed(arbeidsgiverperiode ?: førsteFraværsdag!!.somPeriode()) }
    }

    internal fun inntektsmeldingIkkeHåndtert(aktivitetslogg: IAktivitetslogg, person: Person, forkastede: List<Periode>, sykmeldingsperioder: Sykmeldingsperioder) =
        InntektsmeldingIkkeHåndtert().emit(aktivitetslogg, person, forkastede, sykmeldingsperioder)

    private inner class InntektsmeldingIkkeHåndtert {
        private val meldingsreferanseId = hendelse.metadata.meldingsreferanseId
        private val organisasjonsnummer = when (val bs = hendelse.behandlingsporing) {
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> bs.organisasjonsnummer
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans,
            Behandlingsporing.Yrkesaktivitet.Selvstendig,
            Behandlingsporing.IngenYrkesaktivitet -> error("Inntektsmelding uten arbeidsgiver?!?! Det blir litt vel tøysete spør du meg")
        }
        private val perioderViTrorInntektsmeldingenPrøverÅSiNoeOm = listOfNotNull(
            førsteFraværsdag?.somPeriode(),
            opprinneligPeriode,
            overlappsperiode
        ).plus(arbeidsgiverperioder).grupperSammenhengendePerioder()

        private fun relevanteSykmeldingsperioder(sykmeldingsperioder: List<Periode>) = sykmeldingsperioder.filter { sykmeldingsperiode ->
            perioderViTrorInntektsmeldingenPrøverÅSiNoeOm.any { periodeViTrorInntektsmeldingenPrøverÅSiNoeOm ->
                (Periode.mellom(sykmeldingsperiode, periodeViTrorInntektsmeldingenPrøverÅSiNoeOm)?.count() ?: 0) < MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
            }
        }

        private fun overlapperMed(forkastedePerioder: List<Periode>) = forkastedePerioder.any { forkastetPeriode ->
            perioderViTrorInntektsmeldingenPrøverÅSiNoeOm.any { periodeViTrorInntektsmeldingenPrøverÅSiNoeOm ->
                forkastetPeriode.overlapperMed(periodeViTrorInntektsmeldingenPrøverÅSiNoeOm)
            }
        }

        private fun speilrelatert(person: Person) = person.speilrelatert(*perioderViTrorInntektsmeldingenPrøverÅSiNoeOm.toTypedArray())

        fun emit(aktivitetslogg: IAktivitetslogg, person: Person, forkastede: List<Periode>, sykmeldingsperioder: Sykmeldingsperioder) {
            val relevanteSykmeldingsperioder = relevanteSykmeldingsperioder(sykmeldingsperioder.perioder())
            val overlapperMedForkastet = overlapperMed(forkastede)
            if (relevanteSykmeldingsperioder.isNotEmpty() && !overlapperMedForkastet) {
                person.emitInntektsmeldingFørSøknadEvent(meldingsreferanseId.id, Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer))
                return aktivitetslogg.info("Inntektsmelding før søknad - er relevant for sykmeldingsperioder $relevanteSykmeldingsperioder")
            }
            aktivitetslogg.info("Inntektsmelding ikke håndtert")
            person.emitInntektsmeldingIkkeHåndtert(meldingsreferanseId, organisasjonsnummer, speilrelatert = speilrelatert(person))
        }
    }

    private companion object {
        private const val MAKS_ANTALL_DAGER_MELLOM_TIDLIGERE_OG_NY_AGP_FOR_HÅNDTERING_AV_DAGER = 10
        private const val MAKS_ANTALL_DAGER_MELLOM_FØRSTE_FRAVÆRSDAG_OG_AGP_FOR_HÅNDTERING_AV_DAGER = 20
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
