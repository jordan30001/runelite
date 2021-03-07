package net.runelite.client.plugins.blender;

import com.google.inject.Injector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.api.events.BeforeRender;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.devtools.DevToolsPlugin;
import net.runelite.client.ui.overlay.OverlayPosition;

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

@PluginDescriptor(name = "Blender", description = "Blender plugin", tags = {"Blender"})
public class BlenderPlugin extends Plugin {
    @Inject
    private Client client;

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }

    private Map<String, Object> getCamera() {
        Map<String, Object> map = new HashMap<>();

        map.put("type", "camerapositionupdate");
        map.put("cameraX", client.getCameraX());
        map.put("cameraY", client.getCameraY());
        map.put("cameraZ", client.getCameraZ());
        map.put("cameraPitch", client.getCameraPitch());
        map.put("cameraYaw", client.getCameraYaw());
        map.put("cameraScale", client.getScale());
        return map;
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event) {
        socketConnect(getCamera());
    }


    private void socketConnect(Map<String, Object> params) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("http://localhost:5000/?");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue().toString(), "utf-8")).append("&");
            }
            sb.setLength(sb.length() - 1);

            URL url = new URL(sb.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
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
