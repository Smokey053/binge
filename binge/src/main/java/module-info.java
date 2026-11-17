module com.binge {
    requires javafx.controls;
    requires javafx.web;
    requires javafx.media;
    requires java.net.http;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;

    // Jackson reads the catalog and the user library through reflection.
    opens com.binge.model to com.fasterxml.jackson.databind;
    opens com.binge.data to com.fasterxml.jackson.databind;

    exports com.binge;
}
