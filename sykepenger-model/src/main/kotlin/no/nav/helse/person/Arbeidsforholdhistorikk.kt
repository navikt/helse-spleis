package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.ansattVedSkjæringstidspunkt
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.harArbeidsforholdNyereEnn
import no.nav.helse.person.etterlevelse.SubsumsjonObserver

internal class Arbeidsforholdhistorikk private constructor(
    private val historikk: MutableList<Innslag>
) {

    internal constructor() : this(mutableListOf())

    internal fun lagre(arbeidsforhold: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        val erDuplikat = sisteInnslag(skjæringstidspunkt)?.erDuplikat(arbeidsforhold, skjæringstidspunkt) ?: false
        if (!erDuplikat) {
            historikk.add(Innslag(UUID.randomUUID(), arbeidsforhold, skjæringstidspunkt))
        }
    }

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.preVisitArbeidsforholdhistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsforholdhistorikk(this)
    }

    internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = sisteInnslag(skjæringstidspunkt)?.arbeidsforhold
        ?.ansattVedSkjæringstidspunkt(skjæringstidspunkt)
        ?: false

    internal fun harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
        sisteInnslag(skjæringstidspunkt)?.harArbeidsforholdNyereEnn(skjæringstidspunkt, antallMåneder) ?: false

    private fun sisteInnslag(skjæringstidspunkt: LocalDate) = historikk.lastOrNull { it.gjelder(skjæringstidspunkt) }

    internal class Innslag(private val id: UUID, val arbeidsforhold: List<Arbeidsforhold>, private val skjæringstidspunkt: LocalDate) {
        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.preVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
        }

        internal fun erDuplikat(other: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) =
            skjæringstidspunkt == this.skjæringstidspunkt && arbeidsforhold.size == other.size && arbeidsforhold.containsAll(other)

        internal fun gjelder(skjæringstidspunkt: LocalDate) =
            this.skjæringstidspunkt == skjæringstidspunkt

        internal fun harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
            arbeidsforhold.harArbeidsforholdNyereEnn(skjæringstidspunkt, antallMåneder)
    }

    internal class Arbeidsforhold(
        private val ansattFom: LocalDate,
        private val ansattTom: LocalDate?,
        private val deaktivert: Boolean
    ) {
        internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

        override fun equals(other: Any?) = other is Arbeidsforhold
            && ansattFom == other.ansattFom
            && ansattTom == other.ansattTom
            && deaktivert == other.deaktivert

        internal fun harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) = ansattFom >= skjæringstidspunkt.withDayOfMonth(1).minusMonths(antallMåneder)

        override fun hashCode(): Int {
            var result = ansattFom.hashCode()
            result = 31 * result + (ansattTom?.hashCode() ?: 0)
            result = 31 * result + deaktivert.hashCode()
            return result
        }

        internal fun accept(visitor: ArbeidsforholdVisitor) {
            visitor.visitArbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = deaktivert)
        }

        internal fun deaktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = true)

        internal fun aktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = false)

        companion object {
            private fun List<Arbeidsforhold>.harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) = this
                .filter { it.harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder) }
                .filter { it.gjelder(skjæringstidspunkt) }

            internal fun List<Arbeidsforhold>.harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
                harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder).isNotEmpty()

            internal fun Collection<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = filter { !it.deaktivert }
                .map { it.ansattFom til (it.ansattTom ?: skjæringstidspunkt) }
                .sammenhengende(skjæringstidspunkt)


            internal fun <T> List<Arbeidsforhold>.create(creator: (LocalDate, LocalDate?, Boolean) -> T) =
                map { creator(it.ansattFom, it.ansattTom, it.deaktivert) }

            internal fun Collection<Arbeidsforhold>.ansattVedSkjæringstidspunkt(skjæringstidspunkt: LocalDate) = any { it.gjelder(skjæringstidspunkt) }

            internal fun Iterable<Arbeidsforhold>.toEtterlevelseMap(orgnummer: String) = map {
                mapOf(
                    "orgnummer" to orgnummer,
                    "fom" to it.ansattFom,
                    "tom" to it.ansattTom
                )
            }
        }

    }

    internal companion object {
        internal fun Map<String, List<Arbeidsforhold>>.opptjening(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Opptjening {
            val arbeidsforhold = this
                .filterValues { it.isNotEmpty() }
                .map { (orgnr, arbeidsforhold) -> Opptjening.ArbeidsgiverOpptjeningsgrunnlag(orgnr, arbeidsforhold) }
            return Opptjening(arbeidsforhold, skjæringstidspunkt, subsumsjonObserver)
        }
        internal fun ferdigArbeidsforholdhistorikk(historikk: List<Innslag>): Arbeidsforholdhistorikk =
            Arbeidsforholdhistorikk(historikk.map{it}.toMutableList())
    }
}
