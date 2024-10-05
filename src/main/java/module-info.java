module com.github.simiacryptus.aicoder {
    requires java.desktop;
    requires java.prefs;
    requires kotlin.stdlib;
    requires java.base;
    requires org.slf4j;
    requires org.eclipse.jetty.http;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.webapp;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.jetbrains.annotations;

    requires org.monte.media;
    requires org.monte.media.swing;
    requires org.monte.media.screenrecorder;
    requires kotlin.reflect;

    exports com.github.simiacryptus.aicoder;
    exports com.github.simiacryptus.aicoder.actions;
    exports com.github.simiacryptus.aicoder.actions.code;
    exports com.github.simiacryptus.aicoder.actions.dev;
    exports com.github.simiacryptus.aicoder.actions.generic;
    exports com.github.simiacryptus.aicoder.actions.git;
    exports com.github.simiacryptus.aicoder.actions.legacy;
    exports com.github.simiacryptus.aicoder.actions.markdown;
    exports com.github.simiacryptus.aicoder.actions.problems;
    exports com.github.simiacryptus.aicoder.actions.test;
    exports com.github.simiacryptus.aicoder.config;
    exports com.github.simiacryptus.aicoder.util;
    // Open packages for reflection (needed for Kotlin classes)
    opens com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.actions;
    opens com.github.simiacryptus.aicoder.actions.code;
    opens com.github.simiacryptus.aicoder.actions.dev;
    opens com.github.simiacryptus.aicoder.actions.generic;
    opens com.github.simiacryptus.aicoder.actions.git;
    opens com.github.simiacryptus.aicoder.actions.legacy;
    opens com.github.simiacryptus.aicoder.actions.markdown;
    opens com.github.simiacryptus.aicoder.actions.problems;
    opens com.github.simiacryptus.aicoder.actions.test;
    opens com.github.simiacryptus.aicoder.config;
    opens com.github.simiacryptus.aicoder.util;
}