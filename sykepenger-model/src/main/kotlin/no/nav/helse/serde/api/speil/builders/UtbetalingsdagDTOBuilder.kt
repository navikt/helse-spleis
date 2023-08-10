package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import no.nav.helse.serde.api.dto.UtbetalingsdagDTO
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.ØkonomiVisitor

class UtbetalingsdagDTOBuilder(økonomi: Økonomi, private val type: UtbetalingstidslinjedagType, private val dato: LocalDate): ØkonomiVisitor {
    private val totalGrad = økonomi.brukTotalGrad { totalGrad -> totalGrad }
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