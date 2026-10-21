package fr.nathanael2611.modularvoicechat.client.voice;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.FrameworkMessage;
import fr.nathanael2611.modularvoicechat.ModularVoiceChat;
import fr.nathanael2611.modularvoicechat.network.objects.HelloImAPlayer;
import fr.nathanael2611.modularvoicechat.network.objects.KryoObjects;
import fr.nathanael2611.modularvoicechat.util.Helpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The VoiceClient
 */
public class VoiceClient
{
    /* Delay between two UDP pings, in seconds */
    private static final int UDP_PING_INTERVAL = 5;
    /* Time without any answer to our UDP pings before reconnecting, in milliseconds */
    private static final long UDP_TIMEOUT = 20000;

    /* The player name */
    private String playerName;
    /* The secret proving to the VoiceServer that we are this player */
    private String token;
    /* The server port */
    private int port;
    /* The server host */
    private String host;
    /* The Client */
    private Client client;
    /* Is handshake done */
    private volatile boolean handshakeDone = false;
    /* Last time the server proved that it still receives our UDP packets */
    private volatile long lastUdpAnswer;
    /* Id of the next UDP ping */
    private int udpPingId = 0;

    /* Runs both tasks below on the same thread, so they never connect and close at the same time */
    private final ScheduledExecutorService RECONNECT_SERVICE = Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructor
     * @param playerName the player name
     * @param token the secret received from the server
     * @param port the server port
     */
    VoiceClient(String playerName, String token, String host, int port)
    {
        this.playerName = playerName;
        this.token = token;
        this.port = port;
        this.host = host;
        this.client = new Client(10000000, 10000000);
        client.start();
        KryoObjects.registerObjects(client.getKryo());
        client.addListener(new KryoNetClientListener(this));
        RECONNECT_SERVICE.scheduleAtFixedRate(() -> {
            // An exception escaping from here would silently cancel the task, and the client would never reconnect.
            try
            {
                this.connectIfNeeded();
            } catch (Throwable t)
            {
                Helpers.log("Error in the voice reconnection task: " + t);
                t.printStackTrace();
            }
        }, 5, 15, TimeUnit.SECONDS);
        RECONNECT_SERVICE.scheduleWithFixedDelay(() -> {
            try
            {
                this.checkUdp();
            } catch (Throwable t)
            {
                Helpers.log("Error in the voice UDP check: " + t);
                t.printStackTrace();
            }
        }, UDP_PING_INTERVAL, UDP_PING_INTERVAL, TimeUnit.SECONDS);


    }

    /**
     * Connect to the VoiceServer if not connected, or retry the authentication if it is not done yet
     */
    private void connectIfNeeded()
    {
        if(!this.client.isConnected() && host != null)
        {
            try
            {
                Helpers.log(String.format("Try to connect to the UDP server! [%s:%s]", host, this.port));
                this.handshakeDone = false;
                client.connect(5000, host, port, port);
                // KryoNet only returns once the server answered our UDP registration
                this.lastUdpAnswer = System.currentTimeMillis();
                this.authenticate(playerName);
            } catch (IOException e)
            {
                e.printStackTrace();
                sendChatMessage("§4[" + ModularVoiceChat.MOD_NAME + "] §c" + I18n.format("mvc.error.cantconnect"));
                Helpers.log("Failed to connect to VoiceServer.");
            }
        }
        else if(host == null)
        {
            Helpers.log("Host is null!");
        }
        else if(!isHandshakeDone())
        {
            {
                this.authenticate(playerName);
            }
        }
    }

    /**
     * KryoNet only watches the TCP connection. If the UDP path breaks while TCP stays up (a NAT or VPN gave us another
     * UDP port, so the server ignores our packets and sends its own to the old port), the voice is lost without any
     * disconnection. The server only answers (over TCP) a Ping sent over UDP if it still recognizes our UDP address,
     * so when the answers stop, we reconnect to register our new address.
     */
    private void checkUdp()
    {
        if(!this.client.isConnected())
        {
            return;
        }
        long silence = System.currentTimeMillis() - this.lastUdpAnswer;
        if(silence > UDP_TIMEOUT)
        {
            Helpers.log(String.format("No UDP answer from the VoiceServer for %ss, reconnecting.", silence / 1000));
            sendChatMessage("§6[" + ModularVoiceChat.MOD_NAME + "] §e" + I18n.format("mvc.messages.reconnecting"));
            this.client.close();
            this.connectIfNeeded();
            return;
        }
        FrameworkMessage.Ping ping = new FrameworkMessage.Ping();
        ping.id = this.udpPingId++;
        this.client.sendUDP(ping);
    }

    public void setHandshakeDone()
    {
        this.handshakeDone = true;
        Helpers.log("Successfully authenticate with " + playerName);
        sendChatMessage("§2[" + ModularVoiceChat.MOD_NAME + "] §a" + I18n.format("mvc.messages.connected"));
    }

    public boolean isHandshakeDone()
    {
        return handshakeDone;
    }

    /**
     * Called when the server answered one of our UDP pings
     */
    void onUdpAnswer()
    {
        this.lastUdpAnswer = System.currentTimeMillis();
    }

    /**
     * Called when the connection with the VoiceServer is lost, so the next connection authenticates again
     */
    void onDisconnected()
    {
        this.handshakeDone = false;
        Helpers.log("Disconnected from VoiceServer.");
    }

    /**
     * Send a packet to server
     * @param object packet
     */
    public void send(Object object)
    {
        this.client.sendUDP(object);
    }

    /**
     * Authenticate with a given name
     * @param name player name
     */
    private void authenticate(String name)
    {
        Helpers.log("Try authenticate with username " + name);
        send(new HelloImAPlayer(name, this.token));
    }

    /**
     * Show a message in the chat, on the client thread and only if the player is still in game
     * @param message the message
     */
    private static void sendChatMessage(String message)
    {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if(mc.player != null)
            {
                mc.player.sendMessage(new TextComponentString(message));
            }
        });
    }

    /**
     * Close the VoiceClient
     */
    public void close()
    {
        this.RECONNECT_SERVICE.shutdownNow();
        // Unlike close(), stop() also ends the KryoNet update thread
        this.client.stop();
    }

    /**
     * Check if the connection is connected to server
     * @return true if client is connected
     */
    public boolean isConnected()
    {
        return this.client != null && this.client.isConnected();
    }

}
