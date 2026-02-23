package com.aicommitmessage.action

import com.aicommitmessage.service.AnthropicService
import com.aicommitmessage.settings.AppSettings
import com.aicommitmessage.util.DiffUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ui.Refreshable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenerateCommitMessageAction : AnAction(
    "Generate Commit Message",
    "Generate commit message using Claude AI",
    AllIcons.Actions.Lightning
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitMessage = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) ?: return

        val settings = AppSettings.instance
        if (settings.apiKey.isBlank()) {
            Messages.showWarningDialog(
                project,
                "Please configure your Anthropic API key in Settings > Tools > AI Commit Message.",
                "AI Commit Message"
            )
            return
        }

        // Get checked (included) changes from the commit panel
        val changes = getIncludedChanges(e)
        if (changes.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No changes selected. Please select files to include in the commit.",
                "AI Commit Message"
            )
            return
        }

        val diff = DiffUtil.getDiffFromChanges(changes)
        if (diff.isBlank()) {
            Messages.showInfoMessage(
                project,
                "No diff content found in selected changes.",
                "AI Commit Message"
            )
            return
        }

        commitMessage.setCommitMessage("(generating commit message...)")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = AnthropicService.generateCommitMessage(diff)
                ApplicationManager.getApplication().invokeLater {
                    commitMessage.setCommitMessage(message)
                }
            } catch (ex: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    commitMessage.setCommitMessage("")
                    Messages.showErrorDialog(
                        project,
                        "Failed to generate commit message: ${ex.message}",
                        "AI Commit Message Error"
                    )
                }
            }
        }
    }

    private fun getIncludedChanges(e: AnActionEvent): Collection<com.intellij.openapi.vcs.changes.Change> {
        // 1. Try CheckinProjectPanel (has checked/included changes)
        val refreshable = e.getData(Refreshable.PANEL_KEY)
        if (refreshable is CheckinProjectPanel) {
            val selected = refreshable.selectedChanges
            if (selected.isNotEmpty()) return selected
        }

        // 2. Try VcsDataKeys.CHANGES (highlighted selection)
        val vcsChanges = e.getData(VcsDataKeys.CHANGES)
        if (!vcsChanges.isNullOrEmpty()) return vcsChanges.toList()

        // 3. Fallback: all changes from default changelist
        val project = e.project ?: return emptyList()
        return ChangeListManager.getInstance(project).defaultChangeList.changes
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
