package com.github.simiacryptus.aicoder;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public enum ComputerLanguage {
    Java("JavaDoc", "//", "/*", "*/", "java"),
    Cpp("Doxygen", "//", "/*", "*/", "cpp"),
    Markdown("Markdown", "//", "/*", "*/", "md"),
    Ada("", "--", "/*", "*/", "ada"),
    Assembly("", ";", "/*", "*/", "assembly"),
    Basic("", "'", "/*", "*/", "basic", "bs"),
    Bash("", "#", "", "", "sh"),
    C("Doxygen", "//", "/*", "*/", "c"),
    Clojure("ClojureDocs", ";", "/*", "*/", "cj"),
    COBOL("", "*", "/*", "*/", "cobol", "cob"),
    CSharp("XML", "//", "/*", "*/", "c#", "cs"),
    CSS("", "//", "/*", "*/", "css"),
    Dart("", "//", "/*", "*/", "dart"),
    Delphi("", "//", "/*", "*/", "delphi"),
    Erlang("", "%%", "/*", "*/", "erlang"),
    Elixir("", "//", "/*", "*/", "elixir"),
    FORTRAN("", "!", "/*", "*/", "fortran"),
    FSharp("", "//", "/*", "*/", "f#"),
    Go("", "//", "/*", "*/", "go"),
    Groovy("", "//", "/*", "*/", "groovy"),
    Haskell("", "--", "/*", "*/", "haskell"),
    HTML("", "//", "/*", "*/", "html"),
    Julia("", "#", "/*", "*/", "julia"),
    JavaScript("JSDoc", "//", "/*", "*/", "javascript", "js"),
    Json("JSDoc", "", "", "", "json"),
    Kotlin("KDoc", "//", "/*", "*/", "kotlin", "kt"),
    Lisp("", ";", "/*", "*/", "lisp"),
    Logo("", ";", "/*", "*/", "logo", "log"),
    MATLAB("", "%", "/*", "*/", "matlab", "m"),
    OCaml("", "(*", "*)", "ocaml", "ml"),
    Pascal("", "//", "/*", "*/", "pascal", "pas"),
    PHP("", "//", "/*", "*/", "php"),
    Perl("POD", "#", "/*", "*/", "perl", "pl"),
    Prolog("", "%", "/*", "*/", "prolog"),
    Python("PyDoc", "#", "/*", "*/", "python", "py"),
    R("", "#", "/*", "*/", "r"),
    Ruby("", "#", "/*", "*/", "ruby", "rb"),
    Racket("", "#","#|", "|#", "racket"),
    Rust("Rustdoc", "//", "/*", "*/", "rust", "rs"),
    Scala("ScalaDoc", "//", "/*", "*/", "scala", "sc"),
    Scheme("", ";", "/*", "*/", "scheme"),
    SQL("", "--", "/*", "*/", "sql"),
    Smalltalk("", "\"", "/*", "*/", "smalltalk", "st"),
    Swift("", "//", "/*", "*/", "swift"),
    Tcl("", "#", "/*", "*/", "tcl"),
    TypeScript("TypeDoc", "//", "/*", "*/", "typescript", "ts"),
    VisualBasic("", "'", "/*", "*/", "visualbasic", "vb"),
    YAML("", "#", "", "", "yaml"),
    ZShell("", "#", "", "", "zsh");

    public final List<String> fileExtensions;
    public final String singlelineCommentPrefix;
    public final String documentationStyle;
    public final String multilineCommentPrefix;
    public final String multilineCommentSuffix;

    ComputerLanguage(String documentationStyle, String singlelineCommentPrefix, String multilineCommentPrefix, String multilineCommentSuffix, String... fileExtensions) {
        this.fileExtensions = Arrays.asList(fileExtensions);
        this.singlelineCommentPrefix = singlelineCommentPrefix;
        this.documentationStyle = documentationStyle;
        this.multilineCommentPrefix = multilineCommentPrefix;
        this.multilineCommentSuffix = multilineCommentSuffix;
    }

    @Nullable
    public static ComputerLanguage findByExtension(String extension) {
        return Arrays.stream(values()).filter(x -> x.fileExtensions.contains(extension)).findAny().orElse(null);
    }
}
