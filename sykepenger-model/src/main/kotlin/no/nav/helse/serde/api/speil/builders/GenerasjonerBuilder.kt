package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.dto.Generasjon
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.speil.AnnulleringerAkkumulator
import no.nav.helse.serde.api.speil.ForkastetVedtaksperiodeAkkumulator
import no.nav.helse.serde.api.speil.GenerasjonIderAkkumulator
import no.nav.helse.serde.api.speil.Generasjoner
import no.nav.helse.serde.api.speil.IVedtaksperiode
import no.nav.helse.serde.api.speil.SykdomshistorikkAkkumulator
import no.nav.helse.serde.api.speil.Tidslinjeberegninger
import no.nav.helse.serde.api.speil.Tidslinjeperioder
import no.nav.helse.serde.api.speil.VedtaksperiodeAkkumulator
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal data class GenerasjonIder(
    val beregningId: BeregningId,
    val sykdomshistorikkId: SykdomshistorikkId,
    val vilkårsgrunnlagshistorikkId: VilkårsgrunnlagshistorikkId
)

internal typealias BeregningId = UUID
internal typealias SykdomshistorikkId = UUID
internal typealias VilkårsgrunnlagshistorikkId = UUID
internal typealias KorrelasjonsId = UUID
internal typealias InntektsmeldingId = UUID

// Besøker hele arbeidsgiver-treet
internal class GenerasjonerBuilder(
    private val hendelser: List<HendelseDTO>,
    private val alder: Alder,
    private val arbeidsgiver: Arbeidsgiver,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
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
            alder = alder,
            forkastetVedtaksperiodeIder = forkastetVedtaksperiodeAkkumulator.toList(),
            vedtaksperioder = vedtaksperiodeAkkumulator.toList(),
            tidslinjeberegninger = tidslinjeberegninger,
            vilkårsgrunnlaghistorikk = vilkårsgrunnlaghistorikk
        )
        return Generasjoner(tidslinjeperioder).build()
    }

    override fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode) {
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
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: () -> Inntektskilde
    ) {
        if (tilstand == Vedtaksperiode.TilInfotrygd) return
        val sykdomstidslinje = VedtaksperiodeSykdomstidslinjeBuilder(vedtaksperiode).build()
        val utbetalinger = UtbetalingerBuilder(vedtaksperiode, tilstand).build(id)
        val aktivetsloggForPeriode = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        vedtaksperiodeAkkumulator.leggTil(
            IVedtaksperiode(
                id,
                periode.start,
                periode.endInclusive,
                inntektskilde = inntektskilde(),
                hendelser = hendelser.filter { it.id in hendelseIder.ider().map(UUID::toString) },
                utbetalinger = utbetalinger,
                periodetype = arbeidsgiver.periodetype(periode),
                sykdomstidslinje = sykdomstidslinje,
                oppdatert = oppdatert,
                tilstand = tilstand,
                skjæringstidspunkt = skjæringstidspunkt(),
                aktivitetsloggForPeriode = aktivetsloggForPeriode
            )
        )
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        periode: Periode,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID,
        overføringstidspunkt: LocalDateTime?,
        avsluttet: LocalDateTime?,
        avstemmingsnøkkel: Long?
    ) {
        if (type != Utbetalingtype.ANNULLERING) return
        annulleringer.leggTil(UtbetalingBuilder(utbetaling).build())
    }

    override fun preVisitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        organisasjonsnummer: String,
        sykdomshistorikkElementId: UUID,
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
