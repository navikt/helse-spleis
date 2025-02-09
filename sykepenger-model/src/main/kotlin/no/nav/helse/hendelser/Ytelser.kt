package no.nav.helse.hendelser


import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_9
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge

class Ytelser(
    meldingsreferanseId: UUID,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val foreldrepenger: Foreldrepenger,
    private val svangerskapspenger: Svangerskapspenger,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = organisasjonsnummer
    )
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    private val YTELSER_SOM_KAN_OPPDATERE_HISTORIKK: List<AnnenYtelseSomKanOppdatereHistorikk> = listOf(
        foreldrepenger
    )
    private val sykdomstidslinjer = YTELSER_SOM_KAN_OPPDATERE_HISTORIKK
        .map { it to it.sykdomstidslinje(metadata.meldingsreferanseId, metadata.registrert) }

    companion object {
        internal val Periode.familieYtelserPeriode get() = oppdaterFom(start.minusWeeks(4))
    }

    internal fun erRelevant(other: UUID) = other.toString() == vedtaksperiodeId

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        maksdato: LocalDate,
        harTilkomneInntekter: Boolean,
        erForlengelse: Boolean
    ): Boolean {
        if (periode.start > maksdato) return true

        if (harTilkomneInntekter && !andreYtelserPerioder().erTom()) aktivitetslogg.varsel(RV_IV_9)

        val periodeForOverlappsjekk = periode.start til minOf(periode.endInclusive, maksdato)
        arbeidsavklaringspenger.valider(aktivitetslogg, skjæringstidspunkt, periodeForOverlappsjekk)
        dagpenger.valider(aktivitetslogg, skjæringstidspunkt, periodeForOverlappsjekk)
        foreldrepenger.valider(aktivitetslogg, periodeForOverlappsjekk, erForlengelse)
        if (svangerskapspenger.overlapper(aktivitetslogg, periodeForOverlappsjekk, erForlengelse)) aktivitetslogg.varsel(Varselkode.`Overlapper med svangerskapspenger`)
        if (pleiepenger.overlapper(aktivitetslogg, periodeForOverlappsjekk, erForlengelse)) aktivitetslogg.varsel(Varselkode.`Overlapper med pleiepenger`)
        if (omsorgspenger.overlapper(aktivitetslogg, periodeForOverlappsjekk, erForlengelse)) aktivitetslogg.varsel(Varselkode.`Overlapper med omsorgspenger`)
        if (opplæringspenger.overlapper(aktivitetslogg, periodeForOverlappsjekk, erForlengelse)) aktivitetslogg.varsel(Varselkode.`Overlapper med opplæringspenger`)
        if (institusjonsopphold.overlapper(aktivitetslogg, periodeForOverlappsjekk)) aktivitetslogg.funksjonellFeil(Varselkode.`Overlapper med institusjonsopphold`)

        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun sykdomstidslinje(
        aktivitetslogg: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        periodeRettEtter: Periode?
    ): Sykdomstidslinje? {
        val result = sykdomstidslinjer.mapNotNull { (ytelse, tidslinje) ->
            tidslinje.takeIf { ytelse.skalOppdatereHistorikk(aktivitetslogg, ytelse, periode, skjæringstidspunkt, periodeRettEtter) }
        }
        return if (result.isEmpty()) null else result.merge()
    }

    private fun andreYtelserPerioder(): AndreYtelserPerioder {
        val foreldrepenger = foreldrepenger.perioder()
        val svangerskapspenger = svangerskapspenger.perioder()
        val pleiepenger = pleiepenger.perioder()
        val omsorgspenger = omsorgspenger.perioder()
        val opplæringspenger = opplæringspenger.perioder()
        val arbeidsavklaringspenger = arbeidsavklaringspenger.perioder
        val dagpenger = dagpenger.perioder
        return AndreYtelserPerioder(
            foreldrepenger = foreldrepenger,
            svangerskapspenger = svangerskapspenger,
            pleiepenger = pleiepenger,
            dagpenger = dagpenger,
            arbeidsavklaringspenger = arbeidsavklaringspenger,
            opplæringspenger = opplæringspenger,
            omsorgspenger = omsorgspenger
        )
    }
}

class GradertPeriode(internal val periode: Periode, internal val grad: Int)

data class AndreYtelserPerioder(
    val foreldrepenger: List<Periode>,
    val svangerskapspenger: List<Periode>,
    val pleiepenger: List<Periode>,
    val dagpenger: List<Periode>,
    val arbeidsavklaringspenger: List<Periode>,
    val opplæringspenger: List<Periode>,
    val omsorgspenger: List<Periode>
) {
    internal fun erTom(): Boolean {
        if (foreldrepenger.isNotEmpty()) return false
        if (svangerskapspenger.isNotEmpty()) return false
        if (pleiepenger.isNotEmpty()) return false
        if (dagpenger.isNotEmpty()) return false
        if (arbeidsavklaringspenger.isNotEmpty()) return false
        if (opplæringspenger.isNotEmpty()) return false
        if (omsorgspenger.isNotEmpty()) return false
        return true
    }
}
