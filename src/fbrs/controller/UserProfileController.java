package fbrs.controller;

import fbrs.model.*;
import fbrs.utils.NavigationUtil;
import fbrs.utils.UIUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {
    private static final String PHONE_REGEX = "(^$|^05[0-9]{8}$)";

    //UI
    public BorderPane rootPane;
    public GridPane gridPane;
    public TextField idTextField;
    public TextField nameTextField;
    public TextField phoneTextField;
    public Label balanceLabel;
    public CheckBox isEditable;
    public Label title;
    public Label typeLabel;
    public Button onClickSaveBtn;
    public Button AddFromStorageBtn;
    public Button userEntryBtn;

    private String[] shipType;
    private DatabaseModel model;
    private List<Market> markets;
    private ComboBox<Market> marketComboBox;
    private ComboBox<String> FishermanTypeComboBox;
    private User user;

    public void onClickSave() {
        String updatedFields = "";
        boolean isUpdated = false;

        String newID = idTextField.getText().trim();
        String newName = nameTextField.getText().trim();
        String newPhone = phoneTextField.getText() == null ? null : phoneTextField.getText().trim();

        if (!newID.isEmpty() && user.getDarshKey() != Integer.parseInt(newID)) {
            try {
                model.updateDarshKey(user.getId(), Integer.parseInt(newID));
                updatedFields += "???? ?????????? ?????????? ???????????????? ?????????? : " + newID;
                isUpdated = true;
                user.setDarshKey(Integer.parseInt(newID));
            } catch (SQLException exception) {
                UIUtil.showAlert("???? ?????? ?????????? ?????????? ????????????????",
                        "?????????? ???????????????? ???????????? ???????????? ???? ?????? ???????????? ?????? ???????????? ???????????? ?????? ??????",
                        null,
                        Alert.AlertType.ERROR);
                return;
            }
        }

        if (!user.getName().equals(newName)) {
            if (model.updateUserName(user.getId(), newName)) {
                updatedFields += "\n ???? ?????????? ?????????? ?? : " + newName;
                user.setName(newName);
                isUpdated = true;
            }
        }

        if (newPhone != null && (user.getPhone() == null || !user.getPhone().equals(newPhone))) {
            if (newPhone.matches(PHONE_REGEX)) {
                if (model.updateUserPhone(user.getId(), newPhone)) {
                    updatedFields += "\n ???? ?????????? ?????? ???????????? ?? : " + newPhone;
                    user.setPhone(phoneTextField.getText());
                }
            } else {
                updatedFields += "\n ???? ?????? ?????????? ?????? ???????????? ???????? ???? ?????????? ?????? ???????? ????????  : " + newPhone;
            }
            isUpdated = true;
        }

        if (user instanceof Seller) {
            Seller seller = (Seller) user;
            int marketId = marketComboBox.getValue().getId();

            if (seller.getMarket() != marketId) {
                if (model.updateUserMarket(user.getId(), marketId)) {
                    updatedFields += "\n ???? ?????????? ?????? ???????????? ???????? : " + markets.get(marketId - 1).getName();
                    seller.setMarket(marketId);
                    isUpdated = true;
                }
            }
        } else if (user instanceof Fisherman) {
            Fisherman fisherman = (Fisherman) user;
            int shipType = FishermanTypeComboBox.getValue().equals("??????") ? 5 : 6;

            if (fisherman.getShipType() != shipType) {
                int DarshKey = model.updateDarshKeyByUserType(fisherman.getId(), shipType);
                this.idTextField.setText(Integer.toString(DarshKey));
                user.setDarshKey(DarshKey);
                updatedFields += "???? ?????????? ?????????? ???????????????? ?????????? : " + DarshKey;
                if (model.updateFishermanType(fisherman.getId(), shipType)) {
                    updatedFields += "\n ???? ?????????? ???????????? ?? : " + (shipType == 5 ? this.shipType[0] : this.shipType[1]);
                    fisherman.setShipType((shipType));
                    isUpdated = true;
                }
            }
        }
        if (isUpdated) {
            UIUtil.showAlert("?????? ?????????????? ??????????",
                    "???? ?????????? ???????????????? ??????????",
                    updatedFields,
                    Alert.AlertType.INFORMATION);
        } else {
            UIUtil.showAlert("???? ?????? ?????????? ????????????????",
                    "???? ???????? ???? ????????????",
                    null,
                    Alert.AlertType.INFORMATION);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        shipType = new String[]{"???????? ??????", "???????? ????????"};
        model = DatabaseModel.getModel();
        markets = model.getAllMarkets();
        marketComboBox = new ComboBox<>();
        FishermanTypeComboBox = new ComboBox<>();

        UIUtil.setNumbersOnly(idTextField);
        UIUtil.setNumbersOnly(phoneTextField);

        TextFieldSetEditable(false);

        marketComboBox.setPrefWidth(208.0);
        marketComboBox.setPrefHeight(25.0);
        marketComboBox.setPromptText("???????? ?????? ????????????");

        FishermanTypeComboBox.setPromptText("???????? ?????? ??????????????");
        FishermanTypeComboBox.getItems().addAll("??????", "????????");
        FishermanTypeComboBox.setPrefWidth(208.0);
        FishermanTypeComboBox.setPrefHeight(25.0);

        Platform.runLater(() -> {
            rootPane.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ESCAPE)
                    ((Stage) rootPane.getScene().getWindow()).close();
            });
            rootPane.getScene().getWindow().setOnCloseRequest(event -> {
                if (user.getDarshKey() != Integer.parseInt(idTextField.getText())
                        || !user.getName().equals(nameTextField.getText())
                        || (user.getPhone() == null && phoneTextField.getText() != null)
                        || (user.getPhone() != null && !user.getPhone().equals(phoneTextField.getText()))) {
                    Optional<ButtonType> result =
                            UIUtil.showConfirmDialog("???? ?????? ?????????? ???? ?????????? ???? ?????? ?????? ???????????????? ????????????????",
                                    "???????? ?????? ???????????????? ?????????????? ???? ?????? ?????? ??????????");
                    if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                        event.consume();
                    }
                }
            });
        });
    }

    public void onCheck() {
        TextFieldSetEditable(isEditable.isSelected());
    }

    public void TextFieldSetEditable(boolean isCheck) {
        idTextField.setDisable(!isCheck);
        nameTextField.setDisable(!isCheck);
        phoneTextField.setDisable(!isCheck);
        marketComboBox.setDisable(!isCheck);
        FishermanTypeComboBox.setDisable(!isCheck);
    }

    public void viewUser(User user) {
        this.user = user;
        if (user instanceof Seller) {
            title.setText("?????????? ???????????? ????????????");
            AddFromStorageBtn.setVisible(false);
            userEntryBtn.setText("???????? ????????????");
            idTextField.setText(Integer.toString(user.getDarshKey()));
            nameTextField.setText(user.getName());
            phoneTextField.setText(user.getPhone());
            balanceLabel.setText(Integer.toString(user.getBalance()));
            for (Market market : markets) marketComboBox.getItems().add(market);
            gridPane.add(marketComboBox, 1, 4);
            marketComboBox.setValue(model.getMarketByID(((Seller) user).getMarket()));

        } else if (user instanceof Fisherman) {
            title.setText("?????????? ???????????? ????????????");
            typeLabel.setText("?????? ??????????????");
            userEntryBtn.setText("???????? ????????????");
            idTextField.setText(Integer.toString(user.getDarshKey()));
            nameTextField.setText(user.getName());
            phoneTextField.setText(user.getPhone());
            balanceLabel.setText(Integer.toString(user.getBalance()));
            FishermanTypeComboBox.setValue(((Fisherman) user).getShipType() == 5 ? "??????" : "????????");
            gridPane.add(FishermanTypeComboBox, 1, 4);
        } else {
            System.out.println("Error: A valid user was not sent");
        }
    }

    public void onAddFromStorage() {
        UIUtil.addFromStorage(user);
    }

    public void onUserEntries() throws IOException {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(NavigationUtil.ENTRIES_FXML));
        Parent root = loader.load();

        if (user instanceof Seller) {
            stage.setTitle("???????? ???????????? : " + user.getName());
            stage.getIcons().add(new Image("/fbrs/photos/seller.png"));
        } else if (user instanceof Fisherman) {
            stage.setTitle("???????? ???????????? : " + user.getName());
            stage.getIcons().add(new Image("/fbrs/photos/Fisherman.png"));
        }
        Scene scene = new Scene(root, 1250, 600);
        scene.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        JMetro jMetro = new JMetro(Style.LIGHT);
        jMetro.setScene(scene);
        stage.setScene(scene);
        stage.centerOnScreen();

        EntriesController controller = loader.getController();
        controller.hideBackButton(false);
        controller.setViewType(user);
    }
}
