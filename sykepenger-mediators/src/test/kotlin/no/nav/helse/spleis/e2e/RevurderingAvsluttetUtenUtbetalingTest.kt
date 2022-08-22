package no.nav.helse.spleis.e2e

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import no.nav.helse.ForventetFeil
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

internal class RevurderingAvsluttetUtenUtbetalingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `revurdering ved inntektsmelding for korte perioder`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 5.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 5.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)
        sendNySøknad(SoknadsperiodeDTO(fom = 6.januar, tom = 10.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 6.januar, tom = 10.januar, sykmeldingsgrad = 100)))
        sendNySøknad(SoknadsperiodeDTO(fom = 11.januar, tom = 17.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 11.januar, tom = 17.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelserUtenSykepengehistorikk(2)
        sendVilkårsgrunnlag(2)
        sendYtelserUtenSykepengehistorikk(2)
        sendSimulering(2, SimuleringMessage.Simuleringstatus.OK)

        assertTilstander(0, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")
        assertTilstander(1, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")
        assertTilstander(
            2,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING",
            "AVVENTER_GJENNOMFØRT_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_VILKÅRSPRØVING_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_SIMULERING_REVURDERING",
            "AVVENTER_GODKJENNING_REVURDERING"
        )
    }

    @Test
    fun `revurdering ved inntektsmelding for korte perioder - endring av skjæringstidspunkt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 8.januar, tom = 10.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 8.januar, tom = 10.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)
        sendNySøknad(SoknadsperiodeDTO(fom = 11.januar, tom = 22.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 11.januar, tom = 22.januar, sykmeldingsgrad = 100)))
        sendNySøknad(SoknadsperiodeDTO(fom = 23.januar, tom = 23.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 23.januar, tom = 23.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(
            Periode(fom = 1.januar, tom = 6.januar),
            Periode(fom = 11.januar, tom = 21.januar),
        ), førsteFraværsdag = 11.januar)
        sendYtelserUtenSykepengehistorikk(2)
        sendVilkårsgrunnlag(2)
        sendYtelserUtenSykepengehistorikk(2)
        sendSimulering(2, SimuleringMessage.Simuleringstatus.OK)

        assertTilstander(0, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVSLUTTET_UTEN_UTBETALING")
        assertTilstander(1, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "AVSLUTTET_UTEN_UTBETALING", "AVVENTER_GJENNOMFØRT_REVURDERING")
        assertTilstander(
            2,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "AVSLUTTET_UTEN_UTBETALING",
            "AVVENTER_GJENNOMFØRT_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_VILKÅRSPRØVING_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_SIMULERING_REVURDERING",
            "AVVENTER_GODKJENNING_REVURDERING"
        )
    }

    @Test
    @ForventetFeil("perioden går inn i Avventer historikk revurdering flere ganger")
    fun `revurdering ved inntektsmelding etter utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 22.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 22.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)

        sendNySøknad(SoknadsperiodeDTO(fom = 23.januar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 23.januar, tom = 28.februar, sykmeldingsgrad = 100)))

        sendInntektsmelding(listOf(
            Periode(fom = 9.januar, tom = 24.januar)
        ), førsteFraværsdag = 9.januar)

        sendYtelserUtenSykepengehistorikk(1)
        sendVilkårsgrunnlag(1)
        sendYtelserUtenSykepengehistorikk(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(1, true)
        sendUtbetaling(true)

        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))

        sendYtelserUtenSykepengehistorikk(2)
        sendSimulering(2, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(2, true)
        sendUtbetaling(true)

        val messages = catchErrors("Kan ikke produsere samme behov på samme kontekst") {
            sendInntektsmelding(
                listOf(
                    Periode(fom = 3.januar, tom = 18.januar)
                ), førsteFraværsdag = 3.januar
            )
        }

        assertTrue(messages.isEmpty()) { "Forventet ikke å finne loggmeldinger:\n$messages" }
        assertTilstand(0, "AVVENTER_GJENNOMFØRT_REVURDERING")
        assertTilstand(1, "AVVENTER_GJENNOMFØRT_REVURDERING")
        assertTilstand(2, "AVVENTER_HISTORIKK_REVURDERING")
    }

    private val logCollector = LogCollector()

    private fun catchErrors(vararg filter: String, block: () -> Any): List<ILoggingEvent> {
        val logger = (LoggerFactory.getLogger("tjenestekall") as Logger)
        logger.addAppender(logCollector)
        logCollector.start()
        block()
        logger.detachAppender(logCollector)
        logCollector.stop()
        return logCollector.iterator().asSequence().filter { event ->
            filter.toList().any { filterText -> event.formattedMessage.contains(filterText) }
        }.toList()
    }

    @AfterEach
    fun after() {
        val logger = (LoggerFactory.getLogger("tjenestekall") as Logger)
        logger.detachAndStopAllAppenders()
    }

    private class LogCollector private constructor(private val messages: MutableList<ILoggingEvent>): AppenderBase<ILoggingEvent>(), Iterable<ILoggingEvent> by (messages) {
        constructor() : this(mutableListOf())

        override fun append(eventObject: ILoggingEvent) {
            messages.add(eventObject)
        }
    }
}
