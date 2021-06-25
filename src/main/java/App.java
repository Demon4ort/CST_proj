import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.Group;
import db.Product;
import db.User;
import io.jsonwebtoken.SignatureAlgorithm;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class App extends Application {
    public static final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private static final byte[] apiKeySecretBytes = "my-token-secret-key-aefsfdgsafghjmgfrdsefsgthjmgfdsghjmgbfvdcbgfhbfvdccfvbg".getBytes(StandardCharsets.UTF_8);
    public static final Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
    private static final byte MAGIC_BYTE = 0x13;
    private static final byte[] KEY_BYTES = "qwerty1234567890".getBytes(StandardCharsets.UTF_8);
    HttpClient client;
    ObjectMapper objectMapper;
    String jwt;
    ObservableList<Product> products;
    ListView<Product> productListView;
    Label allValue;
    List<Group> groups;

    private static byte[] encrypt(byte[] message) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        return cipher.doFinal(message);
    }

    private static byte[] decrypt(byte[] message) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKey secretKey = new SecretKeySpec(KEY_BYTES, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(message);
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        client = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();
        products = FXCollections.observableArrayList(new Product());
        groups = new ArrayList<>();
        startUI(stage);

    }

    private void startUI(Stage stage) {
        BorderPane root = new BorderPane();
        root.setBottom(bottom());
        root.setCenter(center());
        //root.setLeft(left());
        root.setTop(top());

        Scene scene = new Scene(root, 1080, 720);
        stage.setScene(scene);
        stage.show();
    }

    private VBox top() {
        MenuBar menuBar = new MenuBar();
        Menu addMenu = new Menu("Add");
        MenuItem productItem = new MenuItem("Product");
        MenuItem groupItem = new MenuItem("Group");

        productItem.setOnAction(actionEvent -> {
            Product product = addProductDialog();
            if (product != null) {
                String res = addProduct(product);
                if (res != null) {
                    doShowMessageDialog("Success", "added product", "");
                    synch();
                } else doShowMessageDialog("Failure", "Error when adding", "");
            }
        });

        groupItem.setOnAction(actionEvent -> {
            Group group = addGroupDialog();
            if (group != null) {
                String res = addGroup(group);
                if (res != null) {
                    doShowMessageDialog("Success", res, "");
                    synch();
                } else doShowMessageDialog("Failure", "Error when adding", "");
            }
        });

        Menu deleteMenu = new Menu("Delete");
        MenuItem productItemDel = new MenuItem("Product");
        MenuItem groupItemDel = new MenuItem("Group");


        productItemDel.setOnAction(actionEvent -> {
            Integer id = productListView.getSelectionModel().getSelectedItem().getId();
            String name = productListView.getSelectionModel().getSelectedItem().getName();
            if (deleteProduct(id)) {
                doShowMessageDialog("Success", "Deleted product - " + name, "");
            } else doShowMessageDialog("Failure", "Error when deleting - " + name, "");
        });

        groupItemDel.setOnAction(actionEvent -> {
            Group group = selectGroupDialog("delete");
            Integer id = group != null ? group.getIdGroup() : null;
            if (id != null) {
                if (deleteGroup(id)) {
                    doShowMessageDialog("Success", "Deleted group:" + id, "");
                    synch();
                } else doShowMessageDialog("Failure", "Error when deleting group:" + id, "");
            }
        });


        Menu updateMenu = new Menu("Update");
        MenuItem prodItemUp = new MenuItem("Product");
        MenuItem groupItemUp = new MenuItem("Group");

        prodItemUp.setOnAction(actionEvent -> {
            Product product = updateProductDialog(productListView.getSelectionModel().getSelectedItem());
            if (product != null) {
                product.setId(productListView.getSelectionModel().getSelectedItem().getId());
                if (updateProduct(product)) {
                    doShowMessageDialog("Success", "Updated product - " + product.getName(), "");
                } else doShowMessageDialog("Failure", "Error when updating - " + product.getName(), "");
            }
        });

        groupItemUp.setOnAction(actionEvent -> {
            synch();
            Group group = selectGroupDialog("update");
            if (group != null) {
                Group toUpdate = updateGroupDialog(group);
                if (toUpdate != null) {
                    if (updateGroup(toUpdate)) {
                        doShowMessageDialog("Success", "Updated group - " + toUpdate.getName(), "");
                    } else doShowMessageDialog("Failure", "Error when updating - " + toUpdate.getName(), "");
                }
            }
        });


        Menu showMenu = new Menu("Show");
        MenuItem byGroupItem = new MenuItem("by Group");
        MenuItem generalItem = new MenuItem("General");

        byGroupItem.setOnAction(actionEvent -> {
            synch();
            Group group = selectGroupDialog("to show stats");
            if (group != null) {
                productListView.setItems(products.filtered(product -> product.getNameGroup().equals(group.getName())));
            }
            allValue.setText("TOTAL VALUE OF GROUP:" + getAllValue(productListView.getItems()));
        });

        generalItem.setOnAction(actionEvent -> {
            synch();
            productListView.setItems(products);
        });

        Menu searchMenu = new Menu("Search");
        MenuItem productSearch = new MenuItem("Product");

        productSearch.setOnAction(actionEvent -> {
            synch();
            String name = searchDialog();
            if (name != null) {
                Product pr = null;
                for (Product product : products) {
                    if (product.getName().equals(name)) {
                        pr = product;
                    }
                }
                if (pr != null) productListView.getSelectionModel().select(pr);
            }
        });


        showMenu.getItems().addAll(generalItem, byGroupItem);
        updateMenu.getItems().addAll(prodItemUp, groupItemUp);
        deleteMenu.getItems().addAll(productItemDel, groupItemDel);
        addMenu.getItems().addAll(productItem, groupItem);
        searchMenu.getItems().add(productSearch);

        menuBar.getMenus().addAll(addMenu, deleteMenu, updateMenu, showMenu, searchMenu);


        allValue = new Label("Total value: " + products);
        allValue.setDisable(true);

        return new VBox(menuBar, allValue);
    }

    private HBox bottom() {
        HBox box = new HBox();
        var login = new TextField();
        login.setPromptText("Login");
        var password = new PasswordField();
        password.setPromptText("Password");

        var connectButton = new Button("Connect");
        connectButton.setOnAction(actionEvent -> {
            if (!login.getText().isEmpty() && !password.getText().isEmpty()) {
                if (connect(login.getText(), password.getText())) {
                    doShowMessageDialog("Success", "-", "You successfully connected");
                } else doShowMessageDialog("Failure", "Error when connecting", "");
            }
        });

        box.getChildren().addAll(login, password, connectButton);
        return box;
    }

    private ListView<Product> center() {
        productListView = new ListView<>(products);
        productListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null || product.getName() == null || product.getId() == null) {
                    setText(null);
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder
                            .append(product.getId())
                            .append("| NAME: ")
                            .append(product.getName())
                            .append("| GROUP: ")
                            .append(product.getNameGroup())
                            .append("| PRODUCER: ")
                            .append(product.getProducer())
                            .append("| PRICE: ")
                            .append(product.getPrice())
                            .append("| AMOUNT: ")
                            .append(product.getAmount())
                            .append("| Total value: ")
                            .append(product.getPrice() * product.getAmount());
                    setText(builder.toString());
                }
            }
        });
        return productListView;
    }

    private Double getAllValue(List<Product> list) {
        double res = 0.0;
        for (Product product : list) {
            res += product.getAmount() * product.getPrice();
        }
        return res;
    }

    private boolean connect(String login, String password) {
        User user = new User(login, password);
        System.out.println("Trying to connect");
        try {
            byte[] requestBody = encrypt(objectMapper.writeValueAsBytes(user));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/login"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            Map<String, List<String>> headers = response.headers().map();
            jwt = headers.get("Authorization").get(0);
            synch();
            return true;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private String searchDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Search product");
        dialog.setHeaderText("Enter info and \n" +
                "press Okay (or click title bar 'X' for cancel).");
        dialog.setResizable(true);

        Label name = new Label("Name: ");
        TextField nameText = new TextField();

        GridPane grid = new GridPane();
        grid.add(name, 1, 1);
        grid.add(nameText, 2, 1);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        dialog.setResultConverter(b -> {

            if (b == buttonTypeOk) {
                return nameText.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private boolean updateGroup(Group toUpdate) {
        System.out.println(toUpdate);
        try {
            byte[] requestBody = encrypt(objectMapper.writeValueAsBytes(toUpdate));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/group/" + toUpdate.getIdGroup()))
                    .setHeader("Authorization", jwt)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println(response.statusCode());
            synch();
            if (response.statusCode() == 204) {
                return true;
            } else {
                return false;
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Group updateGroupDialog(Group group) {
        Dialog<Group> dialog = new Dialog<>();
        dialog.setTitle("Adding group");
        dialog.setHeaderText("Enter info and \n" +
                "press Okay (or click title bar 'X' for cancel).");
        dialog.setResizable(true);

        Label name = new Label("Name: ");
        Label description = new Label("Description: ");
        TextField nameText = new TextField(group.getName());
        TextField descriptionText = new TextField(group.getDescription());

        GridPane grid = new GridPane();
        grid.add(name, 1, 1);
        grid.add(nameText, 2, 1);
        grid.add(description, 1, 2);
        grid.add(descriptionText, 2, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        dialog.setResultConverter(b -> {

            if (b == buttonTypeOk) {
                return new Group(
                        group.getIdGroup(),
                        nameText.getText(),
                        descriptionText.getText()
                );
            }
            return null;
        });

        Optional<Group> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private Product updateProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Adding product");
        dialog.setHeaderText("Enter info and \n" +
                "press Okay (or click title bar 'X' for cancel).");
        dialog.setResizable(true);

        Label name = new Label("Name: ");
        Label description = new Label("Description: ");
        Label producer = new Label("Producer: ");
        Label amount = new Label("Amount: ");
        Label price = new Label("Price: ");
        Label nameGroup = new Label("Group: ");

        Pattern pattern = Pattern.compile("\\d*|\\d+\\,\\d*");
        TextFormatter formatter1 = new TextFormatter((UnaryOperator<TextFormatter.Change>) change ->
                pattern.matcher(change.getControlNewText()).matches() ? change : null);
        TextFormatter formatter2 = new TextFormatter((UnaryOperator<TextFormatter.Change>) change ->
                pattern.matcher(change.getControlNewText()).matches() ? change : null);

        TextField nameText = new TextField(product.getName());
        TextField descriptionText = new TextField(product.getDescription());
        TextField producerText = new TextField(product.getProducer());
        TextField amountText = new TextField(String.valueOf(product.getAmount()));
        amountText.setTextFormatter(formatter1);
        TextField priceText = new TextField(String.valueOf(product.getPrice()));
        priceText.setTextFormatter(formatter2);
        TextField nameGroupText = new TextField(product.getNameGroup());

        GridPane grid = new GridPane();
        grid.add(name, 1, 1);
        grid.add(nameText, 2, 1);
        grid.add(description, 1, 2);
        grid.add(descriptionText, 2, 2);
        grid.add(producer, 1, 3);
        grid.add(producerText, 2, 3);
        grid.add(amount, 1, 4);
        grid.add(amountText, 2, 4);
        grid.add(price, 1, 5);
        grid.add(priceText, 2, 5);
        grid.add(nameGroup, 1, 6);
        grid.add(nameGroupText, 2, 6);
        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        dialog.setResultConverter(b -> {

            if (b == buttonTypeOk) {
                return new Product(
                        nameText.getText(),
                        descriptionText.getText(),
                        producerText.getText(),
                        Double.parseDouble(amountText.getText()),
                        Double.parseDouble(priceText.getText()),
                        nameGroupText.getText()
                );
            }

            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private boolean updateProduct(Product toUpdate) {
        System.out.println(toUpdate);
        try {
            byte[] requestBody = encrypt(objectMapper.writeValueAsBytes(toUpdate));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/good/" + toUpdate.getId()))
                    .setHeader("Authorization", jwt)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println(response.statusCode());
            synch();
            if (response.statusCode() == 204) {
                return true;
            } else {
                return false;
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteProduct(Integer id) {
        System.out.println(id);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/good/" + id))
                    .setHeader("Authorization", jwt)
                    .DELETE()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println(response.statusCode());
            synch();
            if (response.statusCode() == 204) {
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Group selectGroupDialog(String aim) {
        Dialog<Group> dialog = new Dialog<>();
        dialog.setTitle("Adding group");
        dialog.setHeaderText("Choose group to " + aim + "\n" +
                "press Okay (or click title bar 'X' for cancel).");
        dialog.setResizable(true);

        ObservableList<Group> groupObservableList = FXCollections.observableList(getGroups());
        ListView<Group> groupListView = new ListView<>(groupObservableList);

        dialog.getDialogPane().setContent(groupListView);

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        dialog.setResultConverter(b -> {

            if (b == buttonTypeOk) {
                Group group = groupListView.getSelectionModel().getSelectedItem();
                group.setIdGroup(groupListView.getSelectionModel().getSelectedItem().getIdGroup());
                return group;
            }
            return null;
        });

        Optional<Group> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private List<Group> getGroups() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/group"))
                    .setHeader("Authorization", jwt)
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return objectMapper.readValue(decrypt(response.body()), new TypeReference<>() {
            });

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

    }

    private boolean deleteGroup(Integer id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/group/" + id))
                    .setHeader("Authorization", jwt)
                    .DELETE()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println(response.statusCode());
            synch();
            if (response.statusCode() == 204) {
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String addGroup(Group group) {
        try {
            byte[] requestBody = encrypt(objectMapper.writeValueAsBytes(group));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/group"))
                    .setHeader("Authorization", jwt)
                    .setHeader("Length", String.valueOf(requestBody.length))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println(response.statusCode());
            if (response.statusCode() == 201) {
                return objectMapper.readValue(decrypt(response.body()), String.class);
            } else {
                return null;
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Group addGroupDialog() {
        Dialog<Group> dialog = new Dialog<>();
        dialog.setTitle("Adding group");
        dialog.setHeaderText("Enter info and \n" +
                "press Okay (or click title bar 'X' for cancel).");
        dialog.setResizable(true);

        Label name = new Label("Name: ");
        Label description = new Label("Description: ");
        TextField nameText = new TextField();
        TextField descriptionText = new TextField();

        GridPane grid = new GridPane();
        grid.add(name, 1, 1);
        grid.add(nameText, 2, 1);
        grid.add(description, 1, 2);
        grid.add(descriptionText, 2, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        dialog.setResultConverter(b -> {

            if (b == buttonTypeOk) {
                return new Group(
                        nameText.getText(),
                        descriptionText.getText()
                );
            }

            return null;
        });

        Optional<Group> result = dialog.showAndWait();
        return result.orElse(null);

    }

    private Product addProductDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Adding product");
        dialog.setHeaderText("Enter info and \n" +
                "press Okay (or click title bar 'X' for cancel).");
        dialog.setResizable(true);

        Label name = new Label("Name: ");
        Label description = new Label("Description: ");
        Label producer = new Label("Producer: ");
        Label amount = new Label("Amount: ");
        Label price = new Label("Price: ");
        Label nameGroup = new Label("Group: ");

        Pattern pattern = Pattern.compile("\\d*|\\d+\\,\\d*");
        TextFormatter formatter1 = new TextFormatter((UnaryOperator<TextFormatter.Change>) change ->
                pattern.matcher(change.getControlNewText()).matches() ? change : null);
        TextFormatter formatter2 = new TextFormatter((UnaryOperator<TextFormatter.Change>) change ->
                pattern.matcher(change.getControlNewText()).matches() ? change : null);

        TextField nameText = new TextField();
        TextField descriptionText = new TextField();
        TextField producerText = new TextField();
        TextField amountText = new TextField();
        amountText.setTextFormatter(formatter1);
        TextField priceText = new TextField();
        priceText.setTextFormatter(formatter2);
        TextField nameGroupText = new TextField();

        GridPane grid = new GridPane();
        grid.add(name, 1, 1);
        grid.add(nameText, 2, 1);
        grid.add(description, 1, 2);
        grid.add(descriptionText, 2, 2);
        grid.add(producer, 1, 3);
        grid.add(producerText, 2, 3);
        grid.add(amount, 1, 4);
        grid.add(amountText, 2, 4);
        grid.add(price, 1, 5);
        grid.add(priceText, 2, 5);
        grid.add(nameGroup, 1, 6);
        grid.add(nameGroupText, 2, 6);
        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Okay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        dialog.setResultConverter(b -> {

            if (b == buttonTypeOk) {
                return new Product(
                        nameText.getText(),
                        descriptionText.getText(),
                        producerText.getText(),
                        Double.parseDouble(amountText.getText()),
                        Double.parseDouble(priceText.getText()),
                        nameGroupText.getText()
                );
            }

            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String addProduct(Product product) {
        try {
            byte[] requestBody = encrypt(objectMapper.writeValueAsBytes(product));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/good"))
                    .setHeader("Authorization", jwt)
                    .setHeader("Length", String.valueOf(requestBody.length))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            System.out.println(response.statusCode());
            if (response.statusCode() == 201) {
                synch();
                return objectMapper.readValue(decrypt(response.body()), String.class);
            } else {
                return null;
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void synch() {
        getProducts();
        allValue.setText("Total value:" + getAllValue(products));
        allValue.setDisable(false);
        groups = getGroups();
    }

    private void getProducts() {
        // /api/good
        System.out.println("Trying to get all products");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:8080/api/good"))
                    .setHeader("Authorization", jwt)
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            List<Product> list = objectMapper.readValue(decrypt(response.body()), new TypeReference<>() {
            });
            products.setAll(list);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void doShowMessageDialog(String title, String header, String text) {


        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(text);

        alert.showAndWait();
    }

}