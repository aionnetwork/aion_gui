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

package org.aion.gui.controller;

import com.google.common.eventbus.Subscribe;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.aion.gui.events.EventBusRegistry;
import org.aion.gui.events.KernelProcEvent;
import org.aion.gui.events.RefreshEvent;
import org.aion.gui.events.UnexpectedApiDisconnectedEvent;
import org.aion.gui.model.GeneralKernelInfoRetriever;
import org.aion.gui.model.KernelConnection;
import org.aion.gui.model.KernelUpdateTimer;
import org.aion.gui.model.dto.SyncInfoDto;
import org.aion.gui.util.SyncStatusFormatter;
import org.aion.log.AionLoggerFactory;
import org.aion.os.KernelControlException;
import org.aion.os.KernelLauncher;
import org.aion.os.UnixKernelProcessHealthChecker;
import org.aion.wallet.console.ConsoleManager;
import org.slf4j.Logger;

public class DashboardController extends AbstractController {
    private final EventBusRegistry ebr;
    private final KernelLauncher kernelLauncher;
    private final KernelConnection kernelConnection;
    private final KernelUpdateTimer kernelUpdateTimer;
    private final ConsoleManager consoleManager;
    private final GeneralKernelInfoRetriever generalKernelInfoRetriever;
    private final UnixKernelProcessHealthChecker unixKernelProcessHealthChecker;
    private final SyncInfoDto syncInfoDTO;

    @FXML private Button launchKernelButton;
    @FXML private Button terminateKernelButton;

    // These should probably be in their own classes
    @FXML private Label kernelStatusLabel;
    @FXML private Label numPeersLabel;
    @FXML private Label isMining;
    @FXML private Label blocksLabel;

    private static final Logger LOG = AionLoggerFactory.getLogger(org.aion.log.LogEnum.GUI.name());

    public DashboardController(
            EventBusRegistry eventBusRegistry,
            KernelLauncher kernelLauncher,
            KernelConnection kernelConnection,
            KernelUpdateTimer kernelUpdateTimer,
            GeneralKernelInfoRetriever generalKernelInfoRetriever,
            SyncInfoDto syncInfoDTO,
            ConsoleManager consoleManager,
            UnixKernelProcessHealthChecker unixKernelProcessHealthChecker) {
        this.ebr = eventBusRegistry;
        this.kernelConnection = kernelConnection;
        this.kernelLauncher = kernelLauncher;
        this.kernelUpdateTimer = kernelUpdateTimer;
        this.generalKernelInfoRetriever = generalKernelInfoRetriever;
        this.unixKernelProcessHealthChecker = unixKernelProcessHealthChecker;
        this.syncInfoDTO = syncInfoDTO;
        this.consoleManager = consoleManager;
    }

    @Override
    public void internalInit(final URL location, final ResourceBundle resources) {}

    @Override
    protected void registerEventBusConsumer() {
        ebr.getBus(EventBusRegistry.KERNEL_BUS).register(this);
        ebr.getBus(RefreshEvent.ID).register(this);
    }
    // -- Handlers for Events coming from Model ---------------------------------------------------
    @Subscribe
    private void handleUiTimerTick(RefreshEvent event) {
        // peer count
        final Task<Integer> getPeerCountTask =
                getApiTask(o -> generalKernelInfoRetriever.getPeerCount(), null);
        runApiTask(
                getPeerCountTask,
                evt ->
                        Platform.runLater(
                                () -> {
                                    if (getPeerCountTask.getValue() != null) {
                                        numPeersLabel.setText(
                                                String.valueOf(getPeerCountTask.getValue()));
                                    }
                                }),
                getErrorEvent(throwable -> {}, getPeerCountTask),
                getEmptyEvent());
        // sync status
        Task<Void> getSyncInfoTask = getApiTask(o -> syncInfoDTO.loadFromApi(), null);
        runApiTask(
                getSyncInfoTask,
                evt ->
                        Platform.runLater(
                                () ->
                                        blocksLabel.setText(
                                                String.valueOf(
                                                        SyncStatusFormatter
                                                                .formatSyncStatusByBlockNumbers(
                                                                        syncInfoDTO)))),
                getErrorEvent(throwable -> {}, getSyncInfoTask),
                getEmptyEvent());
        // mining status
        Task<Boolean> getMiningStatusTask =
                getApiTask(o -> generalKernelInfoRetriever.isMining(), null);
        runApiTask(
                getMiningStatusTask,
                evt ->
                        Platform.runLater(
                                () -> {
                                    if (getMiningStatusTask.getValue() != null) {
                                        isMining.setText(
                                                String.valueOf(getMiningStatusTask.getValue()));
                                    }
                                }),
                getErrorEvent(throwable -> {}, getSyncInfoTask),
                getEmptyEvent());
    }

