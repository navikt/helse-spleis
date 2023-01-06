package no.nav.helse.person

import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.serde.JsonBuilder
import no.nav.helse.somPersonidentifikator
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykmeldingsperioderBuilderTest() {

    @Test
    fun `serialiserer Sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        val fabrikk = ArbeidsgiverHendelsefabrikk(
            "aktor",
            "12345678910".somPersonidentifikator(),
            "orgnummer"
        )

        sykmeldingsperioder.lagre(fabrikk.lagSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent)))
        sykmeldingsperioder.lagre(fabrikk.lagSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent)))

        val sykmeldingsperioderMap = mutableListOf<Map<String, Any>>()
        val sykmeldingsperioderState = JsonBuilder.SykmeldingsperioderState(sykmeldingsperioderMap)

        sykmeldingsperioder.accept(sykmeldingsperioderState)
        assertEquals(
            listOf(mapOf("fom" to 1.januar, "tom" to 31.januar), mapOf("fom" to 1.mars, "tom" to 31.mars)),
            sykmeldingsperioderMap
        )
    }

}
