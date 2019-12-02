package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

fun JsonNode?.safelyUnwrapDate(): LocalDate? =
    this?.takeUnless { this.isNull }
        ?.let {
            LocalDate.parse(this.textValue())
        }
