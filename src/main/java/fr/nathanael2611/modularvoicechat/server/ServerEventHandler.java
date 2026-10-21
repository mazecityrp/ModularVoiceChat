package fr.nathanael2611.modularvoicechat.server;

import fr.nathanael2611.modularvoicechat.config.ServerConfig;
import fr.nathanael2611.modularvoicechat.network.vanilla.PacketConnectVoice;
import fr.nathanael2611.modularvoicechat.network.vanilla.VanillaPacketHandler;
import fr.nathanael2611.modularvoicechat.util.Helpers;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class ServerEventHandler
{

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (VoiceServerManager.isStarted())
        {
            Helpers.log("Requesting " + event.player.getName() + " to connect to voice-server... Sending packet.");
            String token = VoiceServerManager.getServer().getTokens().create(event.player.getEntityId());
            VanillaPacketHandler.getInstance().getNetwork().sendTo(new PacketConnectVoice(ServerConfig.generalConfig.forcedHostname, VoiceServerManager.getServer().getPort(), event.player.getName(), token, ServerConfig.generalConfig.showWhoSpeak), Helpers.getPlayerMP(event.player));
        }

    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (VoiceServerManager.isStarted())
        {
            VoiceServerManager.getServer().getTokens().remove(event.player.getEntityId());
        }
    }



}
