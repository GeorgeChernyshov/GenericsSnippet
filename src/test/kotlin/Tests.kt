import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.test.runTest
import org.example.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.Serializable

// --- Mock/Helper Classes for Testing (DO NOT put these in your solution files) ---
// These are only for the tests to verify the behavior of your generic functions/classes.

// Mock class for Exercise 2 to capture data and verify behavior of printData
class TestStringDataSource(private val data: List<String>) : DataSource<String> {
    override fun getData(): List<String> = data
}

// Mock class for Exercise 3 to capture consumed items and verify behavior of processAndConsume
class TestIntDataSink : DataSink<Int> {
    val consumedItems = mutableListOf<Int>()
    override fun consume(item: Int) {
        consumedItems.add(item)
    }
}

class TestNumberDataSink : DataSink<Number> {
    val consumedNumbers = mutableListOf<Number>()
    override fun consume(item: Number) {
        consumedNumbers.add(item)
    }
}

// Mock class for Exercise 7 to fulfill combined constraints
data class MyDescribableSerializable(val value: String) : Describable, Serializable {
    override fun getDescription(): String {
        return "Description of: $value"
    }
}

// Mock class for Exercise 6 Mapper
class TestStringToIntMapper : Mapper<String, Int> {
    override fun map(input: String): Int {
        return input.toInt()
    }
}

data class UserLoggedInEvent(val userId: String) : Event
data class OrderPlacedEvent(val orderId: String, val amount: Double) : Event
data class ItemAddedToCartEvent(val itemId: String, val quantity: Int) : Event

class EventMonitorListener<T : Event> : EventListener<T> {
    val receivedEvents = mutableListOf<T>()
    override fun onEvent(event: T) {
        receivedEvents.add(event)
    }
}

//region Exercise 10 Mocks
data class UserSession(val id: String, val username: String)

// A testable implementation of Repository for UserSession
class TestUserSessionRepository(
    private val emissions: List<DataState<UserSession>>
) : Repository<UserSession> {
    override fun getStream() = flow {
        emissions.forEach { emit(it) }
    }
}

//region Exercise 11 Mocks - (Configuration data class should be in your solution file)
// Example Configuration (assuming your solution has this):

data class Configuration(
    val baseUrl: String,
    val timeoutSeconds: Int,
    val enableLogging: Boolean = false
)

// We'll create a builder that returns this Configuration.
// The compile-time safety is achieved by the builder's *design*, not directly testable by JUnit,
// but we can test the successful final build.

// --- Generics Exercises Test Suite ---

class GenericsExercisesTest {

    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @BeforeEach
    fun setUp() {
        // Redirect System.out to capture console output for testing 'print' functions
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        // Restore System.out after each test
        System.setOut(standardOut)
        outputStreamCaptor.reset() // Clear the captor for the next test
    }

    // Exercise 1: Flexible Container
    @Test
    fun `Exercise 1 - Container should store and retrieve items of various types`() {
        val intContainer = Container<Int>()
        intContainer.put(10)
        assertEquals(10, intContainer.get())

        val stringContainer = Container<String>()
        stringContainer.put("Kotlin")
        assertEquals("Kotlin", stringContainer.get())
    }

    @Test
    fun `Exercise 1 - get should return null if no item has been put`() {
        val intContainer = Container<Int>()
        assertNull(intContainer.get())

        val stringContainer = Container<String>()
        assertNull(stringContainer.get())
    }

    @Test
    fun `Exercise 1 - processItem should apply lambda and return a result of potentially different type`() {
        val intContainer = Container<Int>()
        intContainer.put(5)
        val resultString = processItem(intContainer) { "$it is the number" }
        assertEquals("5 is the number", resultString)
        // Verify the original item in the container is unchanged and of its original type
        assertEquals(5, intContainer.get())

        val stringContainer = Container<String>()
        stringContainer.put("123")
        val resultParsedInt = processItem(stringContainer) { it.toInt() * 10 }
        assertEquals(1230, resultParsedInt)
    }

