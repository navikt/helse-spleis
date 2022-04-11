package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
import java.util.UUID
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
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

    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, nødnummer: Nødnummer): Boolean {
        if (!erRelevant(skjæringstidspunkt)) return true
        if (orgnummer.isBlank()) aktivitetslogg.error("String for inntektsopplysning fra Infotrygd mangler")
        else if (orgnummer in nødnummer) aktivitetslogg.error("Det er registrert bruk av på nødnummer")
        return !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkInntektsopplysning(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, lagret)
    }

    private fun erRelevant(skjæringstidspunkt: LocalDate) = sykepengerFom >= skjæringstidspunkt

    private fun addInntekt(appendMode: Inntektshistorikk.AppendMode, hendelseId: UUID) {
        lagret = LocalDateTime.now()
        appendMode.addInfotrygd(sykepengerFom, hendelseId, inntekt)
    }

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

    internal fun erDuplikat(dato: LocalDate, beløp: Inntekt) =
        this.inntekt == beløp && this.sykepengerFom == dato

    internal companion object {
        internal fun addInntekter(liste: List<Inntektsopplysning>, person: Person, aktivitetslogg: IAktivitetslogg, hendelseId: UUID, nødnummer: Nødnummer) =
            liste
                .filterNot { it.orgnummer in nødnummer }
                .groupBy { it.orgnummer }
                .onEach { (orgnummer, opplysninger) -> person.lagreSykepengegrunnlagFraInfotrygd(orgnummer, opplysninger, aktivitetslogg, hendelseId) }
                .isNotEmpty()

        internal fun lagreInntekter(inntektsopplysninger: List<Inntektsopplysning>, inntektshistorikk: Inntektshistorikk, hendelseId: UUID) {
            val nyeInntekter = inntektshistorikk.filtrerBortKjenteInntekter(inntektsopplysninger)
            if (nyeInntekter.isEmpty()) return
            inntektshistorikk.append {
                nyeInntekter.reversed().forEach { it.addInntekt(this, hendelseId) }
            }
        }

        internal fun valider(
            liste: List<Inntektsopplysning>,
            aktivitetslogg: IAktivitetslogg,
            skjæringstidspunkt: LocalDate,
            nødnummer: Nødnummer
        ) {
            liste.forEach { it.valider(aktivitetslogg, skjæringstidspunkt, nødnummer) }
            liste.fjern(nødnummer).validerAlleInntekterForSammenhengendePeriode(skjæringstidspunkt, aktivitetslogg)
            liste.fjern(nødnummer).validerAntallInntekterPerArbeidsgiverPerDato(skjæringstidspunkt, aktivitetslogg)
        }

        internal fun Iterable<Inntektsopplysning>.harInntekterFor(datoer: List<LocalDate>) = map { it.sykepengerFom }.containsAll(datoer)

        private fun List<Inntektsopplysning>.validerAlleInntekterForSammenhengendePeriode(
            skjæringstidspunkt: LocalDate,
            aktivitetslogg: IAktivitetslogg
        ) {
            val relevanteInntektsopplysninger = filter { it.erRelevant(skjæringstidspunkt) }
            if (relevanteInntektsopplysninger.distinctBy { it.orgnummer }.size > 1) {
                return aktivitetslogg.error("Støtter ikke overgang fra infotrygd for flere arbeidsgivere")
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

        internal fun List<Inntektsopplysning>.lagreVilkårsgrunnlag(
            vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
            sykepengegrunnlagFor: (skjæringstidspunkt: LocalDate) -> Sykepengegrunnlag
        ) {
            forEach {
                vilkårsgrunnlagHistorikk.lagre(
                    it.sykepengerFom,
                    VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(it.sykepengerFom, sykepengegrunnlagFor(it.sykepengerFom))
                )
            }
        }

        internal fun List<Inntektsopplysning>.fjern(nødnummer: Nødnummer) = filterNot { it.orgnummer in nødnummer }

        internal fun sorter(inntekter: List<Inntektsopplysning>) =
            inntekter.sortedWith(compareBy({ it.sykepengerFom }, { it.hashCode() }))
    }
}
