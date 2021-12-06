package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.serde.api.v2.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal data class GenerasjonIder(
    val beregningId: BeregningId,
    val sykdomshistorikkId: SykdomshistorikkId,
    val vilkårsgrunnlagshistorikkId: VilkårsgrunnlagshistorikkId
)

internal typealias BeregningId = UUID
internal typealias SykdomshistorikkId = UUID
internal typealias VilkårsgrunnlagshistorikkId = UUID
internal typealias FagsystemId = String

// Besøker hele arbeidsgiver-treet
internal class GenerasjonerBuilder(
    private val hendelser: List<HendelseDTO>,
    private val fødselsnummer: Fødselsnummer,
    private val vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk,
    arbeidsgiver: Arbeidsgiver
) : ArbeidsgiverVisitor {
    private val vedtaksperiodeAkkumulator = VedtaksperiodeAkkumulator()
    private val forkastetVedtaksperiodeAkkumulator = ForkastetVedtaksperiodeAkkumulator()
    private val generasjonIderAkkumulator = GenerasjonIderAkkumulator()
    private val sykdomshistorikkAkkumulator = SykdomshistorikkAkkumulator()
    private val annulleringer = AnnulleringerAkkumulator()

    init {
        arbeidsgiver.accept(this)
    }

    fun build(): List<Generasjon> {
        vedtaksperiodeAkkumulator.supplerMedAnnulleringer(annulleringer)
        val tidslinjeberegninger = Tidslinjeberegninger(generasjonIderAkkumulator.toList(), sykdomshistorikkAkkumulator)
        val tidslinjeperioder = Tidslinjeperioder(
            fødselsnummer,
            forkastetVedtaksperiodeAkkumulator.toList(),
            vilkårsgrunnlagHistorikk,
            vedtaksperiodeAkkumulator.toList(),
            tidslinjeberegninger
        )
        return Generasjoner(tidslinjeperioder).build()
    }

    override fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
        forkastetVedtaksperiodeAkkumulator.leggTil(vedtaksperiode)
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        val sykdomstidslinje = VedtaksperiodeSykdomstidslinjeBuilder(vedtaksperiode).build()
        val utbetalinger = UtbetalingerBuilder(vedtaksperiode).build()
        val simulering = SimuleringBuilder(vedtaksperiode).build()
        val aktivetsloggForPeriode = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        val aktivitetsloggForVilkårsprøving = Vedtaksperiode.hentVilkårsgrunnlagAktiviteter(vedtaksperiode)
        vedtaksperiodeAkkumulator.leggTil(
            IVedtaksperiode(
                id,
                periode.start,
                periode.endInclusive,
                behandlingstype = Behandlingstype.BEHANDLET,
                inntektskilde = inntektskilde,
                hendelser = hendelser.filter { it.id in hendelseIder.map { id -> id.toString() } },
                simuleringsdataDTO = simulering,
                utbetalinger = utbetalinger,
                periodetype = periodetype,
                sykdomstidslinje = sykdomstidslinje,
                tilstand = tilstand,
                oppdatert = oppdatert,
                skjæringstidspunkt = skjæringstidspunkt,
                aktivitetsloggForPeriode = aktivetsloggForPeriode,
                aktivitetsloggForVilkårsprøving = aktivitetsloggForVilkårsprøving
            )
        )
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID
    ) {
        if (type != Utbetaling.Utbetalingtype.ANNULLERING) return
        annulleringer.leggTil(UtbetalingBuilder(utbetaling).build())
    }

    override fun visitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        sykdomshistorikkElementId: UUID,
        inntektshistorikkInnslagId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
    ) {
        generasjonIderAkkumulator.leggTil(id, GenerasjonIder(id, sykdomshistorikkElementId, vilkårsgrunnlagHistorikkInnslagId))
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        SykdomshistorikkBuilder(id, element).build().also { (id, tidslinje) ->
            sykdomshistorikkAkkumulator.leggTil(id, tidslinje)
        }
    }
}
