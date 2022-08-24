package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V145DummyLagreArbeidsforholdForOpptjening : JsonMigration(version = 145) {
    override val description: String = "Lagrer arbeidsforhold relevant til opptjening i vilkårsgrunnlag og arbeidsforhold-historikken"
    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {

    }
}
