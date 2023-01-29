package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.isWithinRangeOf
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.SkatteopplysningVisitor
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class Skatteopplysning(
    private val hendelseId: UUID,
    private val beløp: Inntekt,
    private val måned: YearMonth,
    private val type: Inntekttype,
    private val fordel: String,
    private val beskrivelse: String,
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) {
    internal enum class Inntekttype {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE
    }

    internal fun erRelevantForSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        måned.isWithinRangeOf(skjæringstidspunkt, MAKS_INNTEKT_GAP)

    internal fun accept(visitor: SkatteopplysningVisitor) {
        visitor.visitSkatteopplysning(this, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
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


    internal companion object {
        private const val MAKS_INNTEKT_GAP = 2L

        internal fun sisteTreMåneder(dato: LocalDate, inntektsopplysninger: List<Skatteopplysning>) =
            inntektsopplysninger.filter { it.måned.isWithinRangeOf(dato, 3) }

        // TODO: sette inn en IkkeRapportert inntekt når vi lagrer skatteopplysninger fra Vilkårsgrunnlag,
        // basert på hvilke arbeidsgivere det finnes arbeidsforhold fra uten skatteopplysninger
        internal fun nyoppstartetArbeidsforhold(skjæringstidspunkt: LocalDate, arbeidsforholdhistorikk: Arbeidsforholdhistorikk) =
            IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt).takeIf {
                arbeidsforholdhistorikk.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP)
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
    }
}