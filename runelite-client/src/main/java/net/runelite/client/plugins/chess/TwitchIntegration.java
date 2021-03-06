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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
public class TwitchIntegration {
    private TwitchPubSub twitchPubSub;
    private String channelID;
    private ChessConfig config;
    private ChessOverlay overlay;
    private ChessPlugin plugin;

    public TwitchIntegration(ChessConfig config, ChessPlugin plugin, ChessOverlay overlay) {
        this.config = config;
        this.overlay = overlay;
        this.plugin = plugin;
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

            //user.login - lowercase
            //user.displayname - case sensitive
            //reward.ChannelPointsReward
                //title
                //prompt

/*

ChannelPointsRedemption(id=938b349c-9031-4a7c-a333-98391b5f5cc2
	 user=ChannelPointsUser(id=254547888
	    login=bladebtw
	    displayName=BladeBTW)
	 channelId=254547888
	 redeemedAt=2021-03-06T23:00:05.746467133Z
	 reward=ChannelPointsReward(id=149637ed-048c-4f02-ada1-3895b5fbbab1
	    channelId=254547888
	    title=Hydrate!
	    prompt=Make me take a sip of water
	    cost=300
	    isUserInputRequired=false
	    isSubOnly=false
	    image=null
        defaultImage=ChannelPointsReward.Image(url1x=https://static-cdn.jtvnw.net/custom-reward-images/tree-1.png
        url2x=https://static-cdn.jtvnw.net/custom-reward-images/tree-2.png
        url4x=https://static-cdn.jtvnw.net/custom-reward-images/tree-4.png)
        backgroundColor=#00E5CB
        isEnabled=true
        isPaused=false
        isInStock=true
	 maxPerStream=ChannelPointsReward.MaxPerStream(isEnabled=true
	    maxPerStream=10)
	 shouldRedemptionsSkipRequestQueue=false
	    updatedForIndicatorAt=2020-07-28T14:40:42.012417132Z)
	 userInput=null
	 status=UNFULFILLED)

 */
            //bad twitch4j leaking our informaiton, lets get rid of it :(
            throw new RuntimeException(sw.toString().replace(config.OAUthCode(), ""));
        }
    }

    public void OnChannelPointsRedeemed(RewardRedeemedEvent event) {
        System.err.println(event.getRedemption().toString());
        switch(event.getRedemption().getReward().getTitle()){
            case "chesschangeblack":
            case "chesschangewhite": {

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
}
