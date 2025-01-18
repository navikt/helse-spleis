package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal class SkattSykepengegrunnlag(
    id: UUID,
    inntektsdata: Inntektsdata,
    val inntektsopplysninger: List<Skatteopplysning>,
    val ansattPerioder: List<AnsattPeriode>
) : SkatteopplysningSykepengegrunnlag(id, inntektsdata) {
    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.SkattSykepengegrunnlagDto): SkattSykepengegrunnlag {
            val skatteopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
            return SkattSykepengegrunnlag(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                inntektsopplysninger = skatteopplysninger,
                ansattPerioder = dto.ansattPerioder.map { AnsattPeriode.gjenopprett(it) },
            )
        }
    }

    override fun dto() =
        InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            inntektsopplysninger = inntektsopplysninger.map { it.dto() },
            ansattPerioder = ansattPerioder.map { it.dto() })
}

class AnsattPeriode(
    private val ansattFom: LocalDate,
    private val ansattTom: LocalDate?
) {
    companion object {
        fun gjenopprett(dto: AnsattPeriodeDto) = AnsattPeriode(
            ansattFom = dto.fom,
            ansattTom = dto.tom
        )
    }

    internal fun dto() = AnsattPeriodeDto(ansattFom, ansattTom)
}
