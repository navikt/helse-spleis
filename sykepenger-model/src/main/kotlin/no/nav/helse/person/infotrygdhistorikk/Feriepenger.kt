package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate

class Feriepenger(
    val orgnummer: String,
    val beløp: Double,
    val fom: LocalDate,
    val tom: LocalDate
    )
