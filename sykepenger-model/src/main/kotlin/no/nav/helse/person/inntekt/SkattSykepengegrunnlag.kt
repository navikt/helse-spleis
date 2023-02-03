package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.harArbeidsforholdNyereEnn
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SkattBuilder.Companion.subsumsjonsformat
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.sisteMåneder
import no.nav.helse.økonomi.Inntekt

internal class SkattSykepengegrunnlag(
    private val id: UUID,
    private val hendelseId: UUID,
    dato: LocalDate,
    inntektsopplysninger: List<Skatteopplysning>,
    private val ansattPerioder: List<Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold>,
    tidsstempel: LocalDateTime
) : AvklarbarSykepengegrunnlag(dato, tidsstempel) {
    private companion object {
        private const val MAKS_INNTEKT_GAP = 2
    }

    internal constructor(
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        inntektsopplysninger: List<Skatteopplysning>,
        tidsstempel: LocalDateTime
    ) : this(id, hendelseId, dato, inntektsopplysninger, emptyList(), tidsstempel)

    private val inntektsopplysninger = Skatteopplysning.sisteTreMåneder(dato, inntektsopplysninger)
    private val beløp = Skatteopplysning.omregnetÅrsinntekt(this.inntektsopplysninger)

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.preVisitSkattSykepengegrunnlag(this, id, hendelseId, dato, beløp, tidsstempel)
        inntektsopplysninger.forEach { it.accept(visitor) }
        visitor.postVisitSkattSykepengegrunnlag(this, id, hendelseId, dato, beløp, tidsstempel)
    }

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag? {
        if (this.dato != skjæringstidspunkt) return null
        if (ansattPerioder.isEmpty()) return null
        if (!ansattVedSkjæringstidspunkt(skjæringstidspunkt)) return null
        if (sisteMåneder(skjæringstidspunkt, MAKS_INNTEKT_GAP, inntektsopplysninger).isNotEmpty()) return this
        if (inntektsopplysninger.isNotEmpty()) return null
        if (!nyoppstartetArbeidsforhold(skjæringstidspunkt)) return null
        // todo bare returnere "this" og mappe ut IKKE_RAPPORTERT i SpeilBuilder?
        return IkkeRapportert(
            id = UUID.randomUUID(),
            dato = this.dato,
            tidsstempel = this.tidsstempel
        )
    }

    private fun nyoppstartetArbeidsforhold(skjæringstidspunkt: LocalDate) =
        ansattPerioder.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP)

    private fun ansattVedSkjæringstidspunkt(dato: LocalDate) =
        ansattPerioder.any { ansattPeriode -> ansattPeriode.gjelder(dato) }

    override fun omregnetÅrsinntekt(): Inntekt {
        return beløp
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

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is SkattSykepengegrunnlag && this.dato == other.dato && this.inntektsopplysninger == other.inntektsopplysninger
    }

    internal operator fun plus(other: SkattSykepengegrunnlag?): SkattSykepengegrunnlag {
        if (other == null) return this
        return SkattSykepengegrunnlag(
            id = this.id,
            hendelseId = this.hendelseId,
            dato = this.dato,
            inntektsopplysninger = this.inntektsopplysninger + other.inntektsopplysninger,
            ansattPerioder = this.ansattPerioder + other.ansattPerioder,
            tidsstempel = this.tidsstempel
        )
    }
}