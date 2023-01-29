package no.nav.helse.person.inntekt


import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.inntekt.Inntektsopplysning.Companion.lagre
import no.nav.helse.økonomi.Inntekt

internal class Inntektshistorikk private constructor(private val historikk: MutableList<Innslag>) {

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal val NULLUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        internal fun gjenopprett(list: List<Innslag>) = Inntektshistorikk(list.toMutableList())
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun leggTil(inntekter: List<Inntektsopplysning>) {
        leggTil(Innslag(inntekter))
    }

    private fun leggTil(nyttInnslag: Innslag) {
        val gjeldende = nyesteInnslag() ?: return historikk.add(0, nyttInnslag)
        val oppdatertInnslag = (gjeldende + nyttInnslag) ?: return
        historikk.add(0, oppdatertInnslag)
    }

    internal fun nyesteInnslag() = historikk.firstOrNull()

    internal fun nyesteId() = Innslag.nyesteId(this)

    internal fun isNotEmpty() = historikk.isNotEmpty()

    internal fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?, arbeidsforholdhistorikk: Arbeidsforholdhistorikk): Inntektsopplysning? =
        nyesteInnslag()?.omregnetÅrsinntekt(skjæringstidspunkt, førsteFraværsdag) ?: nyoppstartetArbeidsforhold(skjæringstidspunkt, arbeidsforholdhistorikk)

    internal fun rapportertInntekt(dato: LocalDate) = nyesteInnslag()?.rapportertInntekt(dato)

    private fun nyoppstartetArbeidsforhold(skjæringstidspunkt: LocalDate, arbeidsforholdhistorikk: Arbeidsforholdhistorikk) =
        IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt).takeIf {
            arbeidsforholdhistorikk.harArbeidsforholdNyereEnn(skjæringstidspunkt,
                Arbeidsforholdhistorikk.Arbeidsforhold.MAKS_INNTEKT_GAP
            )
        }

    internal class Innslag private constructor(private val id: UUID, private val inntekter: List<Inntektsopplysning>) {
        constructor(inntekter: List<Inntektsopplysning> = emptyList()) : this(UUID.randomUUID(), inntekter)

        internal fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitInnslag(this, id)
            inntekter.forEach { it.accept(visitor) }
            visitor.postVisitInnslag(this, id)
        }

        internal fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            inntekter
                .mapNotNull { it.omregnetÅrsinntekt(skjæringstidspunkt, førsteFraværsdag) }
                .minOrNull()

        internal fun rapportertInntekt(dato: LocalDate) =
            inntekter
                .sorted()
                .mapNotNull { it.rapportertInntekt(dato) }
                .firstOrNull()

        override fun equals(other: Any?): Boolean {
            return other is Innslag && this.inntekter == other.inntekter
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + inntekter.hashCode()
            return result
        }

        operator fun plus(other: Innslag): Innslag? {
            var inntekter = this.inntekter
            other.inntekter.forEach { inntektsopplysning ->
                inntekter = inntektsopplysning.lagre(inntekter)
            }
            val nytt = Innslag(inntekter)
            if (this == nytt) return null
            return nytt
        }

        internal companion object {
            internal fun gjenopprett(id: UUID, inntektsopplysninger: List<Inntektsopplysning>) =
                Innslag(id, inntektsopplysninger)

            internal fun nyesteId(inntektshistorikk: Inntektshistorikk) = inntektshistorikk.nyesteInnslag()!!.id
        }
    }

    internal fun append(block: InnslagBuilder.() -> Unit) {
        leggTil(InnslagBuilder().build(block))
    }

    internal class InnslagBuilder() {
        private val tidsstempel = LocalDateTime.now()
        private val skatt = mutableListOf<Skatt>()
        private var inntektsopplysninger = listOf<Inntektsopplysning>()

        internal fun build(builder: InnslagBuilder.() -> Unit): Innslag {
            apply(builder)
            return Innslag(inntektsopplysninger)
        }

        internal fun addInntektsmelding(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Inntektsmelding(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

        internal fun add(opplysning: Inntektsopplysning) {
            inntektsopplysninger = opplysning.lagre(inntektsopplysninger)
        }
    }
}
