/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_SPAWN)
public class CommandClaimSpawn extends BaseCommand {

    @CommandAlias("claimspawn")
    @Description("Teleports you to claim spawn if available.")
    @Syntax("[name] [user]")
    @Subcommand("claim spawn")
    public void execute(Player player, @Optional String claimName, @Optional User targetPlayer) {
        final GDPlayerData srcPlayerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDPlayerData targetPlayerData = null;
        if (targetPlayer != null) {
            targetPlayerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), targetPlayer.getUniqueId());
        } else {
            targetPlayerData = srcPlayerData;
        }

        GDClaim claim = null;
        if (claimName != null) {
            for (Claim playerClaim : targetPlayerData.getInternalClaims()) {
                String name = null;
                Component component = playerClaim.getName().orElse(null);
                if (component != null) {
                    name = PlainComponentSerializer.INSTANCE.serialize(component);
                    if (claimName.equalsIgnoreCase(name)) {
                        claim = (GDClaim) playerClaim;
                        break;
                    }
                }
            }
            if (claim == null) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_CLAIMNAME_NOT_FOUND,
                        ImmutableMap.of("name", claimName)));
                return;
            }
        } else {
            claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(targetPlayerData, player.getLocation());
        }

        if (!srcPlayerData.canIgnoreClaim(claim) && !claim.isUserTrusted(player, TrustTypes.ACCESSOR) && !player.hasPermission(GDPermissions.COMMAND_DELETE_CLAIMS)) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ACCESS,
                    ImmutableMap.of("player", claim.getOwnerName())));
            return;
        }

        final Vector3i spawnPos = claim.getData().getSpawnPos().orElse(null);
        if (spawnPos == null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().SPAWN_NOT_SET);
            return;
        }

        final Location<World> spawnLocation = new Location<>(claim.getWorld(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        int teleportDelay = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.PLAYER_TELEPORT_DELAY, claim);
        if (teleportDelay > 0) {
            srcPlayerData.teleportDelay = teleportDelay + 1;
            srcPlayerData.teleportLocation = spawnLocation;
            return;
        }
        player.setLocation(spawnLocation);
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.SPAWN_TELEPORT,
                ImmutableMap.of(
                "location", spawnPos));
        GriefDefenderPlugin.sendMessage(player, message);
    }
}
