package no.nav.helse.spesidaler

import java.time.LocalDate

@JvmInline value class Personidentifikator(val id: String)
@JvmInline value class Kilde(val id: String)
@JvmInline value class Beløp(val ører: Long)
@JvmInline value class Referanse(val id: Long)

data class RegistrertInntekt(
    val referanse: Referanse,
    val kilde: Kilde,
    val beløp: Beløp,
    val fom: LocalDate,
    val tom: LocalDate?
)

data class NyInntekt(
    val kilde: Kilde,
    val beløp: Beløp,
    val fom: LocalDate,
    val tom: LocalDate?
)

internal interface InntektDao {
    fun sisteReferanse(personidentifikator: Personidentifikator): Referanse?
    fun inntekter(personidentifikator: Personidentifikator, periode: Periode): List<RegistrertInntekt>
    fun nyInntekt(personidentifikator: Personidentifikator, nyInntekt: NyInntekt)
}
