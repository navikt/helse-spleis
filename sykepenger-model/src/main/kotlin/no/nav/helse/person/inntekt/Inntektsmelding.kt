package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
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

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag? {
        if (dato == skjæringstidspunkt) return this
        if (førsteFraværsdag == null || dato != førsteFraværsdag) return null
        return this
    }

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    internal fun kanLagres(other: Inntektsmelding) = this.hendelseId != other.hendelseId || this.dato != other.dato

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is Inntektsmelding && this.dato == other.dato && other.beløp == this.beløp
    }

    internal fun kopierTidsnærOpplysning(nyDato: LocalDate, hendelse: IAktivitetslogg, oppholdsperiodeMellom: Periode?): Inntektsmelding {
        if ((ChronoUnit.DAYS.between(this.dato, nyDato) >= 60) ||
            (oppholdsperiodeMellom != null && oppholdsperiodeMellom.count() >= 16 && this.dato < oppholdsperiodeMellom.endInclusive))
            hendelse.varsel(RV_IV_7)
        return Inntektsmelding(UUID.randomUUID(), nyDato,hendelseId, beløp, tidsstempel)
    }
}