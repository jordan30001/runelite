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

import com.google.common.base.Strings;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;

public class ChessOverlay extends Overlay
{
	private final Client					client;
	public final ChessConfig				config;
	private final ChessPlugin				plugin;
	public static Set<String>				chessPieceUsername;
	public static HashMap<String, String>	usernameToType;

	@Inject
	public ChessOverlay(Client client, ChessPlugin plugin, ChessConfig config)
	{
		super(plugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Collection<ColorTileMarker> points = plugin.getPoints();
		BufferedImage image = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		if (config.showBackground())
		{
			g.setColor(config.backgroundColor());
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
		}

		for (final ColorTileMarker point : points)
		{
			WorldPoint worldPoint = point.getWorldPoint();
			if (worldPoint.getPlane() != client.getPlane())
			{
				continue;
			}

			Color tileColor = point.getColor();
			//			if (tileColor == null || !config.rememberTileColors())
			//			{
			//				// If this is an old tile which has no color, or rememberTileColors is off, use marker color
			//				tileColor = config.markerColor();
			//			}

			drawTile((Graphics2D) g, worldPoint, tileColor, point.getLabel());
		}

		Polygon[] polygons = client.getLocalPlayer().getPolygons();
		Triangle[] triangles = getTriangles(client.getLocalPlayer().getModel());

		for (int i = 0; i < polygons.length; i++)
		{
			Triangle t = triangles[i];
			if (!(t.getA().getY() == 6 && t.getB().getY() == 6 && t.getC().getY() == 6))
			{
				clearPolygon(image, polygons[i]);
			}
		}

		for (Player player : client.getPlayers())
		{
			if (chessPieceUsername.contains(player.getName()))
			{
				Polygon[] polygonsFast = player.getPolygons();
				Triangle[] trianglesFast = getTriangles(player.getModel());

				IntStream.range(0, polygonsFast.length).parallel().forEach(index ->
				{
					Triangle t = trianglesFast[index];
					if (!(t.getA().getY() == 6 && t.getB().getY() == 6 && t.getC().getY() == 6))
					{
						clearPolygon(image, polygonsFast[index]);
					}
				});

			}
		}

		graphics.drawImage(image, 0, 0, null);
		return null;
	}

	private void clearPolygon(BufferedImage image, Polygon p)
	{

		Rectangle bounds = p.getBounds();

		for (double y = bounds.getMinY(); y < bounds.getMaxY(); y++)
		{
			for (double x = bounds.getMinX(); x < bounds.getMaxX(); x++)
			{
				if (p.contains(x, y)
						&& x >= 0
						&& x < client.getCanvasWidth()
						&& y >= 0
						&& y < client.getCanvasHeight())
				{
					image.setRGB((int) x, (int) y, 0x00000000);
				}
			}
		}
	}

	private java.util.List<Vertex> getVertices(Model model)
	{
		int[] verticesX = model.getVerticesX();
		int[] verticesY = model.getVerticesY();
		int[] verticesZ = model.getVerticesZ();

		int count = model.getVerticesCount();

		java.util.List<Vertex> vertices = new ArrayList(count);

		for (int i = 0; i < count; ++i)
		{
			Vertex v = new Vertex(
					verticesX[i],
					verticesY[i],
					verticesZ[i]);
			vertices.add(v);
		}

		return vertices;
	}

	private Triangle[] getTriangles(Model model)
	{
		int[] trianglesX = model.getTrianglesX();
		int[] trianglesY = model.getTrianglesY();
		int[] trianglesZ = model.getTrianglesZ();

		List<Vertex> vertices = getVertices(model);

		int count = model.getTrianglesCount();
		Triangle[] triangles = new Triangle[count];

		for (int i = 0; i < count; ++i)
		{
			int triangleX = trianglesX[i];
			int triangleY = trianglesY[i];
			int triangleZ = trianglesZ[i];

			Triangle triangle = new Triangle(
					vertices.get(triangleX),
					vertices.get(triangleY),
					vertices.get(triangleZ));
			triangles[i] = triangle;
		}

		return triangles;
	}

	private static final int MAX_DRAW_DISTANCE = 32;

	private void drawTile(Graphics2D graphics, WorldPoint point, Color color, @Nullable String label)
	{
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
		{ return; }

		LocalPoint lp = LocalPoint.fromWorld(client, point);
		if (lp == null)
		{ return; }

		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly != null)
		{
			graphics.setColor(color);
			final Stroke originalStroke = graphics.getStroke();
			final Color originalColor = graphics.getColor();
			graphics.draw(poly);
			graphics.setColor(originalColor);
			graphics.fill(poly);
		}

		if (!Strings.isNullOrEmpty(label))
		{
			Point canvasTextLocation = Perspective.getCanvasTextLocation(client, graphics, lp, label, 0);
			if (canvasTextLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
			}
		}
	}
}
