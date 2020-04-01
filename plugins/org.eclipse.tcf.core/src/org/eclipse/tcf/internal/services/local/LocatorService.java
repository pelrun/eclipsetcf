/*******************************************************************************
 * Copyright (c) 2007-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tcf.internal.core.LocalPeer;
import org.eclipse.tcf.internal.core.LoggingUtil;
import org.eclipse.tcf.internal.core.RemotePeer;
import org.eclipse.tcf.internal.core.ServiceManager;
import org.eclipse.tcf.core.AbstractChannel;
import org.eclipse.tcf.core.UserDefPeer;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IServiceProvider;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;


/**
 * Locator service uses transport layer to search
 * for peers and to collect and maintain up-to-date
 * data about peer's attributes.
 */
// TODO: research usage of DNS-SD (DNS Service Discovery) to discover TCF peers
public class LocatorService implements ILocator {

    /**
     * Default discovery port
     */
    private static final int DISCOVERY_PORT = 1534;
    /**
     * Max packet size
     */
    private static final int MAX_PACKET_SIZE = 9000 - 40 - 8;
    /**
     * Preferred packet size
     */
    private static final int PREF_PACKET_SIZE = 1500 - 40 - 8;

    private static LocatorService locator;
    private static LocalPeer local_peer;

    private static final Map<String,IPeer> peers = new HashMap<String,IPeer>();
    private static final ArrayList<LocatorListener> listeners = new ArrayList<LocatorListener>();
    private static final HashSet<String> error_log = new HashSet<String>();

    private final HashSet<SubNet> subnets = new HashSet<SubNet>();
    private final ArrayList<Slave> slaves = new ArrayList<Slave>();
    private final byte[] inp_buf = new byte[MAX_PACKET_SIZE];
    private final byte[] out_buf = new byte[MAX_PACKET_SIZE];

    private InetAddress loopback_addr;

    /**
     * Flag indicating whether tracing of the discovery activity is enabled.
     */
    private static boolean TRACE_DISCOVERY = System.getProperty("org.eclipse.tcf.core.tracing.discovery") != null;

    /**
     * Internal Subnetwork Representation
     */
    private static class SubNet {
        final int prefix_length;
        final InetAddress address;
        final InetAddress broadcast;
        final String host_name;

        long last_slaves_req_time;
        boolean send_all_ok;

        SubNet(int prefix_length, InetAddress address, InetAddress broadcast) {
            this.prefix_length = prefix_length;
            this.address = address;
            this.broadcast = broadcast;
            host_name = address.getHostName();
        }

        /**
         * Check whether the IP Address in part the subnetwork
         * @param addr IP Address
         * @return true if the IP Address is contained, otherwise false
         */
        boolean contains(InetAddress addr) {
            if (addr == null || address == null) return false;
            byte[] a1 = addr.getAddress();
            byte[] a2 = address.getAddress();
            if (a1.length != a2.length) return false;
            int i = 0;
            int l = prefix_length <= a1.length * 8 ? prefix_length : a1.length * 8;
            while (i + 8 <= l) {
                int n = i / 8;
                if (a1[n] != a2[n]) return false;
                i += 8;
            }
            while (i < l) {
                int n = i / 8;
                int m = 1 << (7 - i % 8);
                if ((a1[n] & m) != (a2[n] & m)) return false;
                i++;
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SubNet)) return false;
            SubNet x = (SubNet)o;
            return
                prefix_length == x.prefix_length &&
                broadcast.equals(x.broadcast) &&
                address.equals(x.address);
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public String toString() {
            return address.getHostAddress() + "/" + prefix_length;
        }
    }

    /** Represents a peer which was discovered, that is a peer whose Locator was not the first Locator in the network.
     * A master is the peer which discovered a slave, that is a peer whose Locator was the first Locator in the network */
    private static class Slave {
        final InetAddress address;
        final int port;

        /** Time of last packet receiver from this slave */
        long last_packet_time;

        /** Time of last REQ_SLAVES packet received from this slave */
        long last_req_slaves_time;

        Slave(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public String toString() {
            return address.getHostAddress() + "/" + port;
        }
    }

    /**
     * Represents a hostname/address item that is used when resolving IP addresses from hostnames.
     * It contains a timestamp of the last time the resolution of hostname -> ip was done.
     * As well as if it the mapping was used in a DATA_RETENTION_PERIOD
     *
     */
    private static class AddressCacheItem {
        final String host;
        InetAddress address;
        long time_stamp;
        boolean used;

        AddressCacheItem(String host) {
            this.host = host;
        }
    }

    /**
     * Stores Hostname/IP addresses mappings that are used when resolving an IP Address from a hostname
     */
    private static final HashMap<String,AddressCacheItem> addr_cache = new HashMap<String,AddressCacheItem>();

    /**
     * Used to request an inmediate prune and update of addr_cache mappings
     */
    private static boolean addr_request;

    /**
     * Socket used to send/receive the packets to/from remote peers
     */
    private DatagramSocket socket;

    /**
     * The timestamp of the last received packet from a Locator master.
     * If there is no remote locator master, meaning that this is the locator master, then the value is 0.
     */
    private long last_master_packet_time;

    /**
     * LocatorService's thread for refreshing subnet list of peers, as part of the discovery.
     * Its run method delegates the refresh of the subnet list of peers to the dispatch thread, using the {@code refresh_timer} method.
     * Thread's name is: TCF Locator Timer
     */
    private final Thread timer_thread = new Thread() {
        public void run() {
            while (true) {
                try {
                    final HashSet<SubNet> set = getSubNetList();
                    Protocol.invokeAndWait(new Runnable() {
                        public void run() {
                            refresh_timer(set);
                        }
                    });
                    sleep(DATA_RETENTION_PERIOD / 4);
                }
                catch (IllegalStateException x) {
                    // TCF event dispatch is shut down
                    return;
                }
                catch (Throwable x) {
                    log("Unhandled exception in TCF discovery timer thread", x);
                }
            }
        }
    };

