/*
 * Copyright (c) 2018, Joris K <kjorisje@gmail.com>
 * Copyright (c) 2018, Lasse <cronick@zytex.dk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.chess;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IndexedSprite;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chatnotifications.OverheadTextInfo;
import net.runelite.client.plugins.chess.twitchintegration.TwitchIntegration;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(name = "Chess", description = "Chess plugin", tags = {"config", "chess"})

public class ChessPlugin extends Plugin {
	private static final String CONFIG_GROUP = "chessMarker";
	private static final String MARK = "Mark chessboard";
	private static final String UNMARK = "Unmark chessboard";
	private static final String LABEL = "Label tile";
	private static final String WALK_HERE = "Walk here";
	private static final String REGION_PREFIX = "region_";
	private static ChessMarkerPoint SW_Chess_Tile = null;
	private static Set<String> twitchNames;
	private static Set<String> gameNames;
	private static Pattern movePattern = Pattern.compile("$\\s*([a-hA-H][1-8])\\s*([a-hA-H][1-8])\\s*^");
	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Notifier notifier;

	@Getter
	private ChessOverlay chessOverlay;

	@Getter(AccessLevel.PACKAGE)
	private final List<net.runelite.client.plugins.chess.ColorTileMarker> points = new ArrayList<>();

	@Inject
	public Client client;

	@Inject
	private ChessConfig config;

	@Inject
	public ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private ChessOverlay overlay;

	// @Inject
	// private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private Gson gson;
	private LocalPoint localPoint;
	private WorldPoint worldPoint;
	@Getter
	private TwitchIntegration twitchEventManager;
	private TwitchEventRunners twitchListeners;
	public int modIconsStart = -1;
	private BlockingQueue<OverheadTextInfo> overheadTextQueue;

	@Override
	protected void startUp() throws Exception {
		overheadTextQueue = new ArrayBlockingQueue<>(100);
		overlayManager.add(overlay);
		loadPoints();
		this.config = overlay.config;
		onConfigChanged(null);
		loadEmojiIcons();
		twitchListeners = new TwitchEventRunners(this, overlay);
		twitchListeners.init();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		points.clear();

	}

	void savePoints(Collection<ChessMarkerPoint> points) {
		if (points == null || points.isEmpty()) {
			configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX);
			return;
		}

		String json = gson.toJson(points);
		configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX, json);
	}

	@Provides
	ChessConfig getConfig(ConfigManager configManager) {
		return configManager.getConfig(ChessConfig.class);
	}

	void loadPoints() {
		points.clear();

		int[] regions = client.getMapRegions();

		if (regions == null) {
			return;
		}

		for (int regionId : regions) {
			// load points for region
			log.debug("Loading points for region {}", regionId);
			Collection<ChessMarkerPoint> regionPoints = getPointsFromConfig();
			Collection<net.runelite.client.plugins.chess.ColorTileMarker> colorTileMarkers = translateToColorTileMarker(
					regionPoints);
			points.addAll(colorTileMarkers);
		}
	}

	private Collection<ChessMarkerPoint> getPointsFromConfig() {
		String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX);
		if (Strings.isNullOrEmpty(json)) {
			return Collections.emptyList();
		}

		// CHECKSTYLE:OFF
		return gson.fromJson(json, new TypeToken<List<ChessMarkerPoint>>() {
		}.getType());
		// CHECKSTYLE:ON
	}

	private Collection<net.runelite.client.plugins.chess.ColorTileMarker> translateToColorTileMarker(
			Collection<ChessMarkerPoint> points) {
		if (points.isEmpty()) {
			return Collections.emptyList();
		}

		return points.stream()
				.map(point -> new net.runelite.client.plugins.chess.ColorTileMarker(WorldPoint
						.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()),
						point.getColor(), point.getLabel()))
				.flatMap(colorTile -> {
					final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client,
							colorTile.getWorldPoint());
					return localWorldPoints.stream().map(wp -> new net.runelite.client.plugins.chess.ColorTileMarker(wp,
							colorTile.getColor(), colorTile.getLabel()));
				}).collect(Collectors.toList());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		// map region has just been updated
		loadPoints();
	}

	@Subscribe
	public void onConfigChanged(@Nullable ConfigChanged event) {
		String[] splitTwitchNames = config.twitchPlayers().split(",");
		String[] splitGameNames = config.osrsPlayers().split(",");

		if (twitchNames == null)
			twitchNames = new HashSet<String>();
		if (gameNames == null)
			gameNames = new HashSet<String>();

		twitchNames.clear();
		gameNames.clear();
		for (String name : splitTwitchNames) {
			twitchNames.add(name);
		}
		for (String name : splitGameNames) {
			gameNames.add(name);
		}

		String[] splitNames = config.chessPieceUsernames().split(",");
		List<String> allTypes = new ArrayList<>();
		allTypes.addAll(Arrays.asList(config.chessPieceTypes1().split(",")));
		allTypes.addAll(Arrays.asList(config.chessPieceTypes2().split(",")));
		allTypes.addAll(Arrays.asList(config.chessPieceTypes3().split(",")));
		allTypes.addAll(Arrays.asList(config.chessPieceTypes4().split(",")));

		if (ChessOverlay.chessPieceUsername == null)
			ChessOverlay.chessPieceUsername = new HashSet<>();
		if (ChessOverlay.usernameToType == null)
			ChessOverlay.usernameToType = new HashMap<>();
		ChessOverlay.chessPieceUsername.clear();
		ChessOverlay.usernameToType.clear();
		if (allTypes.size() == splitNames.length) {
			for (int i = 0; i < splitNames.length; i++) {
				ChessOverlay.chessPieceUsername.add(splitNames[i]);
				ChessOverlay.usernameToType.put(splitNames[i],
						Strings.isNullOrEmpty(allTypes.get(i)) ? null : allTypes.get(i));
			}
		}

		// TODO: check if is streamer
		if (twitchEventManager == null) {
			twitchEventManager = new TwitchIntegration(config, this, overlay);
			twitchEventManager.start();
		}

		if (event != null) {
			if (event.getKey().equals("whiteTileColor") || event.getKey().equals("blackTileColor")) {
				LocalPoint localPoint = gson.fromJson(configManager.getConfiguration("chess", "localtile"),
						LocalPoint.class);
				markTile(localPoint, false, false, true);
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
		if (hotKeyPressed && event.getOption().equals(WALK_HERE)) {
			final Tile selectedSceneTile = client.getSelectedSceneTile();

			if (selectedSceneTile == null) {
				return;
			}

			final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, selectedSceneTile.getLocalLocation());
			final int regionId = worldPoint.getRegionID();
			// SW_Chess_Tile = new ChessMarkerPoint(regionId, worldPoint.getRegionX(),
			// worldPoint.getRegionY(),
			// client.getPlane(), null, null);
			final boolean exists = getPointsFromConfig().size() > 0;// .contains(SW_Chess_Tile);

			MenuEntry[] menuEntries = client.getMenuEntries();
			menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);

			MenuEntry mark = menuEntries[menuEntries.length - 1] = new MenuEntry();
			mark.setOption(exists ? UNMARK : MARK);
			mark.setTarget(event.getTarget());
			mark.setType(MenuAction.RUNELITE.getId());

			client.setMenuEntries(menuEntries);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuAction().getId() != MenuAction.RUNELITE.getId()) {
			return;
		}

		Tile target = client.getSelectedSceneTile();
		if (target == null) {
			return;
		}

		final String option = event.getMenuOption();
		if (option.equals(MARK) || option.equals(UNMARK)) {
			markTile(target.getLocalLocation(), option.equals(MARK), option.equals(UNMARK), false);
			localPoint = target.getLocalLocation();
			configManager.setConfiguration("chess", "localtile", gson.toJson(localPoint));
			worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event) {
		if (client.getLocalPlayer() == null)
			return;
		OverheadTextInfo overheadInfo = overheadTextQueue.peek();
		if (overheadInfo == null) {
			client.getPlayers().forEach(p -> p.setOverheadText(""));
			client.getLocalPlayer().setOverheadText("");
			return;
		}
		if (overheadInfo.isStarted()) {
			if (overheadInfo.isFinished())
				overheadTextQueue.poll();
		} else {
			overheadInfo.startCountdown();
			client.getPlayers().forEach(p -> p.setOverheadText(overheadInfo.getOverheadText()));
			client.getLocalPlayer().setOverheadText(overheadInfo.getOverheadText());
		}
	}

	public void queueOverheadText(String text, long timeToDisplay) {
		overheadTextQueue.offer(new OverheadTextInfo(text, timeToDisplay));
	}

	private void markTile(LocalPoint localPoint, boolean doMark, boolean doUnmark, boolean updateVisuals) {
		if (localPoint == null) {
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

		int regionId = worldPoint.getRegionID();

		List<ChessMarkerPoint> chessMarkerPoints = new ArrayList<>(getPointsFromConfig());
		if (updateVisuals || doUnmark)
			chessMarkerPoints.clear();

		List<ChessMarkerPoint> chessTiles = new ArrayList<>();

		if (doUnmark == false) {
			for (int y = 0; y < 10; y++) {
				for (int x = 0; x < 10; x++) {
					chessTiles.add(new ChessMarkerPoint(regionId, worldPoint.getRegionX() + x,
							worldPoint.getRegionY() + y, client.getPlane(), WhatColor(x, y), WhatLabel(x, y)));

					if (updateVisuals == false) {
						if ((x >= 1 || x <= 9) && (y >= 1 && y <= 9)) {
							Optional<Player> playerOnTile = Stream
									.concat(Stream.of(client.getLocalPlayer()), client.getPlayers().stream())
									.filter(curPlayer -> ChessOverlay.chessPieceUsername.contains(curPlayer.getName()))
									.findFirst();
							if (playerOnTile.isPresent()) {
								Player player = playerOnTile.get();
								WorldPoint playerPoint = player.getWorldLocation();
								if (ChessOverlay.chessPieceUsername.contains(player.getName())) {
									if (playerPoint.getX() == worldPoint.getX() + x
											&& playerPoint.getY() == worldPoint.getY() + y) {
										String pieceType = ChessOverlay.usernameToType.getOrDefault(player.getName(),
												null);
										if (pieceType == null)
											continue;
										player.setOverheadText(pieceType);
										// notify chess engine of this piece
									}
								}
							}
						}
					}
				}
			}
		}

		for (ChessMarkerPoint element : chessTiles) {
			if (chessMarkerPoints.contains(element)) {
				chessMarkerPoints.remove(element);
			} else {
				chessMarkerPoints.add(element);
			}
		}

		savePoints(chessMarkerPoints);

		loadPoints();
	}

	@Subscribe
	public void onChatMessage(ChatMessage msg) {
		if (("Twitch".equals(msg.getSender()) && twitchNames.contains(msg.getName()))
				|| (msg.getSender() == null && gameNames.contains(msg.getName()))) {
			// validate chess move
			System.out.print("Valid user :)");

			Matcher m = movePattern.matcher(msg.getMessage().trim());
			if (m.find() == false) {
				System.out.println("Invalid move :(");
				return;
			}
			System.out.println("Valid move :)");
			String moveFrom = m.group(1);
			String moveTo = m.group(2);
		}
	}

	private void loadEmojiIcons() {
		final IndexedSprite[] modIcons = client.getModIcons();
		if (modIconsStart != -1 || modIcons == null) {
			return;
		}

		final ChessEmotes[] emojis = ChessEmotes.values();
		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + emojis.length);
		modIconsStart = modIcons.length;

		for (int i = 0; i < emojis.length; i++) {
			final ChessEmotes emoji = emojis[i];

			try {
				final BufferedImage image = emoji.loadImage();
				final IndexedSprite sprite = ImageUtil.getImageIndexedSprite(image, client);
				newModIcons[modIconsStart + i] = sprite;
			} catch (Exception ex) {
				log.warn("Failed to load the sprite for emoji " + emoji, ex);
			}
		}

		log.debug("Adding emoji icons");
		client.setModIcons(newModIcons);

	}

	public Color WhatColor(int x, int y) {
		if (x == 0 || x == 9)
			return new Color(0, 0, 0, 0);
		else if (y == 0 || y == 9)
			return new Color(0, 0, 0, 0);
		else if ((x + y) % 2 == 0) {
			// chessOverlay
			return config.blackTileColor();
		} else {
			return config.whiteTileColor();
		}
	}

	public String WhatLabel(int x, int y) {
		if (y == 0 || y == 9) {
			if (x == 9) {
				return null;
			}
			return getCharForNumber(x);
		}
		if (x == 0 || x == 9) {
			if (y == 9) {
				return null;
			}
			return Integer.toString(y);
		}
		return null;
	}

	private String getCharForNumber(int i) {
		return i > 0 && i < 27 ? String.valueOf((char) (i + 64)) : null;
	}

}
