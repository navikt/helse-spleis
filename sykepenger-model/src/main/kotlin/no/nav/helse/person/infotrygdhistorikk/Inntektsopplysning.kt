package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

    internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate?): Boolean {
        if (!erRelevant(periode, skjæringstidspunkt)) return true
        if (orgnummer.isBlank()) aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
        if (refusjonTom != null && periode.slutterEtter(refusjonTom)) aktivitetslogg.error("Refusjon fra Infotrygd opphører i eller før perioden")
        if (!refusjonTilArbeidsgiver) aktivitetslogg.error("Utbetaling skal gå rett til bruker")
        return !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkInntektsopplysning(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, lagret)
    }

    private fun erRelevant(periode: Periode, skjæringstidspunkt: LocalDate?) =
        sykepengerFom >= (skjæringstidspunkt ?: periode.start.minusMonths(12))

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

    internal companion object {
        internal fun addInntekter(liste: List<Inntektsopplysning>, person: Person, aktivitetslogg: IAktivitetslogg, hendelseId: UUID) {
            liste.groupBy { it.orgnummer }
                .forEach { (orgnummer, opplysninger) -> person.lagreSykepengegrunnlagFraInfotrygd(orgnummer, opplysninger, aktivitetslogg, hendelseId) }
        }

        fun lagreInntekter(list: List<Inntektsopplysning>, inntektshistorikk: Inntektshistorikk, hendelseId: UUID) {
            inntektshistorikk {
                list.reversed().forEach { it.addInntekt(this, hendelseId) }
            }
        }

        fun valider(
            liste: List<Inntektsopplysning>,
            aktivitetslogg: IAktivitetslogg,
            periode: Periode,
            skjæringstidspunkt: LocalDate?
        ) {
            liste.forEach { it.valider(aktivitetslogg, periode, skjæringstidspunkt) }
            liste.validerAlleInntekterForSammenhengendePeriode(skjæringstidspunkt, aktivitetslogg, periode)
            liste.validerAntallInntekterPerArbeidsgiverPerDato(skjæringstidspunkt, aktivitetslogg, periode)
        }

        internal fun Iterable<Inntektsopplysning>.harInntekterFor(datoer: List<LocalDate>) = map { it.sykepengerFom }.containsAll(datoer)

        private fun List<Inntektsopplysning>.validerAlleInntekterForSammenhengendePeriode(
            skjæringstidspunkt: LocalDate?,
            aktivitetslogg: IAktivitetslogg,
            periode: Periode
        ) {
            if (!Toggles.FlereArbeidsgivereUlikFom.enabled) {
                val relevanteInntektsopplysninger = filter { it.erRelevant(periode, skjæringstidspunkt) }
                val harFlereArbeidsgivere = relevanteInntektsopplysninger.distinctBy { it.orgnummer }.size > 1
                val harFlereSkjæringstidspunkt = relevanteInntektsopplysninger.distinctBy { it.sykepengerFom }.size > 1
                if (harFlereArbeidsgivere && harFlereSkjæringstidspunkt) aktivitetslogg.error("Har inntekt på flere arbeidsgivere med forskjellig fom dato")
            }
            if (this.isNotEmpty() && skjæringstidspunkt == null) aktivitetslogg.info("Har inntekt i Infotrygd og skjæringstidspunkt er null")
        }

        private fun List<Inntektsopplysning>.validerAntallInntekterPerArbeidsgiverPerDato(
            skjæringstidspunkt: LocalDate?,
            aktivitetslogg: IAktivitetslogg,
            periode: Periode
        ) {
            val harFlereInntekterPåSammeAGogDato = filter { it.erRelevant(periode, skjæringstidspunkt) }
                .groupBy { it.orgnummer to it.sykepengerFom }
                .any { (_, inntekter) -> inntekter.size > 1 }
            if (harFlereInntekterPåSammeAGogDato)
                aktivitetslogg.warn("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato, den seneste er lagt til grunn. Kontroller sykepengegrunnlaget.")
        }

        internal fun List<Inntektsopplysning>.lagreVilkårsgrunnlag(
            vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
            sykepengegrunnlagFor: (skjæringstidspunkt: LocalDate) -> Sykepengegrunnlag
        ) {
            forEach {
                vilkårsgrunnlagHistorikk.lagre(
                    it.sykepengerFom,
                    VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(sykepengegrunnlagFor(it.sykepengerFom))
                )
            }
        }
    }
}
