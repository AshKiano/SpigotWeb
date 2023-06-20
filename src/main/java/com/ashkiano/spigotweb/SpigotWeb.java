package com.ashkiano.spigotweb;

import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;

public class SpigotWeb extends JavaPlugin {
    private Server server;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        Metrics metrics = new Metrics(this, 18807);

        // Get the port from the configuration file
        int port = this.getConfig().getInt("port");

        // Create a new Jetty server instance with the specified port
        server = new Server(port);

        // Create a servlet context handler and set the resource base to the "www" directory
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(new File("www").getAbsolutePath());

        // Add a default servlet to handle requests
        ServletHolder holder = context.addServlet(DefaultServlet.class, "/");
        holder.setInitParameter("dirAllowed", "true");
        server.setHandler(context);

        try {
            // Start the server
            server.start();
            getLogger().info("Server started on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            if (server != null) {
                // Stop the server if it is running
                server.stop();
                getLogger().info("Server stopped");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}