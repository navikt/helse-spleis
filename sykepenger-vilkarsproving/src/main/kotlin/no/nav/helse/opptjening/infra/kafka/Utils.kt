package no.nav.helse.opptjening.infra.kafka

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = UUID.fromString(this.asText())
