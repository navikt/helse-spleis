package no.nav.helse.dto.deserialisering

import java.util.UUID
import no.nav.helse.dto.serialisering.FeriepengeUtDto
import no.nav.helse.dto.InntektshistorikkDto
import no.nav.helse.dto.RefusjonshistorikkDto
import no.nav.helse.dto.SykdomshistorikkDto
import no.nav.helse.dto.SykmeldingsperioderDto

data class ArbeidsgiverInnDto(
    val id: UUID,
    val organisasjonsnummer: String,
    val inntektshistorikk: InntektshistorikkDto,
    val sykdomshistorikk: SykdomshistorikkDto,
    val sykmeldingsperioder: SykmeldingsperioderDto,
    val vedtaksperioder: List<VedtaksperiodeInnDto>,
    val forkastede: List<ForkastetVedtaksperiodeInnDto>,
    val utbetalinger: List<UtbetalingInnDto>,
    val feriepengeutbetalinger: List<FeriepengeInnDto>,
    val refusjonshistorikk: RefusjonshistorikkDto
)