package com.simiacryptus.aicoder.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import java.util.*

enum class ComputerLanguage(configuration: Configuration) {
  Java(
    Configuration()
      .setDocumentationStyle("JavaDoc")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", " * ", " */"))
      .setFileExtensions("java")
  ),
  Cpp(
    Configuration()
      .setDocumentationStyle("Doxygen")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("cpp")
  ),
  LUA(
    Configuration()
      .setDocumentationStyle("LuaDoc")
      .setLineComments(LineComment.Factory("--"))
      .setBlockComments(BlockComment.Factory("--[[", "", "]]"))
      .setDocComments(BlockComment.Factory("---[[", "", "]]"))
      .setFileExtensions("lua")
  ),
  SVG(
    Configuration()
      .setDocumentationStyle("SVG")
      .setLineComments(LineComment.Factory("<!--"))
      .setBlockComments(BlockComment.Factory("<!--", "", "-->"))
      .setDocComments(BlockComment.Factory("<!--", "", "-->"))
      .setFileExtensions("svg")
  ),
  OpenSCAD(
    Configuration()
      .setDocumentationStyle("OpenSCAD")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("scad")
  ),
  Bash(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setFileExtensions("sh")
  ),
  Markdown(
    Configuration()
      .setDocumentationStyle("Markdown")
      .setLineComments(BlockComment.Factory("<!--", "", "-->"))
      .setBlockComments(BlockComment.Factory("<!--", "", "-->"))
      .setDocComments(BlockComment.Factory("<!--", "", "-->"))
      .setFileExtensions("md")
  ),
  Text(
    Configuration()
      .setDocumentationStyle("Text")
      .setLineComments(LineComment.Factory("#"))
      .setFileExtensions("txt")
  ),
  XML(
    Configuration()
      .setDocumentationStyle("XML")
      .setLineComments(BlockComment.Factory("<!--", "", "-->"))
      .setBlockComments(BlockComment.Factory("<!--", "", "-->"))
      .setDocComments(BlockComment.Factory("<!--", "", "-->"))
      .setFileExtensions("xml")
  ),
  Ada(
    Configuration()
      .setLineComments(LineComment.Factory("--"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("ada")
  ),
  Assembly(
    Configuration()
      .setLineComments(LineComment.Factory(";"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("assembly", "asm")
  ),
  Basic(
    Configuration()
      .setLineComments(LineComment.Factory("'"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("basic", "bs")
  ),
  C(
    Configuration()
      .setDocumentationStyle("Doxygen")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("c")
  ),
  Clojure(
    Configuration()
      .setDocumentationStyle("ClojureDocs")
      .setLineComments(LineComment.Factory(";"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("cj")
  ),
  COBOL(
    Configuration()
      .setLineComments(LineComment.Factory("*"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("cobol", "cob")
  ),
  CSharp(
    Configuration()
      .setDocumentationStyle("XML")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("cs", "c#")
  ),
  CSS(
    Configuration()
      .setLineComments(BlockComment.Factory("/*", "", "*/"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("css")
  ),
  Dart(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("dart")
  ),
  Delphi(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("delphi")
  ),
  Erlang(
    Configuration()
      .setLineComments(LineComment.Factory("%"))
      .setBlockComments(BlockComment.Factory("%%", "", "%%"))
      .setDocComments(BlockComment.Factory("%%%", "%", "%%%"))
      .setFileExtensions("erl")
  ),
  Elixir(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("elixir")
  ),
  FORTRAN(
    Configuration()
      .setLineComments(LineComment.Factory("!"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("f", "for", "ftn", "f77", "f90", "f95", "f03", "f08")
  ),
  FSharp(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("f#")
  ),
  Go(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("go")
  ),
  Groovy(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("groovy", "gradle")
  ),
  Haskell(
    Configuration()
      .setLineComments(LineComment.Factory("--"))
      .setBlockComments(BlockComment.Factory("{-", "-}", "{- -}"))
      .setDocComments(BlockComment.Factory("{-|", "|-}", "{-| -}"))
      .setFileExtensions("hs")
  ),
  HTML(
    Configuration()
      .setLineComments(BlockComment.Factory("<!--", "", "-->"))
      .setBlockComments(BlockComment.Factory("<!--", "", "-->"))
      .setDocComments(BlockComment.Factory("<!--", "", "-->"))
      .setFileExtensions("html")
  ),
  Julia(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("julia")
  ),
  JavaScript(
    Configuration()
      .setDocumentationStyle("JSDoc")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("js", "javascript")
  ),
  Json(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setFileExtensions("json")
  ),
  Kotlin(
    Configuration()
      .setDocumentationStyle("KDoc")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("kt", "kts")
  ),
  Lisp(
    Configuration()
      .setLineComments(LineComment.Factory(";"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("lisp")
  ),
  Logo(
    Configuration()
      .setLineComments(LineComment.Factory(";"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("logo", "log")
  ),
  MATLAB(
    Configuration()
      .setLineComments(LineComment.Factory("%"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("matlab", "m")
  ),
  OCaml(
    Configuration()
      .setLineComments(LineComment.Factory("(Params.create(*"))
      .setBlockComments(BlockComment.Factory("*))", "", "ocaml"))
      .setDocComments(BlockComment.Factory("*))", "", "ocaml"))
      .setFileExtensions("ml")
  ),
  Pascal(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("pascal", "pas")
  ),
  PHP(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("php")
  ),
  Perl(
    Configuration()
      .setDocumentationStyle("POD")
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("perl", "pl")
  ),
  Prolog(
    Configuration()
      .setLineComments(LineComment.Factory("%"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("prolog")
  ),
  Python(
    Configuration()
      .setDocumentationStyle("PyDoc")
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("py", "python")
  ),
  R(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("r")
  ),
  Ruby(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("ruby", "rb")
  ),
  Racket(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("#|", "", "|#"))
      .setDocComments(BlockComment.Factory("#|", "", "|#"))
      .setFileExtensions("racket")
  ),
  Rust(
    Configuration()
      .setDocumentationStyle("Rustdoc")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("rs", "rust")
  ),
  Scala(
    Configuration()
      .setDocumentationStyle("ScalaDoc")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("scala", "sc")
  ),
  Scheme(
    Configuration()
      .setLineComments(LineComment.Factory(";"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("scheme")
  ),
  SCSS(
    Configuration()
      .setDocumentationStyle("SCSS")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(LineComment.Factory("///"))
      .setFileExtensions("scss")
  ),
  SQL(
    Configuration()
      .setLineComments(LineComment.Factory("--"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("sql")
  ),
  Smalltalk(
    Configuration()
      .setLineComments(LineComment.Factory("\""))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("smalltalk", "st")
  ),
  Swift(
    Configuration()
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("swift")
  ),
  Tcl(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("tcl")
  ),
  TypeScript(
    Configuration()
      .setDocumentationStyle("TypeDoc")
      .setLineComments(LineComment.Factory("//"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("typescript", "ts")
  ),
  VisualBasic(
    Configuration()
      .setLineComments(LineComment.Factory("'"))
      .setBlockComments(BlockComment.Factory("/*", "", "*/"))
      .setDocComments(BlockComment.Factory("/**", "*", "*/"))
      .setFileExtensions("visualbasic", "vb")
  ),
  YAML(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setFileExtensions("yaml")
  ),
  ZShell(
    Configuration()
      .setLineComments(LineComment.Factory("#"))
      .setFileExtensions("zsh")
  );

  val extensions: List<CharSequence>
  val docStyle: String
  val lineComment: TextBlockFactory<*>
  val blockComment: TextBlockFactory<*>
  private val docComment: TextBlockFactory<*>

  init {
    extensions = listOf(*configuration.fileExtensions)
    docStyle = configuration.documentationStyle
    lineComment = configuration.lineComments!!
    blockComment = configuration.getBlockComments()!!
    docComment = configuration.getDocComments()!!
  }

  fun getCommentModel(text: String?): TextBlockFactory<*> {
    if (Objects.requireNonNull(docComment)!!.looksLike(text)) return docComment
    return if (Objects.requireNonNull(blockComment)!!.looksLike(text)) blockComment else lineComment
  }

  internal class Configuration {
    var documentationStyle = ""
      private set
    var fileExtensions = arrayOf<CharSequence>()
      private set
    var lineComments: TextBlockFactory<*>? = null
      private set
    private var blockComments: TextBlockFactory<*>? = null
    private var docComments: TextBlockFactory<*>? = null
    fun setDocumentationStyle(documentationStyle: String): Configuration {
      this.documentationStyle = documentationStyle
      return this
    }

    fun setFileExtensions(vararg fileExtensions: CharSequence): Configuration {
      @Suppress("UNCHECKED_CAST")
      this.fileExtensions = fileExtensions as Array<CharSequence>
      return this
    }

    fun setLineComments(lineComments: TextBlockFactory<*>): Configuration {
      this.lineComments = lineComments
      return this
    }

    fun getBlockComments(): TextBlockFactory<*>? {
      return if (null == blockComments) lineComments else blockComments
    }

    fun setBlockComments(blockComments: TextBlockFactory<*>): Configuration {
      this.blockComments = blockComments
      return this
    }

    fun getDocComments(): TextBlockFactory<*>? {
      return if (null == docComments) getBlockComments() else docComments
    }

    fun setDocComments(docComments: TextBlockFactory<*>): Configuration {
      this.docComments = docComments
      return this
    }
  }

  companion object {
    @JvmStatic
    fun findByExtension(extension: CharSequence): ComputerLanguage? {
      return Arrays.stream(values()).filter { x: ComputerLanguage ->
        x.extensions.contains(
          extension
        )
      }.findAny().orElse(null)
    }

    @JvmStatic
    fun getComputerLanguage(e: AnActionEvent?): ComputerLanguage? {
      val file = e?.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
      val extension = if (file.extension != null) file.extension!!.lowercase(Locale.getDefault()) else ""
      return findByExtension(extension)
    }
  }
}