    /**
     * LocatorService's thread for maintaining the hostname/ip mapping held in addr_cache as part of the discovery.
     * It runs while the <code>getInetAddress(String hostname)</code> is not running, to refresh ip resolution of hostsnames that getInetAddress has already added to addr_cache
     * Thread's name is: TCF Locator DNS Lookup
     */
    private Thread dns_lookup_thread = new Thread() {
        public void run() {
            while (true) {
                try {
                    long time;
                    HashSet<AddressCacheItem> set = null;
                    synchronized (addr_cache) {
                        if (!addr_request) addr_cache.wait(DATA_RETENTION_PERIOD);
                        time = System.currentTimeMillis();
                        for (Iterator<AddressCacheItem> i = addr_cache.values().iterator(); i.hasNext();) {
                            AddressCacheItem a = i.next();
                            if (a.time_stamp + DATA_RETENTION_PERIOD * 10 < time) {
                                /*
                                 * Remove entries that have not been used in between sucessive runs of this method
                                 */
                                if (a.used) {
                                    if (set == null) set = new HashSet<AddressCacheItem>();
                                    set.add(a);
                                }
                                else {
                                    i.remove();
                                }
                            }
                        }
                        /*
                         * Once we have pruned the addr_cache we wait for the DATA_RETENTION_PERIOD unless the getInetAddress method sets addr_request true
                         */
                        addr_request = false;
                    }
                    /*
                     * Update the used AddressCacheItems of addr_cache
                     */
                    if (set != null) {
                        for (AddressCacheItem a : set) {
                            InetAddress addr = null;
                            try {
                                addr = InetAddress.getByName(a.host);
                            }
                            catch (UnknownHostException x) {
                            }
                            synchronized (addr_cache) {
                                a.address = addr;
                                a.time_stamp = time;
                                a.used = false;
                            }
                        }
                    }
                }
                catch (Throwable x) {
                    log("Unhandled exception in TCF discovery DNS lookup thread", x);
                }
            }
        }
    };

    /**
     * Wrapper for final class DatagramPacket so its toString() can present
     * the value in the debugger in a readable fashion.
     */
    private static class InputPacket {

        final DatagramPacket p;

        InputPacket(DatagramPacket p) {
            this.p = p;
        }

        DatagramPacket getPacket() {
            return p;
        }

        int getLength() {
            return p.getLength();
        }

        byte[] getData() {
            return p.getData();
        }

        int getPort() {
            return p.getPort();
        }

        InetAddress getAddress() {
            return p.getAddress();
        }

        @Override
        public String toString() {
            return "[address=" + p.getAddress().toString()
                 + ",port=" + p.getPort()
                 + ",data=\"" + new String(p.getData(), 0, p.getLength()) + "\"]";
        }
    }

    /**
     * LocatorService's thread for handling packets received from other Peers, as part of the discovery.
     * Its run method delegates the handling of the packet to the dispatch thread, using the {@code handleDatagramPacket} method.
     * Thread's name is: TCF Locator Receiver
     */
    private final Thread input_thread = new Thread() {
        public void run() {
            try {
                for (;;) {
                    DatagramSocket socket = LocatorService.this.socket;
                    try {
                        final InputPacket p = new InputPacket(new DatagramPacket(inp_buf, inp_buf.length));
                        socket.receive(p.getPacket());
                        Protocol.invokeAndWait(new Runnable() {
                            public void run() {
                                handleDatagramPacket(p);
                            }
                        });
                    }
                    catch (IllegalStateException x) {
                        // TCF event dispatch is shut down
                        return;
                    }
                    catch (Exception x) {
                        if (socket != LocatorService.this.socket) continue;
                        log("Cannot read from datagram socket at port " + socket.getLocalPort(), x);
                        sleep(2000);
                    }
                }
            }
            catch (Throwable x) {
                log("Unhandled exception in socket reading thread", x);
            }
        }
    };

