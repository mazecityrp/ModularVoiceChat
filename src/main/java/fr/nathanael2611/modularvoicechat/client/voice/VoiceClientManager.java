package fr.nathanael2611.modularvoicechat.client.voice;

/**
 * Static VoiceClient manager
 * Used for simply access to the VoiceClient
 */
public class VoiceClientManager
{

    /* The client instance */
    private static VoiceClient INSTANCE;

    /**
     * Start the VoiceClient
     * @param playerName the player name
     * @param token the secret received from the server, proving to the VoiceServer that we are this player
     * @param host VoiceServer hostname
     * @param port VoiceServer port
     */
    public static synchronized void start(String playerName, String token, String host, int port)
    {
        if(isStarted())
        {
            stop();
        }
        INSTANCE = new VoiceClient(playerName, token, host, port);
    }

    /**
     * Stop the VoiceClient
     */
    public static synchronized void stop()
    {
        if (INSTANCE != null)
        {
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    /**
     * Check if VoiceClient is started
     * @return true if VoiceClient is running
     */
    public static boolean isStarted()
    {
        return INSTANCE != null;
    }

    /**
     * Simply the VoiceClient getter
     * @return VoiceClient instance
     */
    public static VoiceClient getClient()
    {
        return INSTANCE;
    }

}