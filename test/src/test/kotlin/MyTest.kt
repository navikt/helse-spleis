import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MyTest {

    @Test
    fun test() {
        val myClass = MyClass()
        assertEquals("Knut", myClass.name())
    }
}
