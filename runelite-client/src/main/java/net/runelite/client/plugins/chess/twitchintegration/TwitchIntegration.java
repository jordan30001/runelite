package net.runelite.client.plugins.chess.twitchintegration;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.Credential;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.common.events.TwitchEvent;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.ChannelInformationList;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelCommerceEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import com.netflix.hystrix.HystrixCommand;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.chess.ChessConfig;
import net.runelite.client.plugins.chess.ChessOverlay;
import net.runelite.client.plugins.chess.ChessPlugin;

@Slf4j
public class TwitchIntegration {
	private TwitchClient client;
	private TwitchPubSub twitchPubSub;
	private String channelID;
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
			TwitchHelix twitchHelix = TwitchHelixBuilder.builder().withClientId(config.clientID())
					.withClientSecret(config.OAUthCode()).build();
			UserList list = twitchHelix.getUsers(config.OAUthCode(), null, Arrays.asList(config.channelUsername()))
					.execute();

			HystrixCommand<ChannelInformationList> request = twitchHelix.getChannelInformation(config.OAUthCode(),
					Arrays.asList(list.getUsers().get(0).getId()));

			ChannelInformationList ci = request.execute();

			channelID = ci.getChannels().get(0).getBroadcasterId();

			client = TwitchClientBuilder.builder().withEnablePubSub(true).withClientId(config.clientID())
					.withClientSecret(config.OAUthCode()).setBotOwnerIds(Arrays.asList(channelID)).build();

			client.getPubSub().connect();

			CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
			credentialManager.registerIdentityProvider(
					new TwitchIdentityProvider(config.clientID(), config.OAUthCode(), "https://localhost"));

			Credential cred = new OAuth2Credential("twitch", "*authToken*");
			credentialManager.addCredential("twitch", cred);

			OAuth2Credential oauth = new OAuth2Credential("twitch", config.OAUthCode());
			client.getPubSub().listenForChannelPointsRedemptionEvents(oauth, channelID);

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			// bad twitch4j leaking our information, lets get rid of it :(
			throw new RuntimeException(sw.toString().replace(config.OAUthCode(), ""));
		}
	}

	public void RegisterPubSubListener(Class<?> eventType) {
		if (subscribedEvents.contains(eventType))
			return;
		client.getEventManager().onEvent(eventType, this::DispatchEvents);
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
	public <E> void DispatchEvents(E event) {
		try {
			lock.readLock().lock();
			List<Consumer<?>> callbacks = eventListeners.getOrDefault((event.getClass()), null);
			if (callbacks == null)
				return;

			callbacks.forEach(c -> ((Consumer<E>) c).accept(event));
		} finally {
			lock.readLock().unlock();
		}
	}

}
