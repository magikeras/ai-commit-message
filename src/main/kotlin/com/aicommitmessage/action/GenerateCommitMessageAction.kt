package com.aicommitmessage.action

import com.aicommitmessage.service.AnthropicService
import com.aicommitmessage.settings.AppSettings
import com.aicommitmessage.util.DiffUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangeListManager
import kotlinx.coroutines.runBlocking

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

        val diff = DiffUtil.getStagedDiff(project)
        if (diff.isBlank()) {
            Messages.showInfoMessage(
                project,
                "No changes found. Please stage your changes first.",
                "AI Commit Message"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating commit message...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Calling Claude API..."

                try {
                    val message = runBlocking {
                        AnthropicService.generateCommitMessage(diff)
                    }

                    ApplicationManager.getApplication().invokeLater {
                        commitMessage.setCommitMessage(message)
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Failed to generate commit message: ${ex.message}",
                            "AI Commit Message Error"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val hasChanges = ChangeListManager.getInstance(project).allChanges.isNotEmpty()
        e.presentation.isEnabled = hasChanges
    }
}
