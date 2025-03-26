package no.nav.helse

import java.time.LocalDate
import java.time.temporal.ChronoUnit.YEARS
import no.nav.helse.dto.AlderDto

class Alder(val fødselsdato: LocalDate, val dødsdato: LocalDate?) {
    internal val syttiårsdagen: LocalDate = fødselsdato.plusYears(70)
    internal val redusertYtelseAlder: LocalDate = fødselsdato.plusYears(67)

    companion object {
        private const val MINSTEALDER_UTEN_FULLMAKT_FRA_VERGE = 18

        internal fun gjenopprett(dto: AlderDto): Alder {
            return Alder(
                fødselsdato = dto.fødselsdato,
                dødsdato = dto.dødsdato
            )
        }

        val LocalDate.alder get() = Alder(this, null)
    }

    internal fun medDød(dødsdato: LocalDate): Alder {
        if (this.dødsdato != null) return this
        return Alder(this.fødselsdato, dødsdato)
    }

    internal fun innenfor67årsgrense(dato: LocalDate) = dato <= redusertYtelseAlder

    internal fun mistetSykepengerett(dato: LocalDate) = dato >= syttiårsdagen

    internal fun alderPåDato(dato: LocalDate): Int {
        val dagen = if (dødsdato != null) minOf(dødsdato, dato) else dato
        return YEARS.between(fødselsdato, dagen).toInt()
    }

    internal fun forUngForÅSøke(søknadstidspunkt: LocalDate) = alderPåDato(søknadstidspunkt) < MINSTEALDER_UTEN_FULLMAKT_FRA_VERGE

    internal fun dto() = AlderDto(fødselsdato = fødselsdato, dødsdato = dødsdato)
}
