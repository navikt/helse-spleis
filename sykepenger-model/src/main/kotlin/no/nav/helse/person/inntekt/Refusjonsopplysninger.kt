package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.RefusjonsopplysningInnDto
import no.nav.helse.dto.deserialisering.RefusjonsopplysningerInnDto
import no.nav.helse.dto.serialisering.RefusjonsopplysningUtDto
import no.nav.helse.dto.serialisering.RefusjonsopplysningerUtDto
import no.nav.helse.hendelser.Avsender
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

    override fun toString() = "$periode ${beløp.daglig} fra $avsender mottatt $tidsstempel ($$meldingsreferanseId)"

    internal companion object {
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

    @Deprecated(
        message = "Denne klassen brukes nå kun for gjenoppretting av refusjonsopplysninger som allerede ligger i inntektsgrunnlag, men det legges ikke til nytt. Denne 'gamle' informasjonen brukes ikke, så hele klassen kan slettes når vi føler oss trygge.",
        replaceWith = ReplaceWith("Refusjonsopplysninger på behandlinger i form av en beløpstidslinje")
    )
    class Refusjonsopplysninger private constructor(
        refusjonsopplysninger: List<Refusjonsopplysning>
    ) {
        private val validerteRefusjonsopplysninger = refusjonsopplysninger.sortedBy { it.fom }

        constructor() : this(emptyList())

        init {
            check(!validerteRefusjonsopplysninger.overlapper()) { "Refusjonsopplysninger skal ikke kunne inneholde overlappende informasjon: $refusjonsopplysninger" }
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
