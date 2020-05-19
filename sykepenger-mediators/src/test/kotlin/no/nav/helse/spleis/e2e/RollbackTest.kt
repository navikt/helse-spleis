package no.nav.helse.spleis.e2e

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RollbackTest : AbstractEndToEndMediatorTest() {


    @Test
    fun rollbacke2e() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInnteksmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        val id = using(sessionOf(dataSource)) {
            it.run(
            queryOf("SELECT id FROM person WHERE fnr = ? ", UNG_PERSON_FNR_2018).map {
                row -> row.long("id")
            }.asList)
        }[2]
        sendRollback(requireNotNull(id))

    }
}
