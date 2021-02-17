package no.nav.helse.serde.api.builders

import no.nav.helse.Toggles
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.ArbeidsgiverDTO
import no.nav.helse.serde.api.HendelseDTO
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*

internal class ArbeidsgiverBuilder(
    arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val organisasjonsnummer: String,
    fødselsnummer: String,
    private val inntektshistorikkBuilder: InntektshistorikkBuilder,
) : BuilderState() {
    private val utbetalingshistorikkBuilder = UtbetalingshistorikkBuilder()
    private val utbetalinger = mutableListOf<Utbetaling>()

    private val perioderBuilder = VedtaksperioderBuilder(arbeidsgiver, fødselsnummer, inntektshistorikkBuilder)
    private val forkastetPerioderBuilder = VedtaksperioderBuilder(arbeidsgiver, fødselsnummer, inntektshistorikkBuilder)

    internal fun build(hendelser: List<HendelseDTO>) = ArbeidsgiverDTO(
        organisasjonsnummer = organisasjonsnummer,
        id = id,
        vedtaksperioder = perioderBuilder.build(hendelser, utbetalinger) + forkastetPerioderBuilder.build(hendelser, utbetalinger).filter { it.tilstand.visesNårForkastet() },
        utbetalingshistorikk = utbetalingshistorikkBuilder.build()
    )

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        pushState(perioderBuilder)
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        pushState(forkastetPerioderBuilder)
    }

    override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
        if (!Toggles.SpeilInntekterVol2Enabled.enabled) return
        inntektshistorikkBuilder.inntektshistorikk(organisasjonsnummer, inntektshistorikk)
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.utbetalinger.addAll(utbetalinger)
        pushState(utbetalingshistorikkBuilder)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        pushState(utbetalingshistorikkBuilder)
    }

    override fun visitUtbetalingstidslinjeberegning(id: UUID, tidsstempel: LocalDateTime, sykdomshistorikkElementId: UUID) {
        utbetalingshistorikkBuilder.visitUtbetalingstidslinjeberegning(id, tidsstempel, sykdomshistorikkElementId)
    }

    override fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        popState()
    }
}
