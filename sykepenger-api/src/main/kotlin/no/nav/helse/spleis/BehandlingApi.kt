package no.nav.helse.spleis

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.deserialisering.BehandlingInnDto
import no.nav.helse.person.EventSubscription.Utbetalingsdag
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype
import no.nav.helse.person.EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO
import no.nav.helse.person.builders.utbetalingsdagerSomIGodkjenningsbehov
import no.nav.helse.spleis.dao.PersonDao
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt

private val fødselsnummerRegex = "\\d{11}".toRegex()

internal fun Application.behandlingApi(personDao: PersonDao) {
    routing {
        authenticate {
            post("/api/behandling") {
                val request = call.receive<BehandlingRequest>()
                val fødselsnummer =
                    request.fødselsnummer?.takeIf { it.matches(fødselsnummerRegex) }
                        ?: throw BadRequestException("fødselsnummer må være 11 siffer")
                val behandlingId =
                    request.behandlingId
                        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                        ?: throw BadRequestException("behandlingId må være en UUID")

                withContext(Dispatchers.IO) {
                    val serialisertPerson =
                        personDao.hentPersonFraFnr(fødselsnummer.toLong())
                            ?: throw NotFoundException("Fant ikke person")
                    val behandling =
                        serialisertPerson
                            .tilPersonDto()
                            .arbeidsgivere
                            .firstNotNullOfOrNull { it.finnBehandling(behandlingId) }
                            ?: throw NotFoundException("Fant ikke behandling")
                    call.respond(behandling)
                }
            }
        }
    }
}

private fun ArbeidsgiverInnDto.finnBehandling(behandlingId: UUID): BehandlingResponse? {
    val alleVedtaksperioder = vedtaksperioder + forkastede.map { it.vedtaksperiode }
    return alleVedtaksperioder.firstNotNullOfOrNull { vedtaksperiode ->
        vedtaksperiode.behandlinger.behandlinger
            .find { it.id == behandlingId }
            ?.tilResponse(
                vedtaksperiodeId = vedtaksperiode.id,
                organisasjonsnummer = organisasjonsnummer,
                yrkesaktivitetstype = yrkesaktivitetstype.name,
            )
    }
}

private fun BehandlingInnDto.tilResponse(
    vedtaksperiodeId: UUID,
    organisasjonsnummer: String,
    yrkesaktivitetstype: String,
): BehandlingResponse {
    val sisteEndring = endringer.last()
    val utbetalingsdager = utbetalingsdagerSomIGodkjenningsbehov(sisteEndring.sykdomstidslinje, sisteEndring.utbetalingstidslinje)
    return BehandlingResponse(
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = id,
        organisasjonsnummer = organisasjonsnummer,
        yrkesaktivitetstype = yrkesaktivitetstype,
        utbetalingstidslinje = utbetalingsdager.map { it.tilResponse(sisteEndring.refusjonstidslinje) },
    )
}

private fun Utbetalingsdag.tilResponse(refusjonstidslinje: BeløpstidslinjeDto): UtbetalingsdagResponse {
    val refusjon = refusjonstidslinje.perioder.firstOrNull { dato in it.fom..it.tom }
    return UtbetalingsdagResponse(
        dato = dato,
        type = type.tilResponse(),
        beløpTilArbeidsgiver = beløpTilArbeidsgiver,
        beløpTilBruker = beløpTilBruker,
        sykdomsgrad = sykdomsgrad,
        dekningsgrad = dekningsgrad,
        begrunnelser = begrunnelser?.map { it.tilResponse() } ?: emptyList(),
        forespurtRefusjon =
            refusjon?.let {
                ForespurtRefusjon(
                    beløp = it.dagligBeløp.roundToInt(),
                    avsender =
                        when (it.kilde.avsender) {
                            AvsenderDto.ARBEIDSGIVER -> "ARBEIDSGIVER"
                            AvsenderDto.SAKSBEHANDLER -> "SAKSBEHANDLER"
                            AvsenderDto.SYKMELDT -> "SYKMELDT"
                            AvsenderDto.SYSTEM -> "SYSTEM"
                        },
                    sasInternInntektsmeldingReferanse =
                        if (it.kilde.avsender ==
                            AvsenderDto.ARBEIDSGIVER
                        ) {
                            it.kilde.meldingsreferanseId.id
                        } else {
                            null
                        },
                )
            },
    )
}

