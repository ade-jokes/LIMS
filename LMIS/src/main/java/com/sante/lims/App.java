package com.sante.lims;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.sante.lims.model.User;
import com.sante.lims.utils.DBConnection;

import java.io.IOException;

public class App extends Application {

    private static Stage primaryStage;
    private static User currentUser;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        // Initialize Database tables and seeds
        System.out.println("Initializing database...");
        DBConnection.initializeDatabase();

        // Load Login Screen initially
        switchScene("login.fxml", "Sante Diagnostics - Login", 800, 600);
        primaryStage.show();
    }

    /**
     * Switched the active scene on the primary stage.
     */
    public static Parent switchScene(String fxml, String title, double width, double height) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/com/sante/lims/" + fxml));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, width, height);
            
            // Apply common styling stylesheet
            String cssPath = App.class.getResource("/com/sante/lims/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(width);
            primaryStage.setMinHeight(height);
            primaryStage.centerOnScreen();
            
            return root;
        } catch (IOException e) {
            System.err.println("Failed to load FXML scene: " + fxml);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set the current logged in user.
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * Get the current logged in user.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}