    static {
        ServiceManager.addServiceProvider(new IServiceProvider() {

            /*
             * LocatorService's ServiceProvider only implements getLocalService, as this service is implemented locally and not remotely
             */
            public IService[] getLocalService(final IChannel channel) {
                channel.addCommandServer(locator, new IChannel.ICommandServer() {
                    public void command(IToken token, String name, byte[] data) {
                        locator.command((AbstractChannel)channel, token, name, data);
                    }
                });
                return new IService[]{ locator };
            }

            public IService getServiceProxy(IChannel channel, String service_name) {
                return null;
            }
        });

        // Bug #400659 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=400659):
        //
        // To workaround Bug 388125 (Oracle bug 7179799) with Java 7, force
        // "java.net.preferIPv4Stack" to be set to "true"
        //
        // Applies on Windows platforms only.
        boolean isWin = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
        if (isWin && System.getProperty("java.net.preferIPv4Stack") == null) { //$NON-NLS-1$
            System.setProperty("java.net.preferIPv4Stack", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Creates a socket which will behave either as a slave locator or master locator, depending on the given slave parameter
     * @param slave true if the socket is representing a slave locator, false otherwise
     * @return the newly created socket
     * @throws SocketException thrown when the socket could not be created on the discovery port (couldn't be a master locator)
     */
    private static DatagramSocket createSocket(boolean slave) throws SocketException {
        DatagramSocket socket = null;
        if (slave) {
            socket = new DatagramSocket();
        }
        else {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
        }
        socket.setBroadcast(true);
        return socket;
    }

    /**
     * Creates a local instance of the LocatorService which involves creating:
     * 1. LocalPeer
     * 2. LocatorService itself
     */
    public static void createLocalInstance() {
        local_peer = new LocalPeer();
        locator = new LocatorService();
    }

    /**
     * LocatorService constructor used in {@code createLocalInstace}
     */
    public LocatorService() {
        try {
            loopback_addr = InetAddress.getByName(null);
            out_buf[0] = 'T';
            out_buf[1] = 'C';
            out_buf[2] = 'F';
            out_buf[3] = CONF_VERSION;
            out_buf[4] = 0;
            out_buf[5] = 0;
            out_buf[6] = 0;
            out_buf[7] = 0;
            try {
                socket = createSocket(false);
                if (TRACE_DISCOVERY) {
                    LoggingUtil.trace("Became the master agent (bound to port " + socket.getLocalPort() + ")");
                }
            }
            catch (SocketException x) {
                socket = createSocket(true);
                if (TRACE_DISCOVERY) {
                    LoggingUtil.trace("Became a slave agent (bound to port " + socket.getLocalPort() + ")");
                }
            }
            input_thread.setName("TCF Locator Receiver");
            timer_thread.setName("TCF Locator Timer");
            dns_lookup_thread.setName("TCF Locator DNS Lookup");
            input_thread.setDaemon(true);
            timer_thread.setDaemon(true);
            dns_lookup_thread.setDaemon(true);
            /*
             * All the locator thread's are run in the dispatch thread
             */
            Protocol.invokeLater(new Runnable() {
                @Override
                public void run() {
                    input_thread.start();
                    timer_thread.start();
                    dns_lookup_thread.start();
                }
            });
            listeners.add(new LocatorListener() {
                @Override
                public void peerAdded(IPeer peer) {
                    sendPeerInfo(peer, null, 0);
                }
                @Override
                public void peerChanged(IPeer peer) {
                    sendPeerInfo(peer, null, 0);
                }
                @Override
                public void peerHeartBeat(String id) {
                }
                @Override
                public void peerRemoved(String id) {
                }
            });
        }
        catch (Exception x) {
            log("Cannot open UDP socket for TCF discovery protocol", x);
        }
    }

    /**
     * Get the LocalPeer instance
     * @return LocalPeer instance
     */
    public static LocalPeer getLocalPeer() {
        return local_peer;
    }

    public static boolean isLocalHostPeer(IPeer peer) {
        assert Protocol.isDispatchThread();
        String host = peer.getAttributes().get(IPeer.ATTR_IP_HOST);
        if (host != null && host.length() > 0 && locator != null) {
            InetAddress addr = locator.getInetAddress(host);
            for (SubNet subnet : locator.subnets) {
                if (host.equals(subnet.host_name)) return true;
                if (addr != null && addr.equals(subnet.address)) return true;
            }
        }
        return false;
    }

    /**
     * Get the Locator Event listeners
     * @return array of locator listeners
     */
    public static LocatorListener[] getListeners() {
        return listeners.toArray(new LocatorListener[listeners.size()]);
    }

    /**
     * Creates an error report using the given TCF error code, and msg parameters
     * @param code the error code, from the Standard TCF error codes
     * @param msg an error message
     * @return return a map representation of the error report
     */
    private Map<String,Object> makeErrorReport(int code, String msg) {
        Map<String,Object> err = new HashMap<String,Object>();
        err.put(IErrorReport.ERROR_TIME, Long.valueOf(System.currentTimeMillis()));
        err.put(IErrorReport.ERROR_CODE, Integer.valueOf(code));
        err.put(IErrorReport.ERROR_FORMAT, msg);
        return err;
    }

    /**
     * Handles all command messages received through the channel
     *
     * @param channel the channel through which the commands are received/sent
     * @param token handle associated with the command
     * @param name command name
     * @param data command arguments in byte array format
     */
    private void command(final AbstractChannel channel, final IToken token, String name, byte[] data) {
        try {
            /*
             * Redirect command
             */
            if (name.equals("redirect")) {
                String peer_id = (String)JSON.parseSequence(data)[0];
                IPeer peer = peers.get(peer_id);
                if (peer == null) {
                    channel.sendResult(token, JSON.toJSONSequence(new Object[]{
                            makeErrorReport(IErrorReport.TCF_ERROR_UNKNOWN_PEER, "Unknown peer ID") }));
                    return;
                }
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ null }));
                if (peer instanceof LocalPeer) {
                    channel.sendEvent(Protocol.getLocator(), "Hello", JSON.toJSONSequence(
                            new Object[]{ channel.getLocalServices() }));
                    return;
                }
                new ChannelProxy(channel, peer.openChannel());
            }
            /*
             * Sync command
             */
            else if (name.equals("sync")) {
                channel.sendResult(token, null);
            }
            /*
             * getPeers command
             */
            else if (name.equals("getPeers")) {
                int i = 0;
                Object[] arr = new Object[peers.size()];
                for (IPeer p : peers.values()) arr[i++] = p.getAttributes();
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ null, arr }));
            }
            /*
             * Unrecognized command
             */
            else {
                channel.rejectCommand(token);
            }
        }
        catch (Throwable x) {
            channel.terminate(x);
        }
    }

    /**
     * Logs the given message and Exception/Error
     * @param msg message to be logged
     * @param x exception or error to be logged
     */
    private void log(String msg, Throwable x) {
        // Don't report same error multiple times to avoid filling up the log file.
        synchronized (error_log) {
            if (error_log.contains(msg)) return;
            error_log.add(msg);
        }
        Protocol.log(msg, x);
    }

    /**
     * Gets the IP Address from a Hostname.
     * To do that it uses the addr_cache, to search for an entry with the given host.
     * If there's such an entry, it returns the address.
     * If there isn't such an entry it constructs an AddressCacheItem entry and adds it to the addr_cache.
     * Marks the given AddressCacheItem's used field as true.
     *
     * @param host Host in Hostname format
     * @return IP Address of the Host
     */
    private InetAddress getInetAddress(String host) {
        if (host == null || host.length() == 0) return null;
        synchronized (addr_cache) {
            AddressCacheItem i = addr_cache.get(host);
            if (i == null) {
                i = new AddressCacheItem(host);
                char ch = host.charAt(0);
                if (ch == '[' || ch == ':' || ch >= '0' && ch <= '9') {
                    try {
                        i.address = InetAddress.getByName(host);
                    }
                    catch (UnknownHostException e) {
                    }
                    i.time_stamp = System.currentTimeMillis();
                }
                else {
                    /* InetAddress.getByName() can cause long delay - delegate to background thread */
                    addr_request = true;
                    addr_cache.notify();
                }
                addr_cache.put(host, i);
            }
            i.used = true;
            return i.address;
        }
    }

    /**
     * Refreshes the Subnetworks lists for slaves
     * @param nets Subnetwork lists
     */
    private void refresh_timer(HashSet<SubNet> nets) {
        long time = System.currentTimeMillis();
        /* Cleanup slave table */
        if (slaves.size() > 0) {
            int i = 0;
            while (i < slaves.size()) {
                Slave s = slaves.get(i);
                /* Removes slave if there is no recent packet receveid from it */
                if (s.last_packet_time + DATA_RETENTION_PERIOD < time) {
                    slaves.remove(i);
                }
                else {
                    i++;
                }
            }
        }
        /* Cleanup peers table */
        ArrayList<RemotePeer> stale_peers = null;
        for (IPeer p : peers.values()) {
            if (p instanceof RemotePeer) {
                RemotePeer r = (RemotePeer)p;
                if (r.getLastUpdateTime() + DATA_RETENTION_PERIOD < time) {
                    if (stale_peers == null) stale_peers = new ArrayList<RemotePeer>();
                    stale_peers.add(r);
                }
            }
        }
        if (stale_peers != null) {
            for (RemotePeer p : stale_peers) p.dispose();
        }
        /* Try to become a master */
        if (socket.getLocalPort() != DISCOVERY_PORT && last_master_packet_time + DATA_RETENTION_PERIOD / 2 <= time) {
            try {
                DatagramSocket s0 = socket;
                socket = createSocket(false);
                if (TRACE_DISCOVERY) {
                    LoggingUtil.trace("Became the master agent (bound to port " + socket.getLocalPort() + ")");
                }
                s0.close();
            }
            catch (Throwable x) {
            }
        }
        if (refreshSubNetList(nets)) sendPeersRequest(null, 0);
        if (socket.getLocalPort() != DISCOVERY_PORT) {
            for (SubNet subnet : subnets) {
                addSlave(subnet.address, socket.getLocalPort(), time);
            }
        }
        sendAll(null, 0, null, time);
    }

    /**
     * Add a slaves with the given Address, port number and timestamp to the array of slaves, and return it
     * @param addr IP Address of the slave
     * @param port Port of the slave
     * @param timestamp timestamp at which the last packet was received
     * @return the slave which was created using the given parameters
     */
    private Slave addSlave(InetAddress addr, int port, long timestamp) {
        /*
         * Check if there's an slave already in the list of known slaves, and if there is refresh the timestamp of the last packet received and return it
         */
        for (Slave s : slaves) {
            if (s.port == port && s.address.equals(addr)) {
                if (s.last_packet_time < timestamp) s.last_packet_time = timestamp;
                return s;
            }
        }
        final Slave s = new Slave(addr, port);
        s.last_packet_time = timestamp;
        slaves.add(s);
        /*
         * Crea
         */
        Protocol.invokeLater(new Runnable() {
            public void run() {
                long time_now = System.currentTimeMillis();
                sendPeersRequest(s.address, s.port);
                sendAll(s.address, s.port, s, time_now);
                sendSlaveInfo(s, time_now);
            }
        });
        return s;
    }

    private boolean refreshSubNetList(HashSet<SubNet> set) {
        if (set == null) return false;
        for (Iterator<SubNet> i = subnets.iterator(); i.hasNext();) {
            SubNet s = i.next();
            if (set.contains(s)) continue;
            i.remove();
        }
        boolean new_nets = false;
        for (Iterator<SubNet> i = set.iterator(); i.hasNext();) {
            SubNet s = i.next();
            if (subnets.contains(s)) continue;
            subnets.add(s);
            new_nets = true;
        }
        if (TRACE_DISCOVERY) {
            StringBuilder str = new StringBuilder("Refreshed subnet list:");
            for (SubNet subnet : subnets) {
                str.append("\n\t* address=" + subnet.address + ", broadcast=" + subnet.broadcast);
            }
            LoggingUtil.trace(str.toString());
        }
        return new_nets;
    }

    /**
     * Finds all subnetworks, adds them to the {@code subnet} set and returns it.
     * @return set of SubNets
     */
    private HashSet<SubNet> getSubNetList() {
        HashSet<SubNet> set = new HashSet<SubNet>();
        try {
            String osname = System.getProperty("os.name", "");
            String java_version = System.getProperty("java.version", "");
            if (osname.startsWith("Windows") && (java_version.startsWith("1.6.") || java_version.startsWith("1.7."))) {
                /*
                 * Workaround for JVM bug:
                 * InterfaceAddress.getNetworkPrefixLength() does not conform to Javadoc
                 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6707289
                 *
                 * The bug shows up only on Windows when IPv6 is enabled.
                 * The bug is supposed to be fixed in Java 1.7.
                 */
                getWindowsSubNetList(set);
            }
            else {
                getSubNetList(set);
            }
        }
        catch (Exception x) {
            log("Cannot get list of network interfaces", x);
            return null;
        }
        return set;
    }

    /**
     * Find and adds all subnets to the given set
     * @param set set of subnetworks
     * @throws SocketException
     */
    private void getSubNetList(HashSet<SubNet> set) throws SocketException {
        for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
            NetworkInterface f = e.nextElement();
            for (InterfaceAddress ia : f.getInterfaceAddresses()) {
                int network_prefix_len = ia.getNetworkPrefixLength();
                InetAddress address = ia.getAddress();
                InetAddress broadcast = ia.getBroadcast();

                if (address instanceof Inet4Address) {
                    if (network_prefix_len == 0) {
                        // Java 1.6.0 on Linux returns network prefix == 0 for loop-back interface
                        byte[] buf = address.getAddress();
                        if (buf[0] == 127) network_prefix_len = 8;
                    }
                    if (broadcast == null) {
                        // Java 1.7.0 on Linux returns broadcast == null for loop-back interface
                        broadcast = address;
                    }
                }

                if (network_prefix_len > 0 && address != null && broadcast != null) {
                    set.add(new SubNet(network_prefix_len, address, broadcast));
                }
            }
        }
    }

    /**
     * Finds and adds Subnetworks to the
     * @param set
     * @throws Exception
     */
    private void getWindowsSubNetList(HashSet<SubNet> set) throws Exception {
        HashMap<String,InetAddress> map = new HashMap<String,InetAddress>();
        for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
            NetworkInterface f = e.nextElement();
            Enumeration<InetAddress> n = f.getInetAddresses();
            while (n.hasMoreElements()) {
                InetAddress addr = n.nextElement();
                if (addr instanceof Inet4Address) {
                    String s = addr.getHostAddress();
                    if (s.startsWith("127.")) {
                        byte[] buf = addr.getAddress();
                        buf[1] = buf[2] = buf[3] = (byte)255;
                        set.add(new SubNet(8, addr, InetAddress.getByAddress(buf)));
                    }
                    else {
                        map.put(s, addr);
                    }
                }
            }
        }
        Process prs = Runtime.getRuntime().exec(new String[]{ "ipconfig", "/all" }, null);
        BufferedReader inp = new BufferedReader(new InputStreamReader(prs.getInputStream()));
        for (;;) {
            String s = inp.readLine();
            if (s == null) break;
            int n = s.indexOf(" : ");
            if (n < 0) continue;
            n += 3;
            int m = n;
            while (m < s.length()) {
                char ch = s.charAt(m);
                if ((ch < '0' || ch > '9') && ch != '.') break;
                m++;
            }
            if (m == n) continue;
            InetAddress addr = map.get(s.substring(n, m));
            if (addr == null) continue;
            do s = inp.readLine();
            while (s != null && s.length() == 0);
            if (s == null) break;
            n = s.indexOf(" : ");
            if (n < 0) continue;
            s = s.substring(n + 3);
            int l = s.length();
            int i_cnt = 0;
            int d_cnt = 0;
            for (int i = 0; i < l; i++) {
                char ch = s.charAt(i);
                if (ch == '.') d_cnt++;
                else if (ch < '0' || ch > '9') i_cnt++;
            }
            if (d_cnt != 3 || i_cnt != 0) continue;
            try {
                byte[] buf = InetAddress.getByName(s).getAddress();
                int prefix_length = 0;
                for (int i = 0; i < 32; i++) {
                    if ((buf[i / 8] & (1 << (7 - i % 8))) == 0) {
                        prefix_length = i;
                        break;
                    }
                }
                if (prefix_length > 0) {
                    buf = addr.getAddress();
                    for (int i = prefix_length; i < 32; i++) {
                        buf[i / 8] |=  1 << (7 - i % 8);
                    }
                    set.add(new SubNet(prefix_length, addr, InetAddress.getByAddress(buf)));
                }
            }
            catch (Exception x) {
            }
        }
        try {
            prs.getErrorStream().close();
            prs.getOutputStream().close();
            inp.close();
        }
        catch (IOException x) {
        }
        prs.waitFor();
    }

    private byte[] getUTF8Bytes(String s) {
        try {
            return s.getBytes("UTF-8");
        }
        catch (Exception x) {
            log("UTF-8 character encoder is not available", x);
            return s.getBytes();
        }
    }

    /** Used for tracing */
    private static String packetTypes[] = new String[] {
            null,
            "CONF_REQ_INFO",
            "CONF_PEER_INFO",
            "CONF_REQ_SLAVES",
            "CONF_SLAVES_INFO",
            "CONF_PEER_REMOVE"
    };

    /**
     * Sends a Datagram packet of the given length to the Peer in the given subnet, with the given address and port
     * @param subnet the subnet on which the the peer address is located
     * @param size the datagram packet size
     * @param addr the peer's IP address
     * @param port the destination port number
     * @return true if the datagram was sent, false is the datagram was not sent
     */
    private boolean sendDatagramPacket(SubNet subnet, int size, InetAddress addr, int port) {
        try {
            if (addr == null) {
                addr = subnet.address.equals(loopback_addr) ? loopback_addr : subnet.broadcast;
                port = DISCOVERY_PORT;
                for (Slave slave : slaves) {
                    sendDatagramPacket(subnet, size, slave.address, slave.port);
                }
            }
            if (!subnet.contains(addr)) return false;
            if (port == socket.getLocalPort() && addr.equals(subnet.address)) return false;
            socket.send(new DatagramPacket(out_buf, size, addr, port));

            if (TRACE_DISCOVERY) {
                Map<String,String> map = null;
                switch (out_buf[4]) {
                case CONF_PEER_INFO: map = parsePeerAtrributes(out_buf, size); break;
                case CONF_SLAVES_INFO: map = parseIDs(out_buf, size); break;
                case CONF_PEERS_REMOVED: map = parseIDs(out_buf, size); break;
                }
                traceDiscoveryPacket(false, packetTypes[out_buf[4]], map, addr, port);
            }
        }
        catch (Exception x) {
            log("Cannot send datagram packet to " + addr, x);
            return false;
        }
        return true;
    }

    /**
     * Parse peer attributes in CONF_PEER_INFO packet data.
     *
     * @param data - the packet data
     * @param size - the packet size
     * @return a map containing the attributes
     * @throws UnsupportedEncodingException
     */
    private static Map<String,String> parsePeerAtrributes(byte[] data, int size) throws UnsupportedEncodingException {
        Map<String,String> map = new HashMap<String,String>();
        String s = new String(data, 8, size - 8, "UTF-8");
        int l = s.length();
        int i = 0;
        while (i < l) {
            int i0 = i;
            while (i < l && s.charAt(i) != '=' && s.charAt(i) != 0) i++;
            int i1 = i;
            if (i < l && s.charAt(i) == '=') i++;
            int i2 = i;
            while (i < l && s.charAt(i) != 0) i++;
            int i3 = i;
            if (i < l && s.charAt(i) == 0) i++;
            String key = s.substring(i0, i1);
            String val = s.substring(i2, i3);
            map.put(key, val);
        }
        return map;
    }

    /**
     * Parse list of IDs in CONF_SLAVES_INFO and CONF_PEERS_REMOVED packet data.
     *
     * @param data - the packet data
     * @param size - the packet size
     * @return a map containing the IDs
     * @throws UnsupportedEncodingException
     */
    private static Map<String,String> parseIDs(byte[] data, int size) throws UnsupportedEncodingException {
        int cnt = 0;
        Map<String,String> map = new HashMap<String,String>();
        String s = new String(data, 8, size - 8, "UTF-8");
        int l = s.length();
        int i = 0;
        while (i < l) {
            int i0 = i;
            while (i < l && s.charAt(i) != 0) i++;
            if (i > i0) {
                String id = s.substring(i0, i);
                map.put(Integer.toString(cnt++), id);
            }
            while (i < l && s.charAt(i) == 0) i++;
        }
        return map;
    }

    /**
     * Sends a Peer Request to the given address and port.
     * If address is null, broadcast on all subnets.
     * @param addr - IP address to send the request, or null
     * @param port - IP port number to send the request
     */
    private void sendPeersRequest(InetAddress addr, int port) {
        out_buf[4] = CONF_REQ_INFO;
        for (SubNet subnet : subnets) {
            sendDatagramPacket(subnet, 8, addr, port);
        }
    }

    /**
     * Send peer info to the given address and port.
     * If address is null, broadcast on all subnets.
     * @param peer - a peer info to send
     * @param addr - IP address to send the info, or null
     * @param port - IP port number to send the info
     */
    private void sendPeerInfo(IPeer peer, InetAddress addr, int port) {
        if (peer instanceof UserDefPeer) return;
        Map<String,String> attrs = peer.getAttributes();
        InetAddress peer_addr = getInetAddress(attrs.get(IPeer.ATTR_IP_HOST));
        if (peer_addr == null) return;
        if (attrs.get(IPeer.ATTR_IP_PORT) == null) return;
        out_buf[4] = CONF_PEER_INFO;
        int i = 8;

        for (SubNet subnet : subnets) {
            if (peer instanceof RemotePeer) {
                if (socket.getLocalPort() != DISCOVERY_PORT) return;
                if (!subnet.address.equals(loopback_addr) && !subnet.address.equals(peer_addr)) continue;
            }
            if (!subnet.address.equals(loopback_addr)) {
                if (!subnet.contains(peer_addr)) continue;
            }
            if (i == 8) {
                StringBuffer sb = new StringBuffer(out_buf.length);
                for (String key : attrs.keySet()) {
                    sb.append(key);
                    sb.append('=');
                    sb.append(attrs.get(key));
                    sb.append((char)0);
                }
                byte[] bt = getUTF8Bytes(sb.toString());
                if (i + bt.length > out_buf.length) return;
                System.arraycopy(bt, 0, out_buf, i, bt.length);
                i += bt.length;
            }
            if (sendDatagramPacket(subnet, i, addr, port)) subnet.send_all_ok = true;
        }
    }

    /**
     * Sends a CONF_SLAVES_INFO empty packet to the given address/port
     * @param addr IP address of the peer
     * @param port port number of the peer
     */
    private void sendEmptyPacket(InetAddress addr, int port) {
        out_buf[4] = CONF_SLAVES_INFO;
        for (SubNet subnet : subnets) {
            if (subnet.send_all_ok) continue;
            sendDatagramPacket(subnet, 8, addr, port);
        }
    }

    /**
     * Sends a packet to
     * @param addr IP address of the slave/peer
     * @param port port number of the slave/peer
     * @param sl slave to which the packet is going to be sent to
     * @param time
     */
    private void sendAll(InetAddress addr, int port, Slave sl, long time) {
        for (SubNet subnet : subnets) subnet.send_all_ok = false;
        for (IPeer peer : peers.values()) sendPeerInfo(peer, addr, port);
        if (addr != null && sl != null && sl.last_req_slaves_time + DATA_RETENTION_PERIOD >= time) {
            sendSlavesInfo(addr, port, time);
        }
        sendEmptyPacket(addr, port);
    }

    /**
     * Sends a CONF_REQ_SLAVES packet to the given address and port number in the given subnet
     * @param subnet subnet in which the address belongs to
     * @param addr IP address of the slave
     * @param port port number of the slave
     */
    private void sendSlavesRequest(SubNet subnet, InetAddress addr, int port) {
        out_buf[4] = CONF_REQ_SLAVES;
        sendDatagramPacket(subnet, 8, addr, port);
    }

    /**
     * Sends a CONF_SLAVES_INFO packet to the given slave with the given timestamp
     * @param x slave
     * @param time
     */
    private void sendSlaveInfo(Slave x, long time) {
        int ttl = (int)(x.last_packet_time + DATA_RETENTION_PERIOD - time);
        if (ttl <= 0) return;
        out_buf[4] = CONF_SLAVES_INFO;
        for (SubNet subnet : subnets) {
            if (!subnet.contains(x.address)) continue;
            int i = 8;
            String s = ttl + ":" + x.port + ":" + x.address.getHostAddress();
            byte[] bt = getUTF8Bytes(s);
            System.arraycopy(bt, 0, out_buf, i, bt.length);
            i += bt.length;
            out_buf[i++] = 0;
            for (Slave y : slaves) {
                if (!subnet.contains(y.address)) continue;
                if (y.last_req_slaves_time + DATA_RETENTION_PERIOD < time) continue;
                sendDatagramPacket(subnet, i, y.address, y.port);
            }
        }
    }

    /**
     * Sends a CONF_SLAVES_INFO packet to the given address, port number with the given timestamp
     * @param addr IP Address of slave
     * @param port Port number of slave
     * @param time timestamp of
     */
    private void sendSlavesInfo(InetAddress addr, int port, long time) {
        out_buf[4] = CONF_SLAVES_INFO;
        for (SubNet subnet : subnets) {
            if (!subnet.contains(addr)) continue;
            int i = 8;
            for (Slave x : slaves) {
                int ttl = (int)(x.last_packet_time + DATA_RETENTION_PERIOD - time);
                if (ttl <= 0) continue;
                if (x.port == port && x.address.equals(addr)) continue;
                if (!subnet.address.equals(loopback_addr)) {
                    if (!subnet.contains(x.address)) continue;
                }
                subnet.send_all_ok = true;
                String s = ttl + ":" + x.port + ":" + x.address.getHostAddress();
                byte[] bt = getUTF8Bytes(s);
                if (i > 8 && i + bt.length >= PREF_PACKET_SIZE) {
                    sendDatagramPacket(subnet, i, addr, port);
                    i = 8;
                }
                System.arraycopy(bt, 0, out_buf, i, bt.length);
                i += bt.length;
                out_buf[i++] = 0;
            }
            if (i > 8) sendDatagramPacket(subnet, i, addr, port);
        }
    }

    /**
     * Checks if the the address and port are from a RemotePeer
     * @param address IP address of peer
     * @param port port number of peer
     * @return true is the address and port are not from a Remote Peer but instead is the Local Peer, else false
     */
    private boolean isRemote(InetAddress address, int port) {
        if (port != socket.getLocalPort()) return true;
        for (SubNet s : subnets) {
            if (s.address.equals(address)) return false;
        }
        return true;
    }

    /**
     * Handles packets received during the auto discovery.
     * It verifies the packet is indeed a TCF packet, and handles it depending on the auto-configuration command
     * and responses codes e.g: CONF_PEER_INFO,
     * CONF_REQ_INFO, etc..
     *
     * @param p packet received
     */
    private void handleDatagramPacket(InputPacket p) {
        try {
            long time = System.currentTimeMillis();
            byte[] buf = p.getData();
            int len = p.getLength();
            if (len < 8) return;
            if (buf[0] != 'T') return;
            if (buf[1] != 'C') return;
            if (buf[2] != 'F') return;
            if (buf[3] != CONF_VERSION) return;
            int remote_port = p.getPort();
            InetAddress remote_address = p.getAddress();
            if (isRemote(remote_address, remote_port)) {
                if (buf[4] == CONF_PEERS_REMOVED) {
                    handlePeerRemovedPacket(p, remote_port == DISCOVERY_PORT && remote_address.isLoopbackAddress());
                }
                else {
                    Slave sl = null;
                    if (remote_port != DISCOVERY_PORT) {
                        sl = addSlave(remote_address, remote_port, time);
                    }
                    switch (buf[4]) {
                    case CONF_PEER_INFO:
                        handlePeerInfoPacket(p);
                        break;
                    case CONF_REQ_INFO:
                        handleReqInfoPacket(p, sl, time);
                        break;
                    case CONF_SLAVES_INFO:
                        handleSlavesInfoPacket(p, time);
                        break;
                    case CONF_REQ_SLAVES:
                        handleReqSlavesPacket(p, sl, time);
                        break;
                    }
                    for (SubNet subnet : subnets) {
                        if (!subnet.contains(remote_address)) continue;
                        long delay = DATA_RETENTION_PERIOD / 3;
                        if (remote_port != DISCOVERY_PORT) delay = DATA_RETENTION_PERIOD / 3 * 2;
                        else if (!subnet.address.equals(remote_address)) delay = DATA_RETENTION_PERIOD / 2;
                        if (subnet.last_slaves_req_time + delay <= time) {
                            sendSlavesRequest(subnet, remote_address, remote_port);
                            subnet.last_slaves_req_time = time;
                        }
                        if (remote_port == DISCOVERY_PORT && subnet.address.equals(remote_address)) {
                            last_master_packet_time = time;
                        }
                    }
                }
            }
        }
        catch (Throwable x) {
            log("Invalid datagram packet received from " + p.getAddress() + "/" + p.getPort(), x);
        }
    }

    /**
     * Handles a received CONF_PEER_INFO packet
     * @param p packet received
     */
    private void handlePeerInfoPacket(InputPacket p) {
        try {
            Map<String,String> map = parsePeerAtrributes(p.getData(), p.getLength());
            if (TRACE_DISCOVERY) traceDiscoveryPacket(true, "CONF_PEER_INFO", map, p);
            String id = map.get(IPeer.ATTR_ID);
            if (id == null) throw new Exception("Invalid peer info: no ID");
            boolean ok = true;
            String host = map.get(IPeer.ATTR_IP_HOST);
            if (host != null) {
                ok = false;
                InetAddress peer_addr = getInetAddress(host);
                if (peer_addr != null) {
                    for (SubNet subnet : subnets) {
                        if (subnet.contains(peer_addr)) {
                            ok = true;
                            break;
                        }
                    }
                }
            }
            if (ok) {
                IPeer peer = peers.get(id);
                if (peer instanceof RemotePeer) {
                    ((RemotePeer)peer).updateAttributes(map);
                }
                else if (peer == null) {
                    new RemotePeer(map);
                }
            }
        }
        catch (Exception x) {
            log("Invalid datagram packet received from " + p.getAddress() + "/" + p.getPort(), x);
        }
    }

    /**
     * Handles a CONF_REQ_INFO packet received from the given slave, at the given time
     * It sendsAll
     * @param p packet received
     * @param sl slave
     * @param time timestamp at which the packet was received
     */
    private void handleReqInfoPacket(InputPacket p, Slave sl, long time) {
        if (TRACE_DISCOVERY) traceDiscoveryPacket(true, "CONF_REQ_INFO", null, p);
        sendAll(p.getAddress(), p.getPort(), sl, time);
    }

    /**
     * Handles a received CONF_SLAVES_INFO
     * @param p packet received
     * @param time_now timestamp when the packet was received
     */
    private void handleSlavesInfoPacket(InputPacket p, long time_now) {
        try {
            Map<String,String> map = parseIDs(p.getData(), p.getLength());
            if (TRACE_DISCOVERY) traceDiscoveryPacket(true, "CONF_SLAVES_INFO", map, p);
            for (String s : map.values()) {
                int i = 0;
                int l = s.length();
                int time0 = i;
                while (i < l&& s.charAt(i) != ':' && s.charAt(i) != 0) i++;
                int time1 = i;
                if (i < l && s.charAt(i) == ':') i++;
                int port0 = i;
                while (i < l&& s.charAt(i) != ':' && s.charAt(i) != 0) i++;
                int port1 = i;
                if (i < l && s.charAt(i) == ':') i++;
                int host0 = i;
                while (i < l && s.charAt(i) != 0) i++;
                int host1 = i;
                int port = Integer.parseInt(s.substring(port0, port1));
                String timestamp = s.substring(time0, time1);
                String host = s.substring(host0, host1);
                if (port != DISCOVERY_PORT) {
                    InetAddress addr = getInetAddress(host);
                    if (addr != null) {
                        long delta = 1000 * 60 * 30; // 30 minutes
                        long time_val = timestamp.length() > 0 ? Long.parseLong(timestamp) : time_now;
                        if (time_val < 3600000) {
                            /* Time stamp is "time to live" in milliseconds */
                            time_val = time_now + time_val - DATA_RETENTION_PERIOD;
                        }
                        else if (time_val < time_now / 1000 + 50000000) {
                            /* Time stamp is in seconds */
                            time_val *= 1000;
                        }
                        else {
                            /* Time stamp is in milliseconds */
                        }
                        if (time_val < time_now - delta || time_val > time_now + delta) {
                            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            String msg =
                                "Invalid slave info timestamp: " + timestamp +
                                " -> " + fmt.format(new Date(time_val));
                            log("Invalid datagram packet received from " +
                                    p.getAddress() + "/" + p.getPort(),
                                    new Exception(msg));
                            time_val = time_now - DATA_RETENTION_PERIOD / 2;
                        }
                        addSlave(addr, port, time_val);
                    }
                }
            }
        }
        catch (Exception x) {
            log("Invalid datagram packet received from " + p.getAddress() + "/" + p.getPort(), x);
        }
    }

    /**
     * Handles CONF_REQ_SLAVES packet
     * SendsSlavesInfo
     * @param p packet received
     * @param sl slave from which the packet was received
     * @param time timestamp of when the packet was received
     */
    private void handleReqSlavesPacket(InputPacket p, Slave sl, long time) {
        if (TRACE_DISCOVERY) traceDiscoveryPacket(true, "CONF_REQ_SLAVES", null, p);
        if (sl != null) sl.last_req_slaves_time = time;
        sendSlavesInfo(p.getAddress(), p.getPort(),  time);
    }

    /**
     * Handles a received CONF_PEER_REMOVED packet
     * @param p packet received
     * @param master_exited true if the master locator was removed, false if it was a slave locator
     */
    private void handlePeerRemovedPacket(InputPacket p, boolean master_exited) {
        try {
            Map<String,String> map = parseIDs(p.getData(), p.getLength());
            if (TRACE_DISCOVERY) traceDiscoveryPacket(true, "CONF_PEERS_REMOVED", map, p);
            for (String id : map.values()) {
                IPeer peer = peers.get(id);
                if (peer instanceof RemotePeer) ((RemotePeer)peer).dispose();
            }
            if (master_exited) {
                // Master locator has exited, let's try to get master port.
                Protocol.invokeLater(500, new Runnable() {
                    public void run() {
                        if (socket.getLocalPort() == DISCOVERY_PORT) return;
                        try {
                            DatagramSocket s0 = socket;
                            socket = createSocket(false);
                            if (TRACE_DISCOVERY) {
                                LoggingUtil.trace("Became the master agent (bound to port " + socket.getLocalPort() + ")");
                            }
                            s0.close();
                        }
                        catch (Throwable x) {
                        }
                    }
                });
            }
        }
        catch (Exception x) {
            log("Invalid datagram packet received from " + p.getAddress() + "/" + p.getPort(), x);
        }
    }

    /*----------------------------------------------------------------------------------*/

    public static LocatorService getLocator() {
        return locator;
    }

    public String getName() {
        return NAME;
    }

    public Map<String,IPeer> getPeers() {
        assert Protocol.isDispatchThread();
        return peers;
    }

    public IToken redirect(String peer_id, DoneRedirect done) {
        throw new Error("Channel redirect cannot be done on local peer");
    }

    public IToken redirect(Map<String,String> peer, DoneRedirect done) {
        throw new Error("Channel redirect cannot be done on local peer");
    }

    public IToken sync(DoneSync done) {
        throw new Error("Channel sync cannot be done on local peer");
    }

    public IToken getAgentID(DoneGetAgentID done) {
        throw new Error("Channel get agent ID cannot be done on local peer");
    }

    public void addListener(LocatorListener listener) {
        assert listener != null;
        assert Protocol.isDispatchThread();
        listeners.add(listener);
    }

    public void removeListener(LocatorListener listener) {
        assert Protocol.isDispatchThread();
        listeners.remove(listener);
    }

    /**
     * Log that a TCF Discovery packet has be sent or received. The trace is
     * sent to stdout. This should be called only if the tracing has been turned
     * on via java property definitions.
     *
     * @param received false if the packet was sent, otherwise it was received
     * @param type a string specifying the type of packet, e.g., "CONF_PEER_INFO"
     * @param attrs a set of attributes relevant to the type of packet (typically a peer's attributes)
     * @param addr the network address the packet is being sent to
     * @param port the port the packet is being sent to
     */
    private static void traceDiscoveryPacket(boolean received, String type, Map<String,String> attrs, InetAddress addr, int port) {
        assert TRACE_DISCOVERY;
        StringBuilder str = new StringBuilder(type + (received ? " received from " : " sent to ") +  addr + "/" + port);
        if (attrs != null) {
            Iterator<Entry<String, String>> iter = attrs.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = iter.next();
                str.append("\n\t" + entry.getKey() + '=' + entry.getValue());
            }
        }
        LoggingUtil.trace(str.toString());
    }

    /**
     * Convenience variant that takes a DatagramPacket for specifying
     * the target address and port.
     *
     * @param received false if the packet was sent, otherwise it was received
     * @param type a string specifying the type of packet, e.g., "CONF_PEER_INFO"
     * @param attrs a set of attributes relevant to the type of packet (typically a peer's attributes)
     * @param packet packet received
     */
    private static void traceDiscoveryPacket(boolean received, String type, Map<String,String> attrs, InputPacket packet) {
        traceDiscoveryPacket(received, type, attrs, packet.getAddress(), packet.getPort());
    }
}
