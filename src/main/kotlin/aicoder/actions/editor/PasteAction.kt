package aicoder.actions.editor

// ... keep existing imports
import aicoder.actions.SelectionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.util.ComputerLanguage
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.models.ChatModel
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.jopenai.proxy.ChatProxy
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor.*

/**
 * Base class for paste actions that convert clipboard content to appropriate code format
 * Supports both text and HTML clipboard content with automatic language detection
 */
abstract class PasteActionBase(private val model: (AppSettingsState) -> ChatModel) : SelectionAction<String>(false) {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  /**
   * API interface for code conversion
   */
  interface VirtualAPI {
    fun convert(text: String, to_language: String): ConvertedText
    @JsonDeserialize(using = ConvertedTextDeserializer::class)
    
    class ConvertedText {
      var converted_text: String? = null
    }
    /**
     * Custom deserializer for ConvertedText that can handle different response formats:
     * - Direct string values
     * - Objects with a single text attribute (regardless of attribute name)
     * - Standard objects with the expected "converted_text" attribute
     */
    class ConvertedTextDeserializer : JsonDeserializer<ConvertedText>() {
      override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ConvertedText {
        val node: JsonNode = p.codec.readTree(p)
        val result = ConvertedText()
        when {
          // Case 1: Direct string value
          node.isTextual -> {
            result.converted_text = node.asText()
          }
          // Case 2: Object with expected "converted_text" field
          node.has("converted_text") -> {
            result.converted_text = node.get("converted_text").asText()
          }
          // Case 3: Object with a single text field (use first field found)
          node.isObject && node.fields().hasNext() -> {
            val fields = node.fields()
            while (fields.hasNext()) {
              val field = fields.next()
              if (field.value.isTextual) {
                result.converted_text = field.value.asText()
                break
              }
            }
          }
        }
        return result
      }
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(PasteActionBase::class.java)
    fun scrubHtml(str: String, maxLength: Int = 100 * 1024): String {
      val document: Document = Jsoup.parse(str)
      // Remove unnecessary elements, attributes, and optimize the document
      document.apply {
        fun qTry(block: () -> Unit) {
          return try {
            block()
          } catch (e: Exception) {
            log.error("Error in scrubHtml", e)
          }
        }
        if ((document.body()?.html()?.length ?: 0) > maxLength) return document.body()?.html()?.substring(0, maxLength) ?: ""
        select("script, style, link, meta, iframe, noscript").remove() // Remove unnecessary and potentially harmful tags
        outputSettings().prettyPrint(false) // Disable pretty printing for compact output
        if ((document.body()?.html()?.length ?: 0) > maxLength) return document.body()?.html()?.substring(0, maxLength) ?: ""
        // Remove comments
        qTry { select("*").forEach { it.childNodes().removeAll { node -> node.nodeName() == "#comment" } } }
        if (document.body().html().length > maxLength) return@apply
        // Remove data-* attributes
        qTry { select("*[data-*]").forEach { it.attributes().removeAll { attr -> attr.key.startsWith("data-") } } }
        if (document.body().html().length > maxLength) return@apply
        qTry {
          select("*").forEach { element ->
            val importantAttributes = setOf("href", "src", "alt", "title", "width", "height", "style", "class", "id", "name")
            element.attributes().removeAll { it.key !in importantAttributes }
          }
        }
        if (document.body().html().length > maxLength) return@apply
        // Remove empty elements
        qTry {
          select("*").forEach { element ->
            if (element.childNodes().isEmpty() && element.attributes().isEmpty()) {
              element.remove()
            }
          }
        }
        if (document.body().html().length > maxLength) return@apply
        // Unwrap single-child elements with no attributes
        qTry {
          select("*").forEach { element ->
            if (element.childNodes().size == 1 && element.childNodes()[0].nodeName() == "#text" && element.attributes().isEmpty()) {
              element.unwrap()
            }
          }
        }
        if (document.body().html().length > maxLength) return@apply
        // Convert relative URLs to absolute
        qTry {
          select("[href],[src]").forEach { element ->
            element.attr("href").let { href -> element.attr("href", href.makeAbsolute()) }
            element.attr("src").let { src -> element.attr("src", src.makeAbsolute()) }
          }
        }
        if (document.body().html().length > maxLength) return@apply
        // Remove empty attributes
        qTry {
          select("*").forEach { element ->
            element.attributes().removeAll { it.value.isBlank() }
          }
        }
      }

      // Truncate if necessary
      val result = document.body().html()
      return if (result.length > maxLength) {
        result.substring(0, maxLength)
      } else {
        result
      }
    }

    fun getClipboard(): Any? {
      try {
        val toolkit = Toolkit.getDefaultToolkit()
        val systemClipboard = toolkit.systemClipboard
        return systemClipboard.getContents(null)?.let { contents ->
          return when {
            contents.isDataFlavorSupported(selectionHtmlFlavor) -> contents.getTransferData(selectionHtmlFlavor).toString().trim().let { scrubHtml(it) }
            contents.isDataFlavorSupported(fragmentHtmlFlavor) -> contents.getTransferData(fragmentHtmlFlavor).toString().trim().let { scrubHtml(it) }
            contents.isDataFlavorSupported(allHtmlFlavor) -> contents.getTransferData(allHtmlFlavor).toString().trim().let { scrubHtml(it) }
            contents.isDataFlavorSupported(stringFlavor) -> contents.getTransferData(stringFlavor)
            contents.isDataFlavorSupported(getTextPlainUnicodeFlavor()) -> contents.getTransferData(getTextPlainUnicodeFlavor())
            else -> null
          }
        }
      } catch (e: Exception) {
        log.error("Failed to access clipboard", e)
        return null
      }
    }

    fun hasClipboard() = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
      return when {
        contents.isDataFlavorSupported(stringFlavor) -> true
        contents.isDataFlavorSupported(getTextPlainUnicodeFlavor()) -> true
        else -> false
      }
    } ?: false

    fun converter(chatClient: ChatClient, chatModel: ChatModel, temp: Double) = ChatProxy(
      clazz = VirtualAPI::class.java,
      api = chatClient,
      model = chatModel,
      temperature = temp
    ).create()
  }

  override fun getConfig(project: Project?): String {
    return ""
  }


  override fun processSelection(state: SelectionState, config: String?, progress: ProgressIndicator): String {
    val progress: ProgressIndicator? = state.progress
    progress?.text = "Reading clipboard content..."
    val clipboardContent = getClipboard() ?: return ""
    val text = clipboardContent.toString().trim()
    if (text.isEmpty()) return ""
    progress?.text = "Converting code format..."
    val converter = converter(api, model(AppSettingsState.instance), AppSettingsState.instance.temperature)
    val convert = converter.convert(text, state.language?.name ?: state.editor?.virtualFile?.extension ?: "")
    return convert.converted_text ?: ""
  }

  override fun isLanguageSupported(computerLanguage: ComputerLanguage?): Boolean {
    return true
  }

  override fun isEnabled(event: AnActionEvent): Boolean {
    if (!hasClipboard()) return false
    return super.isEnabled(event)
  }

}

private fun String.makeAbsolute(): String {
  return if (startsWith("http://") || startsWith("https://") || startsWith("//")) {
    this
  } else {
    "https://$this"
  }
}

class SmartPasteAction : PasteActionBase({ it.smartModel.chatModel() })

/**
 * Fast paste action using faster but simpler model
 */
class FastPasteAction : PasteActionBase({ it.fastModel.chatModel() }) {
  companion object {
    private val log = LoggerFactory.getLogger(FastPasteAction::class.java)
  }

  protected var progress: ProgressIndicator? = null
}