private fun Dagtype.tilResponse() =
    when (this) {
        Dagtype.ArbeidsgiverperiodeDag -> "ArbeidsgiverperiodeDag"
        Dagtype.NavDag -> "NavDag"
        Dagtype.NavHelgDag -> "NavHelgDag"
        Dagtype.Arbeidsdag -> "Arbeidsdag"
        Dagtype.Fridag -> "Fridag"
        Dagtype.AvvistDag -> "AvvistDag"
        Dagtype.UkjentDag -> "UkjentDag"
        Dagtype.ForeldetDag -> "ForeldetDag"
        Dagtype.Permisjonsdag -> "Permisjonsdag"
        Dagtype.Feriedag -> "Feriedag"
        Dagtype.ArbeidIkkeGjenopptattDag -> "ArbeidIkkeGjenopptattDag"
        Dagtype.AndreYtelser -> "AndreYtelser"
        Dagtype.Ventetidsdag -> "Ventetidsdag"
    }

private fun EksternBegrunnelseDTO.tilResponse() =
    when (this) {
        EksternBegrunnelseDTO.SykepengedagerOppbrukt -> "SykepengedagerOppbrukt"
        EksternBegrunnelseDTO.SykepengedagerOppbruktOver67 -> "SykepengedagerOppbruktOver67"
        EksternBegrunnelseDTO.MinimumInntekt -> "MinimumInntekt"
        EksternBegrunnelseDTO.MinimumInntektOver67 -> "MinimumInntektOver67"
        EksternBegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> "EgenmeldingUtenforArbeidsgiverperiode"
        EksternBegrunnelseDTO.MeldingTilNavDagUtenforVentetid -> "MeldingTilNavDagUtenforVentetid"
        EksternBegrunnelseDTO.AvslåttMeldingTilNavDag -> "AvslåttMeldingTilNavDag"
        EksternBegrunnelseDTO.AndreYtelserAap -> "AndreYtelserAap"
        EksternBegrunnelseDTO.AndreYtelserDagpenger -> "AndreYtelserDagpenger"
        EksternBegrunnelseDTO.AndreYtelserForeldrepenger -> "AndreYtelserForeldrepenger"
        EksternBegrunnelseDTO.AndreYtelserOmsorgspenger -> "AndreYtelserOmsorgspenger"
        EksternBegrunnelseDTO.AndreYtelserOpplaringspenger -> "AndreYtelserOpplaringspenger"
        EksternBegrunnelseDTO.AndreYtelserPleiepenger -> "AndreYtelserPleiepenger"
        EksternBegrunnelseDTO.AndreYtelserSvangerskapspenger -> "AndreYtelserSvangerskapspenger"
        EksternBegrunnelseDTO.MinimumSykdomsgrad -> "MinimumSykdomsgrad"
        EksternBegrunnelseDTO.EtterDødsdato -> "EtterDødsdato"
        EksternBegrunnelseDTO.ManglerMedlemskap -> "ManglerMedlemskap"
        EksternBegrunnelseDTO.ManglerOpptjening -> "ManglerOpptjening"
        EksternBegrunnelseDTO.Over70 -> "Over70"
    }

private fun AvsenderDto.tilResponse() =
    when (this) {
        AvsenderDto.ARBEIDSGIVER -> "ARBEIDSGIVER"
        AvsenderDto.SAKSBEHANDLER -> "SAKSBEHANDLER"
        AvsenderDto.SYKMELDT -> "SYKMELDT"
        AvsenderDto.SYSTEM -> "SYSTEM"
    }

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BehandlingRequest(
    val fødselsnummer: String? = null,
    val behandlingId: String? = null,
)

private data class BehandlingResponse(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val organisasjonsnummer: String,
    val yrkesaktivitetstype: String,
    val utbetalingstidslinje: List<UtbetalingsdagResponse>,
)

private data class UtbetalingsdagResponse(
    val dato: LocalDate,
    val type: String,
    val beløpTilArbeidsgiver: Int,
    val beløpTilBruker: Int,
    val sykdomsgrad: Int,
    val dekningsgrad: Int,
    val begrunnelser: List<String>,
    val forespurtRefusjon: ForespurtRefusjon?,
)

private data class ForespurtRefusjon(
    val beløp: Int,
    val avsender: String,
    val sasInternInntektsmeldingReferanse: UUID?, // kun satt hvis avsender er ARBEIDSGIVER, ellers null
)
