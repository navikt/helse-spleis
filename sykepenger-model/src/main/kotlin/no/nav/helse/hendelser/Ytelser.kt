package no.nav.helse.hendelser


import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning

class Ytelser(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    private val vedtaksperiodeId: String,
    private val foreldrepenger: Foreldrepenger,
    private val svangerskapspenger: Svangerskapspenger,
    private val pleiepenger: Pleiepenger,
    private val omsorgspenger: Omsorgspenger,
    private val opplæringspenger: Opplæringspenger,
    private val institusjonsopphold: Institusjonsopphold,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger,
    private val dagpenger: Dagpenger,
    private val inntekterForBeregning: InntekterForBeregning,
    private val selvstendigForsikring: SelvstendigForsikring?
) : Hendelse {
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

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

        return !aktivitetslogg.harFunksjonelleFeil()
    }

    internal fun selvstendigForsikring(): SelvstendigForsikring? = selvstendigForsikring

    internal fun inntektsendringer(): Map<Arbeidsgiverberegning.Yrkesaktivitet, Beløpstidslinje> {
        val kilde = Kilde(metadata.meldingsreferanseId, SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
        return inntekterForBeregning.inntektsperioder
            .groupBy { it.inntektskilde }
            .mapKeys { (inntektskilde, _) -> when (inntektskilde) {
                "SELVSTENDIG" -> Arbeidsgiverberegning.Yrkesaktivitet.Selvstendig
                "FRILANS" -> Arbeidsgiverberegning.Yrkesaktivitet.Frilans
                else -> Arbeidsgiverberegning.Yrkesaktivitet.Arbeidstaker(inntektskilde)
            } }
            .mapValues { (_, inntektsperioder) ->
                inntektsperioder.filterIsInstance<InntekterForBeregning.Inntektsperiode.Beløp>().fold(Beløpstidslinje()) { resultat, inntektsperiode ->
                    resultat + Beløpstidslinje.fra(inntektsperiode.periode, inntektsperiode.beløp, kilde)
                }
            }
    }
}

class GradertPeriode(internal val periode: Periode, internal val grad: Int)
