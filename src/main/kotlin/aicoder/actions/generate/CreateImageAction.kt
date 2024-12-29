package aicoder.actions.generate

import aicoder.actions.BaseAction
import aicoder.actions.agent.toFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.ui.JBUI
import com.simiacryptus.aicoder.config.AppSettingsState
import com.simiacryptus.aicoder.config.AppSettingsState.Companion.imageModel
import com.simiacryptus.aicoder.util.IdeaOpenAIClient
import com.simiacryptus.aicoder.util.UITools
import com.simiacryptus.jopenai.models.chatModel
import com.simiacryptus.skyenet.core.actors.ImageActor
import com.simiacryptus.skyenet.core.actors.ImageResponse
import com.simiacryptus.skyenet.core.util.getModuleRootForFile
import org.slf4j.LoggerFactory
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.*

class CreateImageAction : BaseAction() {
  inner class ImageGenerationDialog(project: Project) : DialogWrapper(project) {
    private val fileNameField = JTextField(generateDefaultFileName(), 20)
    private val instructionsArea = JTextArea(3, 20)

    init {
      log.debug("Initializing ImageGenerationDialog")
      title = "Generate Image"
      init()
    }

    private fun generateDefaultFileName(): String {
      val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      return "generated_image_$timestamp.png"
    }


    override fun createCenterPanel(): JComponent {
      return JPanel(GridBagLayout()).apply {
        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = JBUI.insets(5)
        c.gridx = 0; c.gridy = 0
        add(JLabel("Output filename:"), c)
        c.gridx = 1; c.gridy = 0
        add(fileNameField, c)
        c.gridx = 0; c.gridy = 1
        add(JLabel("Special instructions:"), c)
        c.gridx = 1; c.gridy = 1
        c.fill = GridBagConstraints.BOTH
        add(JScrollPane(instructionsArea), c)
      }
    }

    fun getFileName() = fileNameField.text
    fun getInstructions() = instructionsArea.text
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun handle(e: AnActionEvent) {
    log.info("Starting CreateImageAction handler")
    val rootRef = AtomicReference<Path?>(null)
    val codeFiles: MutableSet<Path> = mutableSetOf()
    val dialog = ImageGenerationDialog(e.project!!)
    if (!dialog.showAndGet()) {
      log.debug("Dialog cancelled by user")
      return
    }
    UITools.runAsync(e.project, "Creating Image", true) { progress ->
      try {
        progress.text = "Analyzing code files..."
        log.debug("Beginning code analysis")
        fun codeSummary() = codeFiles.filter {
          rootRef.get()?.resolve(it)?.toFile()?.exists() ?: false
        }.associateWith { rootRef.get()?.resolve(it)?.toFile()?.readText(Charsets.UTF_8) }.entries.joinToString("\n\n") { (path, code) ->
          val extension = path.toString().split('.').lastOrNull()?.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }
          "# $path\n```$extension\n${code}\n```"
        }

        val dataContext = e.dataContext
        val virtualFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)
        log.debug("Found ${virtualFiles?.size ?: 0} virtual files")
        progress.text = "Determining root directory..."
        val folder = UITools.getSelectedFolder(e)
        rootRef.set(
          if (null != folder) {
            log.debug("Using selected folder as root: ${folder.toFile}")
            folder.toFile.toPath()
          } else if (1 == virtualFiles?.size) {
            log.debug("Using parent of single file as root")
            UITools.getSelectedFile(e)?.parent?.toNioPath()
          } else {
            log.debug("Using module root as root directory")
            getModuleRootForFile(UITools.getSelectedFile(e)?.parent?.toFile ?: throw RuntimeException("No file selected")).toPath()
          }
        )
        progress.text = "Collecting files..."

        val root = rootRef.get() ?: throw RuntimeException("Root path not set")
        if (!Files.exists(root)) {
          throw IOException("Root directory does not exist: $root")
        }
        log.info("Using root directory: $root")
        val files = getFiles(virtualFiles, root)
        codeFiles.addAll(files)
        log.debug("Collected ${codeFiles.size} code files")
        progress.text = "Generating image..."
        log.info("Starting image generation with ${codeFiles.size} files")
        val imageActor = ImageActor(
          prompt = """
                    You are a technical drawing assistant.
                    You will be composing an image about the following code:
                    ${codeSummary()}
                    Special instructions: ${dialog.getInstructions()}
                    """.trimIndent(),
          textModel = AppSettingsState.instance.smartModel.chatModel(),
          imageModel = AppSettingsState.instance.mainImageModel.imageModel()
        ).apply { setImageAPI(IdeaOpenAIClient.instance) }
        log.debug("Sending request to image generation API")
        val response = imageActor.answer(listOf(codeSummary(), dialog.getInstructions()), api)
        log.debug("Image generation completed successfully")
        val imagePath = root.resolve(dialog.getFileName())
        write(response, imagePath)
        VirtualFileManager.getInstance().findFileByNioPath(imagePath)?.refresh(false, false)
      } catch (ex: Throwable) {
        when (ex) {
          is IOException -> log.error("IO error during image creation: ${ex.message}", ex)
          is SecurityException -> log.error("Security error during image creation: ${ex.message}", ex)
          is IllegalArgumentException -> log.error("Invalid argument during image creation: ${ex.message}", ex)
          else -> log.error("Unexpected error during image creation", ex)
        }
        UITools.showErrorDialog("Failed to create image: ${ex.message}", "Error")
      }
    }
  }

  private fun write(
    code: ImageResponse, path: Path
  ) = try {
    log.debug("Creating parent directories for: $path")
    path.parent?.toFile()?.mkdirs()
    val format = path.toString().split(".").last()
    log.debug("Writing image in format: $format")

    val bytes = ByteArrayOutputStream().use { outputStream ->
      if (!ImageIO.write(
          code.image, format, outputStream
        )
      ) {
        throw IOException("Unsupported or invalid image format: $format")
      }
      outputStream.toByteArray()
    }
    path.toFile().writeBytes(bytes)
    path
  } catch (e: Exception) {
    log.error("Failed to write image to $path", e)
    when (e) {
      is IOException -> throw IOException("Failed to write image: ${e.message}", e)
      is SecurityException -> throw SecurityException("Security error writing image: ${e.message}", e)
      else -> throw RuntimeException("Unexpected error writing image: ${e.message}", e)
    }
  }

  private fun getFiles(
    virtualFiles: Array<out VirtualFile>?, root: Path
  ): MutableSet<Path> {
    val codeFiles = mutableSetOf<Path>()
    virtualFiles?.forEach { file ->
      if (file.isDirectory) {
        getFiles(file.children, root)
      } else {
        val relative = root.relativize(file.toNioPath())
        codeFiles.add(relative) //[] = file.contentsToByteArray().toString(Charsets.UTF_8)
      }
    }
    return codeFiles
  }

  override fun isEnabled(event: AnActionEvent): Boolean {
    UITools.getSelectedFile(event) ?: return false
    return true
  }

  companion object {
    private val log = LoggerFactory.getLogger(CreateImageAction::class.java)
  }
}