    // Exercise 2: Data Source
    @Test
    fun `Exercise 2 - printData should print all items from a DataSource`() {
        val dataSource = TestStringDataSource(listOf("Item A", "Item B", "Item C"))
        printData(dataSource)

        val expectedOutput = "Item A\r\nItem B\r\nItem C\r\n" // Assuming newlines after each item
        val actualOutput = outputStreamCaptor.toString()
        assertEquals(expectedOutput, actualOutput)
    }

    // Exercise 3: Data Sink
    @Test
    fun `Exercise 3 - processAndConsume should consume Ints through a DataSink for Numbers`() {
        val intDataSink = TestIntDataSink()
        processAndConsume(42, intDataSink)
        processAndConsume(100, intDataSink)

        assertEquals(listOf(42, 100), intDataSink.consumedItems)
    }

    @Test
    fun `Exercise 3 - processAndConsume should accept DataSink of wider type Number`() {
        // We expect DataSink<Int>, but we're passing DataSink<Number>
        // This is valid because DataSink is contravariant (in T), making DataSink<Number> a subtype of DataSink<Int>
        val numberDataSink = TestNumberDataSink()
        processAndConsume(50, numberDataSink)
        processAndConsume(75, numberDataSink)

        assertEquals(listOf<Number>(50, 75), numberDataSink.consumedNumbers)
    }

    // Exercise 4: Comparable Max Finder
    @Test
    fun `Exercise 4 - findMaximum should return the largest item in a list of comparable types`() {
        assertEquals(null, findMaximum(emptyList<Int>()))
        assertEquals(5, findMaximum(listOf(1, 5, 2, -3)))
        assertEquals("Zebra", findMaximum(listOf("Apple", "Banana", "Zebra", "Cat")))
        assertEquals(3.14, findMaximum(listOf(1.0, 3.14, 2.5, 0.99)))
    }

    @Test
    fun `Exercise 4 - findMaximum should handle single-element lists`() {
        assertEquals(7, findMaximum(listOf(7)))
        assertEquals("Only", findMaximum(listOf("Only")))
    }

    // Exercise 5: Type-Safe List Filter
    @Test
    fun `Exercise 5 - filterByType should return a list containing only elements of the specified type`() {
        val mixedList = listOf(1, "kotlin", 2.5, true, 3, "generics", 4L)

        val ints = filterByType<Int>(mixedList)
        assertEquals(listOf(1, 3), ints)

        val strings = filterByType<String>(mixedList)
        assertEquals(listOf("kotlin", "generics"), strings)

        val doubles = filterByType<Double>(mixedList)
        assertEquals(listOf(2.5), doubles)

        val booleans = filterByType<Boolean>(mixedList)
        assertEquals(listOf(true), booleans)

        val longs = filterByType<Long>(mixedList)
        assertEquals(listOf(4L), longs)

        val noMatches = filterByType<Char>(mixedList)
        assertTrue(noMatches.isEmpty())

        assertTrue(filterByType<Int>(emptyList()).isEmpty())
    }

    // Exercise 6: Generic Mapper
    @Test
    fun `Exercise 6 - applyMapper should transform a list using a given Mapper`() {
        val mapper = TestStringToIntMapper()
        val inputStrings = listOf("10", "20", "30", "40")
        val outputInts = applyMapper(inputStrings, mapper)

        assertEquals(listOf(10, 20, 30, 40), outputInts)

        val emptyStrings = emptyList<String>()
        val emptyInts = applyMapper(emptyStrings, mapper)
        assertTrue(emptyInts.isEmpty())
    }

    // Exercise 7: Combined Constraints
    @Test
    fun `Exercise 7 - debugDescription should return description for Describable and Serializable objects`() {
        val item = MyDescribableSerializable("Test Object")
        assertEquals("Description of: Test Object", debugDescription(item))

        // This test primarily ensures that your `debugDescription` function
        // compiles correctly given the combined type constraints and calls getDescription().
    }

