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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.base.Strings;
import com.google.common.collect.Streams;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class ChessOverlay extends Overlay {
	private static final ExecutorService mainThreads = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private final Client client;
	public final ChessConfig config;
	private final ChessPlugin plugin;
	public static Set<String> chessPieceUsername;
	public static HashMap<String, String> usernameToType;
	private ArrayBlockingQueue<Future<Object>> futs = new ArrayBlockingQueue<>(1000);
	private BufferedImage lastFrame;
	private Graphics lastFrameGraphics;
	@Setter(AccessLevel.PUBLIC)
	private volatile boolean needsUpdate = true;
	@Getter
	private Map<Player, PlayerPolygonsTriangles> playerPolygonsTris;

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
		// render previous frame if the game hasn't requested another gamerender yet
		if (needsUpdate == false && lastFrame != null) {
			graphics.drawImage(lastFrame, 0, 0, null);
			return null;
		}
		final Collection<ColorTileMarker> points = plugin.getPoints();
		BufferedImage image = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		if (config.showBackground()) {
			g.setColor(config.backgroundColor());
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
		}

		if (lastFrame == null) {
			lastFrame = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(),
					BufferedImage.TYPE_INT_ARGB);
			lastFrameGraphics = lastFrame.getGraphics();
			renderTiles(g, points);
		} else {
			if (lastFrame.getWidth() != image.getWidth() || lastFrame.getHeight() != image.getHeight()) {
				lastFrame = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(),
						BufferedImage.TYPE_INT_ARGB);
				lastFrameGraphics = lastFrame.getGraphics();
				renderTiles(g, points);
			} else {
				if (needsUpdate == false)
					g.drawImage(lastFrame, 0, 0, lastFrame.getWidth(), lastFrame.getHeight(), null);
				else {
					renderTiles(g, points);
				}
			}
		}

		// generate PPTs
		client.getPlayers().stream().forEach(p -> {
			if (chessPieceUsername.contains(p.getName()) == false
					&& p.getName().equals(client.getLocalPlayer().getName()) == false) {
				getPlayerPolygonsTris().remove(p);
			} else if (getPlayerPolygonsTris().containsKey(p) == false) {
				getPlayerPolygonsTris().put(p, new PlayerPolygonsTriangles());
			}
		});
		client.getPlayers().stream().forEach(p -> {
			PlayerPolygonsTriangles ppt = getPlayerPolygonsTris().getOrDefault(p, null);
			if (ppt != null) {
				ppt.grabData(p);
			}
		});
		try {
			for (Player p : client.getPlayers()) {
				futs.put(mainThreads.submit(() -> {
					updatePlayerPolygonsTriangles(p, getPlayerPolygonsTris().getOrDefault(p, null));
					return null;
				}));
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		needsUpdate = false;
		try {
			Future<Object> fut;
			while ((fut = futs.poll()) != null) {
				fut.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		int[] dataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

		getPlayerPolygonsTris().keySet().parallelStream().forEach(player -> {
			if (chessPieceUsername.contains(player.getName())
					|| player.getName().equals(client.getLocalPlayer().getName())) {
				PlayerPolygonsTriangles ppt = getPlayerPolygonsTris().get(player);
				Polygon[] polygons = ppt.polygons;
				Triangle[] triangles = ppt.triangles;

				for (int i = 0; i < polygons.length; i++) {
					final int ii = i;
					Triangle t = triangles[i];
					if (!(t.getA().getY() == 6 && t.getB().getY() == 6 && t.getC().getY() == 6)) {
						futs.offer(mainThreads.submit(() -> {
							clearPolygon(dataBuffer, image.getWidth(), image.getHeight(), polygons[ii]);
							return null;
						}));
					}
				}
			}
		});

		try {
			Future<Object> fut;
			while ((fut = futs.poll()) != null) {
				fut.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		lastFrameGraphics.drawImage(image, 0, 0, null);
		graphics.drawImage(image, 0, 0, null);
		return null;
	}

	private void updatePlayerPolygonsTriangles(Player player, PlayerPolygonsTriangles ppt) {
		if (ppt == null)
			return;
		Polygon[] polygons = ppt.nextFramePolygons;
		int polygonHash = Objects.hashCode(polygons);
		long modelHash = ppt.nextFrameModelHash;
		if (ppt.polygons == null || ppt.lastFramePolygonsHash != polygonHash) {
			ppt.polygons = polygons;
			ppt.lastFramePolygonsHash = polygonHash;
		}
		if (ppt.triangles == null || ppt.lastFrameModelHash != modelHash) {
			ppt.triangles = getTriangles(ppt);
			ppt.lastFrameModelHash = modelHash;
		}
	}

	private void renderTiles(Graphics g, Collection<ColorTileMarker> points) {
		for (ColorTileMarker point : points) {
			WorldPoint worldPoint = point.getWorldPoint();
			if (worldPoint.getPlane() != client.getPlane()) {
				continue;
			}

			Color tileColor = point.getColor();

			drawTile((Graphics2D) g, worldPoint, tileColor, point.getLabel());
		}
	}

	private void clearPolygon(int[] dataBuffer, int width, int height, Polygon p) {
		Rectangle bounds = p.getBounds();

		for (double y = bounds.getMinY(); y < bounds.getMaxY(); y++) {
			for (double x = bounds.getMinX(); x < bounds.getMaxX(); x++) {
				if (p.contains(x, y) && x >= 0 && x < client.getCanvasWidth() && y >= 0
						&& y < client.getCanvasHeight()) {
					dataBuffer[(int) (x + width * y)] = 0x00000000;
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
		int lastFramePolygonsHash = 0;
		long lastFrameModelHash = 0;
		Polygon[] polygons;
		Triangle[] triangles;
		Polygon[] nextFramePolygons;
		Model nextFrameModel;
		long nextFrameModelHash;
		public int trianglesCount;
		public int verticesCount;
		int[] trisX;
		int[] trisY;
		int[] trisZ;
		int[] vertsX;
		int[] vertsY;
		int[] vertsZ;

		public void grabData(Player p) {
			Model m = p.getModel();
			trisX = m.getTrianglesX();
			trisY = m.getTrianglesY();
			trisZ = m.getTrianglesZ();
			vertsX = m.getVerticesX();
			vertsY = m.getVerticesY();
			vertsZ = m.getVerticesZ();
			nextFramePolygons = p.getPolygons();
			nextFrameModelHash = m.getHash();
			trianglesCount = m.getTrianglesCount();
			verticesCount = m.getVerticesCount();
		}
	}
}
