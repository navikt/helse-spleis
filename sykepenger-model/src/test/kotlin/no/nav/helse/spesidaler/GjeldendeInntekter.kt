package no.nav.helse.spesidaler

import no.nav.helse.spesidaler.Periode.Companion.trim

internal data class Beløpsperiode(
    val periode: Periode,
    val beløp: Beløp
)

internal class GjeldendeInntekter(
    personidentifikator: Personidentifikator,
    periode: Periode,
    dao: InntektDao
) {
    val referanse = dao.sisteReferanse(personidentifikator) ?: Referanse(0)
    val inntekter = dao.inntekter(personidentifikator, periode)
        // Grupperer på inntektskilden
        .groupBy { it.kilde }
        // Sorterer på referansen slik at vi ser på det nyeste og går bakover i tid
        .mapValues { (_, registrerteInntekter) -> registrerteInntekter.sortedByDescending { it.referanse.id } }
        // Beholder kun det som overlapper med perioden vi er interessert i
        .mapValues { (_, registrerteInntekter) ->
            registrerteInntekter.mapNotNull { registrertInntekt ->
                val overlapp = Periode(
                    fom = registrertInntekt.fom,
                    tom = registrertInntekt.tom ?: maxOf(registrertInntekt.fom, periode.endInclusive
                )).overlappendePeriode(periode) ?: return@mapNotNull null
                Beløpsperiode(overlapp, registrertInntekt.beløp)
            }
        }
        // Tar utgangspunkt i det nyeste og legger kun til perioder vi til en hver tid ikke kjenner til fra før
        .mapValues { (_, beløpsperioder) ->
            beløpsperioder.fold(listOf<Beløpsperiode>()) { sammenslått, aktuell ->
                val nytt = sammenslått.map { it.periode }.sortedBy { it.start }.trim(aktuell.periode)
                sammenslått + nytt.map { Beløpsperiode(it, aktuell.beløp) }
            }
        }
        // Og til slutt fjerner vi alt som er 0'et ut
        .mapValues { (_, beløpsperioder) ->
            beløpsperioder.filter { it.beløp.ører > 0 }
        }
}
