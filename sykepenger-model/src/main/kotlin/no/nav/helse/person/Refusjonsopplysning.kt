package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.til
import no.nav.helse.økonomi.Inntekt

internal class Refusjonsopplysning(
    private val meldingsreferanseId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate?,
    private val beløp: Inntekt
) {
    private val periode = fom til (tom ?: LocalDate.MAX)
    internal companion object {
        internal fun List<Refusjonsopplysning>.merge(nyeOpplysninger: List<Refusjonsopplysning>) = nyeOpplysninger.fold(this,::mergeNyOpplysning)

        private fun mergeNyOpplysning(eksisterndeOpplysninger: List<Refusjonsopplysning>, nyOpplysning: Refusjonsopplysning) : List<Refusjonsopplysning> {
            val (snuter, resten) = eksisterndeOpplysninger.partition { it.fom < nyOpplysning.fom }
            val deFørstSnutene = snuter.dropLast(1)
            val sisteSnute = snuter.lastOrNull()
            val justertSisteSnute = sisteSnute?.let {
                if (!it.periode.overlapperMed(nyOpplysning.periode)) it
                else Refusjonsopplysning(it.meldingsreferanseId, it.fom, nyOpplysning.fom.minusDays(1), it.beløp)
            }
            if (nyOpplysning.tom == null) {
                return deFørstSnutene + listOfNotNull(justertSisteSnute, nyOpplysning)
            }
            val halen = resten.drop(1)
            val haleSisteSnute = sisteSnute?.takeIf { it.periode.slutterEtter(nyOpplysning.tom) }?.let {
                Refusjonsopplysning(it.meldingsreferanseId, nyOpplysning.tom.plusDays(1), it.tom, it.beløp)
            }

            val førsteHale = if (haleSisteSnute == null) resten.firstOrNull()?.let {
                if (!it.periode.overlapperMed(nyOpplysning.periode)) it
                else if (it.tom != null && nyOpplysning.tom.plusDays(1) > it.tom) null
                else Refusjonsopplysning(it.meldingsreferanseId, nyOpplysning.tom.plusDays(1), it.tom, it.beløp)
            } else resten.firstOrNull()
            return deFørstSnutene + listOfNotNull(justertSisteSnute, nyOpplysning, haleSisteSnute, førsteHale) + halen
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