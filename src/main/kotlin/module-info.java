module ru.amsocat.screenshotapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires javafx.swing;


    opens ru.amsocat.screenshotapp to javafx.fxml;
    exports ru.amsocat.screenshotapp;
}