    // Exercise 8: List Printer
    @Test
    fun `Exercise 8 - printAnyListSize should print the size of any given list`() {
        printAnyListSize(listOf(1, 2, 3, 4, 5))
        assertEquals("List size: 5\r\n", outputStreamCaptor.toString())
        outputStreamCaptor.reset() // Clear for next assertion

        printAnyListSize(listOf("apple", "banana"))
        assertEquals("List size: 2\r\n", outputStreamCaptor.toString())
        outputStreamCaptor.reset()

        printAnyListSize(emptyList<String>())
        assertEquals("List size: 0\r\n", outputStreamCaptor.toString())
    }

    @Test
    fun `Exercise 9 - EventBus dispatches to correct specific listeners`() {
        val eventBus = EventBus()
        val userListener = EventMonitorListener<UserLoggedInEvent>()
        val orderListener = EventMonitorListener<OrderPlacedEvent>()

        eventBus.subscribe(userListener)
        eventBus.subscribe(orderListener)

        val userEvent1 = UserLoggedInEvent("user123")
        val orderEvent1 = OrderPlacedEvent("order456", 100.0)
        val userEvent2 = UserLoggedInEvent("user789")

        eventBus.dispatch(userEvent1)
        eventBus.dispatch(orderEvent1)
        eventBus.dispatch(userEvent2)
        eventBus.dispatch(ItemAddedToCartEvent("itemA", 1)) // No listener for this, should be ignored

        assertEquals(listOf(userEvent1, userEvent2), userListener.receivedEvents)
        assertEquals(listOf(orderEvent1), orderListener.receivedEvents)
    }

    @Test
    fun `Exercise 9 - EventBus handles general event listeners (covariance)`() {
        val eventBus = EventBus()
        val generalListener = EventMonitorListener<Event>()
        val userListener = EventMonitorListener<UserLoggedInEvent>()

        eventBus.subscribe(generalListener)
        eventBus.subscribe(userListener)

        val userEvent = UserLoggedInEvent("generalUser")
        val orderEvent = OrderPlacedEvent("generalOrder", 50.0)

        eventBus.dispatch(userEvent)
        eventBus.dispatch(orderEvent)

        // General listener should get both
        assertEquals(listOf(userEvent, orderEvent), generalListener.receivedEvents)
        // Specific user listener should only get user events
        assertEquals(listOf(userEvent), userListener.receivedEvents)
    }

    @Test
    fun `Exercise 9 - EventBus unsubscribes correctly`() {
        val eventBus = EventBus()
        val userListener1 = EventMonitorListener<UserLoggedInEvent>()
        val userListener2 = EventMonitorListener<UserLoggedInEvent>()

        eventBus.subscribe(userListener1)
        eventBus.subscribe(userListener2)

        eventBus.dispatch(UserLoggedInEvent("first"))
        assertEquals(1, userListener1.receivedEvents.size)
        assertEquals(1, userListener2.receivedEvents.size)

        eventBus.unsubscribe(userListener1)
        eventBus.dispatch(UserLoggedInEvent("second"))
        assertEquals(1, userListener1.receivedEvents.size) // Should not have received "second"
        assertEquals(2, userListener2.receivedEvents.size) // Should have received "second"
    }


    // Exercise 10: Generic Data Repository with State Management
    @Test
    fun `Exercise 10 - Repository stream emits Loading, Success, Error states correctly`() = runTest {
        val testUserSession = UserSession("testId", "testUser")
        val errorMessage = "Network error"

        val emissions = listOf(
            DataState.Loading,
            DataState.Success(testUserSession),
            DataState.Error(errorMessage)
        )
        val repository = TestUserSessionRepository(emissions)

        val collectedStates = repository.getStream().toList()

        assertEquals(3, collectedStates.size)
        assertTrue(collectedStates[0] is DataState.Loading)
        assertTrue(collectedStates[1] is DataState.Success)
        assertEquals(testUserSession, (collectedStates[1] as DataState.Success).data)
        assertTrue(collectedStates[2] is DataState.Error)
        assertEquals(errorMessage, (collectedStates[2] as DataState.Error).message)
    }

