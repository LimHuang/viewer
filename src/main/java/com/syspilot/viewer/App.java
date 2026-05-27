package com.syspilot.viewer;

import com.syspilot.viewer.architecture.AppArchitecture;
import com.syspilot.viewer.system.TrajectorySystem;
import com.syspilot.viewer.utility.TrajectoryLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppArchitecture arch = AppArchitecture.getInstance();
        arch.registerSystem(new TrajectorySystem());
        arch.registerUtility(new TrajectoryLoader());
        arch.init();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Scene scene = new Scene(loader.load(), 1400, 900);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("SysPilot Trajectory Viewer");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        AppArchitecture.getInstance().deinit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
