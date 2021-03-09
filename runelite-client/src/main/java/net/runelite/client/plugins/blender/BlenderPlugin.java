package net.runelite.client.plugins.blender;

import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.api.events.BeforeRender;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.chess.ChessConfig;
import net.runelite.client.plugins.corp.CorpConfig;
import net.runelite.client.plugins.corp.CorpPlugin;
import net.runelite.client.plugins.devtools.DevToolsPlugin;
import net.runelite.client.plugins.npchighlight.NpcIndicatorsConfig;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

@PluginDescriptor(
        name = "Blender",
        description = "Blender plugin",
        tags = {"Blender"}
)
@Slf4j
public class BlenderPlugin extends Plugin {

    private ExecutorService es = Executors.newFixedThreadPool(1);

    @Inject
    public Client client;

    @Inject
    private BlenderConfig config;

    @Inject
    private ItemManager itemManager;
    @Inject
    private Gson gson;

    @Provides
    BlenderConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BlenderConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
    }


    @Override
    protected void shutDown() throws Exception {
    }

    private JsonObject getCamera(JsonObject parent) {
        JsonObject cameraJSON = new JsonObject();

        cameraJSON.addProperty("type", "camerapositionupdate");
        cameraJSON.addProperty("cameraX", client.getCameraX());
        cameraJSON.addProperty("cameraY", client.getCameraY());
        cameraJSON.addProperty("cameraZ", client.getCameraZ());
        cameraJSON.addProperty("cameraPitch", client.getCameraPitch());
        cameraJSON.addProperty("cameraYaw", client.getCameraYaw());
        cameraJSON.addProperty("cameraScale", client.getScale());
        parent.getAsJsonArray("data").add(cameraJSON);
        return cameraJSON;
    }

    private JsonObject getPlayerModels(JsonObject parent) {
        JsonObject playerJSON = new JsonObject();
        Player p = client.getLocalPlayer();
        if (p == null) {
            return parent;
        }
        parent.getAsJsonArray("data").add(playerJSON);
        playerJSON.addProperty("type", "playermodelids");

        for (KitType kitType : KitType.values()) {
            int itemId = p.getPlayerComposition().getEquipmentId(kitType);
            if (itemId != -1) {
                playerJSON.addProperty(kitType.name(), itemId);
            }
        }

        return parent;
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event) {
        JsonObject data = new JsonObject();
        data.add("data", new JsonArray());
        if (config.sendPlayerData()) {
            getPlayerModels(data);
        }
        if (config.sendCamera()) {
            getCamera(data);
        }

        es.submit(() -> socketConnect(data.toString()));
//        System.out.println("ViewportXOffset == " + client.getViewportXOffset());
//        System.out.println("CameraX2 == " + client.getCameraX2());
//        System.out.println("CenterY == " + client.getCenterY());
//        System.out.println("RealDimensions == " + client.getRealDimensions());
//        System.out.println("3dZoom == " + client.get3dZoom());
//        System.out.println("3dZoom == " + client.getViewportYOffset());
//        System.out.println("cameraY2 == " + client.getCameraY2());
//        System.out.println("cameraY == " + client.getCameraY());
//        System.out.println("3dZoom == " + client.get3dZoom());
//        System.out.println("3dZoom == " + client.getScale());
//        System.out.println("3dZoom == " + client.getLocalPlayer());
//        System.out.println("3dZoom == " + client.getCachedPlayers());


    }


    private void socketConnect(String data) {
        try {
            String sURL = "http://localhost:8004/?data=" + URLEncoder.encode(data, "utf-8");

            URL url = new URL(sURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(6000);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

//            String line;
//            while ((line = in.readLine()) != null) {
//                System.out.println(line);
//            }
            in.close();
        } catch (
                IOException ioe) {
            ioe.printStackTrace();
        }
    }


}
