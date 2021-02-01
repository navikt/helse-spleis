package no.nav.helse.serde

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class ModelDeepEqualsTest {
    @Test
    fun `ignores objects`() {
        assertDeepEquals(TestDataWithObject(), TestDataWithObject())
    }

    @Test
    fun `checks subclasses in no nav helse`() {
        assertThrows<AssertionError> {
            assertDeepEquals(
                TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("YEP")),
                TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("NOPE"))
            )
        }
    }

    class TestDataWithObject {
        private val state: InnerInterface = InnerObject

        interface InnerInterface

        internal object InnerObject : InnerInterface {
            private val something = UUID.randomUUID()
        }
    }

    class TestDataWithInnerClass(
        val innerValue: InnerClass
    ) {
        class InnerClass(val value: String)
    }
}
