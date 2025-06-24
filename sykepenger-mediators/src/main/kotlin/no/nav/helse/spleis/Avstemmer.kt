package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto

class Avstemmer(person: PersonUtDto) {
    private companion object {
        private val mapper = jacksonObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val melding = mapTilMelding(person)

    //TODO: Fjern når vi har kjørt avstmming Speilvendt vil ha
    fun getMaxOpprettetBehandling(): LocalDateTime {
        return melding.arbeidsgivere.flatMap { it.vedtaksperioder }
            .flatMap { it.behandlinger }.maxOf { it.behandlingOpprettet }
    }
    fun tilJsonMessage() = JsonMessage.newMessage("person_avstemt", mapper.convertValue(melding))

    private fun mapTilMelding(person: PersonUtDto): AvstemmerDto {
        return AvstemmerDto(
            fødselsnummer = person.fødselsnummer,
            arbeidsgivere = person.arbeidsgivere.map { arbeidsgiver ->
                AvstemtArbeidsgiver(
                    organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                    vedtaksperioder = arbeidsgiver.vedtaksperioder.map { mapTilVedtaksperiode(it) },
                    forkastedeVedtaksperioder = arbeidsgiver.forkastede.map { mapTilVedtaksperiode(it.vedtaksperiode) },
                    utbetalinger = arbeidsgiver.utbetalinger.map { utbetaling ->
                        AvstemtUtbetaling(
                            id = utbetaling.id,
                            type = utbetaling.type.toString(),
                            status = utbetaling.tilstand.toString(),
                            opprettet = utbetaling.tidsstempel,
                            oppdatert = utbetaling.oppdatert,
                            avsluttet = utbetaling.avsluttet,
                            vurdering = utbetaling.vurdering?.let { vurdering ->
                                AvstemtVurdering(
                                    ident = vurdering.ident,
                                    tidspunkt = vurdering.tidspunkt,
                                    automatiskBehandling = vurdering.automatiskBehandling,
                                    godkjent = vurdering.godkjent
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private fun mapTilVedtaksperiode(vedtaksperiode: VedtaksperiodeUtDto): AvstemtVedtaksperiode {
        return AvstemtVedtaksperiode(
            id = vedtaksperiode.id,
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
            skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
            tilstand = vedtaksperiode.tilstand.toString(),
            opprettet = vedtaksperiode.opprettet,
            oppdatert = vedtaksperiode.oppdatert,
            utbetalinger = vedtaksperiode.behandlinger.behandlinger.flatMap { generasjon ->
                generasjon.endringer
                    .filterNot { endring -> endring.utbetalingstatus === UtbetalingTilstandDto.FORKASTET }
                    .mapNotNull { endring -> endring.utbetalingId }
            },
            behandlinger = vedtaksperiode.behandlinger.behandlinger.map { generasjon ->
                AvstemtBehandling(
                    behandlingId = generasjon.id,
                    behandlingOpprettet = generasjon.endringer.first().tidsstempel
                )
            }
        )
    }
}

data class AvstemmerDto(
    val fødselsnummer: String,
    val arbeidsgivere: List<AvstemtArbeidsgiver>
)

data class AvstemtBehandling(
    val behandlingId: UUID,
    val behandlingOpprettet: LocalDateTime
)

data class AvstemtArbeidsgiver(
    val organisasjonsnummer: String,
    val vedtaksperioder: List<AvstemtVedtaksperiode>,
    val forkastedeVedtaksperioder: List<AvstemtVedtaksperiode>,
    val utbetalinger: List<AvstemtUtbetaling>
)

data class AvstemtVedtaksperiode(
    val id: UUID,
    val tilstand: String,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val utbetalinger: List<UUID>,
    val behandlinger: List<AvstemtBehandling>
)

data class AvstemtUtbetaling(
    val id: UUID,
    val type: String,
    val status: String,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val avsluttet: LocalDateTime?,
    val vurdering: AvstemtVurdering?
)

data class AvstemtVurdering(
    val ident: String,
    val tidspunkt: LocalDateTime,
    val automatiskBehandling: Boolean,
    val godkjent: Boolean
)
