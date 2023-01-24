package no.nav.helse.serde.api.speil

import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import no.nav.helse.økonomi.Inntekt

private val Double.toDesimaler get() = BigDecimal(this).setScale(2, HALF_UP).toDouble()
internal val Inntekt.dagligAvrundet get() = reflection { _, _, daglig, _ -> daglig }.toDesimaler
internal val Inntekt.måndligAvrundet get() = reflection { _, månedlig, _, _ -> månedlig }.toDesimaler
internal val Inntekt.årligAvrundet get() = reflection { årlig, _, _, _ -> årlig }.toDesimaler