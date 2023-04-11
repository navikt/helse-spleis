package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import no.nav.helse.serde.api.dto.UtbetalingsdagDTO
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.ØkonomiVisitor

class UtbetalingsdagDTOBuilder(økonomi: Økonomi, private val type: UtbetalingstidslinjedagType, private val dato: LocalDate): ØkonomiVisitor {
    private var grad: Double? = null
    private var arbeidsgiverRefusjonsbeløp: Int? = null
    private var dekningsgrunnlag: Int? = null
    private var totalGrad: Double? = null
    private var aktuellDagsinntekt: Int? = null
    private var arbeidsgiverbeløp: Int? = null
    private var personbeløp: Int? = null
    private var er6GBegrenset: Boolean? = null
    init {
        økonomi.accept(this)
    }
    override fun visitAvrundetØkonomi(
        grad: Int,
        arbeidsgiverRefusjonsbeløp: Int,
        dekningsgrunnlag: Int,
        totalGrad: Int,
        aktuellDagsinntekt: Int,
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?,
        er6GBegrenset: Boolean?
    ) {
        this.arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp
        this.dekningsgrunnlag = dekningsgrunnlag
        this.aktuellDagsinntekt = aktuellDagsinntekt
        this.arbeidsgiverbeløp = arbeidsgiverbeløp
        this.personbeløp = personbeløp
        this.er6GBegrenset = er6GBegrenset
    }

    override fun visitØkonomi(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        dekningsgrunnlag: Double,
        totalGrad: Double,
        aktuellDagsinntekt: Double,
        arbeidsgiverbeløp: Double?,
        personbeløp: Double?,
        er6GBegrenset: Boolean?
    ) {
        this.grad = grad
        this.totalGrad = totalGrad
    }

    fun build(): UtbetalingsdagDTO = UtbetalingsdagDTO(
        type = type,
        inntekt = aktuellDagsinntekt!!,
        dato = dato,
        utbetaling = arbeidsgiverbeløp!!,
        arbeidsgiverbeløp = arbeidsgiverbeløp!!,
        personbeløp = personbeløp!!,
        refusjonsbeløp = arbeidsgiverRefusjonsbeløp,
        grad = grad!!,
        totalGrad = totalGrad
    )
}