package no.nav.helse.hendelser

import no.nav.helse.*
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class SykepengesøknadTidslinjeTest {

    private val sykeperiodeFOM = 16.september
    private val sykeperiodeTOM = 5.oktober
    private val egenmeldingFom = 12.september
    private val egenmeldingTom = 15.september
    private val ferieFom = 1.oktober
    private val ferieTom = 4.oktober

    @Test
    fun `Tidslinjen får sykeperiodene (søknadsperiodene) fra søknaden`() {
        val tidslinje = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje())

        assertType(Sykedag::class, tidslinje[sykeperiodeFOM])
        assertType(SykHelgedag::class, tidslinje[sykeperiodeTOM])
        assertEquals(sykeperiodeTOM, tidslinje.sisteDag())
    }

    @Test
    fun `Tidslinjen får egenmeldingsperiodene fra søknaden`() {

        val tidslinje = (sendtSøknad( perioder = listOf(
            Periode.Egenmelding(egenmeldingFom, egenmeldingTom),
            Periode.Sykdom(sykeperiodeFOM, sykeperiodeTOM, 100))
        ).sykdomstidslinje() + nySøknad().sykdomstidslinje())

        assertEquals(egenmeldingFom, tidslinje.førsteDag())
        assertType(Egenmeldingsdag::class, tidslinje[egenmeldingFom])
        assertType(Egenmeldingsdag::class, tidslinje[egenmeldingTom])
    }

    @Test
    fun `Tidslinjen får ferien fra søknaden`() {
        val tidslinje = (sendtSøknad(
            perioder = listOf(
                Periode.Sykdom(sykeperiodeFOM, sykeperiodeTOM, 100),
                Periode.Ferie(ferieFom, ferieTom)
            )
        ).sykdomstidslinje() + nySøknad().sykdomstidslinje())

        assertType(Feriedag::class, tidslinje[ferieFom])
        assertType(Feriedag::class, tidslinje[ferieTom])
    }

    @Test
    fun `Tidslinjen får permisjon fra soknaden`() {
        val tidslinje = sendtSøknad(
            perioder = listOf(
                    Periode.Sykdom(Uke(1).mandag, Uke(1).fredag, 100),
                    Periode.Permisjon(Uke(1).torsdag, Uke(1).fredag)
                )
        ).also {
            it.toString()
        }.sykdomstidslinje()

        assertType(Permisjonsdag::class, tidslinje[Uke(1).torsdag])
        assertType(Permisjonsdag::class, tidslinje[Uke(1).fredag])
    }

    @Test
    fun `Tidslinjen får arbeidsdag resten av perioden hvis soknaden har arbeid gjenopptatt`() {
        val tidslinje = sendtSøknad(
            perioder = listOf(
                Periode.Sykdom(Uke(1).mandag, Uke(1).fredag, 100),
                Periode.Arbeid(Uke(1).onsdag, LocalDate.now())
            )
        ).also {
            it.toString()
        }.sykdomstidslinje()

        assertType(Sykedag::class, tidslinje[Uke(1).mandag])
        assertType(Sykedag::class, tidslinje[Uke(1).tirsdag])
        assertType(Arbeidsdag::class, tidslinje[Uke(1).onsdag])
        assertType(Arbeidsdag::class, tidslinje[Uke(1).torsdag])
        assertType(Arbeidsdag::class, tidslinje[Uke(1).fredag])
    }

    private fun assertType(expected: KClass<*>, actual: Any?) =
        assertEquals(expected, actual?.let { it::class })

    private fun sendtSøknad(perioder: List<Periode> = listOf(Periode.Sykdom(16.september, 5.oktober, 100)),
                            rapportertDato: LocalDateTime = LocalDateTime.now()) =
        ModelSendtSøknad(
            UUID.randomUUID(),
            "fnr",
            "aktørId",
            "orgnr",
            rapportertDato,
            perioder,
            Aktivitetslogger(),
            "{}"
        )

    private fun nySøknad() = ModelNySøknad(
        UUID.randomUUID(),
        "fnr",
        "aktørId",
        "123456789",
        LocalDateTime.now(),
        listOf(Triple(sykeperiodeFOM, sykeperiodeTOM, 100)),
        Aktivitetslogger(),
        SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = "aktørId",
            fnr = "fnr",
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                "123456789"
            ),
            fom = sykeperiodeFOM,
            tom = sykeperiodeTOM,
            opprettet = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = listOf(
                SoknadsperiodeDTO(sykeperiodeFOM, sykeperiodeTOM,100)
            ),
            fravar = emptyList()
        ).toJsonNode().toString()
    )

}

