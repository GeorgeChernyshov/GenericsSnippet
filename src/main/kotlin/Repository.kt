package org.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error<T>(val message: String) : DataState<T>
}

interface Repository<out T> {
    fun getStream(): Flow<DataState<T>>
}

data class UserSession(
    val id: String,
    val userName: String
)

class UserSessionRepository : Repository<UserSession> {

    private val _state = MutableStateFlow<DataState<UserSession>>(DataState.Loading)
    val state = _state.asStateFlow()

    private var counter = 0
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            while (true) {
                val newSession = UserSession(
                    counter.toString(),
                    "User_$counter"
                )

                counter++

                _state.value = DataState.Success(newSession)
            }
        }
    }

    override fun getStream() = state
}