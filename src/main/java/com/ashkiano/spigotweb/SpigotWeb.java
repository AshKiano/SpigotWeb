package com.ashkiano.spigotweb;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//TODO zmenit port a jazyk prikazem
//TODO přidat překlad hlášek
//TODO upravit logování přístupů na web
public class SpigotWeb extends JavaPlugin {
    private Server server;
    private int port;
    private String reloadPermission;

    // Messages
    private String reloadMessage;
    private String noPermissionMessage;
    private String serverStartedMessage;
    private String serverStoppedMessage;

    @Override
    public void onEnable() {
        // Save the default configuration if it does not exist
        this.saveDefaultConfig();
        this.reloadConfig();
        // Get the messages from the configuration file
        loadMessages();
        // Get the reload permission from the configuration file
        reloadPermission = this.getConfig().getString("reload-permission", "spigotweb.reload");
        // Call the server setup method
        setupServer();

        this.getLogger().info("Thank you for using the SpigotWeb plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://paypal.me/josefvyskocil");

        checkForUpdates();
    }

    @Override
    public void onDisable() {
        try {
            if (server != null) {
                // Stop the server if it is running
                server.stop();
                getLogger().info(serverStoppedMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spigotwebreload")) {
            // Check if the sender has the specified reload permission
            if (!(sender instanceof Player) || ((Player) sender).hasPermission(reloadPermission)) {
                this.reloadConfig();
                // Update the messages from the configuration file
                loadMessages();
                // Update the reload permission from the configuration file
                reloadPermission = this.getConfig().getString("reload-permission", "spigotweb.reload");
                port = this.getConfig().getInt("port");
                setupServer();
                sender.sendMessage(reloadMessage);
            } else {
                sender.sendMessage(noPermissionMessage);
            }
            return true;
        }

        return false;
    }

    private void loadMessages() {
        reloadMessage = this.getConfig().getString("reload-message", "Configuration has been reloaded and server has been restarted.");
        noPermissionMessage = this.getConfig().getString("no-permission-message", "You do not have permission to use this command.");
        serverStartedMessage = this.getConfig().getString("server-started-message", "Server started on ");
        serverStoppedMessage = this.getConfig().getString("server-stopped-message", "Server stopped");
    }

    private void setupServer() {
        // Save the default configuration if it does not exist
        this.saveDefaultConfig();

        // Create a new metrics object with this plugin instance and the plugin ID
        Metrics metrics = new Metrics(this, 18807);

        // Get the port from the configuration file
        port = this.getConfig().getInt("port");

        try {
            if (server != null) {
                // Stop the server if it is running
                server.stop();
            }

            // Create a new Jetty server instance with the specified port
            server = new Server(port);

            // Create a servlet context handler with session support
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            // Create the www directory if it doesn't exist
            File wwwDir = new File("www");
            if (!wwwDir.exists()) {
                wwwDir.mkdir();
            }

            // Create the index.html file in the www directory with "Hello World" content if it doesn't exist
            File indexFile = new File(wwwDir, "index.html");
            if (!indexFile.exists()) {
                try (FileWriter writer = new FileWriter(indexFile)) {
                    writer.write("<h1>Hello world</h1>");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Set the resource base to the absolute path of the www directory
            context.setResourceBase(wwwDir.getAbsolutePath());

            // Add a default servlet to handle requests and enable directory listing
            ServletHolder holder = context.addServlet(DefaultServlet.class, "/");
            holder.setInitParameter("dirAllowed", "true");

            // Add a logging servlet to handle requests
            ServletHolder loggingHolder = context.addServlet(LoggingServlet.class, "/*");
            loggingHolder.setInitParameter("dirAllowed", "true");

            server.setHandler(context);

            // Start the server
            server.start();

            // Get the public IP address
            URL ipify = new URL("https://api.ipify.org?format=txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(ipify.openStream()));
            String ip = in.readLine();
            in.close();

            getLogger().info(serverStartedMessage + ip + ":" + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class LoggingServlet extends DefaultServlet {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            System.out.println("Received request from " + req.getRemoteAddr());
            super.service(req, resp);
        }
    }

    private void checkForUpdates() {
        try {
            String pluginName = this.getDescription().getName();
            URL url = new URL("https://www.ashkiano.com/version_check.php?plugin=" + pluginName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("error")) {
                    this.getLogger().warning("Error when checking for updates: " + jsonObject.getString("error"));
                } else {
                    String latestVersion = jsonObject.getString("latest_version");

                    String currentVersion = this.getDescription().getVersion();
                    if (currentVersion.equals(latestVersion)) {
                        this.getLogger().info("This plugin is up to date!");
                    } else {
                        this.getLogger().warning("There is a newer version (" + latestVersion + ") available! Please update!");
                    }
                }
            } else {
                this.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
            }
        } catch (Exception e) {
            this.getLogger().warning("Failed to check for updates. Error: " + e.getMessage());
        }
    }
}