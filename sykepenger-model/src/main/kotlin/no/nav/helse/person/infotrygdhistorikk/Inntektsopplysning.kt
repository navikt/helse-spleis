package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_12
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_13
import no.nav.helse.økonomi.Inntekt

class Inntektsopplysning private constructor(
    private val orgnummer: String,
    private val sykepengerFom: LocalDate,
    private val inntekt: Inntekt,
    private val refusjonTilArbeidsgiver: Boolean,
    private val refusjonTom: LocalDate?,
    private var lagret: LocalDateTime?
) {
    constructor(
        orgnummer: String,
        sykepengerFom: LocalDate,
        inntekt: Inntekt,
        refusjonTilArbeidsgiver: Boolean,
        refusjonTom: LocalDate? = null
    ) : this(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, null)

    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): Boolean {
        if (!erRelevant(skjæringstidspunkt)) return true
        if (orgnummer.isBlank()) aktivitetslogg.funksjonellFeil(RV_IT_12)
        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkInntektsopplysning(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, lagret)
    }

    private fun erRelevant(skjæringstidspunkt: LocalDate) = sykepengerFom >= skjæringstidspunkt

    override fun hashCode() =
        Objects.hash(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom)

    override fun equals(other: Any?): Boolean {
        if (other !is Inntektsopplysning) return false
        return equals(other)
    }

    private fun equals(other: Inntektsopplysning): Boolean {
        if (this.orgnummer != other.orgnummer) return false
        if (this.sykepengerFom != other.sykepengerFom) return false
        if (this.inntekt != other.inntekt) return false
        if (this.refusjonTom != other.refusjonTom) return false
        return this.refusjonTilArbeidsgiver == other.refusjonTilArbeidsgiver
    }

    internal companion object {

        internal fun valider(
            liste: List<Inntektsopplysning>,
            aktivitetslogg: IAktivitetslogg,
            skjæringstidspunkt: LocalDate
        ) {
            liste.forEach { it.valider(aktivitetslogg, skjæringstidspunkt) }
            liste.validerAlleInntekterForSammenhengendePeriode(skjæringstidspunkt, aktivitetslogg)
            liste.validerAntallInntekterPerArbeidsgiverPerDato(skjæringstidspunkt, aktivitetslogg)
        }

        private fun List<Inntektsopplysning>.validerAlleInntekterForSammenhengendePeriode(
            skjæringstidspunkt: LocalDate,
            aktivitetslogg: IAktivitetslogg
        ) {
            val relevanteInntektsopplysninger = filter { it.erRelevant(skjæringstidspunkt) }
            if (relevanteInntektsopplysninger.distinctBy { it.orgnummer }.size > 1) {
                return aktivitetslogg.funksjonellFeil(RV_IT_13)
            }
        }

        private fun List<Inntektsopplysning>.validerAntallInntekterPerArbeidsgiverPerDato(
            skjæringstidspunkt: LocalDate,
            aktivitetslogg: IAktivitetslogg
        ) {
            val harFlereInntekterPåSammeAGogDato = filter { it.erRelevant(skjæringstidspunkt) }
                .toSet()
                .groupBy { it.orgnummer to it.sykepengerFom }
                .any { (_, inntekter) -> inntekter.size > 1 }
            if (harFlereInntekterPåSammeAGogDato)
                aktivitetslogg.info("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato.")
        }

        internal fun sorter(inntekter: List<Inntektsopplysning>) =
            inntekter.sortedWith(compareBy({ it.sykepengerFom }, { it.hashCode() }))

        internal fun ferdigInntektsopplysning(
            orgnummer: String,
            sykepengerFom: LocalDate,
            inntekt: Inntekt,
            refusjonTilArbeidsgiver: Boolean,
            refusjonTom: LocalDate?,
            lagret: LocalDateTime?
        ): Inntektsopplysning =
            Inntektsopplysning(
                orgnummer = orgnummer,
                sykepengerFom = sykepengerFom,
                inntekt = inntekt,
                refusjonTilArbeidsgiver = refusjonTilArbeidsgiver,
                refusjonTom = refusjonTom,
                lagret = lagret
            )
    }
}
