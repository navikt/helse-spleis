package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V222SpissetMigreringForÅForkasteUtbetaling: JsonMigration(222) {

    private companion object {
        private val trøblete = TrøbleteUtbetalinger(setOf(
            "72d67604-c94f-4107-a0c9-094aac0c573e",
            "4084a81f-bb5e-4481-95fe-ad56e19fd3ea",
            "29716dd7-eeee-4f34-8c9d-1e1fa6575bbf",
            "f5326c5b-c76b-4bbf-b750-88d80f5826a5",
            "f718c466-c796-4d1f-9a13-f6572dacca7b",
            "7ed817ff-e842-4f96-a802-d97e0d0bf491",
            "71496c1f-d9b3-491d-aecb-5dda2cd36c20",
            "ded9e0b2-ef9f-49c5-90f4-d37864124099",
            "695ac1e7-0f85-4c9a-a9a2-2ce779ad595a",
            "56f15cfe-5367-44ab-bd57-861a1ceb7b3e",
            "525b4326-5492-4045-88c1-6ac03f2821d5",
            "abea13fe-2d06-4740-bc23-eec55fced40f",
            "d13c0499-6a71-45ae-9fcd-42461d5f4f06",
            "a1dd84d9-add7-4131-a351-588bdc0380b0",
            "488d6a80-1051-45e3-a2a2-de7d7e68429a"
        ))
    }

    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        trøblete.doMigration(jsonNode)
    }
}
