package com.buddycloud.jbuddycloud;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.Subscription;

import com.buddycloud.jbuddycloud.packet.Affiliation;
import com.buddycloud.jbuddycloud.packet.Affiliations;
import com.buddycloud.jbuddycloud.packet.BCAtom;
import com.buddycloud.jbuddycloud.packet.BCSubscription;
import com.buddycloud.jbuddycloud.packet.GeoLoc;
import com.buddycloud.jbuddycloud.provider.BCLeafNode;
import com.buddycloud.jbuddycloud.provider.BCPubSubManager;
import com.buddycloud.jbuddycloud.provider.BCSubscriptionProvider;
import com.buddycloud.jbuddycloud.provider.LocationQueryResponseProvider;

public class BuddycloudClient extends XMPPConnection {

    public static final String VERSION = "0.0.1";

    public static final Pattern JID_PATTERN =
        Pattern.compile("..*@[^.].*\\.[^.][^.][^.]*");

    static {
        ProviderManager pm = ProviderManager.getInstance();
        pm.addIQProvider(
            "query", "http://jabber.org/protocol/disco#items",
             new org.jivesoftware.smackx.provider.DiscoverItemsProvider()
        );
        pm.addIQProvider("query",
                "http://jabber.org/protocol/disco#info",
                new org.jivesoftware.smackx.provider.DiscoverInfoProvider());
        pm.addIQProvider("pubsub",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
        pm.addExtensionProvider(
                        "create",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        pm.addExtensionProvider("items",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
        pm.addExtensionProvider("item",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        pm.addExtensionProvider("item", "",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        pm.addExtensionProvider(
                        "subscriptions",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());
        pm.addExtensionProvider(
                        "subscription",
                        "http://jabber.org/protocol/pubsub",
                        new BCSubscriptionProvider());
        pm.addExtensionProvider(
                        "subscriptions",
                        "http://jabber.org/protocol/pubsub#owner",
                        new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());
        pm.addExtensionProvider(
                        "subscription",
                        "http://jabber.org/protocol/pubsub#owner",
                        new BCSubscriptionProvider());
        pm.addExtensionProvider(
                        "affiliations",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider());
        pm.addExtensionProvider(
                        "affiliation",
                        "http://jabber.org/protocol/pubsub",
                        new org.jivesoftware.smackx.pubsub.provider.AffiliationProvider());
        pm.addExtensionProvider("options",
                "http://jabber.org/protocol/pubsub",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addIQProvider("pubsub",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
        pm.addExtensionProvider("configure",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addExtensionProvider("default",
                "http://jabber.org/protocol/pubsub#owner",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());

        pm.addIQProvider("event",
                "http://jabber.org/protocol/pubsub#event",
                new com.buddycloud.jbuddycloud.provider.EventProvider()
                );

        pm.addExtensionProvider("event",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.EventProvider());
        pm.addExtensionProvider(
                        "configuration",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider());
        pm.addExtensionProvider(
                        "delete",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        pm.addExtensionProvider("options",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
        pm.addExtensionProvider("items",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
        pm.addExtensionProvider("item",
                "http://jabber.org/protocol/pubsub#event",
                new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
        pm.addExtensionProvider(
                        "retract",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.RetractEventProvider());
        pm.addExtensionProvider(
                        "purge",
                        "http://jabber.org/protocol/pubsub#event",
                        new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
        pm.addExtensionProvider(
                "x",
                "jabber:x:data",
                new org.jivesoftware.smackx.provider.DataFormProvider());
        pm.addExtensionProvider(
                "entry",
                "http://www.w3.org/2005/Atom",
                new BCAtom());
        /*
        pm.addExtensionProvider("event",
                PubSubLocationEventProvider.getNS(),
                new PubSubLocationEventProvider()
        );
        */
        pm.addExtensionProvider(
                "geoloc",
                "http://jabber.org/protocol/geoloc",
                new GeoLoc()
        );
        pm.addIQProvider("location",
                LocationQueryResponseProvider.getNS(),
                new LocationQueryResponseProvider());
        SmackConfiguration.setKeepAliveInterval(-1);
    }

    public static BuddycloudClient createAnonymousBuddycloudClient() {
        ConnectionConfiguration config =
            new ConnectionConfiguration("buddycloud.com");
        config.setReconnectionAllowed(false);

        BuddycloudClient connection = null;

        try {
            connection = new BuddycloudClient(config);
            connection.connect();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        if (connection == null || !connection.isConnected()) {
            return null;
        }

        try {
            connection.loginAnonymously();
        } catch (XMPPException e) {
            e.printStackTrace(System.err);
        }

        if (!(connection.isAuthenticated() || connection.isAnonymous())) {
            return null;
        }

        configureFeatures(connection);

        return connection;
    }

    public static BuddycloudClient registerBuddycloudClient(
            String username,
            String password
    ) {
        ConnectionConfiguration config =
            new ConnectionConfiguration("buddycloud.com");
        config.setReconnectionAllowed(false);

        BuddycloudClient connection = null;

        try {
            connection = new BuddycloudClient(config);
            connection.connect();
        } catch (Exception e) {
            // TODO logging
        }

        if (connection == null || !connection.isConnected()) {
            return null;
        }

        AccountManager accountManager = connection.getAccountManager();

        try {
            accountManager.createAccount(username, password);
            connection.login(username, password);
        } catch (XMPPException e) {
            // TODO logging
        }

        if (!connection.isConnected() && !connection.isAuthenticated()) {
            return null;
        }

        configureFeatures(connection);

        return connection;
    }

    public static BuddycloudClient createBuddycloudClient(
            String jid,
            String password,
            String host,
            Integer port,
            String cachedUsername
    ) {

        if (jid == null) {
            return null;
        }

        if (jid.length() == 0 || !JID_PATTERN.matcher(jid).matches()) {
            return null;
        }

        ConnectionConfiguration config =
            new ConnectionConfiguration("buddycloud.com");
        if (host != null && port != null) {
            config = new ConnectionConfiguration(host, port);
        } else
        if (host != null) {
            config = new ConnectionConfiguration(host);
        } else
        if (jid.indexOf('@') != -1) {
            config = new ConnectionConfiguration(
                jid.substring(jid.lastIndexOf('@') + 1)
            );
        }
        config.setReconnectionAllowed(false);

        BuddycloudClient connection = null;

        try {
            connection = new BuddycloudClient(config);
            connection.connect();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        if (connection == null || !connection.isConnected()) {
            return null;
        }

        if (cachedUsername != null) {
            try {
                connection.login(cachedUsername, password);
            } catch (XMPPException e) {
                e.printStackTrace(System.err);
            }

        } else {

            String id = jid + '@';
            while (id.indexOf('@') != -1) {
                id = id.substring(0, id.lastIndexOf('@'));
                if (id.equals(cachedUsername)) {
                    continue;
                }

                try {
                    connection.login(id, password);
                } catch (XMPPException e) {
                    e.printStackTrace(System.err);
                }

                if (connection.isAuthenticated()) {
                    connection.setLoginUsername(id);
                    return connection;
                }

                try {
                    connection.disconnect();
                } catch (Exception e) {
                    // Unimportant
                }
                try {
                    connection = new BuddycloudClient(config);
                    connection.connect();
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }

                if (!connection.isConnected()) {
                    return null;
                }
            }

        }

        if (!(connection.isConnected() && connection.isAuthenticated())) {
            return null;
        }

        configureFeatures(connection);

        return connection;

    }

    private final static void configureFeatures(BuddycloudClient connection) {
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/disco#info");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/pubsub");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/geoloc");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/geoloc+notify");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/geoloc-prev");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/geoloc-prev+notify");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/geoloc-next");
        connection.discoveryManager
            .addFeature("http://jabber.org/protocol/geoloc-next+notify");
        connection.discoveryManager
            .setNodeInformationProvider(
            "http://buddydroid.com/caps#" + VERSION,
            new JBuddycloudFeatures()
        );

        connection.sendPacket(new InitialPresence());
    }

    private ServiceDiscoveryManager discoveryManager;
    private BCPubSubManager pubSubManager;
    private BuddycloudLocationChannelListener locationChannelListener =
        new BuddycloudLocationChannelListener();
    private String loginUsername;

    private BCLeafNode personalNode;

    public BCLeafNode getPersonalNode() {
        return personalNode;
    }

    public BCPubSubManager getPubSubManager() {
        return pubSubManager;
    }

    public BuddycloudClient(ConnectionConfiguration config) {
        super(config);
    }

    public boolean testConnection() {
        if (socket == null) {
            return false;
        }
        if (!isConnected() || !socket.isConnected() || socket.isClosed()) {
            return false;
        }
        if (socket.isInputShutdown() || socket.isOutputShutdown()) {
            return false;
        }
        try {
            if (socket.getSendBufferSize() != 1024) {
                socket.setReceiveBufferSize(1024);
                socket.setSendBufferSize(1024);
                socket.setSoTimeout(100000);
                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);
            }
        } catch (SocketException e) {
            e.printStackTrace(System.err);
        }
        try {
            socket.getInputStream().available();
            writer.append(' ');
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            disconnect();
            return false;
        }
        return true;
    }

    @Override
    public void connect() throws XMPPException {
        super.connect();
        int i = 0;
        while (!isConnected() && i < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i += 1;
        }
        if (!isConnected()) {
            return;
        }
        addPacketListener(locationChannelListener, null);
        discoveryManager = new ServiceDiscoveryManager(this);
        pubSubManager = new BCPubSubManager(this, "pubsub-bridge@broadcaster.buddycloud.com");
    }

    private static class InitialPresence extends Packet {

        @Override
        public void setError(XMPPError error) {
            super.setError(error);
        }

        @Override
        public String toXML() {
            return "<presence><priority>10</priority><status>available</status>"
                    + "<c xmlns=\"http://jabber.org/protocol/caps\" node=\"http://buddydroid.com/caps\" "
                    + "ver=\"" + VERSION + "\"/></presence>";
        }

    }

    @Override
    public synchronized void login(String username, String password)
            throws XMPPException {
        super.login(username, password, "buddydroid");
        String myJid = getUser();
        if (myJid.lastIndexOf('/') != -1) {
            myJid = myJid.substring(0, myJid.lastIndexOf('/'));
        }
        locationChannelListener.setUser(getUser());

        getRoster().setSubscriptionMode(SubscriptionMode.manual);

        for (String jid: new String[]{
                "broadcaster.buddycloud.com",
                "pubsub-bridge@broadcaster.buddycloud.com"
        }) {
            if (!getRoster().contains(jid)) {
                try {
                    getRoster().createEntry(jid, jid, null);
                } catch (Throwable t) {
                    t.printStackTrace(System.err);
                }
            }
            Presence presence = new Presence(Presence.Type.subscribe);
            presence.setTo(jid);
            sendPacket(presence);
        }

        pubSubManager.getSupportedFeatures();
        pubSubManager.getSubscriptions();
        final BuddycloudClient connection = this;
        final String mJid = myJid;
        try {
            personalNode = (BCLeafNode) pubSubManager.getNode("/user/" + myJid + "/channel");
            new Thread() {
                public void run() {
                    Affiliations affs = new Affiliations(
                        "/user/" + mJid + "/channel"
                    );
                    affs.setTo("pubsub-bridge@broadcaster.buddycloud.com");
                    try {
                        List<Subscription> subscriptions =
                            getPersonalNode().getBCSubscriptions();
                        HashSet<String> rosterJids = new HashSet<String>();
                        Iterator<RosterEntry> iter =
                            connection.getRoster().getEntries().iterator();
                        while (iter.hasNext()) {
                            rosterJids.add(iter.next().getUser());
                        }
                        for (Subscription subscription : subscriptions) {
                            if (subscription instanceof BCSubscription) {
                                String jid = subscription.getJid();
                                if (rosterJids.contains(jid)) {
                                    rosterJids.remove(jid);
                                } else if (!mJid.equals(jid)) {
                                    affs.add(new Affiliation(jid, "none"));
                                }
                            }
                        }
                        for (String jid: rosterJids) {
                            if (jid.contains("@")) {
                                affs.add(new Affiliation(jid, "publisher"));
                            }
                        }
                        sendPacket(affs);
                    } catch (XMPPException e) {
                        e.printStackTrace(System.err);
                    }
                }
            }.start();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    private static class JBuddycloudFeatures
    implements NodeInformationProvider {

        public List<String> getNodeFeatures() {
            List<String> features = new ArrayList<String>();
            features.add("http://jabber.org/protocol/disco#info");
            features.add("http://jabber.org/protocol/pubsub");
            features.add("http://jabber.org/protocol/geoloc");
            features.add("http://jabber.org/protocol/geoloc+notify");
            features.add("http://jabber.org/protocol/geoloc-prev");
            features.add("http://jabber.org/protocol/geoloc-prev+notify");
            features.add("http://jabber.org/protocol/geoloc-next");
            features.add("http://jabber.org/protocol/geoloc-next+notify");
            return features;
        }

        public List<Identity> getNodeIdentities() {
            List<Identity> r = new ArrayList<Identity>();
            Identity id = new DiscoverInfo.Identity("client", "BuddycloudClient");
            id.setType("mobile");
            r.add(id);
            return r;
        }

        public List<Item> getNodeItems() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public void addGeoLocListener(BCGeoLocListener bcGeoLocListener) {
        locationChannelListener.addGeoLocListener(bcGeoLocListener);
    }

    public void addAtomListener(BCAtomListener bcAtomListener) {
        locationChannelListener.addAtomListener(bcAtomListener);
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public void setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
    }

}
