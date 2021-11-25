package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import org.slf4j.LoggerFactory

internal class Refusjonsgjødsler(
    private val tidslinje: Utbetalingstidslinje,
    private val refusjonshistorikk: Refusjonshistorikk,
    private val infotrygdhistorikk: Infotrygdhistorikk,
    private val organisasjonsnummer: String
) {
    internal fun gjødsle(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        tidslinje.sammenhengendeUtbetalingsperioder().forEach { utbetalingsperiode ->
            val refusjon = refusjonshistorikk.finnRefusjon(utbetalingsperiode.periode(), aktivitetslogg)
            if (utbetalingsperiode.periode().overlapperMed(periode)) {
                if (refusjon == null) håndterManglendeRefusjon(utbetalingsperiode, aktivitetslogg)
                else if (refusjon.erFørFørsteDagIArbeidsgiverperioden(utbetalingsperiode.periode().start)) {
                    aktivitetslogg.info("Refusjon gjelder ikke for hele utbetalingsperioden")
                    sikkerLogg.info("Refusjon gjelder ikke for hele utbetalingsperioden. Meldingsreferanse:${refusjon.meldingsreferanseId}, Utbetalingsperiode:${utbetalingsperiode.periode()}")
                }
            }

            utbetalingsperiode.forEach { utbetalingsdag ->
                if (refusjon == null) utbetalingsdag.økonomi.settFullArbeidsgiverRefusjon()
                else utbetalingsdag.økonomi.arbeidsgiverRefusjon(refusjon.beløp(utbetalingsdag.dato))
            }
        }
    }

    private fun håndterManglendeRefusjon(utbetalingsperiode: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg) {
        if (infotrygdhistorikk.harBrukerutbetalingerFor(organisasjonsnummer, utbetalingsperiode.periode())) {
            aktivitetslogg.error("Finner ikke informasjon om refusjon i inntektsmelding og personen har brukerutbetaling")
        } else {
            aktivitetslogg.warn("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.")
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
