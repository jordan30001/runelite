package net.runelite.client.plugins.twitch4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.ChannelInformationList;
import com.github.twitch4j.helix.domain.UserList;
import com.google.common.base.Predicate;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCommand;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TwitchIntegration {
	public static final TwitchIntegration INSTANCE = new TwitchIntegration();
	private ReentrantReadWriteLock lock;
	private List<Class<?>> subscribedEvents;
	private Map<Class<?>, List<Consumer<?>>> eventListeners;
	private Map<OAuth2Credential, TwitchHelix> helixEndpoints;
	private Map<OAuth2Credential, TwitchClient> twitchClients;
	private List<Predicate<OAuth2Credential>> shutdownList;

	public TwitchIntegration() {
		eventListeners = new HashMap<>();
		lock = new ReentrantReadWriteLock();
		subscribedEvents = new ArrayList<>();
		helixEndpoints = new HashMap<>();
		shutdownList = new ArrayList<>();
		twitchClients = new HashMap<>();
	}

	public Optional<TwitchHelix> getTwitchHelixEndpoints(OAuth2Credential credential) {
		return Optional.ofNullable(helixEndpoints.getOrDefault(credential, null));
	}

	public TwitchHelix createTwitchHelixEndpointsIfNotExist(String clientID, OAuth2Credential credential) {
		try {
			lock.writeLock().lock();
			Optional<TwitchHelix> optTwitchHelix = getTwitchHelixEndpoints(credential);
			if (optTwitchHelix.isPresent()) {
				return optTwitchHelix.get();
			}
			TwitchHelix twitchHelix = TwitchHelixBuilder.builder().withDefaultAuthToken(credential).withClientId(clientID).build();
			helixEndpoints.put(credential, twitchHelix);
			return twitchHelix;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/***
	 * @return a map of kv's (username/id)
	 */
	public Map<String, String> getTwitchChannelIDsFromName(TwitchHelix twitchHelix, OAuth2Credential credential, List<String> channelNames) {
		UserList list = twitchHelix.getUsers(credential.getAccessToken(), null, channelNames).execute();
		List<String> userIDs = new ArrayList<>();
		Map<String, String> channelIDs = new HashMap<>();

		list.getUsers().forEach(user -> userIDs.add(user.getId()));

		HystrixCommand<ChannelInformationList> request = twitchHelix.getChannelInformation(credential.getAccessToken(), userIDs);

		ChannelInformationList ci = request.execute();

		ci.getChannels().forEach(c -> channelIDs.put(c.getBroadcasterName().toLowerCase(), c.getBroadcasterId()));

		return channelIDs;
	}

	public Optional<TwitchClient> getTwitchClient(OAuth2Credential credential) {
		return Optional.ofNullable(twitchClients.getOrDefault(credential, null));
	}

	public TwitchClient createTwitchClientIfNotExist(String clientID, OAuth2Credential credential, List<String> botOwnerChannelIDs, boolean withChat, boolean withPubSub) {
		try {
			lock.writeLock().lock();
			Optional<TwitchClient> optTwitchClient = getTwitchClient(credential);
			if (optTwitchClient.isPresent()) {
				return optTwitchClient.get();
			}

			TwitchClient twitchClient = TwitchClientBuilder.builder().withEnablePubSub(withPubSub).withEnableChat(withChat).withClientId(clientID).setBotOwnerIds(botOwnerChannelIDs).withChatAccount(credential).build();
			twitchClients.put(credential, twitchClient);
			if (withPubSub) {
				twitchClient.getPubSub().connect();
			}
			if(withChat) {
				twitchClient.getChat().connect();
			}
			return twitchClient;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void RegisterPubSubListener(OAuth2Credential credential, Class<?> eventType) {
		Optional<TwitchClient> client = getTwitchClient(credential);
		if (client.isPresent() == false) {
			throw new IllegalStateException("twitch client has not been previously created");
		}		try {
			lock.writeLock().lock();
		if (subscribedEvents.contains(eventType))
			return;
		subscribedEvents.add(eventType);
		client.get().getPubSub().getEventManager().onEvent(eventType, this::DispatchEvents);		} finally {
			lock.writeLock().unlock();
		}
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

	public boolean sendMessage(OAuth2Credential credential, String channelName, String message) {
		Optional<TwitchClient> twitchClient = getTwitchClient(credential);
		if (twitchClient.isPresent() == false) {
			throw new IllegalStateException("twitch client has not been previously created");
		}
		if (twitchClient.get().getChat().isChannelJoined(channelName) == false) {
			twitchClient.get().getChat().joinChannel(channelName);
		}
		if (twitchClient.get().getChat().sendMessage(channelName, message)) {
			return true;
		}
		return false;
	}

	/***
	 * @param predicate - should always return true if you don't rely on the
	 *                  supplied Twitch Clients
	 */
	public void registerShutdownListener(Predicate<OAuth2Credential> predicate) {
		try {
			lock.writeLock().lock();
			shutdownList.add(predicate);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void shutdown(OAuth2Credential credential) {
		if(credential == null) return;
		try {
			lock.readLock().lock();
//			private Map<OAuth2Credential, TwitchHelix> helixEndpoints;
//			private Map<OAuth2Credential, TwitchClient> twitchClients;

			for (Predicate<OAuth2Credential> pred : shutdownList) {
				if (pred.apply(credential) == false)
					return;
			}
			helixEndpoints.put(credential, null);
			TwitchClient client = twitchClients.getOrDefault(credential, null);
			if (client != null) {
				client.close();
			}

		} finally {
			lock.readLock().unlock();
		}
	}

}
