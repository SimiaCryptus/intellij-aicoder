module com.github.simiacryptus.aicoder.test {
    requires java.desktop;
    requires java.prefs;
    requires kotlin.stdlib;
    requires java.base;

    requires org.monte.media;
    requires org.monte.media.swing;
    requires org.monte.media.screenrecorder;
    requires com.github.simiacryptus.aicoder;

    requires org.seleniumhq.selenium.api;
    requires org.junit.jupiter.api;

    exports com.github.simiacryptus.aicoder.test;
    exports com.github.simiacryptus.aicoder.test.actions;
    exports com.github.simiacryptus.aicoder.test.actions.code;
    exports com.github.simiacryptus.aicoder.test.actions.markdown;
    exports com.github.simiacryptus.aicoder.test.actions.generic;
    exports com.github.simiacryptus.aicoder.test.demotest;
    exports com.github.simiacryptus.aicoder.test.util;
    opens com.github.simiacryptus.aicoder.test to org.junit.platform.commons, com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.test.actions to org.junit.platform.commons, com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.test.actions.code to org.junit.platform.commons, com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.test.actions.markdown to org.junit.platform.commons, com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.test.actions.generic to org.junit.platform.commons, com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.test.demotest to org.junit.platform.commons, com.github.simiacryptus.aicoder;
    opens com.github.simiacryptus.aicoder.test.util to org.junit.platform.commons, com.github.simiacryptus.aicoder;
}