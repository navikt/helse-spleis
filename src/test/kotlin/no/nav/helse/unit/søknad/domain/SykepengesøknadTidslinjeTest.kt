package no.nav.helse.unit.søknad.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.sykdomstidslinje.Feriedag
import no.nav.helse.sykdomstidslinje.SykHelgedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykedag
import no.nav.helse.søknad.domain.Sykepengesøknad
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.*
import java.util.UUID.randomUUID

class SykepengesøknadTidslinjeTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val sykeperiodFOM = LocalDate.of(2019, SEPTEMBER, 16)
        val sykeperiodeTOM = LocalDate.of(2019, OCTOBER, 5)
        val egenmeldingFom = LocalDate.of(2019, SEPTEMBER, 12)
        val egenmeldingTom = LocalDate.of(2019, SEPTEMBER, 15)
        val ferieFom = LocalDate.of(2019, OCTOBER, 1)
        val ferieTom = LocalDate.of(2019, OCTOBER, 4)
        private val søknadDTO = SykepengesoknadDTO(
            id = randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.SENDT,
            aktorId = randomUUID().toString(),
            sykmeldingId = randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                navn = "enArbeidsgiver",
                orgnummer = "123456789"
            ),
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
            arbeidsgiverForskutterer = ArbeidsgiverForskuttererDTO.JA,
            fom = LocalDate.of(2019, SEPTEMBER, 10),
            tom = LocalDate.of(2019, OCTOBER, 5),
            startSyketilfelle = LocalDate.of(2019, SEPTEMBER, 10),
            arbeidGjenopptatt = LocalDate.of(2019, OCTOBER, 6),
            opprettet = LocalDateTime.now(),
            sendtNav = LocalDateTime.now(),
            sendtArbeidsgiver = LocalDateTime.of(2019, SEPTEMBER, 30, 0, 0, 0),
            egenmeldinger = listOf(PeriodeDTO(
                fom = egenmeldingFom,
                tom = egenmeldingTom
            )),
            soknadsperioder = listOf(SoknadsperiodeDTO(
                fom = sykeperiodFOM,
                tom = LocalDate.of(2019, SEPTEMBER, 30)
            ), SoknadsperiodeDTO(
                fom = LocalDate.of(2019, OCTOBER, 5),
                tom = sykeperiodeTOM
            )),
            fravar = listOf(FravarDTO(
                fom = ferieFom,
                tom = ferieTom,
                type = FravarstypeDTO.FERIE
            ))
        )

        private val søknad = Sykepengesøknad(objectMapper.valueToTree(søknadDTO))
    }

    @Test
    fun `Tidslinjen får sykeperiodene (søknadsperiodene) fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomstidslinje

        assertEquals(Sykedag::class, sykdomstidslinje.syketilfeller().dagForDato(sykeperiodFOM)::class)
        assertEquals(SykHelgedag::class, sykdomstidslinje.syketilfeller().dagForDato(sykeperiodeTOM)::class)
        assertEquals(sykeperiodeTOM, sykdomstidslinje.syketilfeller().last().sluttdato())
    }

    @Test
    fun `Tidslinjen får egenmeldingsperiodene fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomstidslinje

        assertEquals(egenmeldingFom, sykdomstidslinje.syketilfeller().first().startdato())
        assertEquals(Sykedag::class, sykdomstidslinje.syketilfeller().dagForDato(egenmeldingFom)::class)
        assertEquals(SykHelgedag::class, sykdomstidslinje.syketilfeller().dagForDato(egenmeldingTom)::class)
    }

    @Test
    fun `Tidslinjen får ferien fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomstidslinje

        assertEquals(Feriedag::class, sykdomstidslinje.syketilfeller().dagForDato(ferieFom)::class)
        assertEquals(Feriedag::class, sykdomstidslinje.syketilfeller().dagForDato(ferieTom)::class)
    }

    fun List<Sykdomstidslinje>.dagForDato(localDate: LocalDate) =
        flatMap { it.flatten() }
            .find { it.startdato() == localDate }!!
}

