package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.økonomi.Inntekt


internal class InntektshistorikkInspektør(inntektshistorikk: Inntektshistorikk) : InntekthistorikkVisitor {

    private val inntektsopplysninger = mutableListOf<Opplysning>()
    internal val size get() = inntektsopplysninger.size
    internal lateinit var inntektshistorikk: Inntektshistorikk

    class Opplysning(
        val dato: LocalDate,
        val sykepengegrunnlag: Inntekt,
    )

    init {
        inntektshistorikk.accept(this)
    }

    internal fun omregnetÅrsinntekt(dato: LocalDate, førsteFraværsdag: LocalDate) =
        inntektshistorikk.avklarSykepengegrunnlag(dato, førsteFraværsdag, null, Arbeidsforholdhistorikk())

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        this.inntektshistorikk = inntektshistorikk
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektsmelding,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(Opplysning(dato, beløp))
    }
}
