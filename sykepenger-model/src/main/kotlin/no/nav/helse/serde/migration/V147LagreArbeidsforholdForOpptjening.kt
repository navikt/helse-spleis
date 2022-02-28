package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V147LagreArbeidsforholdForOpptjening : JsonMigration(version = 147) {
    override val description: String = "Lagrer arbeidsforhold relevant til opptjening i vilkårsgrunnlag og arbeidsforhold-historikken"

    /* DETTE ER VÅR PLAN:
    * To trinns rakett:
    * 1. for hver vilkårsgrunnlagmelding med meldingsreferanse: migrer inn opptjening i vilkårsgrunnlag med samme meldingsreferanse
    * 2. kobler sammen overstyr inntekt-hendelse
    *       a) finn den originale vilkårsgrunnlaget som ble overstyrt
    *       b) kopier opptjening inn i revurderte vilkårsgrunnøag
    *
    * Om vi ikke finner via meldingsreferanse
    *   - prøv fuzzy matching på alle andre felter
    *   - fom/skjæringstidspunkt?
    *   - lag dummyopptjening
    * */
    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {

    }

}
