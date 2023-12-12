package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import no.nav.helse.serde.api.dto.UtbetalingsdagDTO
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.ØkonomiVisitor

class UtbetalingsdagDTOBuilder(økonomi: Økonomi, private val type: UtbetalingstidslinjedagType, private val dato: LocalDate): ØkonomiVisitor {
    // Økonomi runder alltid ned grader, når den går fra flyttall til heltall, siden vi trenger at 19.X er 19
    // _men_ på grunn av at 0.2 er et magisk vanskelig tall å fremstille i en datamaskin risikerer vi også at
    // vi av og til regner ut en totalgrad på 19 komma veldig mange ni-tall, og likevel godkjenner dagen¨
    // disse dagene må vise totalgrad 20. Her er vi i en utbetalingsdag, så da vet vi at vi må ha minst 20 % totalgrad.
    private val totalGrad = økonomi.brukTotalGrad { totalGrad -> totalGrad }.coerceAtLeast(20)
    private var arbeidsgiverbeløp: Int? = null
    private var personbeløp: Int? = null
    init {
        økonomi.accept(this)
    }
    override fun visitAvrundetØkonomi(
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?
    ) {
        this.arbeidsgiverbeløp = arbeidsgiverbeløp
        this.personbeløp = personbeløp
    }

    fun build(): UtbetalingsdagDTO = UtbetalingsdagDTO(
        type = type,
        dato = dato,
        arbeidsgiverbeløp = arbeidsgiverbeløp!!,
        personbeløp = personbeløp!!,
        totalGrad = totalGrad
    )
}