package com.aicommitmessage.settings

import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class AppSettingsComponent {

    private val apiKeyField = JBPasswordField()

    private val modelComboBox = JComboBox(arrayOf(
        "claude-sonnet-4-6",
        "claude-haiku-4-5-20251001",
        "claude-opus-4-6"
    ))

    private val localeComboBox = JComboBox(arrayOf("en", "ru"))

    private val customPromptArea = JBTextArea(5, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Anthropic API Key:", apiKeyField, 1, false)
        .addLabeledComponent("Model:", modelComboBox, 1, false)
        .addLabeledComponent("Commit message language:", localeComboBox, 1, false)
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

    var customPrompt: String
        get() = customPromptArea.text
        set(value) { customPromptArea.text = value }
}
