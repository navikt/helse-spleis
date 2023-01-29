package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.økonomi.Inntekt

internal class SkattSykepengegrunnlag(
    private val id: UUID,
    dato: LocalDate,
    inntektsopplysninger: List<Skatteopplysning>
) : Inntektsopplysning(dato, 40) {

    private val inntektsopplysninger = Skatteopplysning.sisteTreMåneder(dato, inntektsopplysninger)

    override fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitSkattSykepengegrunnlag(this, id, dato)
        inntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitSkattSykepengegrunnlag(this, id, dato)
    }

    override fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
        takeIf { this.dato == skjæringstidspunkt && inntektsopplysninger.any { it.erRelevantForSykepengegrunnlag(skjæringstidspunkt) } }

    override fun omregnetÅrsinntekt(): Inntekt {
        return Skatteopplysning.omregnetÅrsinntekt(inntektsopplysninger)
    }

    override fun subsumerSykepengegrunnlag(subsumsjonObserver: SubsumsjonObserver, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        subsumsjonObserver.`§ 8-28 ledd 3 bokstav a`(
            organisasjonsnummer = organisasjonsnummer,
            skjæringstidspunkt = dato,
            inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
            grunnlagForSykepengegrunnlag = omregnetÅrsinntekt()
        )
        subsumsjonObserver.`§ 8-29`(dato, omregnetÅrsinntekt(), inntektsopplysninger.subsumsjonsformat(), organisasjonsnummer)
    }

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        subsumsjonObserver.`§ 8-15`(
            skjæringstidspunkt = dato,
            organisasjonsnummer = organisasjonsnummer,
            inntekterSisteTreMåneder = inntektsopplysninger.subsumsjonsformat(),
            forklaring = forklaring,
            oppfylt = oppfylt
        )
    }

    override fun kanLagres(other: Inntektsopplysning) = true
    override fun skalErstattesAv(other: Inntektsopplysning): Boolean {
        if (other !is SkattSykepengegrunnlag) return false
        return this.dato == other.dato
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is SkattSykepengegrunnlag && this.dato == other.dato && this.inntektsopplysninger == other.inntektsopplysninger
    }
}