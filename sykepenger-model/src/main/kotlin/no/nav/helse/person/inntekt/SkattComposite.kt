package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class SkattComposite(
    private val id: UUID,
    private val inntektsopplysninger: List<Skatt>
) : Inntektsopplysning(inntektsopplysninger.first()) {

    private val inntekterSisteTreMåneder = inntektsopplysninger.filter { it.erRelevant(3) }

    override fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitSkatt(this, id, dato)
        inntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitSkatt(this, id, dato)
    }

    override fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
        takeIf {
            inntektsopplysninger.any {
                it.omregnetÅrsinntekt(skjæringstidspunkt, førsteFraværsdag) != null
                    && it.erRelevant(Arbeidsforholdhistorikk.Arbeidsforhold.MAKS_INNTEKT_GAP)
            }
        }

    override fun omregnetÅrsinntekt(): Inntekt {
        return inntekterSisteTreMåneder
            .map(Skatt::omregnetÅrsinntekt)
            .summer()
            .coerceAtLeast(Inntekt.INGEN)
            .div(3)
    }

    override fun rapportertInntekt(dato: LocalDate) =
        inntektsopplysninger
            .mapNotNull { it.rapportertInntekt(dato) }
            .flatten()
            .takeIf { it.isNotEmpty() }

    override fun rapportertInntekt(): Inntekt =
        inntektsopplysninger
            .filter { it.erRelevant(12) }
            .map(Skatt::rapportertInntekt)
            .summer()
            .div(12)

    override fun subsumerSykepengegrunnlag(subsumsjonObserver: SubsumsjonObserver, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) {
        subsumsjonObserver.`§ 8-28 ledd 3 bokstav a`(
            organisasjonsnummer = organisasjonsnummer,
            skjæringstidspunkt = dato,
            inntekterSisteTreMåneder = inntekterSisteTreMåneder.subsumsjonsformat(),
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
            inntekterSisteTreMåneder = inntekterSisteTreMåneder.subsumsjonsformat(),
            forklaring = forklaring,
            oppfylt = oppfylt
        )
    }

    override fun skalErstattesAv(other: Inntektsopplysning): Boolean {
        if (other is SkattComposite) return skalErstattesAv(other)
        return this.inntektsopplysninger.any { it.skalErstattesAv(other) }
    }

    private fun skalErstattesAv(other: SkattComposite) =
        other.inntektsopplysninger.any { skalErstattesAv(it) }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is SkattComposite && this.inntektsopplysninger == other.inntektsopplysninger
    }
}