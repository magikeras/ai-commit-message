package com.aicommitmessage.action

import com.aicommitmessage.service.AnthropicService
import com.aicommitmessage.settings.AppSettings
import com.aicommitmessage.util.DiffUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenerateCommitMessageAction : AnAction(
    "Generate Commit Message",
    "Generate commit message using Claude AI",
    AllIcons.Actions.Lightning
) {

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

        val selectedChanges = e.getData(VcsDataKeys.CHANGES)
        if (selectedChanges.isNullOrEmpty()) {
            Messages.showInfoMessage(
                project,
                "No changes selected. Please select files to include in the commit.",
                "AI Commit Message"
            )
            return
        }

        val diff = DiffUtil.getDiffFromChanges(selectedChanges.toList())
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

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val changes = e.getData(VcsDataKeys.CHANGES)
        e.presentation.isEnabled = !changes.isNullOrEmpty()
    }
}
