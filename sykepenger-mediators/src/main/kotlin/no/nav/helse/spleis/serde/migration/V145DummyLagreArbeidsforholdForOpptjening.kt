package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V145DummyLagreArbeidsforholdForOpptjening : JsonMigration(version = 145) {
    override val description: String = "Lagrer arbeidsforhold relevant til opptjening i vilkårsgrunnlag og arbeidsforhold-historikken"
    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {

    }
}
