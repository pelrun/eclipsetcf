/*******************************************************************************
 * Copyright (c) 2007-2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.tcf.core.AbstractChannel;
import org.eclipse.tcf.core.ChannelHTTP;
import org.eclipse.tcf.core.ChannelPIPE;
import org.eclipse.tcf.core.ChannelTCP;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.ITransportProvider;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;

/**
 * Class TansportManager provides static methods for other core and internal TCF
 * classes which concern the maintenance of Channels, ChannelOpenListeners and
 * TransportProviders </br>
 *
 * These methods are used by Protocol, TransientPeer, AbstractChannel, and others.
 */
public class TransportManager {

    /**
     * Collection of Channels
     */
    private static final Collection<AbstractChannel> channels =
        new LinkedList<AbstractChannel>();
    /**
     * Collection of ChannelOpenListeners
     */
    private static final Collection<Protocol.ChannelOpenListener> listeners =
        new LinkedList<Protocol.ChannelOpenListener>();
    /**
     * Map of TransportProviders and their names
     */
    private static final HashMap<String,ITransportProvider> transports =
        new HashMap<String,ITransportProvider>();

    static {
        addTransportProvider(new ITransportProvider() {

            public String getName() {
                return "TCP";
            }

            public IChannel openChannel(IPeer peer) {
                assert getName().equals(peer.getTransportName());
                Map<String,String> attrs = peer.getAttributes();
                String host = attrs.get(IPeer.ATTR_IP_HOST);
                String port = attrs.get(IPeer.ATTR_IP_PORT);
                if (host == null) throw new IllegalArgumentException("No host name");
                return new ChannelTCP(peer, host, parsePort(port), false);
            }
        });

        addTransportProvider(new ITransportProvider() {

            public String getName() {
                return "SSL";
            }

            public IChannel openChannel(IPeer peer) {
                assert getName().equals(peer.getTransportName());
                Map<String,String> attrs = peer.getAttributes();
                String host = attrs.get(IPeer.ATTR_IP_HOST);
                String port = attrs.get(IPeer.ATTR_IP_PORT);
                if (host == null) throw new IllegalArgumentException("No host name");
                return new ChannelTCP(peer, host, parsePort(port), true);
            }
        });

        addTransportProvider(new ITransportProvider() {

            public String getName() {
                return "PIPE";
            }

            public IChannel openChannel(IPeer peer) {
                assert getName().equals(peer.getTransportName());
                String name = peer.getAttributes().get("PipeName");
                if (name == null) name = "//./pipe/TCF-Agent";
                return new ChannelPIPE(peer, name);
            }
        });

        addTransportProvider(new ITransportProvider() {

            public String getName() {
                return "HTTP";
            }

            public IChannel openChannel(IPeer peer) {
                assert getName().equals(peer.getTransportName());
                Map<String,String> attrs = peer.getAttributes();
                String host = attrs.get(IPeer.ATTR_IP_HOST);
                String port = attrs.get(IPeer.ATTR_IP_PORT);
                if (host == null) throw new IllegalArgumentException("No host name");
                return new ChannelHTTP(peer, host, parsePort(port));
            }
        });

        addTransportProvider(new ITransportProvider() {

            public String getName() {
                return "Loop";
            }

            public IChannel openChannel(IPeer peer) {
                assert getName().equals(peer.getTransportName());
                return new ChannelLoop(peer);
            }
        });
    }

    /**
     * Converts a port number from String representation to int
     *
     * @param port port number
     * @return port number as an int
     */
    private static int parsePort(String port) {
        if (port == null) throw new Error("No port number");
        try {
            return Integer.parseInt(port);
        }
        catch (NumberFormatException x) {
            IllegalArgumentException y = new IllegalArgumentException(
                    "Invalid value of \"Port\" attribute. Must be decimal number.");
            y.initCause(x);
            throw y;
        }
    }

    /**
     * Adds different transport providers to the private collection of
     * transports, if a transport was already in the collection, then it a Error
     * is thrown
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}
     *
     * @param transport
     *            TransportProviders such as TCP, Pipe, etc..
     */
    public static void addTransportProvider(ITransportProvider transport) {
        String name = transport.getName();
        assert name != null;
        synchronized (transports) {
            if (transports.get(name) != null) throw new Error("Already registered: " + name);
            transports.put(name, transport);
        }
    }

