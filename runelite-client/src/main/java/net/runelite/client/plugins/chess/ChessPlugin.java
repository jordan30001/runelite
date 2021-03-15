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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chess.data.ChessEmotes;
import net.runelite.client.plugins.chess.data.ChessMarkerPoint;
import net.runelite.client.plugins.chess.data.ChessMarkerPointType;
import net.runelite.client.plugins.chess.data.ColorTileMarker;
import net.runelite.client.plugins.chess.twitchintegration.TwitchEventRunners;
import net.runelite.client.plugins.chess.twitchintegration.TwitchIntegration;
import net.runelite.client.plugins.chess.twitchintegration.TwitchRedemptionInfo;
import net.runelite.client.plugins.chess.twitchintegration.events.ChessboardDisco;
import net.runelite.client.plugins.chess.twitchintegration.events.TwitchRedemptionEvent;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(name = "Chess", description = "Chess plugin", tags = { "config", "chess" })

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
	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Notifier notifier;

	@Getter
	private ChessOverlay chessOverlay;

	@Getter(AccessLevel.PUBLIC)
	private final List<ColorTileMarker> points = new ArrayList<>();

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
	@Getter(AccessLevel.PUBLIC)
	private WorldPoint worldPoint;
	@Getter
	private TwitchIntegration twitchHandler;
	private TwitchEventRunners twitchListeners;
	public int modIconsStart = -1;
	private BlockingQueue<OverheadTextInfo> overheadTextQueue;
	private BlockingQueue<OverheadTextInfo> priorityOverheadTextQueue;
	private Map<Class<TwitchRedemptionEvent>, BlockingQueue<TwitchRedemptionEvent>> twitchRedemptionQueue;
	@Getter(AccessLevel.PUBLIC)
	private ChessHandler chessHandler;

	@Override
	protected void startUp() throws Exception {
		overheadTextQueue = new ArrayBlockingQueue<>(20);
		priorityOverheadTextQueue = new ArrayBlockingQueue<>(20);
		twitchRedemptionQueue = new HashMap<>();
		chessHandler = new ChessHandler(this, overlay);

		overlayManager.add(overlay);
		loadPoints();
		this.config = overlay.config;

		onConfigChanged(null);
		try {
			loadEmojiIcons();
		} catch (Exception e) {
			// probably not on blades pc
		}
		try {
			twitchListeners = new TwitchEventRunners(this, overlay);
			twitchListeners.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		points.clear();

	}

	void savePoints(Collection<ChessMarkerPoint> points, WorldPoint worldPoint, LocalPoint localPoint) {
		if (points == null || points.isEmpty()) {
			configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX);
			return;
		}

		String json = gson.toJson(points);
		configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX, json);

		configManager.setConfiguration(CONFIG_GROUP, "worldpoint", gson.toJson(worldPoint));
		configManager.setConfiguration(CONFIG_GROUP, "localpoint", gson.toJson(localPoint));
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
			Collection<ColorTileMarker> colorTileMarkers = translateToColorTileMarker(regionPoints);
			points.addAll(colorTileMarkers);
		}
	}

	private Collection<ChessMarkerPoint> getPointsFromConfig() {
		String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX);
		if (Strings.isNullOrEmpty(json)) {
			return Collections.emptyList();
		}

		return gson.fromJson(json, new TypeToken<List<ChessMarkerPoint>>() {
		}.getType());
	}

	private Collection<ColorTileMarker> translateToColorTileMarker(Collection<ChessMarkerPoint> points) {
		if (points.isEmpty()) {
			return Collections.emptyList();
		}

		return points.stream().map(point -> new ColorTileMarker(WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()), point.getType(), point.getColor(), point.getLabel(), true))
				.flatMap(colorTile -> {
					final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client, colorTile.getWorldPoint());
					return localWorldPoints.stream().map(wp -> new ColorTileMarker(wp, colorTile.getType(), colorTile.getColor(), colorTile.getLabel(), colorTile.isTemporary()));
				}).collect(Collectors.toList());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		switch (event.getGameState()) {
		case UNKNOWN:
		case STARTING:
		case LOGIN_SCREEN:
		case LOGIN_SCREEN_AUTHENTICATOR:
		case LOGGING_IN:
		case LOADING:
		case CONNECTION_LOST:
		case HOPPING:
			overlay.getPlayerPolygonsTris().clear();
			overlay.setNeedsUpdate(true);
			return;
		case LOGGED_IN:
			overlay.setNeedsUpdate(true);
			break;
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
			twitchNames.add(name.toLowerCase());
		}
		for (String name : splitGameNames) {
			gameNames.add(name.toLowerCase());
		}

		String sWorldPoint = configManager.getConfiguration(CONFIG_GROUP, "worldpoint");
		String sLocalPoint = configManager.getConfiguration(CONFIG_GROUP, "localpoint");

		if (Strings.isNullOrEmpty(sWorldPoint) == false) {
			worldPoint = gson.fromJson(sWorldPoint, WorldPoint.class);
		}
		if (Strings.isNullOrEmpty(sLocalPoint) == false) {
			localPoint = gson.fromJson(sLocalPoint, LocalPoint.class);
		}

		String[] splitNames = config.chessPieceUsernames().split(",");
		char[] pieceTypes = config.chessPieceTypes().toCharArray();

		if (ChessOverlay.chessPieceUsername == null)
			ChessOverlay.chessPieceUsername = new HashSet<>();
		if (ChessOverlay.usernameToType == null)
			ChessOverlay.usernameToType = new HashMap<>();
		ChessOverlay.chessPieceUsername.clear();
		ChessOverlay.usernameToType.clear();
		if (pieceTypes.length == splitNames.length) {
			for (int i = 0; i < splitNames.length; i++) {
				ChessOverlay.chessPieceUsername.add(splitNames[i].trim());
				ChessOverlay.usernameToType.put(splitNames[i].trim(), pieceTypes[i]);
			}
		}

		// TODO: check if is streamer
		if (twitchHandler == null) {
			try {
				twitchHandler = new TwitchIntegration(config, this, overlay);
				twitchHandler.start();
			} catch (Exception e) {
				e.printStackTrace();
				twitchHandler = null;
			}
		}

		if (event != null) {
			if (event.getKey().equals("whiteTileColor") || event.getKey().equals("blackTileColor")) {
				getPoints().forEach(ctm -> {
					if (ctm.getType() != null) {
						switch (ctm.getType()) {
						case BLACK:
							ctm.setColor(config.blackTileColor());
							break;
						case WHITE:
							ctm.setColor(config.whiteTileColor());
							break;
						case FULL_ALPHA:
							ctm.setColor(Constants.FULL_ALPHA);
							break;
						}
					}
				});
			}
		}

		if (event != null && "debugMultithreadingThreads".equalsIgnoreCase(event.getKey())) {
			overlay.mainThreadPool.shutdown();
			int threads = config.debugMultithreadingThreads();
			if (threads == 0)
				threads = Runtime.getRuntime().availableProcessors();
			overlay.mainThreadPool = new ForkJoinPool(threads);
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
			localPoint = target.getLocalLocation();
			configManager.setConfiguration("chess", "localtile", gson.toJson(localPoint));
			worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
			markTile(target.getLocalLocation(), option.equals(MARK), option.equals(UNMARK), false);
		}
	}

	public void restartBoard() {
		markTile(localPoint, true, false, false);
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event) {
		if (client.getLocalPlayer() == null)
			return;
		if (config.debugCatJam() && twitchRedemptionQueue.get(ChessboardDisco.class).size() == 0) {
			queueTwitchRedemption(new ChessboardDisco(this));
		}
		overlay.setNeedsUpdate(true);
		// priority overhead text queue

		OverheadTextInfo priorityInfo = priorityOverheadTextQueue.peek();
		OverheadTextInfo overheadInfo = overheadTextQueue.peek();
		if (priorityInfo != null) {
			if (overheadInfo != null) {
				overheadInfo.reset();
			}

			if (priorityInfo.isStarted()) {
				if (priorityInfo.isFinished()) {
					priorityOverheadTextQueue.poll();
					client.getPlayers().forEach(p -> p.setOverheadText(""));
					client.getLocalPlayer().setOverheadText("");
				}
			} else {
				priorityInfo.startCountdown();
				client.getPlayers().forEach(p -> p.setOverheadText(priorityInfo.getOverheadText()));
				client.getLocalPlayer().setOverheadText(priorityInfo.getOverheadText());
			}
		}

		// overhead text queue

		if (overheadInfo != null) {
			if (overheadInfo.isStarted()) {
				if (overheadInfo.isFinished()) {
					overheadTextQueue.poll();
					client.getPlayers().forEach(p -> p.setOverheadText(""));
					client.getLocalPlayer().setOverheadText("");
				}
			} else {
				overheadInfo.startCountdown();
				client.getPlayers().forEach(p -> p.setOverheadText(overheadInfo.getOverheadText()));
				client.getLocalPlayer().setOverheadText(overheadInfo.getOverheadText());
			}
		}

		// twitch redemption

		Iterator<Entry<Class<TwitchRedemptionEvent>, BlockingQueue<TwitchRedemptionEvent>>> it = twitchRedemptionQueue.entrySet().iterator();

		while (it.hasNext()) {
			Entry<Class<TwitchRedemptionEvent>, BlockingQueue<TwitchRedemptionEvent>> e = it.next();
			// Class<TwitchRedemptionEvent> redemption = e.getKey();
			BlockingQueue<TwitchRedemptionEvent> queue = e.getValue();
			TwitchRedemptionEvent redemption = queue.peek();
			if (redemption == null)
				continue;
			TwitchRedemptionInfo twitchRedemptionEventInfo = redemption.getTwitchRedemptionInfo();

			if (twitchRedemptionEventInfo.isStarted() == false) {
				twitchRedemptionEventInfo.startCountdown(redemption.getRepeatedDelayTime());
				break;
			} else {
				if (twitchRedemptionEventInfo.isCurrentExecutionFinished()) {
					if (twitchRedemptionEventInfo.isFinished()) {
						queue.poll();
						break;
					} else if (redemption.execute(twitchRedemptionEventInfo.getCurrentCallCount())) {
						twitchRedemptionEventInfo.startCountdown(redemption.getEndingDelayTime());
						break;
					} else {
						twitchRedemptionEventInfo.resetForNextExecution();
						break;
					}
				}
			}
		}
	}

	public void queueOverheadText(String text, long timeToDisplay, boolean priority) {
		if (priority)
			priorityOverheadTextQueue.offer(new OverheadTextInfo(text, timeToDisplay));
		else
			overheadTextQueue.offer(new OverheadTextInfo(text, timeToDisplay));
	}

	public <T extends TwitchRedemptionEvent> void queueTwitchRedemption(T event) {
		twitchRedemptionQueue.get(event.getClass()).add(event);
	}

	private void markTile(LocalPoint localPoint, boolean doMark, boolean doUnmark, boolean updateVisuals) {
		if (localPoint == null) {
			return;
		}

		int regionId = worldPoint.getRegionID();

		List<ChessMarkerPoint> chessMarkerPoints = new ArrayList<>(getPointsFromConfig());
		if (updateVisuals || doUnmark)
			chessMarkerPoints.clear();

		List<ChessMarkerPoint> chessTiles = new ArrayList<>();

		if (doUnmark == false) {
			for (int y = 0; y < 10; y++) {// letters
				for (int x = 0; x < 10; x++) {// numbers
					chessTiles.add(new ChessMarkerPoint(regionId, worldPoint.getRegionX() + x, worldPoint.getRegionY() + y, client.getPlane(), WhatType(x, y), WhatColor(x, y), WhatLabel(x, y)));
					if (updateVisuals == false) {
						if ((x >= 1 || x <= 9) && (y >= 1 && y <= 9)) {
							List<Player> players = Stream.concat(Stream.of(client.getLocalPlayer()), client.getPlayers().stream()).filter(p -> ChessOverlay.chessPieceUsername.contains(p.getName()))
									.collect(Collectors.toCollection(ArrayList::new));
							for (int i = 0; i < players.size(); i++) {
								Player player = players.get(i);
								WorldPoint playerPoint = player.getWorldLocation();
								if (playerPoint.getX() == worldPoint.getX() + x && playerPoint.getY() == worldPoint.getY() + y) {
									char pieceType = ChessOverlay.usernameToType.getOrDefault(player.getName(), null);
									if (pieceType <= 0)
										continue;
									player.setOverheadText(pieceType + "");
									// notify chess engine of this piece
									chessHandler.initPiece(x, y, pieceType);
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

		savePoints(chessMarkerPoints, worldPoint, localPoint);

		loadPoints();
	}

	@Subscribe
	public void onChatMessage(ChatMessage msg) {
		switch (msg.getType()) {
		case PUBLICCHAT:
		case MODCHAT:
		case FRIENDSCHAT:
		case PRIVATECHAT:
		case PRIVATECHATOUT:
		case MODPRIVATECHAT:
			break;
		default:
			return;
		}

		System.err.println(msg.toString());

		if (("Twitch".equals(msg.getSender()) && twitchNames.contains(msg.getName().toLowerCase())) || (msg.getSender() == null && gameNames.contains(msg.getName().toLowerCase()))) {
			/*
			 * 
			 * ChatCommands.onChatMessage(msg);
			 * 
			 * if ("test".equals(sanitisedInput)) { chessHandler.initBaseBoard(); return; }
			 * 
			 * Matcher m = movePattern.matcher(sanitisedInput);
			 * 
			 * if (m.find() == false) { //
			 * queueOverheadText(String.format("Invalid move %s", //
			 * ChessEmotes.ThreeHead.toHTMLString(modIconsStart)), 6000, true); return; }
			 * 
			 * String moveFrom = m.group(1).toUpperCase(); String moveTo =
			 * m.group(2).toUpperCase(); int[] move = ChessHandler.getXYOffset(moveFrom,
			 * moveTo); Either<Exception, Position> moveResult = chessHandler.tryMove(move);
			 * if (moveResult.isRight()) { ChessMarkerPoint cmp = new
			 * ChessMarkerPoint(worldPoint.getRegionID(), worldPoint.getRegionX() + move[1]
			 * + 1, worldPoint.getRegionY() + move[0] + 1, client.getPlane(), null,
			 * Color.RED, null); ChessMarkerPoint cmp2 = new
			 * ChessMarkerPoint(worldPoint.getRegionID(), worldPoint.getRegionX() + move[3]
			 * + 1, worldPoint.getRegionY() + move[2] + 1, client.getPlane(), null,
			 * Color.GREEN, null);
			 * 
			 * while (points.size() >= 900) { points.remove(points.size() - 1); }
			 * 
			 * Stream.of(cmp, cmp2).map(point -> new
			 * ColorTileMarker(WorldPoint.fromRegion(point.getRegionId(),
			 * point.getRegionX(), point.getRegionY(), point.getZ()), point.getType(),
			 * point.getColor(), point.getLabel())) .flatMap(colorTile -> { final
			 * Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client,
			 * colorTile.getWorldPoint()); return localWorldPoints.stream().map(wp -> new
			 * ColorTileMarker(wp, colorTile.getType(), colorTile.getColor(),
			 * colorTile.getLabel())); }).forEach(ctm -> points.add(ctm));
			 * 
			 * if (chessHandler.getHistory().parent != null) { Position position =
			 * chessHandler.getHistory().parent.relatedPosition; char a =
			 * position.getPieceAt(new BoardCell(move[0], move[1])).toFEN();
			 * System.err.println(String.format("Move: %s From: %s, To: %s",
			 * ChessAscii.fromFEN(a).ascii, moveFrom, moveTo)); }
			 * 
			 * // twitchEventManager.sendMessage(String.format("Move: %s From: %s, To: %s",
			 * // ChessAscii.fromFEN(a).ascii, moveFrom, moveTo));
			 * queueOverheadText(String.format("%s%s", moveFrom, moveTo), 6000, true);
			 * 
			 * } else { twitchHandler.sendMessage(String.
			 * format("From: %s, To: %s is a Invalid move %s", moveFrom, moveTo,
			 * ChessEmotes.ThreeHead.trigger));
			 * queueOverheadText(String.format("Invalid move %s",
			 * ChessEmotes.ThreeHead.toHTMLString(modIconsStart)), 6000, true); }
			 */}
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
				// log.warn("Failed to load the sprite for emoji " + emoji, ex);
			}
		}

		log.debug("Adding emoji icons");
		client.setModIcons(newModIcons);

	}

	public ChessMarkerPointType WhatType(int x, int y) {
		if (x == 0 || x == 9)
			return ChessMarkerPointType.FULL_ALPHA;
		else if (y == 0 || y == 9)
			return ChessMarkerPointType.FULL_ALPHA;
		else if ((x + y) % 2 == 0) {
			// chessOverlay
			return ChessMarkerPointType.BLACK;
		} else {
			return ChessMarkerPointType.WHITE;
		}
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
			return Utils.getCharForNumber(x);
		}
		if (x == 0 || x == 9) {
			if (y == 9) {
				return null;
			}
			return Integer.toString(y);
		}
		return null;
	}
}