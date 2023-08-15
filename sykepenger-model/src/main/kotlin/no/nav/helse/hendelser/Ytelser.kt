package no.nav.helse.hendelser


import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

class Ytelser(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val foreldrepermisjon: Foreldrepermisjon,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    companion object {
        internal val Periode.familieYtelserPeriode get() = oppdaterFom(start.minusWeeks(4))
    }

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate, maksdato: LocalDate): Boolean {
        if (periode.start > maksdato) return true

        val periodeForOverlappsjekk = periode.start til minOf(periode.endInclusive, maksdato)
        arbeidsavklaringspenger.valider(this, skjæringstidspunkt, periodeForOverlappsjekk)
        dagpenger.valider(this, skjæringstidspunkt, periodeForOverlappsjekk)
        if (foreldrepermisjon.overlapper(this, periodeForOverlappsjekk)) varsel(Varselkode.`Overlapper med foreldrepenger eller svangerskapspenger`)
        if (pleiepenger.overlapper(this, periodeForOverlappsjekk)) varsel(Varselkode.`Overlapper med pleiepenger`)
        if (omsorgspenger.overlapper(this, periodeForOverlappsjekk)) varsel(Varselkode.`Overlapper med omsorgspenger`)
        if (opplæringspenger.overlapper(this, periodeForOverlappsjekk)) varsel(Varselkode.`Overlapper med opplæringspenger`)
        if (institusjonsopphold.overlapper(this, periodeForOverlappsjekk)) funksjonellFeil(Varselkode.`Overlapper med institusjonsopphold`)

        return !harFunksjonelleFeilEllerVerre()
    }
}
