package com.github.simiacryptus.aicoder.util;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public enum ComputerLanguage {
    Java(new Configuration()
            .setDocumentationStyle("JavaDoc")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", " * ", " */"))
            .setFileExtensions("java")),
    Cpp(new Configuration()
            .setDocumentationStyle("Doxygen")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("cpp")),
    SVG(new Configuration()
            .setDocumentationStyle("SVG")
            .setLineComments(new LineComment.Factory("<!--"))
            .setBlockComments(new BlockComment.Factory("<!--", "", "-->"))
            .setDocComments(new BlockComment.Factory("<!--", "", "-->"))
            .setFileExtensions("svg")),
    OpenSCAD(new Configuration()
            .setDocumentationStyle("OpenSCAD")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("scad")),
    Bash(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setFileExtensions("sh")),
    Markdown(new Configuration()
            .setDocumentationStyle("Markdown")
            .setLineComments(new BlockComment.Factory("<!--", "", "-->"))
            .setBlockComments(new BlockComment.Factory("<!--", "", "-->"))
            .setDocComments(new BlockComment.Factory("<!--", "", "-->"))
            .setFileExtensions("md")),
    Ada(new Configuration()
            .setLineComments(new LineComment.Factory("--"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("ada")),
    Assembly(new Configuration()
            .setLineComments(new LineComment.Factory(";"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("assembly", "asm")),
    Basic(new Configuration()
            .setLineComments(new LineComment.Factory("'"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("basic", "bs")),
    C(new Configuration()
            .setDocumentationStyle("Doxygen")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("c")),
    Clojure(new Configuration()
            .setDocumentationStyle("ClojureDocs")
            .setLineComments(new LineComment.Factory(";"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("cj")),
    COBOL(new Configuration()
            .setLineComments(new LineComment.Factory("*"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("cobol", "cob")),
    CSharp(new Configuration()
            .setDocumentationStyle("XML")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("c#", "cs")),
    CSS(new Configuration()
            .setLineComments(new BlockComment.Factory("/*", "", "*/"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("css")),
    Dart(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("dart")),
    Delphi(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("delphi")),
    Erlang(new Configuration()
            .setLineComments(new LineComment.Factory("%"))
            .setBlockComments(new BlockComment.Factory("%%", "", "%%"))
            .setDocComments(new BlockComment.Factory("%%%", "%", "%%%"))
            .setFileExtensions("erl")),
    Elixir(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("elixir")),
    FORTRAN(new Configuration()
            .setLineComments(new LineComment.Factory("!"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("f", "for", "ftn", "f77", "f90", "f95", "f03", "f08")),
    FSharp(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("f#")),
    Go(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("go")),
    Groovy(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("groovy")),
    Haskell(new Configuration()
            .setLineComments(new LineComment.Factory("--"))
            .setBlockComments(new BlockComment.Factory("{-", "-}", "{- -}"))
            .setDocComments(new BlockComment.Factory("{-|", "|-}", "{-| -}"))
            .setFileExtensions("hs")),
    HTML(new Configuration()
            .setLineComments(new BlockComment.Factory("<!--", "", "-->"))
            .setBlockComments(new BlockComment.Factory("<!--", "", "-->"))
            .setDocComments(new BlockComment.Factory("<!--", "", "-->"))
            .setFileExtensions("html")),
    Julia(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("julia")),
    JavaScript(new Configuration()
            .setDocumentationStyle("JSDoc")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("js", "javascript")),
    Json(new Configuration()
            .setFileExtensions("json")),
    Kotlin(new Configuration()
            .setDocumentationStyle("KDoc")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("kotlin", "kt", "kts")),
    Lisp(new Configuration()
            .setLineComments(new LineComment.Factory(";"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("lisp")),
    Logo(new Configuration()
            .setLineComments(new LineComment.Factory(";"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("logo", "log")),
    MATLAB(new Configuration()
            .setLineComments(new LineComment.Factory("%"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("matlab", "m")),
    OCaml(new Configuration()
            .setLineComments(new LineComment.Factory("(Params.create(*"))
            .setBlockComments(new BlockComment.Factory("*))", "", "ocaml"))
            .setDocComments(new BlockComment.Factory("*))", "", "ocaml"))
            .setFileExtensions("ml")),
    Pascal(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("pascal", "pas")),
    PHP(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("php")),
    Perl(new Configuration()
            .setDocumentationStyle("POD")
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("perl", "pl")),
    Prolog(new Configuration()
            .setLineComments(new LineComment.Factory("%"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("prolog")),
    Python(new Configuration()
            .setDocumentationStyle("PyDoc")
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("python", "py")),
    R(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("r")),
    Ruby(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("ruby", "rb")),
    Racket(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("#|", "", "|#"))
            .setDocComments(new BlockComment.Factory("#|", "", "|#"))
            .setFileExtensions("racket")),
    Rust(new Configuration()
            .setDocumentationStyle("Rustdoc")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("rust", "rs")),
    Scala(new Configuration()
            .setDocumentationStyle("ScalaDoc")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("scala", "sc")),
    Scheme(new Configuration()
            .setLineComments(new LineComment.Factory(";"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("scheme")),
    SCSS(new Configuration()
            .setDocumentationStyle("SCSS")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new LineComment.Factory("///"))
            .setFileExtensions("scss")),
    SQL(new Configuration()
            .setLineComments(new LineComment.Factory("--"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("sql")),
    Smalltalk(new Configuration()
            .setLineComments(new LineComment.Factory("\""))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("smalltalk", "st")),
    Swift(new Configuration()
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("swift")),
    Tcl(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("tcl")),
    TypeScript(new Configuration()
            .setDocumentationStyle("TypeDoc")
            .setLineComments(new LineComment.Factory("//"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("typescript", "ts")),
    VisualBasic(new Configuration()
            .setLineComments(new LineComment.Factory("'"))
            .setBlockComments(new BlockComment.Factory("/*", "", "*/"))
            .setDocComments(new BlockComment.Factory("/**", "*", "*/"))
            .setFileExtensions("visualbasic", "vb")),
    YAML(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setFileExtensions("yaml")),
    ZShell(new Configuration()
            .setLineComments(new LineComment.Factory("#"))
            .setFileExtensions("zsh"));

    public final @NotNull List<CharSequence> extensions;
    public final String docStyle;
    public final @Nullable TextBlockFactory<?> lineComment;
    public final @Nullable TextBlockFactory<?> blockComment;
    public final @Nullable TextBlockFactory<?> docComment;

    ComputerLanguage(@NotNull Configuration configuration) {
        this.extensions = Arrays.asList(configuration.getFileExtensions());
        this.docStyle = configuration.getDocumentationStyle();
        this.lineComment = configuration.getLineComments();
        this.blockComment = configuration.getBlockComments();
        this.docComment = configuration.getDocComments();
    }

    @Nullable
    public static ComputerLanguage findByExtension(CharSequence extension) {
        return Arrays.stream(values()).filter(x -> x.extensions.contains(extension)).findAny().orElse(null);
    }

    @Nullable
    public static ComputerLanguage getComputerLanguage(@NotNull AnActionEvent e) {
        @Nullable VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return null;
        @NotNull String extension = file.getExtension() != null ? file.getExtension().toLowerCase() : "";
        return findByExtension(extension);
    }

    @Nullable
    public Language psiLanguage() {
        return Language.findLanguageByID(name());
    }

    public @Nullable CharSequence getMultilineCommentSuffix() {
        if (docComment instanceof BlockComment.Factory) {
            return ((BlockComment.Factory) docComment).blockSuffix;
        }
        return null;
    }

    public @Nullable TextBlockFactory<?> getCommentModel(String text) {
        if (Objects.requireNonNull(docComment).looksLike(text)) return docComment;
        if (Objects.requireNonNull(blockComment).looksLike(text)) return blockComment;
        return lineComment;
    }

    static class Configuration {
        private String documentationStyle = "";
        private CharSequence[] fileExtensions = new CharSequence[]{};
        private @Nullable TextBlockFactory<?> lineComments = null;
        private @Nullable TextBlockFactory<?> blockComments = null;
        private @Nullable TextBlockFactory<?> docComments = null;

        public String getDocumentationStyle() {
            return documentationStyle;
        }

        public @NotNull Configuration setDocumentationStyle(String documentationStyle) {
            this.documentationStyle = documentationStyle;
            return this;
        }

        public CharSequence[] getFileExtensions() {
            return fileExtensions;
        }

        public @NotNull Configuration setFileExtensions(CharSequence... fileExtensions) {
            this.fileExtensions = fileExtensions;
            return this;
        }

        public @Nullable TextBlockFactory<?> getLineComments() {
            return lineComments;
        }

        public @NotNull Configuration setLineComments(TextBlockFactory<?> lineComments) {
            this.lineComments = lineComments;
            return this;
        }

        public @Nullable TextBlockFactory<?> getBlockComments() {
            if (null == blockComments) return getLineComments();
            return blockComments;
        }

        public @NotNull Configuration setBlockComments(TextBlockFactory<?> blockComments) {
            this.blockComments = blockComments;
            return this;
        }

        public @Nullable TextBlockFactory<?> getDocComments() {
            if (null == docComments) return getBlockComments();
            return docComments;
        }

        public @NotNull Configuration setDocComments(TextBlockFactory<?> docComments) {
            this.docComments = docComments;
            return this;
        }
    }
}
