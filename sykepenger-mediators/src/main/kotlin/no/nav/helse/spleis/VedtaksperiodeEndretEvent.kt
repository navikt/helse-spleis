package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.toMap
import no.nav.helse.rapids_rivers.JsonMessage

internal fun PersonObserver.VedtaksperiodeEndretTilstandEvent.toJson() = JsonMessage.newMessage(mapOf(
    "@event_name" to "vedtaksperiode_endret",
    "aktørId" to this.aktørId,
    "fødselsnummer" to this.fødselsnummer,
    "organisasjonsnummer" to this.organisasjonsnummer,
    "vedtaksperiodeId" to this.id,
    "gjeldendeTilstand" to this.gjeldendeTilstand,
    "forrigeTilstand" to this.forrigeTilstand,
    "endringstidspunkt" to this.endringstidspunkt,
    "på_grunn_av" to (this.sykdomshendelse::class.simpleName ?: "UKJENT"),
    "aktivitetslogg" to this.aktivitetslogg.toMap(),
    "timeout" to this.timeout.toSeconds(),
    "hendelser" to this.hendelser
)).toJson()


