package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.analyzer.AnalyzerController;

public class Main extends Application {

    public static AnalyzerController analyzeController = null;

    public static Controller inputController = null;

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader fxmlRootLoader = new FXMLLoader();
        Parent root = fxmlRootLoader.load(getClass().getResource("main.fxml"));
        Controller rootController = (Controller)fxmlRootLoader.getController();

        primaryStage.setTitle("Surface Touch Localization");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        FXMLLoader fxmlAnaylzerLoader = new FXMLLoader();
        Parent analyzerWindow = fxmlAnaylzerLoader.load(getClass().getResource("analyzer.fxml"));
        AnalyzerController analyzeController = (AnalyzerController)fxmlAnaylzerLoader.getController();
        Stage stage = new Stage();
        stage.setTitle("Surface Touch Localization Analyzer");
        stage.setScene(new Scene(analyzerWindow));
        stage.show();

        //rootController.setAnalyzerController(analyzeController);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
