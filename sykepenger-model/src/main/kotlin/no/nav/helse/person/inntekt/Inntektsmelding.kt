package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.hendelser.til
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.InntektsmeldingVisitor
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal class Inntektsmelding(
    private val id: UUID,
    dato: LocalDate,
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Inntektsopplysning(dato, 60) {

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

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? {
        if (dato == skjæringstidspunkt) return this
        if (førsteFraværsdag == null || dato != førsteFraværsdag) return null
        if (YearMonth.from(skjæringstidspunkt) == YearMonth.from(førsteFraværsdag)) return this
        return IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt)
    }

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    override fun kanLagres(other: Inntektsopplysning) = !skalErstattesAv(other)

    override fun skalErstattesAv(other: Inntektsopplysning) =
        other is Inntektsmelding && this.dato == other.dato

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is Inntektsmelding && this.dato == other.dato && other.beløp == this.beløp
    }
}