/*
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.chess.Utils.Function;
import net.runelite.client.plugins.chess.data.ChessMarkerPointType;
import net.runelite.client.plugins.chess.data.ColorTileMarker;
import net.runelite.client.plugins.chess.data.Triangle;
import net.runelite.client.plugins.chess.data.Vertex;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class ChessOverlay extends Overlay {
	public ForkJoinPool mainThreadPool = new ForkJoinPool(4);

	// functions
	private Function<Stream<Player>> getPlayers = () -> getClient().getPlayers().stream();
	private Predicate<Player> renderPlayer = (p) -> getConfig().debugShowRandomPlayers() || chessPieceUsername.contains(p.getName());
	// vars
	@Getter(AccessLevel.PRIVATE)
	private final Client client;
	@Getter(AccessLevel.PRIVATE)
	public final ChessConfig config;
	private final ChessPlugin plugin;
	public static Set<String> chessPieceUsername;
	public static HashMap<String, Character> usernameToType;
	private BufferedImage globalImage;
	private Graphics globalGraphics;
	@Setter(AccessLevel.PUBLIC)
	private volatile boolean needsUpdate = true;
	@Getter
	private Map<String, PlayerPolygonsTriangles> playerPolygonsTris;
	private AtomicInteger atom = new AtomicInteger(10);
	@Inject
	private ClientUI clientUI;

	@Inject
	public ChessOverlay(Client client, ChessPlugin plugin, ChessConfig config) {
		super(plugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		playerPolygonsTris = new HashMap<>();
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		Graphics imageG = null;
		try {
			if (globalImage != null && (client.getCanvasWidth() == globalImage.getWidth() && client.getCanvasHeight() == globalImage.getHeight())) {
				graphics.drawImage(globalImage, 0, 0, null);
			} else {
				if (globalGraphics != null)
					globalGraphics.dispose();
				globalImage = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(), BufferedImage.TYPE_INT_ARGB);
				globalGraphics = globalImage.getGraphics();
			}
			atom.set(getConfig().debugShowRandomPlayersCount());
			long startTime = System.currentTimeMillis();
//		 render previous frame if the game hasn't requested another gamerender yet
//			if (needsUpdate == false && image != null) {
//				graphics.drawImage(image, 0, 0, null);
//				return null;
//			}
			BufferedImage image = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(), BufferedImage.TYPE_INT_ARGB);
			imageG = image.getGraphics();
			final Collection<ColorTileMarker> points = plugin.getPoints();
			if (config.showBackground()) {
				imageG.setColor(config.backgroundColor());
				imageG.fillRect(0, 0, image.getWidth(), image.getHeight());
			}

			renderTiles(imageG, points);

			new HashSet<>(getPlayerPolygonsTris().keySet()).forEach(playerName -> {
				boolean remove = true;
				for (Player p : client.getPlayers()) {
					if (p.getName().equals(playerName)) {
						remove = false;
						break;
					}
				}
				if (remove)
					getPlayerPolygonsTris().remove(playerName);
			});

			// generate PPTs
			getPlayers.get().filter(renderPlayer).forEach(ChessOverlay.this::generatePPTs);
			// grab PPT data
			getPlayerPolygonsTris().values().stream().sequential().forEach(PlayerPolygonsTriangles::grabData);
			// update PPTs
			if (getConfig().debugUseMultithreading()) {
				mainThreadPool.submit(() -> getPlayerPolygonsTris().values().parallelStream().forEach(ppt -> updatePlayerPolygonsTriangles(ppt))).get();
			} else {
				getPlayerPolygonsTris().values().stream().forEach(ppt -> updatePlayerPolygonsTriangles(ppt));
			}

			needsUpdate = false;

			int[] dataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

			mainThreadPool.submit(() -> getPlayerPolygonsTris().values().parallelStream().forEach(ppt -> {
				if (ppt == null || ppt.trisX == null)
					return;
				Polygon[] polygons = ppt.polygons;
				Triangle[] triangles = ppt.triangles;

				for (int i = 0; i < polygons.length; i++) {
					final int ii = i;
					Triangle t = triangles[i];
					if (!(t.getA().getY() == 6 && t.getB().getY() == 6 && t.getC().getY() == 6)) {
						clearPolygon(dataBuffer, image.getWidth(), image.getHeight(), polygons[ii]);
					}
				}
			})).get();

			graphics.drawImage(image, 0, 0, null);
			if (getConfig().debugShowFrameTimes()) {
				int frameTime = (int) (System.currentTimeMillis() - startTime);
				clientUI.getFrame().setTitle(String.format("Frame Time: %d, Average Frame Time: %d", frameTime, Utils.FrameTimeLogger.INSTANCE.getFrameTimeAverage(frameTime)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			imageG.dispose();
		}
		return null;
	}

	private void generatePPTs(Player player) {
		String name = player.getName();
		if (getConfig().debugShowRandomPlayers() && atom.get() <= 0) {
			getPlayerPolygonsTris().remove(name);
			return;
		}
		if (getPlayerPolygonsTris().containsKey(name) == false)
			getPlayerPolygonsTris().put(name, new PlayerPolygonsTriangles(player));
		atom.decrementAndGet();
	}

	private PlayerPolygonsTriangles updatePlayerPolygonsTriangles(PlayerPolygonsTriangles ppt) {
		if (ppt == null || ppt.trisX == null)
			return ppt;

		Polygon[] polygons = ppt.nextFramePolygons;
		int polygonHash = Objects.hashCode(polygons);
		long modelHash = ppt.nextFrameModelHash;
//		if (ppt.polygons == null || ppt.lastFramePolygonsHash != polygonHash) {
//		if (ppt.polygons == null) {
		ppt.polygons = polygons;
		ppt.lastFramePolygonsHash = polygonHash;
//		}
//		if (ppt.triangles == null || ppt.lastFrameModelHash != modelHash) {
//		if (ppt.triangles == null) {
		ppt.trianglesCount = ppt.nextFrameTrianglesCount;
		ppt.verticesCount = ppt.nextFrameVerticesCount;
		ppt.triangles = getTriangles(ppt);
		ppt.lastFrameModelHash = modelHash;
//		}

		return ppt;
	}

	private void renderTiles(Graphics g, Collection<ColorTileMarker> points) {
		for (ColorTileMarker point : points) {
			WorldPoint worldPoint = point.getWorldPoint();
			if (worldPoint.getPlane() != client.getPlane()) {
				continue;
			}

			Color tileColor = null;
			ChessMarkerPointType type = point.getType();
			switch (type) {
			case BLACK:
				tileColor = getConfig().blackTileColor();
				break;
			case WHITE:
				tileColor = getConfig().whiteTileColor();
				break;
			case FULL_ALPHA:
				tileColor = Constants.FULL_ALPHA;
				break;
			default:
				tileColor = point.getColor();
				break;
			}

			// point.isBlack() ? getConfig().blackTileColor() :
			// getConfig().whiteTileColor();

			drawTile((Graphics2D) g, worldPoint, tileColor, point.getLabel());
		}
	}

	private void clearPolygon(BufferedImage bi, int width, int height, Polygon p) {
		Rectangle bounds = p.getBounds();

		for (double y = bounds.getMinY(); y < bounds.getMaxY(); y++) {
			for (double x = bounds.getMinX(); x < bounds.getMaxX(); x++) {
				if (p.contains(x, y) && x >= 0 && x < client.getCanvasWidth() && y >= 0 && y < client.getCanvasHeight()) {
					bi.setRGB((int) x, (int) y, 0);
				}
			}
		}
	}

	private void clearPolygon(int[] dataBuffer, int width, int height, Polygon p) {
		Rectangle bounds = p.getBounds();

		for (double y = bounds.getMinY(); y < bounds.getMaxY(); y++) {
			for (double x = bounds.getMinX(); x < bounds.getMaxX(); x++) {
				if (p.contains(x, y) && x >= 0 && x < client.getCanvasWidth() && y >= 0 && y < client.getCanvasHeight()) {
					dataBuffer[(int) x + width * (int) y] = 0x00000000;
				}
			}
		}
	}

	private java.util.List<Vertex> getVertices(PlayerPolygonsTriangles ppt) {
		int[] verticesX = ppt.vertsX;
		int[] verticesY = ppt.vertsY;
		int[] verticesZ = ppt.vertsZ;

		int count = ppt.verticesCount;

		java.util.List<Vertex> vertices = new ArrayList(count);

		for (int i = 0; i < count; ++i) {
			Vertex v = new Vertex(verticesX[i], verticesY[i], verticesZ[i]);
			vertices.add(v);
		}

		return vertices;
	}

	private Triangle[] getTriangles(PlayerPolygonsTriangles ppt) {
		int[] trianglesX = ppt.trisX;
		int[] trianglesY = ppt.trisY;
		int[] trianglesZ = ppt.trisZ;

		List<Vertex> vertices = getVertices(ppt);

		int count = ppt.trianglesCount;
		Triangle[] triangles = new Triangle[count];

		for (int i = 0; i < count; ++i) {
			int triangleX = trianglesX[i];
			int triangleY = trianglesY[i];
			int triangleZ = trianglesZ[i];

			Triangle triangle = new Triangle(vertices.get(triangleX), vertices.get(triangleY), vertices.get(triangleZ));
			triangles[i] = triangle;
		}

		return triangles;
	}

	private static final int MAX_DRAW_DISTANCE = 32;

	private void drawTile(Graphics2D graphics, WorldPoint point, Color color, @Nullable String label) {
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) {
			return;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, point);
		if (lp == null) {
			return;
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly != null) {
			graphics.setColor(color);
			final Stroke originalStroke = graphics.getStroke();
			final Color originalColor = graphics.getColor();
			graphics.draw(poly);
			graphics.setColor(originalColor);
			graphics.fill(poly);
		}

		if (!Strings.isNullOrEmpty(label)) {
			Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);
			if (canvasTextLocation != null) {
				OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
			}
		}
	}

	private static class PlayerPolygonsTriangles {
		Player player;
		int lastFramePolygonsHash = 0;
		long lastFrameModelHash = 0;
		Polygon[] polygons;
		Triangle[] triangles;
		Polygon[] nextFramePolygons;
		Model nextFrameModel;
		long nextFrameModelHash;
		public int trianglesCount;
		public int verticesCount;
		public int nextFrameTrianglesCount;
		public int nextFrameVerticesCount;
		int[] trisX;
		int[] trisY;
		int[] trisZ;
		int[] vertsX;
		int[] vertsY;
		int[] vertsZ;

		public PlayerPolygonsTriangles(Player player) {
			this.player = player;
		}

		public boolean isNull() {
			return player == null || polygons == null || triangles == null;
		}

		public void grabData() {
			try {
				Model m = player.getModel();
				trisX = m.getTrianglesX();
				trisY = m.getTrianglesY();
				trisZ = m.getTrianglesZ();
				vertsX = m.getVerticesX();
				vertsY = m.getVerticesY();
				vertsZ = m.getVerticesZ();
				nextFramePolygons = player.getPolygons();
				nextFrameModelHash = m.getHash();
				nextFrameTrianglesCount = m.getTrianglesCount();
				nextFrameVerticesCount = m.getVerticesCount();
			} catch (Exception aioobe) {
				trisX = null;
			}
		}
	}
}
