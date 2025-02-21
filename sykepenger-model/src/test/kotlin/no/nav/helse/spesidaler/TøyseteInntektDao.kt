package no.nav.helse.spesidaler

internal class TøyseteInntektDao: InntektDao {
    private data class Rad(
        val referanse: Referanse,
        val personidentifikator: Personidentifikator,
        val inntekt: NyInntekt
    )
    private val inntekter = mutableListOf<Rad>()
    private var referanseteller = 1L
    private fun nesteReferanse() = Referanse(referanseteller++)

    override fun sisteReferanse(personidentifikator: Personidentifikator): Referanse? =
        inntekter
            .filter { it.personidentifikator == personidentifikator }
            .map { it.referanse }
            .maxByOrNull { it.id }

    override fun inntekter(personidentifikator: Personidentifikator, periode: Periode): List<RegistrertInntekt> =
        inntekter
            .filter { it.personidentifikator == personidentifikator }
            .filter { rad ->
                val tom = when (val reelTom = rad.inntekt.tom) {
                    null -> maxOf(rad.inntekt.fom, periode.endInclusive)
                    else -> reelTom
                }
                Periode(rad.inntekt.fom, tom).overlapperMed(periode)
            }
            .map { RegistrertInntekt(
                referanse = it.referanse,
                kilde = it.inntekt.kilde,
                beløp = it.inntekt.beløp,
                fom = it.inntekt.fom,
                tom = it.inntekt.tom
            ) }

    override fun nyInntekt(personidentifikator: Personidentifikator, nyInntekt: NyInntekt) {
        val finnes = inntekter
            .filter { it.personidentifikator == personidentifikator }
            .any { it.inntekt == nyInntekt }
        if (finnes) return
        inntekter.add(Rad(
            referanse = nesteReferanse(),
            personidentifikator = personidentifikator,
            inntekt = nyInntekt
        ))
    }
}
