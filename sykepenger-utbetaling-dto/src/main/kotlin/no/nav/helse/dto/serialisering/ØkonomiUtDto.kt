package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.ProsentdelDto

data class ØkonomiUtDto(
    val grad: ProsentdelDto,
    val totalGrad: ProsentdelDto,
    val arbeidsgiverRefusjonsbeløp: InntektDto,
    val aktuellDagsinntekt: InntektDto,
    val beregningsgrunnlag: InntektDto,
    val dekningsgrunnlag: InntektDto,
    val grunnbeløpgrense: InntektDto?,
    val arbeidsgiverbeløp: InntektDto?,
    val personbeløp: InntektDto?,
    val er6GBegrenset: Boolean?
)