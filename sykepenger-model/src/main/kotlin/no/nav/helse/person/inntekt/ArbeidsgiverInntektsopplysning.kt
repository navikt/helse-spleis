package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.person.ArbeidsgiverInntektsopplysningVisitor
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Revurderingseventyr
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.Inntektsopplysning.Companion.valider
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

class ArbeidsgiverInntektsopplysning(
    private val orgnummer: String,
    private val inntektsopplysning: Inntektsopplysning,
    private val refusjonsopplysninger: Refusjonsopplysninger
) {
    private fun omregnetÅrsinntekt(acc: Inntekt): Inntekt {
        return acc + inntektsopplysning.omregnetÅrsinntekt()
    }

    internal fun harInntektFraAOrdningen() =
        inntektsopplysning is SkattComposite || inntektsopplysning is IkkeRapportert

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    internal fun accept(visitor: ArbeidsgiverInntektsopplysningVisitor) {
        visitor.preVisitArbeidsgiverInntektsopplysning(this, orgnummer)
        visitor.preVisitRefusjonsopplysninger(refusjonsopplysninger)
        inntektsopplysning.accept(visitor)
        refusjonsopplysninger.accept(visitor)
        visitor.postVisitRefusjonsopplysninger(refusjonsopplysninger)
        visitor.postVisitArbeidsgiverInntektsopplysning(this, orgnummer)
    }

    private fun overstyr(overstyringer: List<ArbeidsgiverInntektsopplysning>): ArbeidsgiverInntektsopplysning {
        val overstyring = overstyringer.singleOrNull { it.orgnummer == this.orgnummer } ?: return this
        return overstyring.overstyrer(this)
    }

    private fun overstyrer(overstyrt: ArbeidsgiverInntektsopplysning): ArbeidsgiverInntektsopplysning {
        return ArbeidsgiverInntektsopplysning(orgnummer = this.orgnummer, inntektsopplysning = overstyrt.inntektsopplysning.overstyres(this.inntektsopplysning), refusjonsopplysninger = overstyrt.refusjonsopplysninger.merge(this.refusjonsopplysninger))
    }

    private fun subsummer(subsumsjonObserver: SubsumsjonObserver, opptjening: Opptjening?) {
        inntektsopplysning.subsumerSykepengegrunnlag(subsumsjonObserver, orgnummer, opptjening?.startdatoFor(orgnummer))
    }

    private fun subsummerArbeidsforhold(forklaring: String, oppfylt: Boolean, subsumsjonObserver: SubsumsjonObserver) {
        inntektsopplysning.subsumerArbeidsforhold(subsumsjonObserver, orgnummer, forklaring, oppfylt)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArbeidsgiverInntektsopplysning) return false
        if (orgnummer != other.orgnummer) return false
        if (inntektsopplysning != other.inntektsopplysning) return false
        if (refusjonsopplysninger != other.refusjonsopplysninger) return false
        return true
    }

    override fun hashCode(): Int {
        var result = orgnummer.hashCode()
        result = 31 * result + inntektsopplysning.hashCode()
        result = 31 * result + refusjonsopplysninger.hashCode()
        return result
    }

    internal fun ikkeGhost(): Boolean {
        return inntektsopplysning is Inntektsmelding
    }

    internal companion object {
        internal fun List<ArbeidsgiverInntektsopplysning>.deaktiver(deaktiverte: List<ArbeidsgiverInntektsopplysning>, orgnummer: String, forklaring: String, subsumsjonObserver: SubsumsjonObserver) =
            this.fjernInntekt(deaktiverte, orgnummer, forklaring, true, subsumsjonObserver)

        internal fun List<ArbeidsgiverInntektsopplysning>.aktiver(aktiverte: List<ArbeidsgiverInntektsopplysning>, orgnummer: String, forklaring: String, subsumsjonObserver: SubsumsjonObserver) =
            this.fjernInntekt(aktiverte, orgnummer, forklaring, false, subsumsjonObserver)

        // flytter inntekt for *orgnummer* fra *this* til *deaktiverte*
        // aktive.deaktiver(deaktiverte, orgnummer) er direkte motsetning til deaktiverte.deaktiver(aktive, orgnummer)
        private fun List<ArbeidsgiverInntektsopplysning>.fjernInntekt(deaktiverte: List<ArbeidsgiverInntektsopplysning>, orgnummer: String, forklaring: String, oppfylt: Boolean, subsumsjonObserver: SubsumsjonObserver): Pair<List<ArbeidsgiverInntektsopplysning>, List<ArbeidsgiverInntektsopplysning>> {
            val fjernet = checkNotNull(this.singleOrNull { it.orgnummer == orgnummer }) {
                "Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til"
            }
            val aktive = this.filterNot { it.orgnummer == orgnummer }
            fjernet.subsummerArbeidsforhold(forklaring, oppfylt, subsumsjonObserver)
            return aktive to (deaktiverte + fjernet)
        }

        // overskriver eksisterende verdier i *this* med verdier fra *other*,
        // og ignorerer ting i *other* som ikke finnes i *this*
        internal fun List<ArbeidsgiverInntektsopplysning>.overstyrInntekter(opptjening: Opptjening?, other: List<ArbeidsgiverInntektsopplysning>, subsumsjonObserver: SubsumsjonObserver) = this
            .map { inntekt -> inntekt.overstyr(other) }
            .also { it.subsummer(subsumsjonObserver, opptjening) }
        internal fun List<ArbeidsgiverInntektsopplysning>.erOverstyrt() = any { it.inntektsopplysning is Saksbehandler }

        internal fun List<ArbeidsgiverInntektsopplysning>.refusjonsopplysninger(organisasjonsnummer: String) =
            singleOrNull{it.gjelder(organisasjonsnummer)}?.refusjonsopplysninger ?: Refusjonsopplysninger()

        internal fun List<ArbeidsgiverInntektsopplysning>.inntekt(organisasjonsnummer: String) =
            firstOrNull { it.orgnummer == organisasjonsnummer }?.inntektsopplysning?.omregnetÅrsinntekt()

        internal fun List<ArbeidsgiverInntektsopplysning>.subsummer(subsumsjonObserver: SubsumsjonObserver, opptjening: Opptjening? = null) {
            subsumsjonObserver.`§ 8-30 ledd 1`(omregnetÅrsinntektPerArbeidsgiver(), omregnetÅrsinntekt())
            forEach { it.subsummer(subsumsjonObserver, opptjening) }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.build(builder: VedtakFattetBuilder) {
            builder.omregnetÅrsinntektPerArbeidsgiver(omregnetÅrsinntektPerArbeidsgiver())
        }

        private fun List<ArbeidsgiverInntektsopplysning>.omregnetÅrsinntektPerArbeidsgiver() =
            associate { it.orgnummer to it.inntektsopplysning.omregnetÅrsinntekt() }

        internal fun List<ArbeidsgiverInntektsopplysning>.valider(aktivitetslogg: IAktivitetslogg) {
            map { it.inntektsopplysning }.valider(aktivitetslogg)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.harInntekt(organisasjonsnummer: String) =
            singleOrNull { it.orgnummer == organisasjonsnummer } != null

        internal fun List<ArbeidsgiverInntektsopplysning>.omregnetÅrsinntekt() =
            fold(INGEN) { acc, item -> item.omregnetÅrsinntekt(acc)}

        internal fun List<ArbeidsgiverInntektsopplysning>.medInntekt(organisasjonsnummer: String, `6G`: Inntekt, skjæringstidspunkt: LocalDate, dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Økonomi? {
            return singleOrNull { it.orgnummer == organisasjonsnummer }?.let { arbeidsgiverInntektsopplysning ->
                val inntekt = arbeidsgiverInntektsopplysning.inntektsopplysning.omregnetÅrsinntekt()
                val refusjonsbeløp = arbeidsgiverInntektsopplysning.refusjonsopplysninger.refusjonsbeløpOrNull(dato) ?: inntekt
                økonomi.inntekt(
                    aktuellDagsinntekt = inntekt,
                    dekningsgrunnlag = inntekt.dekningsgrunnlag(dato, regler, subsumsjonObserver),
                    skjæringstidspunkt = skjæringstidspunkt,
                    `6G` = `6G`,
                    arbeidsgiverperiode = arbeidsgiverperiode,
                    refusjonsbeløp = refusjonsbeløp
                )
            }
        }

        private fun List<ArbeidsgiverInntektsopplysning>.arbeidsgiverInntektsopplysning(organisasjonsnummer: String) = checkNotNull(singleOrNull { it.orgnummer == organisasjonsnummer }) {
            "Fant ikke arbeidsgiver $organisasjonsnummer i sykepengegrunnlaget. Arbeidsgiveren må være i sykepengegrunnlaget for å legge til utbetalingsopplysninger. Arbeidsgiverne i sykepengegrunlaget er ${map { it.orgnummer }}"
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.medUtbetalingsopplysninger(organisasjonsnummer: String, `6G`: Inntekt, skjæringstidspunkt: LocalDate, dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver, manglerRefusjonsopplysning: ManglerRefusjonsopplysning): Økonomi {
            val arbeidsgiverInntektsopplysning = arbeidsgiverInntektsopplysning(organisasjonsnummer)
            val inntekt = arbeidsgiverInntektsopplysning.inntektsopplysning.omregnetÅrsinntekt()
            val refusjonsbeløp = arbeidsgiverInntektsopplysning.refusjonsopplysninger.refusjonsbeløp(
                skjæringstidspunkt = skjæringstidspunkt,
                dag = dato,
                manglerRefusjonsopplysning = manglerRefusjonsopplysning
            )
            return økonomi.inntekt(
                aktuellDagsinntekt = inntekt,
                dekningsgrunnlag = inntekt.dekningsgrunnlag(dato, regler, subsumsjonObserver),
                skjæringstidspunkt = skjæringstidspunkt,
                `6G` = `6G`,
                arbeidsgiverperiode = arbeidsgiverperiode,
                refusjonsbeløp = refusjonsbeløp
            )
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.inneholderAlleArbeidsgivereI(other: List<ArbeidsgiverInntektsopplysning>) =
            this.map { it.orgnummer }.containsAll(other.map { it.orgnummer })

        internal fun List<ArbeidsgiverInntektsopplysning>.finnEventyr(
            skjæringstidspunkt: LocalDate,
            other: List<ArbeidsgiverInntektsopplysning>
        ): Revurderingseventyr {
            val endringsDatoer = this.mapNotNull { ny ->
                val gammel = other.singleOrNull { it.orgnummer == ny.orgnummer }
                when {
                    (gammel == null || ny.inntektsopplysning != gammel.inntektsopplysning) -> skjæringstidspunkt
                    else -> ny.refusjonsopplysninger.finnFørsteDatoForEndring(gammel.refusjonsopplysninger)
                }
            }
            return Revurderingseventyr.arbeidsgiveropplysninger(skjæringstidspunkt, endringsDatoer.minOrNull() ?: skjæringstidspunkt)
        }
    }
}
