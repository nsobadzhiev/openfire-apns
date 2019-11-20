package com.feinfone.apns;

import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


public class ApnsPlugin implements Plugin, PacketInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApnsPlugin.class);

    private InterceptorManager interceptorManager;
    private ApnsDBHandler dbManager;
    private PushManager pushManager;

    public ApnsPlugin() {
        interceptorManager = InterceptorManager.getInstance();
        dbManager = new ApnsDBHandler();
    }

    public static String keystorePath() {
        // Here, it's assumed that /usr/local/openfire/ exists and
        // its permissions allow writing into the folder
        // This might not be true for all installations
        return "/usr/local/openfire/authKey.p8";
    }

    public void setTeamId(String teamId) {
        JiveGlobals.setProperty("plugin.apns.teamId", teamId);
    }

    public String getTeamId() {
        return JiveGlobals.getProperty("plugin.apns.teamId", "");
    }

    public void setKeyId(String keyId) {
        JiveGlobals.setProperty("plugin.apns.keyId", keyId);
    }

    public String getKeyId() {
        return JiveGlobals.getProperty("plugin.apns.keyId", "");
    }

    public void setTopic(String keyId) {
        JiveGlobals.setProperty("plugin.apns.topic", keyId);
    }

    public String getTopic() {
        return JiveGlobals.getProperty("plugin.apns.topic", "");
    }

    public void setBadge(String badge) {
        JiveGlobals.setProperty("plugin.apns.badge", badge);
    }

    public int getBadge() {
        return Integer.parseInt(JiveGlobals.getProperty("plugin.apns.badge", "1"));
    }

    public void setSound(String sound) {
        JiveGlobals.setProperty("plugin.apns.sound", sound);
    }

    public String getSound() {
        return JiveGlobals.getProperty("plugin.apns.sound", "default");
    }

    public void setProduction(String production) {
        JiveGlobals.setProperty("plugin.apns.production", production);
    }

    public boolean getProduction() {
        return Boolean.parseBoolean(JiveGlobals.getProperty("plugin.apns.production", "false"));
    }

    public void initializePlugin(PluginManager pManager, File pluginDirectory) {
        interceptorManager.addInterceptor(this);

        IQHandler myHandler = new ApnsIQHandler();
        IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
        iqRouter.addHandler(myHandler);
    }

    public void destroyPlugin() {
        interceptorManager.removeInterceptor(this);
    }

    public void interceptPacket(Packet packet, Session session, boolean read, boolean processed) throws PacketRejectedException {

        if (isValidTargetPacket(packet, read, processed)) {
            Packet original = packet;

            if(original instanceof Message) {
                Message receivedMessage = (Message) original;

                if (receivedMessage.getType() == Message.Type.chat) {
                    JID targetJID = receivedMessage.getTo();

                    String user = targetJID.getNode();
                    String body = receivedMessage.getBody();
                    String payloadString = user + ": " + body;

                    String deviceToken = dbManager.getDeviceToken(targetJID);
                    if (deviceToken == null) return;

                    sendPush(payloadString, getBadge(), getSound(), deviceToken);
                } else if (receivedMessage.getType() == Message.Type.groupchat) {
                    JID sourceJID = receivedMessage.getFrom();
                    JID targetJID = receivedMessage.getTo();

                    String user = sourceJID.getNode();
                    String body = receivedMessage.getBody();
                    String payloadString = user + ": " + body;
                    String roomName = targetJID.getNode();

                    List<String> deviceTokens = dbManager.getDeviceTokens(roomName);
                    for (String token : deviceTokens) {
                        sendPush(payloadString, getBadge(), getSound(), token);
                    }
                }
            }
        }
    }

    private PushManager getPushManager() {
        if (pushManager == null) {
            try {
                PushEnvironment env = getProduction() ? PushEnvironment.PRODUCTION : PushEnvironment.STAGE;
                pushManager = new PushManager(keystorePath(), getTeamId(), getKeyId(), getTopic(), env);
            } catch (IOException e) {
                log.error("Unable to create push manager");
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                log.error("The key is invalid");
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                log.error("No such algorithm");
                e.printStackTrace();
            }
        }
        return pushManager;
    }

    public void sendPush(String message, int badgeNumber, String soundName, String token) {
        PushManager manager = getPushManager();
        if (manager != null) {
            manager.sendPush(message, badgeNumber, soundName, token);
        } else {
            log.error("No configured push manager. Not sending push message");
        }
    }

    private boolean isValidTargetPacket(Packet packet, boolean read, boolean processed) {
        return  !processed && read && packet instanceof Message;
    }
}
