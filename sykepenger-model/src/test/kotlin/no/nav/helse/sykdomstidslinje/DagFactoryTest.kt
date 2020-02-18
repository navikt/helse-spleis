package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.sykdomstidslinje.dag.Dag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class DagFactoryTest {

    private val enDag = LocalDate.now()

    @Test
    internal fun sykmeldingFactory() {
        val factory = NySøknad.SykmeldingDagFactory
        assertThrows<IllegalStateException> { factory.arbeidsdag(enDag) }
        assertThrows<IllegalStateException> { factory.egenmeldingsdag(enDag) }
        assertThrows<IllegalStateException> { factory.feriedag(enDag) }
        assertThrows<IllegalStateException> { factory.permisjonsdag(enDag) }
        assertThrows<IllegalStateException> { factory.studiedag(enDag) }
        assertThrows<IllegalStateException> { factory.ubestemtdag(enDag) }
        assertThrows<IllegalStateException> { factory.utenlandsdag(enDag) }

        assertDag(enDag, factory.implisittDag(enDag))
        assertDag(enDag, factory.sykedag(enDag))
        assertDag(enDag, factory.sykHelgedag(enDag))
    }

    @Test
    internal fun søknadFactory() {
        val factory =  SendtSøknad.SøknadDagFactory
        assertDag(enDag, factory.arbeidsdag(enDag))
        assertDag(enDag, factory.egenmeldingsdag(enDag))
        assertDag(enDag, factory.feriedag(enDag))
        assertDag(enDag, factory.implisittDag(enDag))
        assertDag(enDag, factory.permisjonsdag(enDag))
        assertDag(enDag, factory.studiedag(enDag))
        assertDag(enDag, factory.sykedag(enDag))
        assertDag(enDag, factory.sykHelgedag(enDag))
        assertDag(enDag, factory.ubestemtdag(enDag))
        assertDag(enDag, factory.utenlandsdag(enDag))
    }

    @Test
    internal fun inntektsmeldingFactory() {
        val factory = Inntektsmelding.InntektsmeldingDagFactory
        assertDag(enDag, factory.arbeidsdag(enDag))
        assertDag(enDag, factory.egenmeldingsdag(enDag))
        assertDag(enDag, factory.feriedag(enDag))
        assertDag(enDag, factory.implisittDag(enDag))
        assertThrows<IllegalStateException> { factory.permisjonsdag(enDag) }
        assertDag(enDag, factory.studiedag(enDag))
        assertDag(enDag, factory.sykHelgedag(enDag))
        assertDag(enDag, factory.ubestemtdag(enDag))
        assertDag(enDag, factory.utenlandsdag(enDag))
    }

    private fun assertDag(expectedDato: LocalDate, actual: Dag) {
        assertEquals(expectedDato, actual.dagen)
    }
}
