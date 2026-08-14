package no.nav.helse.opptjening.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.infra.db.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.infra.kafka.GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver
import no.nav.helse.opptjening.infra.kafka.OpptjeningsvurderingResultatRiver
import no.nav.helse.opptjening.infra.kafka.OpptjeningsvurderingRiver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal val sikkerLogg: Logger = LoggerFactory
    .getLogger("tjenestekall")

class VilkårsprøvingModule(
    rapidsConnection: RapidsConnection
) {
    private val vilkårsvurderingRepository = InMemoryVilkårsvurderingRepository()

    init {
        GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver(
            rapidsConnection = rapidsConnection,
            opptjeningService = OpptjeningService(vilkårsvurderingRepository)
        )
        OpptjeningsvurderingRiver(
            rapidsConnection = rapidsConnection,
            opptjeningService = OpptjeningService(vilkårsvurderingRepository)
        )
        OpptjeningsvurderingResultatRiver(
            rapidsConnection = rapidsConnection,
            vilkårsvurderingRepository = vilkårsvurderingRepository
        )
    }
}
