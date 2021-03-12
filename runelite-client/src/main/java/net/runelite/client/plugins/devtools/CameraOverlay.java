/*
 * Copyright (c) 2018, Matthew Steglinski <https://github.com/sainttx>
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
package net.runelite.client.plugins.devtools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class CameraOverlay extends OverlayPanel
{
	private final Client client;
	private final DevToolsPlugin plugin;
	public static final int[] SINE = new int[2048]; // sine angles for each of the 2048 units, * 65536 and stored as an int
	public static final int[] COSINE = new int[2048]; // cosine

	@Inject
	CameraOverlay(Client client, DevToolsPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.getCameraPosition().isActive())
		{
			return null;
		}

		int cameraPitch = client.getCameraPitch();
		int cameraYaw = client.getCameraYaw();

		int pitchSin = SINE[cameraPitch];
		int pitchCos = COSINE[cameraPitch];
		int yawSin = SINE[cameraYaw];
		int yawCos = COSINE[cameraYaw];




		panelComponent.getChildren().add(TitleComponent.builder()
				.text("Camera")
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("ViewXOff")
				.right("" + client.getViewportYOffset())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("ViewXOff")
				.right("" + client.getViewportXOffset())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("X")
				.right("" + client.getCameraX())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("Y")
				.right("" + client.getCameraY())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("Z")
				.right("" + client.getCameraZ())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("Pitch")
				.right("" + client.getCameraPitch())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("Yaw")
				.right("" + client.getCameraYaw())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("Scale")
				.right("" + client.getScale())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("WLoc")
				.right("" + client.getLocalPlayer().getWorldLocation())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("Orient")
				.right("" + client.getLocalPlayer().getOrientation())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("CanvP")
				.right("" + client.getLocalPlayer().getCanvasTilePoly())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("LocL")
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getLocalPlayer().getLocalLocation())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("PlayX")
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getLocalPlayer().getLocalLocation().getX())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getLocalPlayer().getLocalLocation().getSceneX())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("VHeight")
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getViewportHeight())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("VWidth")
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getViewportWidth())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("PlayY")
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getLocalPlayer().getLocalLocation().getY())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.right("" + client.getLocalPlayer().getLocalLocation().getSceneY())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("OcuY")
				.right("" + client.getOculusOrbFocalPointY())
				.build());

		panelComponent.getChildren().add(LineComponent.builder()
				.left("OcuX")
				.right("" + client.getOculusOrbFocalPointX())
				.build());


		return super.render(graphics);
	}
}
