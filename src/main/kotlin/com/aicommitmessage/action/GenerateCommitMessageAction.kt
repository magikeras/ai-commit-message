package com.aicommitmessage.action

import com.aicommitmessage.service.AnthropicService
import com.aicommitmessage.settings.AppSettings
import com.aicommitmessage.util.BranchTicket
import com.aicommitmessage.util.DiffUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.CommitMessageI
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ui.Refreshable
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
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
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message")
            return
        }

        val (changes, unversionedFiles) = getIncludedChangesAndFiles(e)

        if (changes.isEmpty() && unversionedFiles.isEmpty()) {
            commitMessage.setCommitMessage("no selected changes")
            return
        }

        val diff = DiffUtil.getDiffFromChanges(changes, unversionedFiles)
        if (diff.isBlank()) {
            commitMessage.setCommitMessage("no selected changes")
            return
        }

        val ticketId = if (settings.appendBranchTicket) findBranchTicketId(project, changes, unversionedFiles) else null

        if (settings.askSkipTag) {
            showSkipTagPopup(e, project, commitMessage, diff, ticketId)
        } else {
            val skipTag = if (settings.defaultSkipTagEnabled && settings.skipTag != "none") {
                settings.skipTag
            } else {
                "none"
            }
            generate(project, commitMessage, diff, skipTag, ticketId)
        }
    }

    private fun showSkipTagPopup(
        e: AnActionEvent,
        project: Project,
        commitMessage: CommitMessageI,
        diff: String,
        ticketId: String?
    ) {
        val settings = AppSettings.instance
        val items = listOf(
            "No skip tag",
            "[#skip-ci]",
            "[#skip-test]",
            "[#skip-ci] [#skip-test]"
        )

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setTitle("Skip Tag")
            .setItemChosenCallback { chosen ->
                val skipTag = if (chosen == "No skip tag") "none" else chosen
                if (skipTag != "none") settings.skipTag = skipTag
                generate(project, commitMessage, diff, skipTag, ticketId)
            }
            .createPopup()
            .showInBestPositionFor(e.dataContext)
    }

    private fun generate(
        project: Project,
        commitMessage: CommitMessageI,
        diff: String,
        skipTag: String,
        ticketId: String?
    ) {
        commitMessage.setCommitMessage("(generating commit message...)")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val generated = AnthropicService.generateCommitMessage(diff, skipTag)
                val message = ticketId?.let { BranchTicket.appendTicket(generated, it) } ?: generated
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

    private fun findBranchTicketId(
        project: Project,
        changes: Collection<Change>,
        unversionedFiles: Collection<VirtualFile>
    ): String? {
        val branchName = findRepository(project, changes, unversionedFiles)?.currentBranchName ?: return null
        return BranchTicket.extractTicketId(branchName)
    }

    private fun findRepository(
        project: Project,
        changes: Collection<Change>,
        unversionedFiles: Collection<VirtualFile>
    ): GitRepository? {
        val manager = GitRepositoryManager.getInstance(project)
        return changes.firstNotNullOfOrNull { change ->
            (change.afterRevision?.file ?: change.beforeRevision?.file)?.let { manager.getRepositoryForFileQuick(it) }
        } ?: unversionedFiles.firstNotNullOfOrNull { manager.getRepositoryForFileQuick(it) }
            ?: manager.repositories.firstOrNull()
    }

    private fun getIncludedChangesAndFiles(e: AnActionEvent): Pair<Collection<Change>, Collection<VirtualFile>> {
        val refreshable = e.getData(Refreshable.PANEL_KEY)
        if (refreshable is CheckinProjectPanel) {
            val changes = refreshable.selectedChanges
            val allFiles = refreshable.virtualFiles
            val changedPaths = changes.mapNotNull {
                (it.afterRevision?.file ?: it.beforeRevision?.file)?.path
            }.toSet()
            val unversionedFiles = allFiles.filter { it.path !in changedPaths }
            return Pair(changes, unversionedFiles)
        }

        val vcsChanges = e.getData(VcsDataKeys.CHANGES)
        if (!vcsChanges.isNullOrEmpty()) {
            return Pair(vcsChanges.toList(), emptyList())
        }

        val project = e.project ?: return Pair(emptyList(), emptyList())
        return Pair(ChangeListManager.getInstance(project).defaultChangeList.changes, emptyList())
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
