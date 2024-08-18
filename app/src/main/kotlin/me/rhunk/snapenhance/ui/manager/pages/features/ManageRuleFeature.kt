package me.rhunk.snapenhance.ui.manager.pages.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.RuleState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncUpdateDispatcher
import me.rhunk.snapenhance.storage.clearRuleIds
import me.rhunk.snapenhance.storage.getRuleIds
import me.rhunk.snapenhance.storage.setRule
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendDialog
import me.rhunk.snapenhance.ui.manager.pages.social.AddFriendDialog.Actions
import me.rhunk.snapenhance.ui.util.AlertDialogs
import me.rhunk.snapenhance.ui.util.Dialog

class ManageRuleFeature : Routes.Route()  {
    @Composable
    fun SelectRuleTypeRadio(
        checked: Boolean,
        text: String,
        onStateChanged: (Boolean) -> Unit,
        selectedBlock: @Composable () -> Unit = {},
    ) {
        Box(modifier = Modifier.clickable {
            onStateChanged(!checked)
        }) {
            Column(
                modifier = Modifier
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = checked, onClick = null)
                    Text(text)
                }
                if (checked) {
                    Column(modifier = Modifier
                        .offset(x = 15.dp)
                        .padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        selectedBlock()
                    }
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = content@{ navBackStackEntry ->
        val currentRuleType = navBackStackEntry.arguments?.getString("rule_type")?.let {
            MessagingRuleType.getByName(it)
        } ?: return@content

        var ruleState by remember {
            mutableStateOf(context.config.root.rules.getRuleState(currentRuleType))
        }

        val propertyKeyPair = remember {
            context.config.root.rules.getPropertyPair(currentRuleType.key)
        }

        val updateDispatcher = rememberAsyncUpdateDispatcher()
        val currentRuleIds by rememberAsyncMutableState(defaultValue = mutableListOf(), updateDispatcher = updateDispatcher) {
            context.database.getRuleIds(currentRuleType.key)
        }

        fun setRuleState(newState: RuleState?) {
            ruleState = newState
            propertyKeyPair.value.setAny(newState?.key)
            context.coroutineScope.launch {
                context.config.writeConfig(dispatchConfigListener = false)
            }
        }

        var addFriendDialog by remember { mutableStateOf(null as AddFriendDialog?) }

        LaunchedEffect(addFriendDialog) {
            if (addFriendDialog == null) {
                updateDispatcher.dispatch()
            }
        }

        fun showAddFriendDialog() {
            addFriendDialog = AddFriendDialog(
                context = context,
                pinnedIds = currentRuleIds,
                actionHandler = Actions(
                    onFriendState = { friend, state ->
                        context.database.setRule(friend.userId, currentRuleType.key, state)
                        if (state) {
                            currentRuleIds.add(friend.userId)
                        } else {
                            currentRuleIds.remove(friend.userId)
                        }
                    },
                    onGroupState = { group, state ->
                        context.database.setRule(group.conversationId, currentRuleType.key, state)
                        if (state) {
                            currentRuleIds.add(group.conversationId)
                        } else {
                            currentRuleIds.remove(group.conversationId)
                        }
                    },
                    getFriendState = { friend ->
                        currentRuleIds.contains(friend.userId)
                    },
                    getGroupState = { group ->
                        currentRuleIds.contains(group.conversationId)
                    }
                )
            )
        }

        if (addFriendDialog != null) {
            addFriendDialog?.Content {
                addFriendDialog = null
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = remember {
                        context.translation[propertyKeyPair.key.propertyName()]
                    },
                    fontSize = 20.sp,
                )
                Text(
                    text = remember {
                        context.translation[propertyKeyPair.key.propertyDescription()]
                    },
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            SelectRuleTypeRadio(checked = ruleState == null, text = translation["disable_state_option"], onStateChanged = {
                setRuleState(null)
            }) {
                Text(text = translation["disable_state_subtext"], fontWeight = FontWeight.Light, fontSize = 12.sp)
            }
            SelectRuleTypeRadio(checked = ruleState == RuleState.WHITELIST, text = translation["whitelist_state_option"], onStateChanged = {
                setRuleState(RuleState.WHITELIST)
            }) {
                Text(text = translation.format("whitelist_state_subtext", "count" to currentRuleIds.size.toString()), fontWeight = FontWeight.Light, fontSize = 12.sp)
                OutlinedButton(onClick = {
                    showAddFriendDialog()
                }) {
                    Text(text = translation["whitelist_state_button"])
                }
            }
            SelectRuleTypeRadio(checked = ruleState == RuleState.BLACKLIST, text = translation["blacklist_state_option"], onStateChanged = {
                setRuleState(RuleState.BLACKLIST)
            }) {
                Text(text = translation.format("blacklist_state_subtext", "count" to currentRuleIds.size.toString()), fontWeight = FontWeight.Light, fontSize = 12.sp)
                OutlinedButton(onClick = { showAddFriendDialog() }) {
                    Text(text = translation["blacklist_state_button"])
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                var confirmationDialog by remember { mutableStateOf(false) }

                if (confirmationDialog) {
                    Dialog(onDismissRequest = {
                        confirmationDialog = false
                    }) {
                        remember { AlertDialogs(context.translation) }.ConfirmDialog(
                            title = translation["dialog_clear_confirmation_text"],
                            onDismiss = { confirmationDialog = false },
                            onConfirm = {
                                context.database.clearRuleIds(currentRuleType.key)
                                context.coroutineScope.launch(context.database.executor.asCoroutineDispatcher()) {
                                    updateDispatcher.dispatch()
                                }
                                confirmationDialog = false
                            }
                        )
                    }
                }

                Button(onClick = { confirmationDialog = true }) {
                    Text(text = translation["clear_list_button"])
                }
            }
        }
    }
}