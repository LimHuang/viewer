module com.syspilot.viewer {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.syspilot.viewer to javafx.fxml;
    opens com.syspilot.viewer.controller to javafx.fxml;
    opens com.syspilot.viewer.model to com.fasterxml.jackson.databind;
    exports com.syspilot.viewer;
}