    /**
     * Remove transport providers to the private collection of transports, if a
     * transport if a transport was already in the collection
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}
     *
     * @param transport TransportProviders such as TCP, Pipe, etc..
     */
    public static void removeTransportProvider(ITransportProvider transport) {
        String name = transport.getName();
        assert name != null;
        synchronized (transports) {
            if (transports.get(name) == transport) transports.remove(name);
        }
    }

    /**
     * Opens the Channel to which the peer belongs to, using the underlying
     * TransportProvider. The channel is not added to the list of open channels
     * yet because the channel is not fully open.
     *
     * @param peer
     *            on which to open the channel
     * @return the channel instance to be opened
     */
    public static IChannel openChannel(IPeer peer) {
        String name = peer.getTransportName();
        if (name == null) throw new Error("No transport name");
        ITransportProvider transport = null;
        synchronized (transports) {
            transport = transports.get(name);
            if (transport == null) throw new Error("Unknown transport name: " + name);
        }
        return transport.openChannel(peer);
    }

    /**
     * Adds the channel to the collection of open channels. The channel is fully
     * open by this time.
     *
     * @param channel the channel
     */
    public static void channelOpened(final AbstractChannel channel) {
        assert !channels.contains(channel);
        channels.add(channel);
        Protocol.ChannelOpenListener[] array = listeners.toArray(new Protocol.ChannelOpenListener[listeners.size()]);
        for (Protocol.ChannelOpenListener l : array) {
            try {
                l.onChannelOpen(channel);
            }
            catch (Throwable x) {
                Protocol.log("Exception in channel listener", x);
            }
        }
    }

    /**
     * Removes the channel from the collection of channels. This method is called by the Channel whenever it closes.
     *
     * @param channel channel to close
     * @param x Error to throw
     */
    public static void channelClosed(final AbstractChannel channel, final Throwable x) {
        assert channels.contains(channel);
        channels.remove(channel);
    }

    /**
     * Returns an array of all the open channels
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}
     */
    public static IChannel[] getOpenChannels() {
        return channels.toArray(new IChannel[channels.size()]);
    }

    /**
     * Add a Channel Open Listener, i.e: a listener that will be notified when a channel is opened
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}
     *
     * @param listener listener to be added
     */
    public static void addChanelOpenListener(Protocol.ChannelOpenListener listener) {
        assert listener != null;
        listeners.add(listener);
    }

    /**
     * Remove a Channel Open Listener, i.e: a listener that will be notified when a channel is opened
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}
     *
     * @param listener to be removed
     */
    public static void removeChanelOpenListener(Protocol.ChannelOpenListener listener) {
        listeners.remove(listener);
    }

    /**
     * Transmit TCF event message.
     * The message is sent to all open communication channels - broadcasted.
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}.
     *
     * @param service_name service name
     * @param event_name
     * @param data
     */
    public static void sendEvent(String service_name, String event_name, byte[] data) {
        for (AbstractChannel c : channels) {
            // Skip channels that are executing "redirect" command - STATE_OPENING
            if (c.getState() == IChannel.STATE_OPEN) {
                IService s = c.getLocalService(service_name);
                if (s != null) c.sendEvent(s, event_name, data);
            }
        }
    }

    /**
     * Call back after TCF messages sent by this host up to this moment are delivered
     * to their intended targets. This method is intended for synchronization of messages
     * across multiple channels.
     *
     * Note: Cross channel synchronization can reduce performance and throughput.
     * Most clients don't need cross channel synchronization and should not call this method.
     *
     * @param done will be executed by dispatch thread after communication
     * messages are delivered to corresponding targets.
     *
     * This is internal API, TCF clients should use {@code org.eclipse.tcf.protocol.Protocol}.
     */
    public static void sync(final Runnable done) {
        final Set<IToken> set = new HashSet<IToken>();
        ILocator.DoneSync done_sync = new ILocator.DoneSync() {
            public void doneSync(IToken token) {
                assert set.contains(token);
                set.remove(token);
                if (set.isEmpty()) done.run();
            }
        };
        for (AbstractChannel c : channels) {
            if (c.getState() == IChannel.STATE_OPEN) {
                ILocator s = c.getRemoteService(ILocator.class);
                if (s != null) set.add(s.sync(done_sync));
            }
        }
        if (set.isEmpty()) Protocol.invokeLater(done);
    }
}
