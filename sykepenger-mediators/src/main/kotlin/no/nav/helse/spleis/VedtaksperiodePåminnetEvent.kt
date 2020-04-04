package no.nav.helse.spleis

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.rapids_rivers.JsonMessage

internal fun Påminnelse.toJson() = JsonMessage.newMessage(mapOf(
    "@event_name" to "vedtaksperiode_påminnet",
    "aktørId" to this.aktørId(),
    "fødselsnummer" to this.fødselsnummer(),
    "organisasjonsnummer" to this.organisasjonsnummer(),
    "vedtaksperiodeId" to this.vedtaksperiodeId,
    "tilstand" to this.tilstand(),
    "antallGangerPåminnet" to this.antallGangerPåminnet(),
    "tilstandsendringstidspunkt" to this.tilstandsendringstidspunkt(),
    "påminnelsestidspunkt" to this.påminnelsestidspunkt(),
    "nestePåminnelsestidspunkt" to this.nestePåminnelsestidspunkt()
)).toJson()
