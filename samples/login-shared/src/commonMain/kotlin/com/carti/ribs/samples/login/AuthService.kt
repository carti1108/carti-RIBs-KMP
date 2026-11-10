package com.carti.ribs.samples.login

import kotlinx.coroutines.delay

/** 로그인 결과. 실패는 사용자에게 보여 줄 메시지를 담는다. */
sealed class AuthResult {
    data class Success(val userName: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/** 로그인 요청을 처리하는 서비스. 호출한 코루틴이 취소되면 요청도 취소된다. */
interface AuthService {
    suspend fun signIn(userName: String): AuthResult
}

/**
 * 네트워크 대신 지연만 주는 샘플 서비스. 이름이 [FAILING_NAME]이면 실패한다.
 * `delay`는 스레드를 막지 않으므로 메인 디스패처에서 불러도 된다.
 */
class DelayedAuthService(private val delayMillis: Long) : AuthService {
    init {
        require(delayMillis >= 0) { "delayMillis must not be negative: $delayMillis" }
    }

    override suspend fun signIn(userName: String): AuthResult {
        delay(delayMillis)
        return if (userName == FAILING_NAME) {
            AuthResult.Failure(FAILURE_MESSAGE)
        } else {
            AuthResult.Success(userName)
        }
    }

    companion object {
        const val FAILING_NAME: String = "fail"
        const val FAILURE_MESSAGE: String = "로그인에 실패했습니다"
    }
}
