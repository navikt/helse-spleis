package no.nav.helse.hendelser


import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.InntekterForBeregning.Builder
import no.nav.helse.person.inntekt.Inntektskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge

class Ytelser(
    meldingsreferanseId: MeldingsreferanseId,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val foreldrepenger: Foreldrepenger,
    private val svangerskapspenger: Svangerskapspenger,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger,
    private val inntekterForBeregning: InntekterForBeregning
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

    private val YTELSER_SOM_KAN_OPPDATERE_HISTORIKK: List<AnnenYtelseSomKanOppdatereHistorikk> = emptyList()
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
        erForlengelse: Boolean
    ): Boolean {
        if (periode.start > maksdato) return true

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

    internal fun inntektsendringer(builder: Builder) {
        inntekterForBeregning.inntektsperioder.forEach { inntektsperiode ->
            builder.inntektsendringer(
                inntektskilde = Inntektskilde(inntektsperiode.inntektskilde),
                fom = inntektsperiode.fom,
                tom = inntektsperiode.tom,
                inntekt = inntektsperiode.inntekt,
                meldingsreferanseId = metadata.meldingsreferanseId
            )
        }
    }
}

class GradertPeriode(internal val periode: Periode, internal val grad: Int)
