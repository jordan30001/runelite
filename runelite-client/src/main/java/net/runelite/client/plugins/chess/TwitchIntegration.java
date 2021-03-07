package net.runelite.client.plugins.chess;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.ChannelInformationList;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.netflix.hystrix.HystrixCommand;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class TwitchIntegration {
    private TwitchPubSub twitchPubSub;
    private String channelID;
    private ChessConfig config;
    private ChessOverlay overlay;
    private ChessPlugin plugin;
    private Timer timer;

    public TwitchIntegration(ChessConfig config, ChessPlugin plugin, ChessOverlay overlay) {
        this.config = config;
        this.overlay = overlay;
        this.plugin = plugin;
        this.timer = new Timer(true);
    }

    public void start() {
        try {
            TwitchHelix twitchHelix = TwitchHelixBuilder.builder()
                    .withClientId(config.clientID())
                    .withClientSecret(config.OAUthCode())
                    .build();
            UserList list = twitchHelix.getUsers(config.OAUthCode(), null, Arrays.asList(config.channelUsername())).execute();

            HystrixCommand<ChannelInformationList> request = twitchHelix.getChannelInformation(config.OAUthCode(), Arrays.asList(list.getUsers().get(0).getId()));

            ChannelInformationList ci = request.execute();

            channelID = ci.getChannels().get(0).getBroadcasterId();

            TwitchClient client = TwitchClientBuilder.builder()
                    .withEnablePubSub(true)
                    .withClientId(config.clientID())
                    .withClientSecret(config.OAUthCode())
                    .setBotOwnerIds(Arrays.asList(channelID))
                    .build();

            client.getPubSub().connect();

            CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
            credentialManager.registerIdentityProvider(new TwitchIdentityProvider(config.clientID(), config.OAUthCode(), "https://localhost"));

            Credential cred = new OAuth2Credential("twitch", "*authToken*");
            credentialManager.addCredential("twitch", cred);

            client.getPubSub().listenForChannelPointsRedemptionEvents(new OAuth2Credential("twitch", config.OAUthCode()), channelID);
            client.getEventManager().onEvent(RewardRedeemedEvent.class, this::OnChannelPointsRedeemed);

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);


            //bad twitch4j leaking our informaiton, lets get rid of it :(
            throw new RuntimeException(sw.toString().replace(config.OAUthCode(), ""));
        }
    }

    public void OnChannelPointsRedeemed(RewardRedeemedEvent event) {

        //user.login - lowercase
        //user.displayname - case sensitive
        //reward.ChannelPointsReward
        //title
        //prompt
        System.err.println(event.getRedemption().toString());
        switch (event.getRedemption().getReward().getTitle()) {
            case "Change Black Chessboard Tiles":
            case "Change White Chessboard Tiles": {
                boolean isBlack = event.getRedemption().getReward().getTitle().equals("Change Black Chessboard Tiles");
                Color color = ColorFromString(event.getRedemption().getUserInput(), isBlack ? config.blackTileColor() : config.whiteTileColor());
                if (isBlack) plugin.configManager.setConfiguration("chess", "blackTileColor", color.getRGB());
                else plugin.configManager.setConfiguration("chess", "whiteTileColor", color.getRGB());
                //(CONFIG_GROUP, REGION_PREFIX + regionId, json);
            }
            break;
            case "chesskill": {

            }
            break;
            case "chessdance": {

            }
            break;

            case "chesssubtime":
            case "chessaddtime": {

            }
            break;
        }
    }

    public Color ColorFromString(String str, Color defaultColor) {
        if (str.indexOf("#") >= 0) {
            return new Color(Integer.valueOf(str.substring(1, 3), 16),
                    Integer.valueOf(str.substring(3, 5), 16),
                    Integer.valueOf(str.substring(5, 7), 16));
        } else {
            switch (str.toLowerCase()) {
                case "black":
                    return Color.BLACK;
                case "blue":
                    return Color.BLUE;
                case "cyan":
                    return Color.CYAN;
                case "darkgray":
                    return Color.DARK_GRAY;
                case "gray":
                    return Color.GRAY;
                case "green":
                    return Color.GREEN;
                case "yellow":
                    return Color.YELLOW;
                case "lightgray":
                    return Color.LIGHT_GRAY;
                case "magneta":
                    return Color.MAGENTA;
                case "orange":
                    return Color.ORANGE;
                case "pink":
                    return Color.PINK;
                case "red":
                    return Color.RED;
                case "white":
                    return Color.WHITE;
                default:
                    String curOverhead = "<bold>Beep Boop Invalid Color: " + str + "<img=" + (plugin.modIconsStart + ChessEmotes.SADKEK.ordinal()) + ">";
                    plugin.client.getLocalPlayer().setOverheadText(curOverhead);
                    ActionListener listener = (ActionListener) e -> {

                    };
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            if (plugin.client.getLocalPlayer().getOverheadText().equals(curOverhead)) {
                                String curOverhead = "";

                                plugin.client.getLocalPlayer().setOverheadText(curOverhead);
                            }
                        }
                    };
                    timer.schedule(task, 6000);
            }
        }
        return defaultColor;
    }
}
