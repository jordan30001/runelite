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
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Chess",
        description = "Chess plugin",
        tags = {"config", "chess"}
)

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
    private LocalPoint localPoint;
    private WorldPoint worldPoint;

    void savePoints(int regionId, Collection<ChessMarkerPoint> points) {
        if (points == null || points.isEmpty()) {
            configManager.unsetConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
            return;
        }

        String json = gson.toJson(points);
        configManager.setConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId, json);
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
            Collection<ChessMarkerPoint> regionPoints = getPoints(regionId);
            Collection<net.runelite.client.plugins.chess.ColorTileMarker> colorTileMarkers = translateToColorTileMarker(regionPoints);
            points.addAll(colorTileMarkers);
        }
    }

    Collection<ChessMarkerPoint> getPoints(int regionId) {
        String json = configManager.getConfiguration(CONFIG_GROUP, REGION_PREFIX + regionId);
        if (Strings.isNullOrEmpty(json)) {
            return Collections.emptyList();
        }

        // CHECKSTYLE:OFF
        return gson.fromJson(json, new TypeToken<List<ChessMarkerPoint>>() {
        }.getType());
        // CHECKSTYLE:ON
    }

    private Collection<net.runelite.client.plugins.chess.ColorTileMarker> translateToColorTileMarker(Collection<ChessMarkerPoint> points) {
        if (points.isEmpty()) {
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
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        loadPoints();
        this.config = overlay.config;

        String[] splitTwitchNames = config.twitchPlayers().split(",");
        String[] splitGameNames = config.osrsPlayers().split(",");

        twitchNames = new HashSet<String>();
        gameNames = new HashSet<String>();
        for (String name : splitTwitchNames) {
            twitchNames.add(name);
        }
        for (String name : splitGameNames) {
            gameNames.add(name);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        points.clear();
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
    public void onMenuEntryAdded(MenuEntryAdded event) {
        final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
        if (hotKeyPressed && event.getOption().equals(WALK_HERE)) {
            final Tile selectedSceneTile = client.getSelectedSceneTile();

            if (selectedSceneTile == null) {
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
            markTile(target.getLocalLocation());
            localPoint = target.getLocalLocation();
            worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
        }
    }


    public Color WhatColor(int x, int y) {
        if (x == 0 || x == 9)
            return new Color(0, 0, 0, 0);
        else if (y == 0 || y == 9)
            return new Color(0, 0, 0, 0);
        else if ((x + y) % 2 == 0) {
//			chessOverlay
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


    private void markTile(LocalPoint localPoint) {
        if (localPoint == null) {
            return;
        }

        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

        int regionId = worldPoint.getRegionID();

        List<ChessMarkerPoint> chessMarkerPoints = new ArrayList<>(getPoints(regionId));

        List<ChessMarkerPoint> chessTiles = new ArrayList<>();

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                chessTiles.add(new ChessMarkerPoint(regionId, worldPoint.getRegionX() + x, worldPoint.getRegionY() + y, client.getPlane(), WhatColor(x, y), WhatLabel(x, y)));

                if ((x >= 1 || x <= 9) && (y >= 1 && y <= 9)) {
                    for (Player player : client.getPlayers()) {
                        WorldPoint playerPoint = player.getWorldLocation();
                        if (playerPoint.equals(worldPoint)) {
                            if (ChessOverlay.chessPieceUsername.contains(player.getName())) {
                                String pieceType = ChessOverlay.usernameToType.getOrDefault(player.getName(), null);
                                if (pieceType == null) continue;
                                System.out.println("Chess Piece: " + pieceType + " x: " + x + " y: " + y);
                                player.setOverheadText(pieceType);
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

        savePoints(regionId, chessMarkerPoints);

        loadPoints();
    }

    @Subscribe
    public void onChatMessage(ChatMessage msg) {
        switch (msg.getType()) {
            case MODCHAT:
            case PUBLICCHAT:
            case FRIENDSCHAT:
            case AUTOTYPER:
            case MODAUTOTYPER:
                if (client.getLocalPlayer() != null && Text.toJagexName(Text.removeTags(msg.getName())).equals(client.getLocalPlayer().getName())) {
                    return;
                }
                break;
        }

        Matcher m = movePattern.matcher(msg.getMessage().trim());
        if (m.find() == false) {
            return;
        }
        String moveFrom = m.group(1);
        String moveTo = m.group(2);

        if (("Twitch".equals(msg.getSender()) && twitchNames.contains(msg.getName()))
                || (msg.getSender() == null && gameNames.contains(msg.getName()))) {
            //validate chess move
            System.out.print("Valid user typed :)");
        }

        //getSender() == Twitch/etc
        //getName() == twitch username/game username etc



       /* if (config.highlightOwnName() && usernameMatcher != null)
        {
            final String message = messageNode.getValue();
            Matcher matcher = usernameMatcher.matcher(message);
            if (matcher.find())
            {
                final int start = matcher.start();
                final String username = client.getLocalPlayer().getName();
                final String closeColor = MoreObjects.firstNonNull(getLastColor(message.substring(0, start)), "</col>");
                final String replacement = "<col" + ChatColorType.HIGHLIGHT.name() + "><u>" + username + "</u>" + closeColor;
                messageNode.setValue(matcher.replaceAll(replacement));
                update = true;
                if (config.notifyOnOwnName() && (chatMessage.getType() == ChatMessageType.PUBLICCHAT
                        || chatMessage.getType() == ChatMessageType.PRIVATECHAT
                        || chatMessage.getType() == ChatMessageType.FRIENDSCHAT
                        || chatMessage.getType() == ChatMessageType.MODCHAT
                        || chatMessage.getType() == ChatMessageType.MODPRIVATECHAT))
                {
                    sendNotification(chatMessage);
                }
            }
        }

        if (highlightMatcher != null)
        {
            String nodeValue = messageNode.getValue();
            Matcher matcher = highlightMatcher.matcher(nodeValue);
            boolean found = false;
            StringBuffer stringBuffer = new StringBuffer();

            while (matcher.find())
            {
                String value = matcher.group();

                // Determine the ending color by:
                // 1) use the color from value if it has one
                // 2) use the last color from stringBuffer + <content between last match and current match>
                // To do #2 we just search for the last col tag after calling appendReplacement
                String endColor = getLastColor(value);

                // Strip color tags from the highlighted region so that it remains highlighted correctly
                value = stripColor(value);

                matcher.appendReplacement(stringBuffer, "<col" + ChatColorType.HIGHLIGHT + '>' + value);

                if (endColor == null)
                {
                    endColor = getLastColor(stringBuffer.toString());
                }

                // Append end color
                stringBuffer.append(endColor == null ? "<col" + ChatColorType.NORMAL + ">" : endColor);

                update = true;
                found = true;
            }

            if (found)
            {
                matcher.appendTail(stringBuffer);
                messageNode.setValue(stringBuffer.toString());

                if (config.notifyOnHighlight())
                {
                    sendNotification(chatMessage);
                }
            }
        }

        if (update)
        {
            messageNode.setRuneLiteFormatMessage(messageNode.getValue());
            chatMessageManager.update(messageNode);
        }*/
    }
}