    @Test
    fun `Exercise 10 - Repository stream emits only Success on successful fetch`() = runTest {
        val testUserSession = UserSession("successId", "successUser")
        val emissions = listOf(
            DataState.Loading,
            DataState.Success(testUserSession)
        )
        val repository = TestUserSessionRepository(emissions)

        val collectedStates = repository.getStream().toList()

        assertEquals(2, collectedStates.size)
        assertTrue(collectedStates[0] is DataState.Loading)
        assertTrue(collectedStates[1] is DataState.Success)
        assertEquals(testUserSession, (collectedStates[1] as DataState.Success).data)
    }

    @Test
    fun `Exercise 10 - Repository stream emits only Loading and Error on failed fetch`() = runTest {
        val errorMessage = "API limit exceeded"
        val emissions = listOf(
            DataState.Loading,
            DataState.Error(errorMessage)
        )
        val repository = TestUserSessionRepository(emissions)

        val collectedStates = repository.getStream().toList()

        assertEquals(2, collectedStates.size)
        assertTrue(collectedStates[0] is DataState.Loading)
        assertTrue(collectedStates[1] is DataState.Error)
        assertEquals(errorMessage, (collectedStates[1] as DataState.Error).message)
    }


    // Exercise 11: Typed Configuration Builder
    @Test
    fun `Exercise 11 - ConfigurationBuilder creates valid Configuration when all mandatory fields are set`() {
        // This test primarily checks the *runtime result* of a correctly configured builder.
        // The compile-time safety is ensured by the builder's type system,
        // preventing the build() method from even being callable before mandatory fields are set.
        val config = ConfigurationBuilder()
            .withBaseUrl("https://api.example.com")
            .withTimeout(30)
            .withLogging(true) // Optional field
            .build() // This should only compile if all mandatory fields are set

        assertNotNull(config)
        assertEquals("https://api.example.com", config.baseUrl)
        assertEquals(30, config.timeoutSeconds)
        assertTrue(config.enableLogging)
    }

    @Test
    fun `Exercise 11 - ConfigurationBuilder creates valid Configuration with default optional field`() {
        val config = ConfigurationBuilder()
            .withBaseUrl("http://localhost:8080")
            .withTimeout(60)
            // .withLogging() not called, should use default
            .build()

        assertNotNull(config)
        assertEquals("http://localhost:8080", config.baseUrl)
        assertEquals(60, config.timeoutSeconds)
        assertFalse(config.enableLogging) // Default should be false
    }

    // You would manually try to compile these scenarios to verify compile-time safety:

//    @Test
//    fun `Exercise 11 - ConfigurationBuilder build() should fail to compile if baseUrl is missing`() {
//        // This code should produce a compile-time error
//        val config = ConfigurationBuilder()
//            .withTimeout(30)
//            .build()
//    }
//
//    @Test
//    fun `Exercise 11 - ConfigurationBuilder build() should fail to compile if timeout is missing`() {
//        // This code should produce a compile-time error
//        val config = ConfigurationBuilder()
//            .withBaseUrl("https://api.example.com")
//            .build()
//    }

    @Test
    fun `basic select from clause generates correct SQL`() {
        val sql = query {
            select(Users.name, Users.age)
                .from(Users)
        }.build()

        // Normalize spacing for comparison
        val expected = "SELECT name, age FROM Users".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `select with single column generates correct SQL`() {
        val sql = query {
            select(Users.id)
                .from(Users)
        }.build()

        val expected = "SELECT id FROM Users".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `select from where with eq operator generates correct SQL`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.id eq 100 }
        }.build()

