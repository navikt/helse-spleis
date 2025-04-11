package no.nav.helse.hendelser

import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

class Foreldrepenger(private val foreldrepengeytelse: List<GradertPeriode>) {
    private val perioder get() = foreldrepengeytelse.map { it.periode }

    internal fun valider(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode, erForlengelse: Boolean) {
        if (foreldrepengeytelse.isEmpty()) return aktivitetslogg.info("Bruker har ingen foreldrepenger")
        varselHvisOverlapperMedForeldrepenger(aktivitetslogg, erForlengelse, sykdomsperiode)
        varselHvisForlengerForeldrepengerMerEnn14Dager(aktivitetslogg, sykdomsperiode)
    }

    private fun varselHvisOverlapperMedForeldrepenger(aktivitetslogg: IAktivitetslogg, erForlengelse: Boolean, sykdomsperiode: Periode) {
        val overlappsperiode = if (erForlengelse) sykdomsperiode else sykdomsperiode.familieYtelserPeriode
        val overlapperMedForeldrepenger = perioder.any { ytelse -> ytelse.overlapperMed(overlappsperiode) }
        if (overlapperMedForeldrepenger) {
            aktivitetslogg.varsel(Varselkode.`Overlapper med foreldrepenger`)
        } else {
            aktivitetslogg.info("Bruker har foreldrepenger, men det slår ikke ut på overlappsjekken")
        }
    }

    private fun varselHvisForlengerForeldrepengerMerEnn14Dager(aktivitetslogg: IAktivitetslogg, sykdomsperiode: Periode) {
        val foreldrepengeperiodeFør = foreldrepengeytelse.lastOrNull { it.periode.endInclusive < sykdomsperiode.start } ?: return
        val harForeldrepengerAlleDager = foreldrepengeperiodeFør.periode.erRettFør(sykdomsperiode) && foreldrepengeperiodeFør.periode.count() > 14 && foreldrepengeperiodeFør.grad == 100
        if (!harForeldrepengerAlleDager) return
        aktivitetslogg.varsel(Varselkode.`Forlenger foreldrepenger med mer enn 14 dager`)
    }

    internal fun perioder(): List<Periode> {
        return foreldrepengeytelse.map { it.periode }
    }
}
