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

package org.aion.gui.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.aion.zero.impl.config.dynamic.InFlightConfigReceiver;
import org.aion.zero.impl.config.dynamic.InFlightConfigReceiverMBean;
import org.junit.Test;

public class ConfigManipulatorIntegTest {
    // should probably have some logic to randomly try ports in case some other process is using
    // this one
    private final int JMX_PORT = 31234;

    /**
     * Tests that {@link ConfigManipulator.JmxCaller#sendConfigProposal(String)} ()} creates a proxy
     * that can talk to JMX.
     */
    @Test
    public void testJmxCallerGetInFlightConfigReceiver() throws Exception {
        // set up the jmx "server" that the proxy will connect to
        LocateRegistry.createRegistry(JMX_PORT);
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url =
                new JMXServiceURL(
                        String.format(
                                "service:jmx:rmi://localhost/jndi/rmi://localhost:%d/jmxrmi",
                                JMX_PORT));
        JMXConnectorServer svr = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
        svr.start();
        InFlightConfigReceiverMBean proxyTarget = mock(InFlightConfigReceiver.class);
        ObjectName objectName = new ObjectName(InFlightConfigReceiver.DEFAULT_JMX_OBJECT_NAME);
        server.registerMBean(proxyTarget, objectName);

        ConfigManipulator.JmxCaller client = new ConfigManipulator.JmxCaller();
        client.sendConfigProposal(JMX_PORT, "<myXml/>");
        verify(proxyTarget).propose("<myXml/>");
    }
}
