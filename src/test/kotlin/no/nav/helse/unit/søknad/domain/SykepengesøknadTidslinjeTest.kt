package no.nav.helse.unit.søknad.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

        val sykeperiodFOM = LocalDate.of(2019, SEPTEMBER, 14)
        val sykeperiodeTOM = LocalDate.of(2019, OCTOBER, 5)
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
                fom = LocalDate.of(2019, SEPTEMBER, 10),
                tom = LocalDate.of(2019, SEPTEMBER, 13)
            )),
            soknadsperioder = listOf(SoknadsperiodeDTO(
                fom = sykeperiodFOM,
                tom = LocalDate.of(2019, SEPTEMBER, 30)
            ), SoknadsperiodeDTO(
                fom = LocalDate.of(2019, OCTOBER, 5),
                tom = sykeperiodeTOM
            )),
            fravar = listOf(FravarDTO(
                fom = LocalDate.of(2019, OCTOBER, 1),
                tom = LocalDate.of(2019, OCTOBER, 4),
                type = FravarstypeDTO.FERIE
            ))
        )

        private val søknad = Sykepengesøknad(objectMapper.valueToTree(søknadDTO))
    }

    @Test
    fun `Tidslinjen får sykeperiodene (søknadsperiodene) fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomsTidslinje

        assertEquals(sykeperiodFOM, sykdomstidslinje.syketilfeller().first().startdato())
        assertEquals(sykeperiodeTOM, sykdomstidslinje.syketilfeller().last().sluttdato())
    }
}
