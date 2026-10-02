package com.ScienceFiction.DronePassAndroid.feature.profile

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal const val ACCOUNT_DELETION_FUNCTION_NAME = "deleteDronePassAccount"
internal const val ACCOUNT_DELETION_SUCCESS_STATUS = "deleted"
internal const val ACCOUNT_DELETION_TIMEOUT_SECONDS = 540L
private const val USER_NOT_FOUND_ERROR_CODE = "ERROR_USER_NOT_FOUND"
private const val APPLE_PROVIDER_ID = "apple.com"

internal fun accountDeletionCallablePayload(): Map<String, Any> = emptyMap()

internal fun isAccountDeletionSuccessResponse(data: Any?): Boolean {
    val response = data as? Map<*, *> ?: return false
    return response["status"] == ACCOUNT_DELETION_SUCCESS_STATUS
}

internal fun hasAppleLoginProvider(providerIds: List<String>): Boolean {
    return APPLE_PROVIDER_ID in providerIds
}

internal enum class AccountReloadStatus { EXISTS, DELETED }

internal fun isUserNotFoundErrorCode(errorCode: String?): Boolean {
    return errorCode == USER_NOT_FOUND_ERROR_CODE
}

internal suspend fun reloadAccountStatus(user: FirebaseUser): AccountReloadStatus {
    return try {
        user.reload().await()
        AccountReloadStatus.EXISTS
    } catch (error: FirebaseAuthInvalidUserException) {
        if (isUserNotFoundErrorCode(error.errorCode)) AccountReloadStatus.DELETED else throw error
    }
}

internal suspend fun completeAccountDeletion(
    reload: suspend () -> AccountReloadStatus,
    beforeCall: suspend () -> Unit,
    call: suspend () -> Any?,
) {
    if (reload() == AccountReloadStatus.DELETED) return
    beforeCall()

    try {
        if (!isAccountDeletionSuccessResponse(call())) {
            throw AccountDeletionException(AccountDeletionFailureReason.INVALID_SERVER_RESPONSE)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        val accountWasDeleted = try {
            reload() == AccountReloadStatus.DELETED
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            false
        }
        if (!accountWasDeleted) throw error
    }
}

internal enum class AccountDeletionFailureReason {
    NOT_AUTHENTICATED,
    APPLE_REAUTHENTICATION_FAILED,
    APPLE_ACCESS_TOKEN_MISSING,
    APPLE_TOKEN_REVOCATION_FAILED,
    SERVER_REQUEST_FAILED,
    INVALID_SERVER_RESPONSE,
}

internal class AccountDeletionException(
    val reason: AccountDeletionFailureReason,
    cause: Throwable? = null,
) : Exception(cause)

interface AccountDeletionService {
    suspend fun deleteCurrentAccount(activity: Activity): Result<Unit>
}

@Singleton
class FirebaseAccountDeletionService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFunctions: FirebaseFunctions,
) : AccountDeletionService {

    override suspend fun deleteCurrentAccount(activity: Activity): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: throw AccountDeletionException(AccountDeletionFailureReason.NOT_AUTHENTICATED)

            completeAccountDeletion(
                reload = { reloadAccountStatus(user) },
                beforeCall = {
                    if (hasAppleLoginProvider(user.providerData.map { it.providerId })) {
                        revokeAppleAccessToken(activity)
                    }
                },
                call = {
                    try {
                        firebaseFunctions
                            .getHttpsCallable(ACCOUNT_DELETION_FUNCTION_NAME)
                            .withTimeout(ACCOUNT_DELETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .call(accountDeletionCallablePayload())
                            .await()
                            .data
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        throw AccountDeletionException(
                            reason = AccountDeletionFailureReason.SERVER_REQUEST_FAILED,
                            cause = error,
                        )
                    }
                },
            )
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun revokeAppleAccessToken(activity: Activity) {
        val user = firebaseAuth.currentUser
            ?: throw AccountDeletionException(AccountDeletionFailureReason.NOT_AUTHENTICATED)
        val provider = OAuthProvider.newBuilder(APPLE_PROVIDER_ID).apply {
            setScopes(listOf("email", "name"))
        }.build()
        val authResult = try {
            user.startActivityForReauthenticateWithProvider(activity, provider).await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw AccountDeletionException(
                reason = AccountDeletionFailureReason.APPLE_REAUTHENTICATION_FAILED,
                cause = error,
            )
        }
        val accessToken = (authResult.credential as? OAuthCredential)
            ?.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: throw AccountDeletionException(AccountDeletionFailureReason.APPLE_ACCESS_TOKEN_MISSING)

        try {
            firebaseAuth.revokeAccessToken(accessToken).await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw AccountDeletionException(
                reason = AccountDeletionFailureReason.APPLE_TOKEN_REVOCATION_FAILED,
                cause = error,
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountDeletionModule {

    @Binds
    @Singleton
    abstract fun bindAccountDeletionService(
        implementation: FirebaseAccountDeletionService,
    ): AccountDeletionService
}
