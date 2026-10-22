package net.nyana.fluid.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.nyana.fluid.api.NyanaFluid;
import net.nyana.fluid.api.event.NyanaFluidReadyEvent;
import net.nyana.fluid.api.internal.Core;
import net.nyana.fluid.platform.ce269.SoftTickerBridge;
import net.nyana.fluid.plugin.builtin.Builtins;
import net.nyana.fluid.plugin.ce.CeRegistrations;
import net.nyana.fluid.plugin.listener.FollowUpUseGuard;
import net.nyana.fluid.plugin.platform.PaperPlatformInfo;
import net.nyana.fluid.plugin.platform.PluginBridge;
import net.nyana.fluid.plugin.platform.PluginBridges;
import net.nyana.fluid.plugin.render.ResourceGenerator;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class Main extends JavaPlugin implements Listener {
    private @Nullable PluginService service;
    private @Nullable PluginBridge bridge;
    private @Nullable ResourceGenerator resources;
    private @Nullable Consumer<net.nyana.fluid.api.fluid.FluidRegistryChange> registryListener;

    @Override
    public void onLoad() {
        this.resources = CeRegistrations.register(this);
    }

    @Override
    public void onEnable() {
        PaperPlatformInfo info = PaperPlatformInfo.detect(CraftEngine.class.getClassLoader());
        PluginBridge bridge = PluginBridges.select(this, info);
        this.bridge = bridge;
        PluginService installed = new PluginService(this, info, bridge);
        this.service = installed;
        NyanaFluid.install(installed);
        this.registryListener = installed::onRegistryChange;
        Core.get().fluids.addListener(change -> {
            Consumer<net.nyana.fluid.api.fluid.FluidRegistryChange> listener = this.registryListener;
            if (listener != null) listener.accept(change);
        });

        Builtins.register(this, bridge.vanilla());
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new FollowUpUseGuard(), this);
        if (this.resources != null) Bukkit.getPluginManager().registerEvents(this.resources, this);

        bridge.scheduler().runGlobal(() -> {
            installed.setReady(true);
            Bukkit.getPluginManager().callEvent(new NyanaFluidReadyEvent());
        });

        Bukkit.getConsoleSender().sendMessage(Component.empty());
        Bukkit.getConsoleSender().sendMessage(Component.text("Ciallo～(∠・ω< )⌒★", NamedTextColor.YELLOW));
        Bukkit.getConsoleSender().sendMessage(Component.text("nyana-fluid 由 ChengZhiMeow 开发", NamedTextColor.YELLOW));
        Bukkit.getConsoleSender().sendMessage(Component.text("“在意的话，会让眼前的幸福逃走的，傻子才会在意。”", NamedTextColor.YELLOW));
        Bukkit.getConsoleSender().sendMessage(Component.empty());
    }

    @Override
    public void onDisable() {
        this.registryListener = null;
        if (this.service != null) {
            this.service.setReady(false);
            NyanaFluid.uninstall(this.service);
            this.service = null;
        }
        Core.get().registrations.closeAll(this);
        if (this.bridge != null && this.bridge.ticker() instanceof SoftTickerBridge soft) soft.shutdown();
        this.bridge = null;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabled = event.getPlugin();
        if (disabled != this) Core.get().registrations.closeAll(disabled);
    }
}
