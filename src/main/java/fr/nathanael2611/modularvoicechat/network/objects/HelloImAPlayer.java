package fr.nathanael2611.modularvoicechat.network.objects;

/**
 * This object will be send from client to the server to inform it that a new player is connected to
 * the voice-server.
 */
public class HelloImAPlayer
{

    /**
     * The player-name.
     */
    public String playerName;

    /**
     * The secret the server gave to this player in PacketConnectVoice, proving that the connection is really theirs.
     */
    public String token;

    /**
     * Constructor
     * @param playerName the name of the player that will be assigned to the connection.
     * @param token the secret received in PacketConnectVoice.
     */
    public HelloImAPlayer(String playerName, String token)
    {
        this.playerName = playerName;
        this.token = token;
    }

    /**
     * Constructor
     * Empty for serialization
     */
    public HelloImAPlayer()
    {
    }

}
