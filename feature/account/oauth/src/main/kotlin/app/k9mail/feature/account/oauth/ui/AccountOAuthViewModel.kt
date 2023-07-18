package app.k9mail.feature.account.oauth.ui

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.account.oauth.domain.DomainContract.UseCase
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Error
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.State
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.ViewModel
import kotlinx.coroutines.launch

class AccountOAuthViewModel(
    initialState: State = State(),
    private val getOAuthRequestIntent: UseCase.GetOAuthRequestIntent,
    private val finishOAuthSignIn: UseCase.FinishOAuthSignIn,
) : BaseViewModel<State, Event, Effect>(initialState), ViewModel {

    override fun initState(state: State) {
        updateState {
            state.copy()
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.OnOAuthResult -> onOAuthResult(event.resultCode, event.data)

            Event.SignInClicked -> onSignIn()

            Event.OnNextClicked -> TODO()

            Event.OnBackClicked -> navigateBack()

            Event.OnRetryClicked -> onRetry()
        }
    }

    private fun onSignIn() {
        val result = getOAuthRequestIntent.execute(
            hostname = state.value.hostname,
            emailAddress = state.value.emailAddress,
        )

        when (result) {
            AuthorizationIntentResult.NotSupported -> {
                updateState { state ->
                    state.copy(
                        error = Error.NotSupported,
                    )
                }
            }

            is AuthorizationIntentResult.Success -> {
                emitEffect(Effect.LaunchOAuth(result.intent))
            }
        }
    }

    private fun onRetry() {
        updateState { state ->
            state.copy(
                error = null,
            )
        }
        onSignIn()
    }

    private fun onOAuthResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            finishSignIn(data)
        } else {
            updateState { state ->
                state.copy(error = Error.Cancelled)
            }
        }
    }

    private fun finishSignIn(data: Intent) {
        updateState { state ->
            state.copy(
                isLoading = true,
            )
        }
        viewModelScope.launch {
            when (val result = finishOAuthSignIn.execute(state.value.authorizationState, data)) {
                AuthorizationResult.BrowserNotAvailable -> updateErrorState(Error.BrowserNotAvailable)
                AuthorizationResult.Canceled -> updateErrorState(Error.Cancelled)
                is AuthorizationResult.Failure -> updateErrorState(Error.Unknown(result.error))
                is AuthorizationResult.Success -> {
                    updateState { state ->
                        state.copy(
                            authorizationState = result.state,
                            isLoading = false,
                        )
                    }
                    navigateNext()
                }
            }
        }
    }

    private fun updateErrorState(error: Error) = updateState { state ->
        state.copy(
            error = error,
            isLoading = false,
        )
    }

    private fun navigateBack() = emitEffect(Effect.NavigateBack)

    private fun navigateNext() = emitEffect(Effect.NavigateNext(state.value.authorizationState))
}
