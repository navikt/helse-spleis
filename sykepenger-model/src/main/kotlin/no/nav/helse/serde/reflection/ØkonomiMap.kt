package no.nav.helse.serde.reflection

import no.nav.helse.økonomi.Økonomi

internal fun serialiserØkonomi(økonomi: Økonomi) =
    mutableMapOf<String, Any>().also { map ->
        økonomi.medData { grad,
                          arbeidsgiverBetalingProsent,
                          dekningsgrunnlag,
                          skjæringstidspunkt,
                          totalGrad,
                          aktuellDagsinntekt,
                          arbeidsgiverbeløp,
                          personbeløp,
                          er6GBegrenset ->
            map["grad"] = grad
            map["arbeidsgiverBetalingProsent"] = arbeidsgiverBetalingProsent
            map.compute("skjæringstidspunkt") { _, _ -> skjæringstidspunkt }
            map.compute("totalGrad") { _, _ -> totalGrad }
            map.compute("dekningsgrunnlag") { _, _ -> dekningsgrunnlag }
            map.compute("aktuellDagsinntekt") { _, _ -> aktuellDagsinntekt }
            map.compute("arbeidsgiverbeløp") { _, _ -> arbeidsgiverbeløp }
            map.compute("personbeløp") { _, _ -> personbeløp }
            map.compute("er6GBegrenset") { _, _ -> er6GBegrenset }
        }
    }
