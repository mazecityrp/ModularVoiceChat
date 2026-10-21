package fr.nathanael2611.modularvoicechat.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secrets proving that a VoiceServer connection belongs to the player it claims to be.
 * The server gives one to each player through the Minecraft connection (PacketConnectVoice), and the voice client
 * sends it back in HelloImAPlayer. Without it, anyone reaching the voice port could take the place of any online
 * player by sending their name: hear what they hear (calls, radio) and speak for them, while they become mute.
 * Keyed by entity id rather than name, so that the logout of a replaced session never removes the new session's secret.
 */
public class VoiceTokens
{

    private final SecureRandom random = new SecureRandom();
    private final Map<Integer, String> tokens = new ConcurrentHashMap<>();

    /**
     * Create the secret of a player who just logged in
     * @param entityId the player entity id
     * @return the secret to send to the player
     */
    public String create(int entityId)
    {
        byte[] bytes = new byte[16];
        this.random.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte b : bytes)
        {
            token.append(String.format("%02x", b));
        }
        this.tokens.put(entityId, token.toString());
        return token.toString();
    }

    /**
     * Forget the secret of a player who left
     * @param entityId the player entity id
     */
    public void remove(int entityId)
    {
        this.tokens.remove(entityId);
    }

    /**
     * Check a secret sent by a voice client
     * @param entityId the entity id of the player the client claims to be
     * @param token the secret sent by the client
     * @return true if it is the secret given to this player
     */
    public boolean isValid(int entityId, String token)
    {
        String expected = this.tokens.get(entityId);
        return expected != null && token != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

}
