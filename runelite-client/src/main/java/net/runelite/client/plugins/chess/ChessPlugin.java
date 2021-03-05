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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chess.ChessConfig;
import net.runelite.client.plugins.chess.ChessOverlay;
import net.runelite.client.plugins.chess.ColorTileMarker;
import net.runelite.client.plugins.chess.ChessMarkerPoint;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "Chess",
		description = "Chess plugin",
		tags = {"config", "chess"}
)

public class ChessPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "chessMarker";
	private static final String MARK = "Mark chessboard";
	private static final String UNMARK = "Unmark chessboard";
	private static final String LABEL = "Label tile";
	private static final String WALK_HERE = "Walk here";
	private static final String REGION_PREFIX = "region_";
	private static ChessMarkerPoint SW_Chess_Tile = null;

	@Getter
	private ChessOverlay chessOverlay;

	@Getter(AccessLevel.PACKAGE)
	private final List<net.runelite.client.plugins.chess.ColorTileMarker> points = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private ChessConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private ChessOverlay overlay;

//	@Inject
//	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private Gson gson;

	void savePoints(int regionId, Collection<ChessMarkerPoint> points)
	{
		if (points == null || points.isEmpty())
		{
			configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
			return;
		}

		String json = gson.toJson(points);
		configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId, json);
	}

	@Provides
	ChessConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChessConfig.class);
	}

	void loadPoints()
	{
		points.clear();

		int[] regions = client.getMapRegions();

		if (regions == null)
		{
			return;
		}

		for (int regionId : regions)
		{
			// load points for region
			log.debug("Loading points for region {}", regionId);
			Collection<ChessMarkerPoint> regionPoints = getPoints(regionId);
			Collection<net.runelite.client.plugins.chess.ColorTileMarker> colorTileMarkers = translateToColorTileMarker(regionPoints);
			points.addAll(colorTileMarkers);
		}
	}

	Collection<ChessMarkerPoint> getPoints(int regionId)
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
		if (Strings.isNullOrEmpty(json))
		{
			return Collections.emptyList();
		}

		// CHECKSTYLE:OFF
		return gson.fromJson(json, new TypeToken<List<ChessMarkerPoint>>(){}.getType());
		// CHECKSTYLE:ON
	}

	private Collection<net.runelite.client.plugins.chess.ColorTileMarker> translateToColorTileMarker(Collection<ChessMarkerPoint> points)
	{
		if (points.isEmpty())
		{
			return Collections.emptyList();
		}

		return points.stream()
				.map(point -> new net.runelite.client.plugins.chess.ColorTileMarker(
						WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()),
						point.getColor(), point.getLabel()))
				.flatMap(colorTile ->
				{
					final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client, colorTile.getWorldPoint());
					return localWorldPoints.stream().map(wp -> new net.runelite.client.plugins.chess.ColorTileMarker(wp, colorTile.getColor(), colorTile.getLabel()));
				})
				.collect(Collectors.toList());
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		loadPoints();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		points.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// map region has just been updated
		loadPoints();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
		if (hotKeyPressed && event.getOption().equals(WALK_HERE))
		{
			final Tile selectedSceneTile = client.getSelectedSceneTile();

			if (selectedSceneTile == null)
			{
				return;
			}

			final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, selectedSceneTile.getLocalLocation());
			final int regionId = worldPoint.getRegionID();
			SW_Chess_Tile = new ChessMarkerPoint(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), client.getPlane(), null, null);
			final boolean exists = getPoints(regionId).contains(SW_Chess_Tile);

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
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction().getId() != MenuAction.RUNELITE.getId())
		{
			return;
		}

		Tile target = client.getSelectedSceneTile();
		if (target == null)
		{
			return;
		}

		final String option = event.getMenuOption();
		if (option.equals(MARK) || option.equals(UNMARK))
		{
			markTile(target.getLocalLocation());
		}
	}


	public Color WhatColor(int x, int y) {
		if ( x % 2 == 0 && y % 2 == 0 || x == y)
		{
//			chessOverlay
			return config.blackTileColor();
		}
		else
		{
			return config.whiteTileColor();
		}
	}

	public String WhatLabel(int x, int y) {
		if (y == 0)
		{
			if (x == 9)
			{
				return null;
			}
			return getCharForNumber(x);
		}
		if (x == 0)
		{
			if (y == 9)
			{
				return null;
			}
			return Integer.toString(y);
		}
		return null;
	}

	private String getCharForNumber(int i) {
		return i > 0 && i < 27 ? String.valueOf((char)(i + 64)) : null;
	}



	private void markTile(LocalPoint localPoint)
	{
		if (localPoint == null)
		{
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

		int regionId = worldPoint.getRegionID();

		List<ChessMarkerPoint> chessMarkerPoints = new ArrayList<>(getPoints(regionId));

		List<ChessMarkerPoint> chessTiles = new ArrayList<ChessMarkerPoint>();

		for (int y=0;y<10;y++)
		{
			// If y = 1 or y = 9 LABEL TILE SET OPACITY to 0 & set color to white
			for(int x=0;x<10;x++)
			{
				chessTiles.add(new ChessMarkerPoint(regionId, worldPoint.getRegionX()+x, worldPoint.getRegionY()+y, client.getPlane(), WhatColor(x,y), WhatLabel(x,y)));
			}
		}

		for (ChessMarkerPoint element : chessTiles)
		{
			if (chessMarkerPoints.contains(element))
			{
				chessMarkerPoints.remove(element);
			}
			else
			{
				chessMarkerPoints.add(element);
			}
		}

		savePoints(regionId, chessMarkerPoints);

		loadPoints();
	}

	private void labelTile(Tile tile)
	{
		LocalPoint localPoint = tile.getLocalLocation();
		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
		final int regionId = worldPoint.getRegionID();

		ChessMarkerPoint searchPoint = new ChessMarkerPoint(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), client.getPlane(), null, null);
		Collection<ChessMarkerPoint> points = getPoints(regionId);
		ChessMarkerPoint existing = points.stream()
				.filter(p -> p.equals(searchPoint))
				.findFirst().orElse(null);
		if (existing == null)
		{
			return;
		}

		chatboxPanelManager.openTextInput("Tile label")
				.value(Optional.ofNullable(existing.getLabel()).orElse(""))
				.onDone((input) ->
				{
					input = Strings.emptyToNull(input);

					ChessMarkerPoint newPoint = new ChessMarkerPoint(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), client.getPlane(), existing.getColor(), input);
					points.remove(searchPoint);
					points.add(newPoint);
					savePoints(regionId, points);

					loadPoints();
				})
				.build();
	}
}
