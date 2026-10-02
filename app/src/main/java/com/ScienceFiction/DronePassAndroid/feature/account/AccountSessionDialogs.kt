package com.ScienceFiction.DronePassAndroid.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.account.AccountPrompt
import com.ScienceFiction.DronePassAndroid.core.account.AccountSessionFlows
import com.ScienceFiction.DronePassAndroid.core.account.ImportPromptKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AccountSessionViewModel @Inject constructor(
    private val flows: AccountSessionFlows,
) : ViewModel() {
    val prompt: StateFlow<AccountPrompt> = flows.prompt

    fun onForeground() = flows.onForeground()
    fun chooseImport(uid: String) = flows.chooseImport(uid)
    fun deleteDeviceDataForImport(uid: String) = flows.chooseDeleteDeviceDataForImport(uid)
    fun cancelImport(uid: String) = flows.cancelImport(uid)
    fun retryImportCheck() = flows.retryImportCheck()
    fun confirmReplace(uid: String) = flows.confirmReplaceOtherAccountData(uid)
    fun cancelReplace(uid: String) = flows.cancelReplaceOtherAccountData(uid)
    fun acknowledgeSessionLost() = flows.acknowledgeSessionLost()
    fun deleteDeviceDataAfterSessionLost() = flows.deleteDeviceDataAfterSessionLost()
    fun dismissAccountDeletedNotice() = flows.dismissAccountDeletedNotice()
}

/** [가져오기] 확인창의 버튼 순서. 다른 계정 데이터일 수 있으면 [삭제]를 기본(첫째)으로 둔다. */
internal enum class ImportPromptButton { IMPORT, DELETE, CANCEL }

internal fun importPromptButtons(kind: ImportPromptKind): List<ImportPromptButton> = when (kind) {
    ImportPromptKind.LEGACY_OTHER_ACCOUNT ->
        listOf(ImportPromptButton.DELETE, ImportPromptButton.IMPORT, ImportPromptButton.CANCEL)
    ImportPromptKind.GUEST, ImportPromptKind.LEGACY_SAME_ACCOUNT ->
        listOf(ImportPromptButton.IMPORT, ImportPromptButton.DELETE, ImportPromptButton.CANCEL)
}

/**
 * 기기 데이터 주인 확인창(3.6.0). 로그인 시트가 아니라 앱 최상위에 띄운다. 창을 그냥 닫으면 [취소]로 처리하고,
 * 같은 응답이 두 번 들어와도 [AccountSessionFlows] 가 한 번만 처리한다. 삭제는 개수를 보여 주며 한 번 더 묻는다.
 * [onSignInAgain] 은 세션 끊김 안내에서 로그인 화면을 연다.
 */
@Composable
fun AccountSessionDialogs(
    onSignInAgain: () -> Unit,
    viewModel: AccountSessionViewModel = hiltViewModel(),
) {
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    var confirmingDeleteFor by remember { mutableStateOf<AccountPrompt?>(null) }
    if (confirmingDeleteFor != null && confirmingDeleteFor != prompt) confirmingDeleteFor = null

    when (val current = prompt) {
        AccountPrompt.None -> Unit
        is AccountPrompt.Import -> if (confirmingDeleteFor == current) {
            DeleteDeviceDataConfirmDialog(
                itemCount = current.deviceItemCount,
                onConfirm = {
                    confirmingDeleteFor = null
                    viewModel.deleteDeviceDataForImport(current.uid)
                },
                onBack = { confirmingDeleteFor = null },
            )
        } else {
            val message = if (current.kind == ImportPromptKind.LEGACY_OTHER_ACCOUNT) {
                stringResource(R.string.account_import_message_legacy_other, current.shapeCount, current.sketchCount, current.droneCount)
            } else {
                stringResource(R.string.account_import_message, current.shapeCount, current.sketchCount, current.droneCount)
            }
            AlertDialog(
                onDismissRequest = { viewModel.cancelImport(current.uid) },
                title = { Text(stringResource(R.string.account_import_title)) },
                text = { Text(message) },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        importPromptButtons(current.kind).forEach { button ->
                            when (button) {
                                ImportPromptButton.IMPORT -> TextButton(onClick = { viewModel.chooseImport(current.uid) }) {
                                    Text(stringResource(R.string.account_import_action))
                                }
                                ImportPromptButton.DELETE -> TextButton(onClick = { confirmingDeleteFor = current }) {
                                    Text(stringResource(R.string.account_import_delete), color = MaterialTheme.colorScheme.error)
                                }
                                ImportPromptButton.CANCEL -> TextButton(onClick = { viewModel.cancelImport(current.uid) }) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                            }
                        }
                    }
                },
            )
        }
        is AccountPrompt.ImportCheckFailed -> AlertDialog(
            onDismissRequest = { viewModel.cancelImport(current.uid) },
            title = { Text(stringResource(R.string.account_import_check_failed_title)) },
            text = { Text(stringResource(R.string.account_import_check_failed_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.retryImportCheck() }) {
                    Text(stringResource(R.string.account_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImport(current.uid) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
        is AccountPrompt.ReplaceOtherAccountData -> AlertDialog(
            onDismissRequest = { viewModel.cancelReplace(current.uid) },
            title = { Text(stringResource(R.string.account_replace_title)) },
            text = { Text(stringResource(R.string.account_replace_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmReplace(current.uid) }) {
                    Text(stringResource(R.string.account_replace_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelReplace(current.uid) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
        AccountPrompt.SessionLost -> if (confirmingDeleteFor == current) {
            AlertDialog(
                onDismissRequest = { confirmingDeleteFor = null },
                title = { Text(stringResource(R.string.account_session_lost_delete)) },
                text = { Text(stringResource(R.string.account_session_lost_delete_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmingDeleteFor = null
                        viewModel.deleteDeviceDataAfterSessionLost()
                    }) {
                        Text(stringResource(R.string.account_import_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmingDeleteFor = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.acknowledgeSessionLost() },
                title = { Text(stringResource(R.string.account_session_lost_title)) },
                text = { Text(stringResource(R.string.account_session_lost_message)) },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = {
                            viewModel.acknowledgeSessionLost()
                            onSignInAgain()
                        }) {
                            Text(stringResource(R.string.account_session_lost_relogin))
                        }
                        TextButton(onClick = { confirmingDeleteFor = current }) {
                            Text(stringResource(R.string.account_session_lost_delete), color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { viewModel.acknowledgeSessionLost() }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                },
            )
        }
        AccountPrompt.AccountDeletedElsewhere -> AlertDialog(
            onDismissRequest = { viewModel.dismissAccountDeletedNotice() },
            title = { Text(stringResource(R.string.account_deleted_elsewhere_title)) },
            text = { Text(stringResource(R.string.account_deleted_elsewhere_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAccountDeletedNotice() }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }
}

@Composable
private fun DeleteDeviceDataConfirmDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(stringResource(R.string.account_import_delete_confirm_title)) },
        text = { Text(stringResource(R.string.account_import_delete_confirm_message, itemCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.account_import_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
