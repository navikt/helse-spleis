package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.etterlevelse.SkattBuilder.Companion.subsumsjonsformat
import no.nav.helse.etterlevelse.SkatteopplysningPort
import no.nav.helse.isWithinRangeOf
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.YTELSE_FRA_OFFENTLIGE
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

class Skatteopplysning(
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    private val måned: YearMonth,
    private val type: Inntekttype,
    private val fordel: String,
    private val beskrivelse: String,
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) {
    enum class Inntekttype {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE;
        fun somStreng() = when (this) {
            LØNNSINNTEKT -> "LØNNSINNTEKT"
            NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }

    fun accept(visitor: SkatteopplysningVisitor) {
        visitor.visitSkatteopplysning(this, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
    }

    fun <T> accept(pseudoVisitor: (beløp: Inntekt, måned: YearMonth, type: String, fordel: String, beskrivelse: String) -> T) {
        pseudoVisitor(beløp, måned, type.somStreng(), fordel, beskrivelse)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Skatteopplysning) return false
        if (beløp != other.beløp) return false
        if (måned != other.måned) return false
        if (type != other.type) return false
        if (fordel != other.fordel) return false
        if (beskrivelse != other.beskrivelse) return false
        return true
    }

    override fun hashCode(): Int {
        var result = hendelseId.hashCode()
        result = 31 * result + beløp.hashCode()
        result = 31 * result + måned.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + fordel.hashCode()
        result = 31 * result + beskrivelse.hashCode()
        result = 31 * result + tidsstempel.hashCode()
        return result
    }


    companion object {
        fun sisteMåneder(dato: LocalDate, antallMåneder: Int, inntektsopplysninger: List<Skatteopplysning>) =
            inntektsopplysninger.filter { it.måned.isWithinRangeOf(dato, antallMåneder.toLong()) }

        fun sisteTreMåneder(dato: LocalDate, inntektsopplysninger: List<Skatteopplysning>) =
            sisteMåneder(dato, 3, inntektsopplysninger)

        fun List<Skatteopplysning>.sjekkMuligeGhostsUtenArbeidsforhold(aktivitetslogg: IAktivitetslogg, dato: LocalDate) {
            if (sisteTreMåneder(dato, this).filterNot { it.type == YTELSE_FRA_OFFENTLIGE }.isEmpty()) return
            aktivitetslogg.varsel(Varselkode.RV_IV_1)
        }

        fun omregnetÅrsinntekt(liste: List<Skatteopplysning>) = liste
            .map { it.beløp }
            .summer()
            .coerceAtLeast(Inntekt.INGEN)
            .div(3)

        fun rapportertInntekt(liste: List<Skatteopplysning>) = liste
            .map { it.beløp }
            .summer()
            .div(12)

        fun List<Skatteopplysning>.subsumsjonsformat() = this.map { SkatteopplysningAdapter(it) }.subsumsjonsformat()
    }
}

class SkatteopplysningAdapter(private val skatteopplysning: Skatteopplysning): SkatteopplysningPort {
    override fun <T> accept(pseudoVisitor: (beløp: Inntekt, måned: YearMonth, type: String, fordel: String, beskrivelse: String) -> T) = skatteopplysning.accept(pseudoVisitor)
}