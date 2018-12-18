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

package org.aion.gui.util;

import com.google.common.eventbus.EventBus;
import java.util.TimerTask;
import javafx.application.Platform;
import org.aion.gui.events.EventBusRegistry;
import org.aion.gui.events.RefreshEvent;
import org.aion.log.AionLoggerFactory;
import org.slf4j.Logger;

public class DataUpdater extends TimerTask {
    private static final Logger LOG = AionLoggerFactory.getLogger(org.aion.log.LogEnum.GUI.name());

    //    public static final String UI_DATA_REFRESH = "gui.data_refresh";

    private final EventBus eventBus = /*EventBusRegistry.INSTANCE.getBus(UI_DATA_REFRESH)*/
            EventBusRegistry.INSTANCE.getBus(RefreshEvent.ID);

    @Override
    public void run() {
        Platform.runLater(
                () -> {
                    eventBus.post(new RefreshEvent(RefreshEvent.Type.TIMER));
                });
    }
}
