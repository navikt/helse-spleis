package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.til
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal class Inntektsmelding(
    private val id: UUID,
    dato: LocalDate,
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    tidsstempel: LocalDateTime
) : AvklarbarSykepengegrunnlag(dato, tidsstempel) {

    override fun accept(visitor: InntektsopplysningVisitor) {
        accept(visitor as InntektsmeldingVisitor)
    }

    internal fun accept(visitor: InntektsmeldingVisitor) {
        visitor.visitInntektsmelding(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun overstyres(ny: Inntektsopplysning): Inntektsopplysning {
        if (ny !is Saksbehandler && ny !is Inntektsmelding) return this
        val måned = this.dato.withDayOfMonth(1) til this.dato.withDayOfMonth(this.dato.lengthOfMonth())
        if (ny is Inntektsmelding && ny.dato !in måned) return this
        return super.overstyres(ny)
    }

    override fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {
        throw IllegalStateException("Kan ikke overstyre arbeidsforhold for en arbeidsgiver som har sykdom")
    }

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag? {
        if (dato == skjæringstidspunkt) return this
        if (førsteFraværsdag == null || dato != førsteFraværsdag) return null
        return this
    }

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    internal fun kanLagres(other: Inntektsmelding) = this.hendelseId != other.hendelseId

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is Inntektsmelding && this.dato == other.dato && other.beløp == this.beløp
    }
}