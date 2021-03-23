package net.runelite.client.plugins.chess.twitchintegration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.api.service.IEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.enums.TMIConnectionState;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.ChannelInformationList;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.netflix.hystrix.HystrixCommand;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.chess.ChessConfig;
import net.runelite.client.plugins.chess.ChessOverlay;
import net.runelite.client.plugins.chess.ChessPlugin;

@Slf4j
public class TwitchIntegration {
	private TwitchClient twitchClient;
	private TwitchHelix twitchHelix;

	private String channelID;
	private String channelID2;
	@Inject
	private ChessConfig config;
	private ChessOverlay overlay;
	private ChessPlugin plugin;
	private ReentrantReadWriteLock lock;
	private List<Class<?>> subscribedEvents;
	private Map<Class<?>, List<Consumer<?>>> eventListeners;
	private Timer timer;

	public TwitchIntegration(ChessConfig config, ChessPlugin plugin, ChessOverlay overlay) {
		this.config = config;
		this.overlay = overlay;
		this.plugin = plugin;
		this.timer = new Timer(true);
		eventListeners = new HashMap<>();
		lock = new ReentrantReadWriteLock();
		subscribedEvents = new ArrayList<>();
	}

	public void start() {
		try {
			OAuth2Credential oauth = new OAuth2Credential("twitch", config.OAUthCode() + "a");
			twitchHelix = TwitchHelixBuilder.builder().withClientId(config.clientID()).withClientSecret(config.OAUthCode() + "a").build();
			UserList list = twitchHelix.getUsers(config.OAUthCode() + "a", null, Arrays.asList(config.channelUsername(), config.channelName())).execute();

			List<String> ids = new ArrayList<>();
			list.getUsers().forEach(user -> ids.add(user.getId()));

			HystrixCommand<ChannelInformationList> request = twitchHelix.getChannelInformation(config.OAUthCode() + "a", ids);

			ChannelInformationList ci = request.execute();

			channelID = ci.getChannels().get(0).getBroadcasterId();
			if (ci.getChannels().size() == 2)
				channelID2 = ci.getChannels().get(1).getBroadcasterId();

			twitchClient = TwitchClientBuilder.builder().withEnablePubSub(true).withEnableChat(true).withClientId(config.clientID()).withClientSecret(config.OAUthCode() + "a").setBotOwnerIds(Arrays.asList(channelID)).withChatAccount(oauth)
					.build();
			twitchClient.getChat().joinChannel(config.channelName());
			twitchClient.getChat().getEventManager().onEvent(ChannelMessageEvent.class, this::DispatchEvents);

			twitchClient.getPubSub().connect();

			CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
			credentialManager.registerIdentityProvider(new TwitchIdentityProvider(config.clientID(), config.OAUthCode() + "a", "https://localhost"));

			credentialManager.addCredential("twitch", oauth);

			twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(oauth, channelID);
			twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(oauth, channelID2);

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			log.error("twitch exception", e);
			// bad twitch4j leaking our information, lets get rid of it :(
			//throw new RuntimeException(sw.toString().replace(config.OAUthCode() + "a", ""));
		} finally {
		}
	}

	public boolean isRunning() {
		try {
			return twitchClient != null && twitchClient.getChat() != null && twitchClient.getChat().getConnectionState().equals(TMIConnectionState.CONNECTED);
		} catch (RuntimeException re) {
			re.printStackTrace();
			return false;
		}
	}

	public void RegisterPubSubListener(Class<?> eventType) {
		if (subscribedEvents.contains(eventType))
			return;
		subscribedEvents.add(eventType);
		twitchClient.getPubSub().getEventManager().onEvent(eventType, this::DispatchEvents);
	}

	public <E> void RegisterListener(Class<E> eventClass, Consumer<E> callback) {
		try {
			lock.writeLock().lock();
			List<Consumer<?>> listeners = eventListeners.get(eventClass);
			if (listeners == null) {
				listeners = new ArrayList<>();
				eventListeners.put(eventClass, listeners);
			}
			listeners.add(callback);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private <E> void DispatchEvents(E event) {
		try {
			lock.readLock().lock();
			List<Consumer<?>> callbacks = eventListeners.getOrDefault((event.getClass()), null);
			if (callbacks == null)
				return;

			callbacks.forEach(c -> ((Consumer<E>) c).accept(event));
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			lock.readLock().unlock();
		}
	}

	public void sendMessage(String message) {
		if (twitchClient.getChat().isChannelJoined(config.channelName()) == false) {
			twitchClient.getChat().joinChannel(config.channelName());
		}
		if (twitchClient.getChat().sendMessage(config.channelName(), message)) {
			System.err.println("not sent");
		}

	}

	public void shutdown() {
		twitchClient.getChat().close();
		twitchClient.getPubSub().close();
		twitchClient.close();
	}

}
