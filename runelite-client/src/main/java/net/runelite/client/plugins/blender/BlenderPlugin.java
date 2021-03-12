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
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
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
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static net.runelite.api.GameState.*;

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

    private ServerSocket server;
    private ExecutorService serverThread;
    private ArrayBlockingQueue<JsonObject> dataToSend;
    private Future<Object> serverFuture;
    private boolean stop = true;


    @Override
    protected void startUp() throws Exception {
        server = PortAvailable(config.getPortNumber());
        if (server == null) return;
        dataToSend = new ArrayBlockingQueue<>(10);
        serverThread = Executors.newFixedThreadPool(1);
        serverFuture = serverThread.submit(runServer());
    }

    public static final ServerSocket PortAvailable(int port) {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
            return socket;
        } catch (IOException ioe) {
            log.info(String.format("blender port (%d) already in use", port));
            try {
                if (socket != null) socket.close();
            } catch (IOException ioe2) {

            }
        }
        return null;
    }

    public Callable<Object> runServer() {
        return () -> {
            while (true) {
                try (Socket blenderClient = server.accept();) {
                    JsonObject jsonObj = dataToSend.peek();
                    if (jsonObj != null) jsonObj = dataToSend.poll();
                    else {
                        blenderClient.close();
                        continue;
                    }
                    OutputStream os = blenderClient.getOutputStream();
                    os.write(jsonObj.toString().getBytes("utf-8"));
                    if (Thread.currentThread().isInterrupted()) return null;
                } catch (Throwable t) {
                    if (t instanceof InterruptedException || Thread.currentThread().isInterrupted()) return null;
                    else if (t instanceof SocketException) {
                        t.printStackTrace();
                        continue;
                    } else {
                        t.printStackTrace();
                        throw t;
                    }
                }
            }
        };
    }

    @Override
    protected void shutDown() throws Exception {

        if (serverFuture != null) serverFuture.cancel(true);
        if (serverThread != null) serverThread.shutdownNow();
        try {
            if (server != null) server.close();
        } catch (Exception e) {
        }
        if (serverThread != null) serverThread.awaitTermination(5, TimeUnit.SECONDS);
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
        playerJSON.addProperty("type", "playermodelids");

        for (KitType kitType : KitType.values()) {
            if(p.getPlayerComposition() == null) {
                return parent;
            }
            int itemId = p.getPlayerComposition().getEquipmentId(kitType);
            if (itemId != -1) {
                playerJSON.addProperty(kitType.name(), itemId);
            }
        }

        parent.getAsJsonArray("data").add(playerJSON);
        return parent;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event){
        switch(event.getGameState()){
            case UNKNOWN:
            case STARTING:
            case LOGIN_SCREEN:
            case LOGIN_SCREEN_AUTHENTICATOR:
            case LOGGING_IN:
            case LOADING:
            case CONNECTION_LOST:
            case HOPPING:
                stop = true;
                break;
            case LOGGED_IN:
                stop = false;
                break;
        }
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event) {
        if(stop) return;
        JsonObject data = new JsonObject();
        data.add("data", new JsonArray());
        if (config.sendPlayerData()) {
            getPlayerModels(data);
        }
        if (config.sendCamera()) {
            getCamera(data);
        }

        dataToSend.offer(data);
//        System.out.println("".format("Real Dimensions = %s", client.getViewportHeight()));
//        System.out.println("".format("Real Dimensions = %s", client.getViewportWidth()));
//        System.out.println("".format("Real Dimensions = %s", client.getViewportXOffset()));
//        System.out.println("".format("Real Dimensions = %s", client.getViewportYOffset()));
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

}
