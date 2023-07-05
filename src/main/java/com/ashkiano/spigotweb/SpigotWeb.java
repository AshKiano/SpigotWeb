package com.ashkiano.spigotweb;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

//TODO zmenit port a jazyk prikazem
//TODO přidat překlad hlášek
//TODO přidat logování přístupů na web
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
}