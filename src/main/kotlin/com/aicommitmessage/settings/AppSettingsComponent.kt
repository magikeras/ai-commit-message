package com.aicommitmessage.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import java.awt.Component

class AppSettingsComponent {

    private val apiKeyField = JBPasswordField()

    private val modelComboBox = JComboBox(arrayOf(
        "claude-sonnet-4-6",
        "claude-haiku-4-5-20251001",
        "claude-opus-4-6"
    ))

    private val localeComboBox = JComboBox(arrayOf("en", "ru"))

    private val messageDepthComboBox = JComboBox(arrayOf("short", "medium", "detailed")).apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = when (value) {
                    "short" -> "Short — feat(auth): add login endpoint"
                    "medium" -> "Medium — subject + short body (2-3 lines)"
                    "detailed" -> "Detailed — subject + body with bullet points"
                    else -> value.toString()
                }
                return this
            }
        }
    }

    private val skipTagComboBox = JComboBox(arrayOf("none", "[#skip-ci]", "[#skip-test]", "[#skip-ci] [#skip-test]")).apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = when (value) {
                    "none" -> "None"
                    "[#skip-ci]" -> "[#skip-ci] — skip CI pipeline"
                    "[#skip-test]" -> "[#skip-test] — skip tests"
                    "[#skip-ci] [#skip-test]" -> "[#skip-ci] [#skip-test] — skip both"
                    else -> value.toString()
                }
                return this
            }
        }
    }

    private val askSkipTagCheckBox = JBCheckBox("Ask about skip tag each time (popup before generation)")
    private val defaultSkipTagEnabledCheckBox = JBCheckBox("Append skip tag by default (when not asking)")

    private val customPromptArea = JBTextArea(5, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Anthropic API Key:", apiKeyField, 1, false)
        .addLabeledComponent("Model:", modelComboBox, 1, false)
        .addLabeledComponent("Commit message language:", localeComboBox, 1, false)
        .addLabeledComponent("Message depth:", messageDepthComboBox, 1, false)
        .addSeparator()
        .addLabeledComponent("Skip tag:", skipTagComboBox, 1, false)
        .addComponent(askSkipTagCheckBox)
        .addComponent(defaultSkipTagEnabledCheckBox)
        .addSeparator()
        .addLabeledComponent("Custom prompt (optional):", JScrollPane(customPromptArea), 1, true)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    val preferredFocusedComponent: JComponent
        get() = apiKeyField

    var apiKey: String
        get() = String(apiKeyField.password)
        set(value) { apiKeyField.text = value }

    var model: String
        get() = modelComboBox.selectedItem as String
        set(value) { modelComboBox.selectedItem = value }

    var locale: String
        get() = localeComboBox.selectedItem as String
        set(value) { localeComboBox.selectedItem = value }

    var messageDepth: String
        get() = messageDepthComboBox.selectedItem as String
        set(value) { messageDepthComboBox.selectedItem = value }

    var skipTag: String
        get() = skipTagComboBox.selectedItem as String
        set(value) { skipTagComboBox.selectedItem = value }

    var askSkipTag: Boolean
        get() = askSkipTagCheckBox.isSelected
        set(value) { askSkipTagCheckBox.isSelected = value }

    var defaultSkipTagEnabled: Boolean
        get() = defaultSkipTagEnabledCheckBox.isSelected
        set(value) { defaultSkipTagEnabledCheckBox.isSelected = value }

    var customPrompt: String
        get() = customPromptArea.text
        set(value) { customPromptArea.text = value }
}
