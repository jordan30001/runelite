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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.base.Strings;

import lombok.AccessLevel;
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

	@Inject
	public ChessOverlay(Client client, ChessPlugin plugin, ChessConfig config) {
		super(plugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if(needsUpdate == false && lastFrame != null) {
			graphics.drawImage(lastFrame, 0, 0, null);
		}
		long start = System.currentTimeMillis();
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
			lastFrameGraphics.drawImage(image, 0, 0, null);
		} else {
			if (lastFrame.getWidth() != image.getWidth() || lastFrame.getHeight() != image.getHeight()) {
				lastFrame = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(),
						BufferedImage.TYPE_INT_ARGB);
				lastFrameGraphics = lastFrame.getGraphics();
				renderTiles(g, points);
				lastFrameGraphics.drawImage(image, 0, 0, null);
			} else {
				if (needsUpdate == false)
					g.drawImage(lastFrame, 0, 0, lastFrame.getWidth(), lastFrame.getHeight(), null);
				else {
					renderTiles(g, points);
					lastFrame.getGraphics().drawImage(image, 0, 0, null);
				}
			}
		}
		needsUpdate = false;

		Future<Object> future = mainThreads.submit(() -> {
			try {
				// Polygon[] polygons = client.getLocalPlayer().getPolygons();
			} catch (ArrayIndexOutOfBoundsException aioobe) {
				aioobe.printStackTrace();
			}

			int[] dataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

			futs.put(mainThreads.submit(() -> {
				Polygon[] polygons = client.getLocalPlayer().getPolygons();
				Triangle[] triangles = getTriangles(client.getLocalPlayer().getModel());

				for (int i = 0; i < polygons.length; i++) {
					Triangle t = triangles[i];
					if (!(t.getA().getY() == 6 && t.getB().getY() == 6 && t.getC().getY() == 6)) {
						clearPolygon(dataBuffer, image.getWidth(), image.getHeight(), polygons[i]);
					}
				}
				return null;
			}));

			for (Player player : client.getPlayers()) {
				if (chessPieceUsername.contains(player.getName())) {
					futs.put(mainThreads.submit(() -> {
						Polygon[] polygonsFast = player.getPolygons();
						Triangle[] trianglesFast = getTriangles(player.getModel());

						for (int i = 0; i < polygonsFast.length; i++) {
							Triangle t = trianglesFast[i];
							if (!(t.getA().getY() == 6 && t.getB().getY() == 6 && t.getC().getY() == 6)) {
								clearPolygon(dataBuffer, image.getWidth(), image.getHeight(), polygonsFast[i]);
							}
						}
						return null;
					}));
				}
			}
			Future<Object> fut;
			while ((fut = futs.poll()) != null) {
				fut.get();
			}
			return null;
		});
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		lastFrameGraphics.drawImage(image, 0, 0, null);
		graphics.drawImage(image, 0, 0, null);
		System.out.println("Elapsed: " + (System.currentTimeMillis() - start));
		return null;
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


	private java.util.List<Vertex> getVertices(Model model) {
		int[] verticesX = model.getVerticesX();
		int[] verticesY = model.getVerticesY();
		int[] verticesZ = model.getVerticesZ();

		int count = model.getVerticesCount();

		java.util.List<Vertex> vertices = new ArrayList(count);

		for (int i = 0; i < count; ++i) {
			Vertex v = new Vertex(verticesX[i], verticesY[i], verticesZ[i]);
			vertices.add(v);
		}

		return vertices;
	}

	private Triangle[] getTriangles(Model model) {
		int[] trianglesX = model.getTrianglesX();
		int[] trianglesY = model.getTrianglesY();
		int[] trianglesZ = model.getTrianglesZ();

		List<Vertex> vertices = getVertices(model);

		int count = model.getTrianglesCount();
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
}
