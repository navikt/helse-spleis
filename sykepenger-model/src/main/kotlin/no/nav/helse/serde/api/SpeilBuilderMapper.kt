package no.nav.helse.serde.api

import no.nav.helse.Toggles
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import java.util.*

internal fun MutableMap<String, Any?>.mapTilPersonDto() =
    if (Toggles.SpeilInntekterVol2Enabled.enabled) {
        PersonDTO(
            fødselsnummer = this["fødselsnummer"] as String,
            aktørId = this["aktørId"] as String,
            arbeidsgivere = this["arbeidsgivere"].cast(),
            inntektsgrunnlag = this["inntektsgrunnlag"].cast()
        )
    } else {
        PersonDTO(
            fødselsnummer = this["fødselsnummer"] as String,
            aktørId = this["aktørId"] as String,
            arbeidsgivere = this["arbeidsgivere"].cast(),
            inntektsgrunnlag = emptyList()
        )
    }

internal fun MutableMap<String, Any?>.mapTilArbeidsgiverDto(utbetalingshistorikk: List<UtbetalingshistorikkElementDTO>) = ArbeidsgiverDTO(
    organisasjonsnummer = this["organisasjonsnummer"] as String,
    id = this["id"] as UUID,
    vedtaksperioder = this["vedtaksperioder"].cast(),
    utbetalingshistorikk = utbetalingshistorikk
)

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : Any> Any?.cast() = this as List<T>
