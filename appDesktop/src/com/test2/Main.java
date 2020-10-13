package com.test2;

import com.mongodb.client.MongoCollection;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.swing.text.View;
import java.util.Collections;

import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;

public class Main extends Application {

    Stage window;
    Scene scene1, scene0, defineScene, modifyScene;
    MongoDB database;

    TextField nameInput, producerInput, priceInput, weightInput, searchById, newValue, idInput;

    public static void main(String[] args) {
	    launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Window
        window = primaryStage;
        // Database
        database = new MongoDB();

        //// Some useful buttons ////

        // Confirm box
        Button button4 = new Button("ConfirmBox");
        button4.setOnAction(e -> {
            boolean result = ConfirmBox.display("Title ConfirmBox", "Are you sure about that?");
            System.out.println(result);
        });

        // Close program
        Button closeButton = new Button("Close program");
        closeButton.setOnAction(e -> closeProgram());

        // Button back
        Button buttonBack = new Button("Back");
        buttonBack.setOnAction(e -> {
            window.setScene(scene1);
            searchById.clear();
            newValue.clear();
        });

        // Button back2
        Button buttonBack2 = new Button("Back");
        buttonBack2.setOnAction(e -> {
            window.setScene(scene1);
            idInput.clear();
            nameInput.clear();
            producerInput.clear();
            priceInput.clear();
            weightInput.clear();
        });


        //// First scene ////

        // GridPane
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(8);
        gridPane.setHgap(10);

        // Name label
        Label nameLabel = new Label("Username:");
        GridPane.setConstraints(nameLabel, 0, 0);

        // Name input
        TextField userInput = new TextField();
        userInput.setPromptText("username");
        GridPane.setConstraints(userInput,1, 0);

        // Password label
        Label passwordLabel = new Label("Password:");
        GridPane.setConstraints(passwordLabel, 0, 1);

        // Password input
        TextField passInput = new TextField();
        passInput.setPromptText("password");
        GridPane.setConstraints(passInput, 1, 1);

        gridPane.getChildren().addAll(nameLabel, userInput, passwordLabel, passInput);

        Label enterLabel = new Label("Please register");

        Button button2 = new Button("Login");

        // Try to login
        button2.setOnAction(e -> {
            String pass = passInput.getText();
            String user = userInput.getText();

            if (database.check(user, pass)) {
                window.setScene(scene1);
                passInput.clear();
                userInput.clear();
            } else {
                AlertBox.display("Alert", "Wrong username or password");
            }

        });

        Button buttonClear = new Button("Clear");
        buttonClear.setOnAction(e -> {
            passInput.clear();
            userInput.clear();
        });
        VBox rightMenu = new VBox();
        rightMenu.getChildren().addAll(button2, buttonClear);

        BorderPane borderPane2 = new BorderPane();
        borderPane2.setTop(enterLabel);
        borderPane2.setCenter(gridPane);
        borderPane2.setRight(rightMenu);

        scene0 = new Scene(borderPane2, 400, 150);

        //// Second  scene ////

        // Label question
        Label label1 = new Label("What action?");
        HBox topMenu = new HBox();
        topMenu.getChildren().addAll(label1);

        // Button return to welcome scene
        Button button1 = new Button("Back to login");
        button1.setOnAction(e -> {
            window.setScene(scene0);
        });

        // Button to define a product
        Button defineButton = new Button("Define new product");
        defineButton.setOnAction(e -> window.setScene(defineScene));

        // Button to modify a product
        Button searchButton = new Button("Modify a product");
        searchButton.setOnAction(e -> window.setScene(modifyScene));

        VBox leftMenu = new VBox();
        leftMenu.getChildren().addAll(button1, defineButton, searchButton, closeButton);

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(topMenu);
        borderPane.setLeft(leftMenu);

        scene1 = new Scene(borderPane, 400, 150);

        //// Define Scene ////
        MongoCollection collectionDB = database.connect();

        idInput = new TextField();
        idInput.setPromptText("Barcode");

        nameInput = new TextField();
        nameInput.setPromptText("Name");

        producerInput = new TextField();
        producerInput.setPromptText("Producer");

        weightInput = new TextField();
        weightInput.setPromptText("Weight(GR)");

        priceInput = new TextField();
        priceInput.setPromptText("Price(RON)");

        Button modifyOk = new Button("Ok");
        modifyOk.setOnAction(e -> defineButtonClicked(collectionDB));

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(10, 10, 10, 10));
        hBox.setSpacing(10);
        hBox.getChildren().addAll(idInput, nameInput, producerInput, weightInput, priceInput, modifyOk, buttonBack2);

