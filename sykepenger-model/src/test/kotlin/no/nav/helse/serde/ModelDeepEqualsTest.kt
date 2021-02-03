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
                TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("YEP", "JA")),
                TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("NOPE", "JA"))
            )
        }
    }

    @Test
    fun `checks private field in subclasses in no nav helse`() {
        assertThrows<AssertionError> {
            assertDeepEquals(
                TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("YEP", "JA")),
                TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("YEP", "NEI"))
            )
        }
    }

    @Test
    fun `do not check lazy getters in subclasses in no nav helse`() {
        assertDeepEquals(
            TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("YEP", "JA")),
            TestDataWithInnerClass(TestDataWithInnerClass.InnerClass("YEP", "JA"))
        )
    }

    class TestDataWithObject {
        private val state: InnerInterface = InnerObject

        interface InnerInterface

        internal object InnerObject : InnerInterface {
            private val something = UUID.randomUUID()
        }
    }

    class TestDataWithInnerClass(
        @JvmField
        val innerValue: InnerClass
    ) {
        class InnerClass(
            @JvmField
            val value: String,
            @JvmField
            private val myValue: String
        )

        val getValue get() = System.nanoTime().toString()
        private val myGetValue get() = System.nanoTime().toString()
    }
}
