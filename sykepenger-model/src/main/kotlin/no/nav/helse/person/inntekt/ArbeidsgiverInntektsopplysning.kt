package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.memento.ArbeidsgiverInntektsopplysningMemento
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.GodkjenningsbehovBuilder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.inntekt.Inntektsopplysning.Companion.markerFlereArbeidsgivere
import no.nav.helse.person.inntekt.Inntektsopplysning.Companion.validerSkjønnsmessigAltEllerIntet
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

class ArbeidsgiverInntektsopplysning(
    private val orgnummer: String,
    private val gjelder: Periode,
    private val inntektsopplysning: Inntektsopplysning,
    private val refusjonsopplysninger: Refusjonsopplysninger
) {
    private fun fastsattÅrsinntekt(acc: Inntekt, skjæringstidspunkt: LocalDate): Inntekt {
        return acc + beregningsgrunnlag(skjæringstidspunkt)
    }
    private fun omregnetÅrsinntekt(acc: Inntekt, skjæringstidspunkt: LocalDate): Inntekt {
        return acc + omregnetÅrsinntekt(skjæringstidspunkt)
    }

    private fun fastsattÅrsinntekt(dagen: LocalDate): Inntekt {
        if (dagen > gjelder.endInclusive) return INGEN
        return inntektsopplysning.fastsattÅrsinntekt()
    }

    private fun beregningsgrunnlag(skjæringstidspunkt: LocalDate): Inntekt {
        if (this.gjelder.start > skjæringstidspunkt) return INGEN
        return inntektsopplysning.fastsattÅrsinntekt()
    }

    private fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate): Inntekt {
        if (this.gjelder.start > skjæringstidspunkt) return INGEN
        return inntektsopplysning.omregnetÅrsinntekt().fastsattÅrsinntekt()
    }

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    internal fun accept(visitor: ArbeidsgiverInntektsopplysningVisitor) {
        visitor.preVisitArbeidsgiverInntektsopplysning(this, orgnummer, gjelder)
        inntektsopplysning.accept(visitor)
        refusjonsopplysninger.accept(visitor)
        visitor.postVisitArbeidsgiverInntektsopplysning(this, orgnummer, gjelder)
    }

    private fun overstyr(overstyringer: List<ArbeidsgiverInntektsopplysning>): ArbeidsgiverInntektsopplysning {
        val overstyring = overstyringer.singleOrNull { it.orgnummer == this.orgnummer } ?: return this
        return overstyring.overstyrer(this)
    }

    private fun overstyrer(gammel: ArbeidsgiverInntektsopplysning): ArbeidsgiverInntektsopplysning {
        return ArbeidsgiverInntektsopplysning(orgnummer = this.orgnummer, gjelder = this.gjelder, inntektsopplysning = gammel.inntektsopplysning.overstyresAv(this.inntektsopplysning), refusjonsopplysninger = gammel.refusjonsopplysninger.merge(this.refusjonsopplysninger))
    }

    private fun rullTilbake() = ArbeidsgiverInntektsopplysning(this.orgnummer, gjelder = this.gjelder, this.inntektsopplysning.omregnetÅrsinntekt(), refusjonsopplysninger)

    private fun subsummer(subsumsjonObserver: SubsumsjonObserver, opptjening: Opptjening?) {
        inntektsopplysning.subsumerSykepengegrunnlag(subsumsjonObserver, orgnummer, opptjening?.startdatoFor(orgnummer))
    }

    private fun deaktiver(forklaring: String, oppfylt: Boolean, subsumsjonObserver: SubsumsjonObserver): ArbeidsgiverInntektsopplysning {
        inntektsopplysning.subsumerArbeidsforhold(subsumsjonObserver, orgnummer, forklaring, oppfylt)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArbeidsgiverInntektsopplysning) return false
        if (orgnummer != other.orgnummer) return false
        if (inntektsopplysning != other.inntektsopplysning) return false
        if (refusjonsopplysninger != other.refusjonsopplysninger) return false
        if (this.gjelder != other.gjelder) return false
        return true
    }

    override fun hashCode(): Int {
        var result = orgnummer.hashCode()
        result = 31 * result + inntektsopplysning.hashCode()
        result = 31 * result + refusjonsopplysninger.hashCode()
        result = 31 * result + gjelder.hashCode()
        return result
    }

    internal fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        inntektsmelding: Inntektsmelding
    ) {
        inntektsopplysning.arbeidsgiveropplysningerKorrigert(person, inntektsmelding)
    }

    internal fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        saksbehandleroverstyring: OverstyrArbeidsgiveropplysninger
    ) {
        inntektsopplysning.arbeidsgiveropplysningerKorrigert(person, orgnummer, saksbehandleroverstyring)
    }

    internal fun ghosttidslinje(organisasjonsnummer: String, sisteDag: LocalDate): Sykdomstidslinje? {
        if (organisasjonsnummer != this.orgnummer) return null
        if (sisteDag < gjelder.start) return Sykdomstidslinje()
        return Sykdomstidslinje.ghostdager(gjelder.start til sisteDag)
    }

    internal fun gjenoppliv(forrigeSkjæringstidspunkt: LocalDate, nyttSkjæringstidspunkt: LocalDate): ArbeidsgiverInntektsopplysning {
        if (forrigeSkjæringstidspunkt == nyttSkjæringstidspunkt) return this // Intet nytt fra vestfronten
        if (nyttSkjæringstidspunkt > gjelder.endInclusive) return this // Unngår å havne i problemer med ugyldige perioder
        return ArbeidsgiverInntektsopplysning(orgnummer, nyttSkjæringstidspunkt til gjelder.endInclusive, inntektsopplysning, refusjonsopplysninger.gjenoppliv(nyttSkjæringstidspunkt))
    }

    internal companion object {
        internal fun List<ArbeidsgiverInntektsopplysning>.validerSkjønnsmessigAltEllerIntet() = map { it.inntektsopplysning }.validerSkjønnsmessigAltEllerIntet()

        internal fun List<ArbeidsgiverInntektsopplysning>.finn(orgnummer: String) = firstOrNull { it.gjelder(orgnummer) }

        internal fun List<ArbeidsgiverInntektsopplysning>.deaktiver(deaktiverte: List<ArbeidsgiverInntektsopplysning>, orgnummer: String, forklaring: String, subsumsjonObserver: SubsumsjonObserver) =
            this.fjernInntekt(deaktiverte, orgnummer, forklaring, true, subsumsjonObserver)

        internal fun List<ArbeidsgiverInntektsopplysning>.aktiver(aktiveres: List<ArbeidsgiverInntektsopplysning>, orgnummer: String, forklaring: String, subsumsjonObserver: SubsumsjonObserver): Pair<List<ArbeidsgiverInntektsopplysning>, List<ArbeidsgiverInntektsopplysning>> {
            val (deaktiverte, aktiverte) = this.fjernInntekt(aktiveres, orgnummer, forklaring, false, subsumsjonObserver)
            // Om inntektene i sykepengegrunnlaget var skjønnsmessig fastsatt før aktivering sikrer vi at alle "rulles tilbake" slik at vi ikke lager et sykepengegrunnlag med mix av SkjønnsmessigFastsatt & andre inntektstyper.
            return deaktiverte to aktiverte.map { ArbeidsgiverInntektsopplysning(it.orgnummer, it.gjelder, it.inntektsopplysning.omregnetÅrsinntekt(), it.refusjonsopplysninger) }
        }

        // flytter inntekt for *orgnummer* fra *this* til *deaktiverte*
        // aktive.deaktiver(deaktiverte, orgnummer) er direkte motsetning til deaktiverte.deaktiver(aktive, orgnummer)
        private fun List<ArbeidsgiverInntektsopplysning>.fjernInntekt(deaktiverte: List<ArbeidsgiverInntektsopplysning>, orgnummer: String, forklaring: String, oppfylt: Boolean, subsumsjonObserver: SubsumsjonObserver): Pair<List<ArbeidsgiverInntektsopplysning>, List<ArbeidsgiverInntektsopplysning>> {
            val inntektsopplysning = checkNotNull(this.singleOrNull { it.orgnummer == orgnummer }) {
                "Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til"
            }.deaktiver(forklaring, oppfylt, subsumsjonObserver)
            val aktive = this.filterNot { it === inntektsopplysning }
            return aktive to (deaktiverte + listOfNotNull(inntektsopplysning))
        }

        // overskriver eksisterende verdier i *this* med verdier fra *other*,
        // og ignorerer ting i *other* som ikke finnes i *this*
        internal fun List<ArbeidsgiverInntektsopplysning>.overstyrInntekter(opptjening: Opptjening?, other: List<ArbeidsgiverInntektsopplysning>, subsumsjonObserver: SubsumsjonObserver): List<ArbeidsgiverInntektsopplysning> {
            val omregnetÅrsinntekt = map { it.inntektsopplysning }
            val endringen = this
                .map { inntekt -> inntekt.overstyr(other) }
                .also { it.subsummer(subsumsjonObserver, opptjening, this) }
            val omregnetÅrsinntektEtterpå = endringen.map { it.inntektsopplysning }
            if (Inntektsopplysning.erOmregnetÅrsinntektEndret(omregnetÅrsinntekt, omregnetÅrsinntektEtterpå)) return endringen.map { it.rullTilbake() }
            return endringen
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.sjekkForNyArbeidsgiver(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening, orgnummer: String) {
            val arbeidsforholdAktivePåSkjæringstidspunktet = singleOrNull { opptjening.ansattVedSkjæringstidspunkt(it.orgnummer) } ?: return
            if (arbeidsforholdAktivePåSkjæringstidspunktet.orgnummer == orgnummer) return
            aktivitetslogg.varsel(Varselkode.RV_VV_8)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg: IAktivitetslogg, opptjening: Opptjening) {
            if (none { opptjening.startdatoFor(it.orgnummer) == null }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_1)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
            return map { it.inntektsopplysning }.markerFlereArbeidsgivere(aktivitetslogg)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.refusjonsopplysninger(organisasjonsnummer: String) =
            singleOrNull{it.gjelder(organisasjonsnummer)}?.refusjonsopplysninger ?: Refusjonsopplysninger()

        internal fun List<ArbeidsgiverInntektsopplysning>.inntekt(organisasjonsnummer: String) =
            firstOrNull { it.orgnummer == organisasjonsnummer }?.inntektsopplysning?.fastsattÅrsinntekt()

        internal fun List<ArbeidsgiverInntektsopplysning>.inntektsdata(skjæringstidspunkt: LocalDate, organisasjonsnummer: String) =
            firstOrNull { it.gjelder(organisasjonsnummer) }?.inntektsopplysning?.inntektsdata(skjæringstidspunkt)


        private fun List<ArbeidsgiverInntektsopplysning>.finnEndredeInntektsopplysninger(forrige: List<ArbeidsgiverInntektsopplysning>): List<ArbeidsgiverInntektsopplysning> {
            val forrigeInntektsopplysninger = forrige.map { it.inntektsopplysning }
            return filterNot { it.inntektsopplysning in forrigeInntektsopplysninger }
        }
        internal fun List<ArbeidsgiverInntektsopplysning>.subsummer(subsumsjonObserver: SubsumsjonObserver, opptjening: Opptjening? = null, forrige: List<ArbeidsgiverInntektsopplysning>) {
            val endredeInntektsopplysninger = finnEndredeInntektsopplysninger(forrige)
            if (endredeInntektsopplysninger.isEmpty()) return
            endredeInntektsopplysninger.forEach { it.subsummer(subsumsjonObserver, opptjening) }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.build(builder: VedtakFattetBuilder.FastsattISpleisBuilder) {
            forEach { arbeidsgiver ->
                builder.arbeidsgiver(
                    arbeidsgiver = arbeidsgiver.orgnummer,
                    omregnetÅrsinntekt = arbeidsgiver.inntektsopplysning.omregnetÅrsinntekt().fastsattÅrsinntekt(),
                    skjønnsfastsatt = if (arbeidsgiver.inntektsopplysning is SkjønnsmessigFastsatt) arbeidsgiver.inntektsopplysning.fastsattÅrsinntekt() else null
                )
            }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.harInntekt(organisasjonsnummer: String) =
            singleOrNull { it.orgnummer == organisasjonsnummer } != null

        internal fun List<ArbeidsgiverInntektsopplysning>.ingenRefusjonsopplysninger(organisasjonsnummer: String): Boolean {
            val refusjonsopplysninger = singleOrNull { it.orgnummer == organisasjonsnummer }?.refusjonsopplysninger ?: Refusjonsopplysninger()
            return refusjonsopplysninger.erTom
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.fastsattÅrsinntekt(skjæringstidspunkt: LocalDate) =
            fold(INGEN) { acc, item -> item.fastsattÅrsinntekt(acc, skjæringstidspunkt)}

        internal fun List<ArbeidsgiverInntektsopplysning>.totalOmregnetÅrsinntekt(skjæringstidspunkt: LocalDate) =
            fold(INGEN) { acc, item -> item.omregnetÅrsinntekt(acc, skjæringstidspunkt) }

        internal fun List<ArbeidsgiverInntektsopplysning>.medInntekt(organisasjonsnummer: String, `6G`: Inntekt, skjæringstidspunkt: LocalDate, dato: LocalDate, økonomi: Økonomi, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Økonomi? {
            val arbeidsgiverInntektsopplysning = singleOrNull { it.orgnummer == organisasjonsnummer } ?: return null
            val inntekt = arbeidsgiverInntektsopplysning.fastsattÅrsinntekt(dato)
            val beregningsgrunnlag = arbeidsgiverInntektsopplysning.beregningsgrunnlag(skjæringstidspunkt)
            val refusjonsbeløp = arbeidsgiverInntektsopplysning.refusjonsopplysninger.refusjonsbeløpOrNull(dato) ?: inntekt
            return økonomi.inntekt(
                aktuellDagsinntekt = inntekt,
                beregningsgrunnlag = beregningsgrunnlag,
                dekningsgrunnlag = inntekt.dekningsgrunnlag(dato, regler, subsumsjonObserver),
                `6G` = `6G`,
                refusjonsbeløp = refusjonsbeløp
            )
        }

        private fun List<ArbeidsgiverInntektsopplysning>.arbeidsgiverInntektsopplysning(organisasjonsnummer: String, dato: LocalDate) = checkNotNull(singleOrNull { it.orgnummer == organisasjonsnummer }) {
            """Arbeidsgiver $organisasjonsnummer mangler i sykepengegrunnlaget ved utbetaling av $dato. 
                Arbeidsgiveren må være i sykepengegrunnlaget for å legge til utbetalingsopplysninger. 
                Arbeidsgiverne i sykepengegrunlaget er ${map { it.orgnummer }}"""
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.medUtbetalingsopplysninger(organisasjonsnummer: String, `6G`: Inntekt, skjæringstidspunkt: LocalDate, dato: LocalDate, økonomi: Økonomi, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Økonomi {
            val arbeidsgiverInntektsopplysning = arbeidsgiverInntektsopplysning(organisasjonsnummer, dato)
            val inntekt = arbeidsgiverInntektsopplysning.fastsattÅrsinntekt(dato)
            val beregningsgrunnlag = arbeidsgiverInntektsopplysning.beregningsgrunnlag(skjæringstidspunkt)
            val refusjonsbeløp = checkNotNull(arbeidsgiverInntektsopplysning.refusjonsopplysninger.refusjonsbeløpOrNull(dato)) {
                "Har ingen refusjonsopplysninger på vilkårsgrunnlag med skjæringstidspunkt $skjæringstidspunkt for utbetalingsdag $dato"
            }
            return økonomi.inntekt(
                aktuellDagsinntekt = inntekt,
                beregningsgrunnlag = beregningsgrunnlag,
                dekningsgrunnlag = inntekt.dekningsgrunnlag(dato, regler, subsumsjonObserver),
                `6G` = `6G`,
                refusjonsbeløp = refusjonsbeløp
            )
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.finnEndringsdato(
            skjæringstidspunkt: LocalDate,
            other: List<ArbeidsgiverInntektsopplysning>
        ): LocalDate {
            val endringsDatoer = this.mapNotNull { ny ->
                val gammel = other.singleOrNull { it.orgnummer == ny.orgnummer }
                when {
                    (gammel == null || ny.inntektsopplysning != gammel.inntektsopplysning) -> skjæringstidspunkt
                    else -> ny.refusjonsopplysninger.finnFørsteDatoForEndring(gammel.refusjonsopplysninger)
                }
            }
            return endringsDatoer.minOrNull() ?: skjæringstidspunkt
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.lagreTidsnæreInntekter(
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            hendelse: IAktivitetslogg,
            oppholdsperiodeMellom: Periode?
        ) {
            this.forEach {
                it.inntektsopplysning.lagreTidsnærInntekt(
                    skjæringstidspunkt,
                    arbeidsgiver,
                    hendelse,
                    oppholdsperiodeMellom,
                    it.refusjonsopplysninger,
                    it.orgnummer
                )
            }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.omregnedeÅrsinntekter(builder: GodkjenningsbehovBuilder) {
            builder.orgnummereMedRelevanteArbeidsforhold(this.map { it.orgnummer }.toSet())
            this.forEach{it.inntektsopplysning.omregnetÅrsinntekt(builder, it.orgnummer)}
        }
    }

    internal fun memento() = ArbeidsgiverInntektsopplysningMemento(
        orgnummer = this.orgnummer,
        gjelder = this.gjelder.memento(),
        inntektsopplysning = this.inntektsopplysning.memento(),
        refusjonsopplysninger = this.refusjonsopplysninger.memento()
    )
}