        defineScene = new Scene(hBox);

        //// Modify Scene ////

        searchById = new TextField();
        searchById.setPromptText("Type the id of the product");

        newValue = new TextField();
        newValue.setPromptText("New value");

        // ChoiceBox
        ChoiceBox<String> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().addAll("Name", "Producer", "Weight", "Price");

        Button modifyOk2 = new Button("Ok");
        modifyOk2.setOnAction(e -> modifyButtonClicked(collectionDB, choiceBox));

        HBox hBox2 = new HBox();
        hBox2.setPadding(new Insets(10, 10, 10, 10));
        hBox2.setSpacing(10);
        hBox2.getChildren().addAll(searchById, newValue, choiceBox, modifyOk2, buttonBack);

        modifyScene = new Scene(hBox2);

        //// Close program ////
        window.setOnCloseRequest(e -> {
            e.consume();
            closeProgram();
        });

        //// Start ////
        window.setScene(scene0);
        window.setTitle("Application");
        window.show();
    }

    private void closeProgram() {
        Boolean answer = ConfirmBox.display("  ", "Are you sure?");
        if (answer)
            window.close();
    }

    // Define a product
    public void defineButtonClicked(MongoCollection collection) {
        boolean result = ConfirmBox.display("Title ConfirmBox", "Are you sure about that?");

        if (result == true) {
            if (nameInput.getText().length() == 0 || idInput.getText().length() == 0 || idInput.getText().length() == 0 ||
                    weightInput.getText().length() == 0 || priceInput.getText().length() == 0) {
                AlertBox.display("Alert", "You must complete all fields");
                return;
            }

            Document document = new Document("_id", idInput.getText());

            document.append("name", nameInput.getText());
            document.append("producer", producerInput.getText());

            try {
                double convertWeight = Double.parseDouble(weightInput.getText());
                document.append("weight(GR)", convertWeight);
            }
            catch (NumberFormatException nfe) {
                AlertBox.display("Alert", "Weight must be a number");
                return;
            }

            try {
                double convertPrice = Double.parseDouble(priceInput.getText());
                document.append("price(RON)", convertPrice);
            }
            catch (NumberFormatException nfe) {
                AlertBox.display("Alert", "Price must be a number");
                return;
            }

            int zero = 0;
            document.append("stock", zero);

            collection.insertOne(document);

            nameInput.clear();
            priceInput.clear();
            producerInput.clear();
            weightInput.clear();
            idInput.clear();

            window.setScene(scene1);
        }
    }

    public void modifyButtonClicked(MongoCollection collection, ChoiceBox<String> choiceBox) {
        String type = choiceBox.getValue();

        Document search = new Document("_id", searchById.getText());
        Document found = (Document) collection.find(search).first();

        // Update database
        if (found != null) {

            if (type.equals("Name")) {
                Bson updatedValue = new Document("name", newValue.getText());
                Bson updateOperation = new Document("$set", updatedValue);
                collection.updateOne(found, updateOperation);
            } else if (type.equals("Producer")) {
                Bson updatedValue = new Document("producer", newValue.getText());
                Bson updateOperation = new Document("$set", updatedValue);
                collection.updateOne(found, updateOperation);
            } else if (type.equals("Weight")) {
                try {
                    double convert = Double.parseDouble(newValue.getText());


                    Bson updatedValue = new Document("weight(GR)", convert);
                    Bson updateOperation = new Document("$set", updatedValue);
                    collection.updateOne(found, updateOperation);
                }
                catch (NumberFormatException nfe) {
                    AlertBox.display("Alert", "Weight must be a number");
                    return;
                }
            } else if (type.equals("Price")) {
                try {
                    double convert = Double.parseDouble(newValue.getText());

                    Bson updatedValue = new Document("price(RON)", convert);
                    Bson updateOperation = new Document("$set", updatedValue);
                    collection.updateOne(found, updateOperation);
                }
                catch (NumberFormatException nfe) {
                    AlertBox.display("Alert", "Price must be a number");
                    return;
                }
            }
        } else {
            // Item not found
            AlertBox.display("Alert", "Item not found");
            return;
        }

        searchById.clear();
        newValue.clear();
        window.setScene(scene1);
    }
}
