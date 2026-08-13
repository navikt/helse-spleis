package no.nav.helse.opptjening.application

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.opptjening.domain.Arbeidssituasjon

class Opptjeningsbehov private constructor(
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidssituasjon: Arbeidssituasjon,
    tidspunktForKvittertUt: Instant?,
    vilkårsvurderingId: UUID?
) {
    companion object {
        fun fraLagring(
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            arbeidssituasjon: Arbeidssituasjon,
            tidspunktForKvittertUt: Instant?,
            vilkårsvurderingId: UUID?
        ) = Opptjeningsbehov(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidssituasjon = arbeidssituasjon,
            tidspunktForKvittertUt = tidspunktForKvittertUt,
            vilkårsvurderingId = vilkårsvurderingId
        )

        fun nytt(fødselsnummer: String, skjæringstidspunkt: LocalDate, arbeidssituasjon: Arbeidssituasjon) = Opptjeningsbehov(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidssituasjon = arbeidssituasjon,
            tidspunktForKvittertUt = null,
            vilkårsvurderingId = null
        )
    }

    var tidspunktForKvittertUt: Instant? = tidspunktForKvittertUt
        private set

    var vilkårsvurderingId: UUID? = vilkårsvurderingId
        private set

    fun kvitterUt(vilkårsvurderingId: UUID) {
        this.tidspunktForKvittertUt = Instant.now()
        this.vilkårsvurderingId = vilkårsvurderingId
    }
}