    @Subscribe
    private void handleKernelLaunched(final KernelProcEvent.KernelLaunchedEvent ev) {
        kernelConnection.connect(); // TODO: what if we launched the process but can't connect?

        kernelUpdateTimer.start();
        Platform.runLater(
                () -> {
                    kernelStatusLabel.setText("Running");
                    enableTerminateButton();
                });
    }

    @Subscribe
    private void handleKernelTerminated(final KernelProcEvent.KernelTerminatedEvent ev) {
        Platform.runLater(
                () -> {
                    enableLaunchButton();
                    kernelStatusLabel.setText("Not running");
                    numPeersLabel.setText("--");
                    blocksLabel.setText("--");
                    isMining.setText("--");
                });
    }

    @Subscribe
    private void handleLaunchKernelFailed(final KernelProcEvent.KernelLaunchFailedEvent ev) {
        Platform.runLater(
                () -> {
                    String kernelLog =
                            kernelLauncher.getStorageLocation().getAbsolutePath()
                                    + "/aion-kernel-output";
                    consoleManager.addLog(
                            "Kernel launch failed; check kernel logs for details: " + kernelLog,
                            ConsoleManager.LogType.KERNEL);
                    enableLaunchButton();
                    kernelStatusLabel.setText("Not running");
                    numPeersLabel.setText("--");
                    blocksLabel.setText("--");
                    isMining.setText("--");
                });
    }

    @Subscribe
    private void handleUnexpectedApiDisconnect(UnexpectedApiDisconnectedEvent event) {
        if (!kernelLauncher.hasLaunchedInstance()) {
            // Probably in the middle of disconnecting; no action needed
            return;
        }
        final boolean actuallyRunning;
        try {
            actuallyRunning =
                    unixKernelProcessHealthChecker.checkIfKernelRunning(
                            kernelLauncher.getLaunchedInstance().getPid());
        } catch (KernelControlException kce) {
            // If we get here we're so broken we don't know how to proceed; either the OS
            // is in a broken state or there's a bug/defect within our code.
            LOG.error(
                    "Detected connection error with Kernel API, but was not able to determine state of Kernel process.  "
                            + "It is recommended that you restart the GUI.");
            return;
        }

        if (actuallyRunning) {
            LOG.error(
                    "Detected connection error from Kernel API, but Kernel process is still running.  "
                            + "Please check that Java API is enabled in its configuration.");
        } else {
            LOG.info(
                    "Detected unexpected termination of Kernel process.  Internal resources will be cleaned up.");
            kernelUpdateTimer.stop();
            kernelConnection.disconnect();
            kernelLauncher.cleanUpDeadProcess();
        }
    }

    // -- Handlers for View components ------------------------------------------------------------
    public void launchKernel(MouseEvent ev) throws Exception {
        disableLaunchTerminateButtons();
        kernelStatusLabel.setText("Starting...");
        try {
            kernelLauncher.launch();
            consoleManager.addLog("Kernel launch started", ConsoleManager.LogType.KERNEL);
        } catch (RuntimeException ex) {
            consoleManager.addLog("Kernel launch failed", ConsoleManager.LogType.KERNEL);
            enableLaunchButton();
        }
    }

    public void terminateKernel(MouseEvent ev) throws Exception {
        disableLaunchTerminateButtons();
        kernelStatusLabel.setText("Terminating...");

        if (kernelLauncher.hasLaunchedInstance()
                || (!kernelLauncher.hasLaunchedInstance() && kernelLauncher.tryResume())) {
            kernelUpdateTimer.stop();
            final Task<Integer> termKernel =
                    getApiTask(
                            o -> {
                                kernelConnection.disconnect();
                                try {
                                    kernelLauncher.terminate();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return null;
                            },
                            null);
            runApiTask(
                    termKernel,
                    evt -> {
                        enableLaunchButton();
                        consoleManager.addLog(
                                "Kernel successfully terminated", ConsoleManager.LogType.KERNEL);
                    },
                    getErrorEvent(
                            throwable -> {
                                consoleManager.addLog(
                                        "Error terminating the kernel",
                                        ConsoleManager.LogType.KERNEL);
                                LOG.error("Error terminating the kernel", throwable);
                                enableTerminateButton();
                            },
                            termKernel),
                    getEmptyEvent());
        }
    }

    public void openConsole() {
        consoleManager.show();
    }

    // -- Helpers methods -------------------------------------------------------------------------
    private void enableLaunchButton() {
        Platform.runLater(() -> launchKernelButton.setDisable(false));
        Platform.runLater(() -> terminateKernelButton.setDisable(true));
    }

    private void enableTerminateButton() {
        Platform.runLater(() -> launchKernelButton.setDisable(true));
        Platform.runLater(() -> terminateKernelButton.setDisable(false));
    }

    private void disableLaunchTerminateButtons() {
        Platform.runLater(() -> launchKernelButton.setDisable(true));
        Platform.runLater(() -> terminateKernelButton.setDisable(true));
    }
}
