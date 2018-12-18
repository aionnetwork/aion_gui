/*
 * Copyright (c) 2017-2018 Aion foundation.
 *
 *     This file is part of the aion network project.
 *
 *     The aion network project is free software: you can redistribute it
 *     and/or modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation, either version 3 of
 *     the License, or any later version.
 *
 *     The aion network project is distributed in the hope that it will
 *     be useful, but WITHOUT ANY WARRANTY; without even the implied
 *     warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *     See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the aion network project source files.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Aion foundation.
 */
package org.aion.wallet.ui.components.partials;

import com.google.common.eventbus.Subscribe;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import org.aion.gui.controller.ControllerFactory;
import org.aion.gui.events.EventBusRegistry;
import org.aion.wallet.account.AccountManager;
import org.aion.wallet.console.ConsoleManager;
import org.aion.wallet.dto.AccountDTO;
import org.aion.wallet.events.AccountEvent;
import org.aion.wallet.exception.ValidationException;
import org.slf4j.Logger;

public class UnlockAccountDialog implements Initializable {

    private static final Logger LOGGER =
            org.aion.log.AionLoggerFactory.getLogger(org.aion.log.LogEnum.GUI.name());

    private final Popup popup = new Popup();
    private final AccountManager accountManager;
    private final ConsoleManager consoleManager;

    @FXML private PasswordField unlockPassword;
    @FXML private Label validationError;
    private AccountDTO account;

    public UnlockAccountDialog(AccountManager accountManager, ConsoleManager consoleManager) {
        this.accountManager = accountManager;
        this.consoleManager = consoleManager;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        registerEventBusConsumer();
        Platform.runLater(() -> unlockPassword.requestFocus());
    }

    public void open(final MouseEvent mouseEvent) {
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        Pane unlockAccountDialog;
        try {
            //            unlockAccountDialog =
            // FXMLLoader.load(getClass().getResource("UnlockAccountDialog.fxml"));
            FXMLLoader loader =
                    new FXMLLoader((getClass().getResource("UnlockAccountDialog.fxml")));
            loader.setControllerFactory(
                    new ControllerFactory()
                            .withAccountManager(
                                    accountManager) /* TODO a specialization only has what we need */);
            unlockAccountDialog = loader.load();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        Node eventSource = (Node) mouseEvent.getSource();
        final double windowX = eventSource.getScene().getWindow().getX();
        final double windowY = eventSource.getScene().getWindow().getY();
        popup.setX(
                windowX
                        + eventSource.getScene().getWidth() / 2
                        - unlockAccountDialog.getPrefWidth() / 2);
        popup.setY(
                windowY
                        + eventSource.getScene().getHeight() / 2
                        - unlockAccountDialog.getPrefHeight() / 2);
        popup.getContent().addAll(unlockAccountDialog);
        popup.show(eventSource.getScene().getWindow());
    }

    private void close(final InputEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    public void unlockAccount(final InputEvent event) {
        final String password = unlockPassword.getText();
        if (password != null && !password.isEmpty()) {
            try {
                accountManager.unlockAccount(account, password);
                consoleManager.addLog(
                        "Account" + account.getPublicAddress() + " unlocked",
                        ConsoleManager.LogType.ACCOUNT);
                close(event);
            } catch (ValidationException e) {
                consoleManager.addLog(
                        "Account" + account.getPublicAddress() + " could not be unlocked",
                        ConsoleManager.LogType.ACCOUNT,
                        ConsoleManager.LogLevel.WARNING);
                validationError.setText(e.getMessage());
                validationError.setVisible(true);
            }
        } else {
            validationError.setText("Please insert a password!");
            validationError.setVisible(true);
        }
    }

    public void resetValidation() {
        validationError.setVisible(false);
    }

    @Subscribe
    private void handleUnlockStarted(final AccountEvent event) {
        if (AccountEvent.Type.UNLOCKED.equals(event.getType())) {
            this.account = event.getPayload();
        }
    }

    @FXML
    private void submitOnEnterPressed(final KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            unlockAccount(event);
        }
    }

    private void registerEventBusConsumer() {
        EventBusRegistry.INSTANCE.getBus(AccountEvent.ID).register(this);
    }
}
