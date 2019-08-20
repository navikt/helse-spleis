package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

fun JsonNode.safelyUnwrapDate(): LocalDate? {
    return if (isNull) {
        null
    } else {
        LocalDate.parse(textValue())
    }
}
