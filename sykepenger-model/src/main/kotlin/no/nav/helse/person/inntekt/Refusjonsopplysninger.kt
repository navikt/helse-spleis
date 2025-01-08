package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.RefusjonsopplysningInnDto
import no.nav.helse.dto.deserialisering.RefusjonsopplysningerInnDto
import no.nav.helse.dto.serialisering.RefusjonsopplysningUtDto
import no.nav.helse.dto.serialisering.RefusjonsopplysningerUtDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.overlapper
import no.nav.helse.hendelser.til
import no.nav.helse.økonomi.Inntekt

data class Refusjonsopplysning private constructor(
    val meldingsreferanseId: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: Inntekt,
    val avsender: Avsender,
    val tidsstempel: LocalDateTime
) {
    init {
        check(tom == null || tom <= tom) { "fom ($fom) kan ikke være etter tom ($tom) " }
    }

    private val periode = fom til (tom ?: LocalDate.MAX)

    private fun trim(periodeSomSkalFjernes: Periode): List<Refusjonsopplysning> {
        return this.periode
            .trim(periodeSomSkalFjernes)
            .map {
                Refusjonsopplysning(
                    fom = it.start,
                    tom = it.endInclusive.takeUnless { tom -> tom == LocalDate.MAX },
                    beløp = this.beløp,
                    meldingsreferanseId = this.meldingsreferanseId,
                    avsender = this.avsender,
                    tidsstempel = this.tidsstempel
                )
            }
    }

    private fun begrensetFra(dato: LocalDate): Refusjonsopplysning? {
        if (periode.endInclusive < dato) return null
        if (periode.start >= dato) return this
        return Refusjonsopplysning(meldingsreferanseId, dato, tom, beløp, avsender, tidsstempel)
    }

    private fun funksjoneltLik(other: Refusjonsopplysning) =
        this.periode == other.periode && this.beløp == other.beløp

    override fun toString() = "$periode, ${beløp.daglig} ($meldingsreferanseId), ($avsender), ($tidsstempel)"

    internal companion object {
        private fun List<Refusjonsopplysning>.mergeInnNyeOpplysninger(nyeOpplysninger: List<Refusjonsopplysning>): List<Refusjonsopplysning> {
            val begrensetFra = minOfOrNull { it.fom } ?: LocalDate.MIN
            return nyeOpplysninger
                .fold(this) { resultat, nyOpplysning -> resultat.mergeInnNyOpplysning(nyOpplysning, begrensetFra) }
                .sortedBy { it.fom }
        }

        private fun List<Refusjonsopplysning>.mergeInnNyOpplysning(nyOpplysning: Refusjonsopplysning, begrensetFra: LocalDate): List<Refusjonsopplysning> {
            // begrenser refusjonsopplysningen slik at den ikke kan strekke tilbake i tid
            val nyOpplysningBegrensetStart = nyOpplysning.begrensetFra(begrensetFra) ?: return this
            // bevarer eksisterende opplysning hvis ny opplysning finnes fra før (dvs. vi bevarer meldingsreferanseId på forrige)
            if (any { it.funksjoneltLik(nyOpplysningBegrensetStart) }) return this
            // Beholder de delene som ikke dekkes av den nye opplysningen og legger til den nye opplysningen
            return flatMap { eksisterendeOpplysning -> eksisterendeOpplysning.trim(nyOpplysningBegrensetStart.periode) }.plus(nyOpplysningBegrensetStart)
        }

        internal fun gjenopprett(dto: RefusjonsopplysningInnDto): Refusjonsopplysning {
            return Refusjonsopplysning(
                meldingsreferanseId = dto.meldingsreferanseId,
                fom = dto.fom,
                tom = dto.tom,
                beløp = Inntekt.gjenopprett(dto.beløp),
                avsender = Avsender.gjenopprett(dto.avsender),
                tidsstempel = dto.tidsstempel
            )
        }
    }

    class Refusjonsopplysninger private constructor(
        refusjonsopplysninger: List<Refusjonsopplysning>
    ) {
        private val validerteRefusjonsopplysninger = refusjonsopplysninger.sortedBy { it.fom }

        constructor() : this(emptyList())

        init {
            check(!validerteRefusjonsopplysninger.overlapper()) { "Refusjonsopplysninger skal ikke kunne inneholde overlappende informasjon: $refusjonsopplysninger" }
        }

        internal fun merge(other: Refusjonsopplysninger): Refusjonsopplysninger {
            return Refusjonsopplysninger(validerteRefusjonsopplysninger.mergeInnNyeOpplysninger(other.validerteRefusjonsopplysninger))
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Refusjonsopplysninger) return false
            return validerteRefusjonsopplysninger == other.validerteRefusjonsopplysninger
        }

        override fun hashCode() = validerteRefusjonsopplysninger.hashCode()

        override fun toString() = validerteRefusjonsopplysninger.toString()

        internal companion object {
            private fun List<Refusjonsopplysning>.overlapper() = map { it.periode }.overlapper()
            internal val Refusjonsopplysning.refusjonsopplysninger get() = Refusjonsopplysninger(listOf(this))

            internal fun gjenopprett(dto: RefusjonsopplysningerInnDto) = Refusjonsopplysninger(
                refusjonsopplysninger = dto.opplysninger.map { gjenopprett(it) }
            )
        }

        internal fun dto() = RefusjonsopplysningerUtDto(
            opplysninger = this.validerteRefusjonsopplysninger.map { it.dto() }
        )
    }

    internal fun dto() = RefusjonsopplysningUtDto(
        meldingsreferanseId = this.meldingsreferanseId,
        fom = this.fom,
        tom = this.tom,
        beløp = this.beløp.dto(),
        avsender = this.avsender.dto(),
        tidsstempel = this.tidsstempel
    )
}
