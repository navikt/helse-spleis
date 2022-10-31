package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
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

    init {
        validerRefusjonsopplysning(tom)
    }

    private fun validerRefusjonsopplysning(tom: LocalDate?) {
        tom?.let {
            fom til tom
        }
    }

    private val periode get() = fom til tom!!
    private fun oppdatertFom(nyFom: LocalDate) = if (tom != null && nyFom > tom) null else Refusjonsopplysning(meldingsreferanseId, nyFom, tom, beløp)
    private fun oppdatertTom(nyTom: LocalDate) = if (nyTom < fom) null else Refusjonsopplysning(meldingsreferanseId, fom, nyTom, beløp)
    private fun erEtter(other: Refusjonsopplysning) = other.tom != null && fom > other.tom

    private fun minus(nyOpplysning: Refusjonsopplysning): List<Refusjonsopplysning> {
        // Om den nye opplysningen ligger etter oss er vi fortsatt gjeldende i vår helhet
        if (nyOpplysning.erEtter(this)) return listOf(this)
        // Om den nye opplysningen ikke har tom er det kun en eventuell snute som fortsatt er gjeldende
        if (nyOpplysning.tom == null) return listOfNotNull(oppdatertTom(nyOpplysning.fom.forrigeDag))
        // Om vi ikke har tom er det eventuelt en del av snuten vår som blir kortere, ellers fortsatt gjeldende
        if (tom == null) return listOfNotNull(oppdatertFom(nyOpplysning.tom.nesteDag))

        // Finner den overlappende perioden som den nye opplysningen skal erstatte. Om det ikke noe overlapp returnerer vi oss selv
        val overlapp = periode.overlappendePeriode(nyOpplysning.periode)?: return listOf(this)

        // Finner den eventuelle delen foran & bak den nye opplysningen som fortsatt er gjeldende
        val snute = oppdatertTom(overlapp.start.forrigeDag)
        val hale = oppdatertFom(overlapp.endInclusive.nesteDag)
        return listOfNotNull(snute, hale)
    }
    internal companion object {
        private fun Periode.overlappendePeriode(other: Periode) =
            intersect(other).takeUnless { it.isEmpty() }?.let { Periode(it.min(), it.max()) }

        private fun List<Refusjonsopplysning>.merge(nyeOpplysninger: List<Refusjonsopplysning>): List<Refusjonsopplysning> {
            return nyeOpplysninger.fold(this, ::mergeNyOpplysning).sortedBy { it.fom }
        }

        private fun mergeNyOpplysning(eksisterendeOpplysninger: List<Refusjonsopplysning>, nyOpplysning: Refusjonsopplysning) : List<Refusjonsopplysning> {
            val eksisterendeSomIkkeHarBlittErstattet = mutableListOf<Refusjonsopplysning>()
            eksisterendeOpplysninger.forEach { eksisterendeRefusjonsopplysning ->
                eksisterendeSomIkkeHarBlittErstattet.addAll(eksisterendeRefusjonsopplysning.minus(nyOpplysning))
            }
            return eksisterendeSomIkkeHarBlittErstattet + nyOpplysning
        }
    }

    internal fun accept(visitor: RefusjonsopplysningerVisitor) {
        visitor.visitRefusjonsopplysning(meldingsreferanseId, fom, tom, beløp)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Refusjonsopplysning) return false
        return meldingsreferanseId == other.meldingsreferanseId && fom == other.fom && tom == other.tom && beløp == other.beløp
    }

    override fun toString() = "$fom - $tom, ${beløp.reflection { _, _, dagligDouble, _ -> dagligDouble }} ($meldingsreferanseId)"
    override fun hashCode(): Int {
        var result = meldingsreferanseId.hashCode()
        result = 31 * result + fom.hashCode()
        result = 31 * result + (tom?.hashCode() ?: 0)
        result = 31 * result + beløp.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }

    internal class Refusjonsopplysninger(
        refusjonsopplysninger: List<Refusjonsopplysning>, // TODO: Kan vi anta at refusjonsopplysnigene sendes inn i rekkefølgen som de skal tolkes?
    ) {
        private val validerteRefusjonsopplysninger = validerteRefusjonsopplysninger(refusjonsopplysninger)
        internal constructor(): this(emptyList())

        internal fun accept(visitor: RefusjonsopplysningerVisitor) {
            validerteRefusjonsopplysninger.forEach { it.accept(visitor) }
        }

        private fun validerteRefusjonsopplysninger(refusjonsopplysninger: List<Refusjonsopplysning>): List<Refusjonsopplysning> {
            if (refusjonsopplysninger.isEmpty()) return refusjonsopplysninger
            val (første, resten) = refusjonsopplysninger.first() to refusjonsopplysninger.drop(1)
            return listOf(første).merge(resten)
        }

        internal fun merge(nyeRefusjonsopplysninger: Refusjonsopplysninger): Refusjonsopplysninger {
            return Refusjonsopplysninger(validerteRefusjonsopplysninger.merge(nyeRefusjonsopplysninger.validerteRefusjonsopplysninger))
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Refusjonsopplysninger) return false
            return validerteRefusjonsopplysninger == other.validerteRefusjonsopplysninger
        }

        override fun hashCode() = validerteRefusjonsopplysninger.hashCode()

        override fun toString() = validerteRefusjonsopplysninger.toString()

        internal class RefusjonsopplysningerBuilder {
            private val refusjonsopplysninger = mutableListOf<Pair<LocalDateTime, Refusjonsopplysning>>()
            internal fun leggTil(refusjonsopplysning: Refusjonsopplysning, tidspunkt: LocalDateTime) = apply{
                refusjonsopplysninger.add(Pair(tidspunkt, refusjonsopplysning))
            }

            internal fun build(): Refusjonsopplysninger{
                return Refusjonsopplysninger(refusjonsopplysninger.sortedWith(compareBy({it.first}, {it.second.fom})).map{it.second})
            }
        }
    }
}

interface RefusjonsopplysningerVisitor {
    fun visitRefusjonsopplysning(meldingsreferanseId: UUID, fom: LocalDate, tom: LocalDate?, beløp: Inntekt)
}