        val expected = "SELECT name FROM Users WHERE id = 100".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `where with string eq operator generates correct SQL with quotes`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.name eq "Alice" }
        }.build()

        val expected = "SELECT name FROM Users WHERE name = 'Alice'".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `where with gt and lt operators generates correct SQL`() {
        val sql1 = query {
            select(Users.name)
                .from(Users)
                .where { Users.age gt 25 }
        }.build()
        val expected1 = "SELECT name FROM Users WHERE age > 25".replace(Regex("\\s+"), " ").trim()
        assertEquals(expected1, sql1.replace(Regex("\\s+"), " ").trim())

        val sql2 = query {
            select(Products.productId)
                .from(Products)
                .where { Products.price lt 99.99 }
        }.build()
        val expected2 = "SELECT product_id FROM Products WHERE price < 99.99".replace(Regex("\\s+"), " ").trim()
        assertEquals(expected2, sql2.replace(Regex("\\s+"), " ").trim())
    }

    @Test
    fun `where with and operator generates correct SQL`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.age gt 18 and (Users.age lt 65) }
        }.build()

        val expected = "SELECT name FROM Users WHERE age > 18 AND age < 65".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `where with or operator generates correct SQL`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.name eq "Alice" or (Users.name eq "Bob") }
        }.build()

        val expected = "SELECT name FROM Users WHERE name = 'Alice' OR name = 'Bob'".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `where with combined and or generates correct SQL with parentheses`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.age gt 25 and (Users.name eq "Alice" or (Users.name eq "Bob")) }
        }.build()

        val expected = "SELECT name FROM Users WHERE age > 25 AND (name = 'Alice' OR name = 'Bob')".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `where with in operator for int list generates correct SQL`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.id `in` listOf(1, 2, 3) }
        }.build()

        val expected = "SELECT name FROM Users WHERE id IN (1, 2, 3)".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `where with in operator for string list generates correct SQL`() {
        val sql = query {
            select(Users.name)
                .from(Users)
                .where { Users.name `in` listOf("Alice", "Bob") }
        }.build()

        val expected = "SELECT name FROM Users WHERE name IN ('Alice', 'Bob')".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    @Test
    fun `query for products table generates correct SQL`() {
        val sql = query {
            select(Products.productId, Products.price)
                .from(Products)
                .where { Products.price gt 50.0 and (Products.stock lt 10) }
        }.build()

        val expected = "SELECT product_id, price FROM Products WHERE price > 50.0 AND stock_count < 10".replace(Regex("\\s+"), " ").trim()
        val actual = sql.replace(Regex("\\s+"), " ").trim()
        assertEquals(expected, actual)
    }

    // --- COMPILE-TIME ERROR TEST CASES (UNCOMMENT AND VERIFY MANUALLY) ---
    // These tests are designed to *not compile* if your type-state builder
    // and generic constraints are correctly implemented.
    // Uncomment them one by one in your IDE and observe the compile errors.

     @Test
     fun `build fails if from clause is missing`() {
         query {
             select(Users.name)
//              build() // This line should produce a compile-time error
         }
     }

     @Test
     fun `build fails if select clause is missing`() {
         query {
             from(Users)
//              build() // This line should produce a compile-time error
         }
     }

     @Test
     fun `select fails if called twice`() {
         query {
             select(Users.name)
//                 .select(Users.age) // This line should produce a compile-time error
                 .from(Users)
         }.build()
     }

     @Test
     fun `from fails if called twice`() {
         query {
             select(Users.name)
                 .from(Users)
//                 .from(Products) // This line should produce a compile-time error
         }.build()
     }

     @Test
     fun `where fails if called twice`() {
         query {
             select(Users.name)
                 .from(Users)
                 .where { Users.age gt 18 }
//                 .where { Users.id eq 1 }
         }.build()
     }

     @Test
     fun `where type mismatch with eq operator fails to compile`() {
         query {
             select(Users.name)
                 .from(Users)
//                 .where { Users.age eq "not an int" } // This line should produce a compile-time error
         }.build()
     }

     @Test
     fun `where type mismatch with gt operator fails to compile`() {
         query {
             select(Users.name)
                 .from(Users)
//                 .where { Users.name gt 10 } // This line should produce a compile-time error
         }.build()
     }

     @Test
     fun `where type mismatch with in operator fails to compile`() {
         query {
             select(Users.name)
                 .from(Users)
//                 .where { Users.age `in` listOf("a", "b") } // This line should produce a compile-time error
         }.build()
     }

     @Test
     fun `selecting column from wrong table fails to compile`() {
//         query {
//             select(Products.productId) // This line should produce a compile-time error
//                 .from(Users)
//         }.build()
     }

     @Test
     fun `where with column from wrong table fails to compile`() {
         query {
             select(Users.name)
                 .from(Users)
//                 .where { Products.price lt 100.0 } // This line should produce a compile-time error
         }.build()
     }
}