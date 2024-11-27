package no.nav.helse.dto.serialisering

import no.nav.helse.dto.RefusjonsservitørDto
import no.nav.helse.dto.SykdomshistorikkDto
import no.nav.helse.dto.SykmeldingsperioderDto
import java.util.UUID

data class ArbeidsgiverUtDto(
    val id: UUID,
    val organisasjonsnummer: String,
    val inntektshistorikk: InntektshistorikkUtDto,
    val sykdomshistorikk: SykdomshistorikkDto,
    val sykmeldingsperioder: SykmeldingsperioderDto,
    val vedtaksperioder: List<VedtaksperiodeUtDto>,
    val forkastede: List<ForkastetVedtaksperiodeUtDto>,
    val utbetalinger: List<UtbetalingUtDto>,
    val feriepengeutbetalinger: List<FeriepengeUtDto>,
    val refusjonshistorikk: RefusjonshistorikkUtDto,
    val ubrukteRefusjonsopplysninger: RefusjonsservitørDto,
)
