package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.økonomi.Inntekt

internal class Refusjonsopplysning(
    private val meldingsreferanseId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate?,
    private val beløp: Inntekt
) {
    private val periode get() = fom til tom!!
    private fun oppdatertFom(nyFom: LocalDate) = if (tom != null && nyFom > tom) null else Refusjonsopplysning(meldingsreferanseId, nyFom, tom, beløp)
    private fun oppdatertTom(nyTom: LocalDate) = if (nyTom < fom) null else Refusjonsopplysning(meldingsreferanseId, fom, nyTom, beløp)
    private fun minus(nyOpplysning: Refusjonsopplysning): List<Refusjonsopplysning> {
        // Håndterer først om denne eller den nye opplysningen ikke har tom
        if (nyOpplysning.tom == null) return listOfNotNull(oppdatertTom(nyOpplysning.fom.forrigeDag))
        if (tom == null) return listOfNotNull(oppdatertFom(nyOpplysning.tom.nesteDag))

        // Finner den overlappende perioden som den nye opplysningen skal erstatte. Om det ikke noe overlapp returnerer vi oss selv
        val overlapp = periode.overlappendePeriode(nyOpplysning.periode)?: return listOf(this)

        // Finner den eventuelle delen foran & bak den nye opplysningen som fortsatt er gjeldende
        val snute = oppdatertTom(overlapp.start.forrigeDag)
        val hale = oppdatertFom(overlapp.endInclusive.nesteDag)
        return listOfNotNull(snute, hale)
    }
    internal companion object {
        private fun Periode.overlappendePeriode(other: Periode) = intersect(other).takeUnless { it.isEmpty() }?.let { Periode(it.min(), it.max()) }

        internal fun List<Refusjonsopplysning>.merge(nyeOpplysninger: List<Refusjonsopplysning>) = nyeOpplysninger.fold(this,::mergeNyOpplysning).sortedBy { it.fom }

        private fun mergeNyOpplysning(eksisterendeOpplysninger: List<Refusjonsopplysning>, nyOpplysning: Refusjonsopplysning) : List<Refusjonsopplysning> {
            val eksisterendeSomIkkeHarBlittErstattet = mutableListOf<Refusjonsopplysning>()
            eksisterendeOpplysninger.forEach { eksisterendeRefusjonsopplysning ->
                eksisterendeSomIkkeHarBlittErstattet.addAll(eksisterendeRefusjonsopplysning.minus(nyOpplysning))
            }
            return eksisterendeSomIkkeHarBlittErstattet + nyOpplysning
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Refusjonsopplysning) return false
        return meldingsreferanseId == other.meldingsreferanseId && fom == other.fom && tom == other.tom && beløp == other.beløp
    }

    override fun toString() = "$fom - $tom, $beløp ($meldingsreferanseId)"
    override fun hashCode(): Int {
        var result = meldingsreferanseId.hashCode()
        result = 31 * result + fom.hashCode()
        result = 31 * result + (tom?.hashCode() ?: 0)
        result = 31 * result + beløp.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }
}