/**
 * 
 */
package com.buddycloud.android.buddydroid;

import java.util.ArrayList;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;

import android.content.ContentResolver;
import android.content.ContentValues;

import com.buddycloud.content.BuddyCloud.ChannelData;
import com.buddycloud.content.BuddyCloud.Roster;
import com.buddycloud.jbuddycloud.packet.BCAtom;
import com.buddycloud.jbuddycloud.packet.EventIQ;
import com.buddycloud.jbuddycloud.packet.GeoLoc;
import com.googlecode.asmack.connection.XmppTransportService;

public final class BCConnectionAtomListener
    implements PacketListener {

    private final ContentResolver resolver;
    private String[] accountJids;
    private XmppTransportService service;

    public BCConnectionAtomListener(
            ContentResolver resolver, XmppTransportService service
    ) {
        this.resolver = resolver;
        this.service = service;
        this.accountJids = service.getAllAccountJids(false);
    }

    public ContentValues receive(String node, BCAtom atom) {
        if (node.startsWith("/user/")) {
            node = node.substring(6);
            String jid = node.substring(0, node.indexOf('/'));
            node = node.substring(node.indexOf('/') + 1);
            if (node.startsWith("geo/")) {
                ContentValues values = new ContentValues();
                GeoLoc loc = atom.getGeoloc();
                if (node.equals("geo/future")) {
                    values.put(Roster.GEOLOC_NEXT, loc.getText());
                } else
                if (node.equals("geo/current")) {
                    values.put(Roster.GEOLOC, loc.getText());
                } else
                if (node.equals("geo/previous")) {
                    values.put(Roster.GEOLOC_PREV, loc.getText());
                }
                resolver.update(Roster.CONTENT_URI, values,
                        Roster.JID + "='" + jid + "'",
                        null);
                return null;
            }
            if (!node.equals("channel")) {
                // /user/jid/mood ?
                return null;
            }
            node = "/user/" + jid + "/" + node;
        }
        // Channel !
        ContentValues values = new ContentValues();
        values.put(ChannelData.NODE_NAME,
                   node);
        values.put(ChannelData.AUTHOR,
                   atom.getAuthorName());
        values.put(ChannelData.AUTHOR_JID,
                   atom.getAuthorJid());
        values.put(ChannelData.AUTHOR_AFFILIATION,
                   atom.getAffiliation());
        values.put(ChannelData.CONTENT,
                   atom.getContent());
        values.put(ChannelData.CONTENT_TYPE,
                   atom.getContentType());
        values.put(ChannelData.ITEM_ID,
                   atom.getId());
        values.put(ChannelData.LAST_UPDATED,
                   atom.getId());
        if (atom.getParent() == null) {
            values.put(ChannelData.PARENT, 0);
        } else {
            values.put(ChannelData.PARENT,
                    atom.getParent());
        }
        values.put(ChannelData.PUBLISHED,
                   atom.getPublished());
        GeoLoc loc = atom.getGeoloc();
        boolean unread = true;
        for (String me: accountJids) {
            if (me.equals(node)) {
                unread = false;
            }
        }
        if (unread) {
            values.put(ChannelData.UNREAD, unread);
        }
        if (loc != null) {
            values.put(ChannelData.GEOLOC_ACCURACY,
                       loc.getAccuracy());
            values.put(ChannelData.GEOLOC_AREA,
                       loc.getArea());
            values.put(ChannelData.GEOLOC_COUNTRY,
                       loc.getCountry());
            values.put(ChannelData.GEOLOC_LAT,
                       loc.getLat());
            values.put(ChannelData.GEOLOC_LOCALITY,
                       loc.getLocality());
            values.put(ChannelData.GEOLOC_LON,
                       loc.getLon());
            values.put(ChannelData.GEOLOC_REGION,
                       loc.getRegion());
            values.put(ChannelData.GEOLOC_TEXT,
                       loc.getText());
            if (loc.getLocType() != null) {
                values.put(ChannelData.GEOLOC_TYPE,
                           loc.getLocType().toString());
            }
        }

        return values;
    }

    @Override
    public void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        if (!(packet instanceof EventIQ)) {
            return;
        }
        EventIQ event = (EventIQ) packet;
        for (PacketExtension eventExtension : event.getExtensions()) {
            if (!(eventExtension instanceof ItemsExtension)) {
                continue;
            }
            ItemsExtension items = (ItemsExtension) eventExtension;
            String node = items.getNode();
            ArrayList<ContentValues> values = new ArrayList<ContentValues>();
            for (PacketExtension itemsExtension: items.getItems()) {
                if (!(itemsExtension instanceof PayloadItem<?>)) {
                    continue;
                }
                Object payload = ((PayloadItem<?>)itemsExtension).getPayload();
                if (payload instanceof BCAtom) {
                    ContentValues contentValues =
                                            receive(node, (BCAtom) payload);
                    if (contentValues != null) {
                        values.add(contentValues);
                    }
                }
            }
            if (values.size() > 0) {
                resolver.bulkInsert(ChannelData.CONTENT_URI, values.toArray(
                    new ContentValues[values.size()]
                ));
            }
        }
    }

}