package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.text.uppercase
import no.nav.helse.Tidslinje
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.AnnenInntektskilde
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet
import no.nav.helse.økonomi.Prosentdel

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
    private val selvstendigForsikring: SelvstendigForsikring?,
    private val andreYtelser: AndreYtelser = AndreYtelser(emptyList())
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

    internal fun inntektsendringer(): Map<Arbeidsgiverberegning.Inntektskilde, Beløpstidslinje> {
        val kilde = Kilde(metadata.meldingsreferanseId, SYSTEM, LocalDateTime.now())
        return inntekterForBeregning.inntektsperioder
            .groupBy { it.inntektskilde }
            .mapKeys { (inntektskilde, _) -> inntektskilde.uppercase().let {
                when {
                    it == "SELVSTENDIG" -> Yrkesaktivitet.Selvstendig
                    it == "FRILANS" -> Yrkesaktivitet.Frilans
                    it.matches("\\d{9}".toRegex()) -> Yrkesaktivitet.Arbeidstaker(it)
                    else -> AnnenInntektskilde(it)
                }
            }}
            .mapValues { (_, inntektsperioder) ->
                inntektsperioder.fold(Beløpstidslinje()) { sammenslått, ny ->
                    sammenslått + Beløpstidslinje.fra(ny.periode, ny.beløp, kilde)
                }
            }
    }

    internal fun andreYtelser() = andreYtelser.perioder.fold(AndreYtelserTidslinje()) { sammenslått , periode ->
        sammenslått + AndreYtelserTidslinje(periode.periode to periode.prosent)
    }
}

class GradertPeriode(internal val periode: Periode, internal val grad: Int)

internal class AndreYtelserTidslinje(vararg perioder: Pair<Periode, Prosentdel>): Tidslinje<Prosentdel, AndreYtelserTidslinje>(*perioder) {
    override fun opprett(vararg perioder: Pair<Periode, Prosentdel>) = AndreYtelserTidslinje(*perioder)
    override fun pluss(eksisterendeVerdi: Prosentdel, nyVerdi: Prosentdel) = eksisterendeVerdi + nyVerdi
}
