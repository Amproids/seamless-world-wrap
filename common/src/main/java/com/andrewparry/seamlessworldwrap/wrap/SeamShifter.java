package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.EnumSet;

/**
 * Moves an entity that has wandered past a seam back into the canonical range. Runs between
 * ticks, never in the middle of one, so code that works out a move over several steps (minecart
 * rails, pathfinding, collisions) always sees continuous coordinates. Passengers move with their
 * vehicle. Players are teleported by exactly one world size (relative, so speed and facing are
 * kept); the chunks around where they land were already sent (SeamTrackingView).
 */
public final class SeamShifter {

    private SeamShifter() {
    }

    /** Shifts {@code root} and its passengers into range if needed. Returns true if anything moved. */
    public static boolean shiftIntoRange(Entity root, WrapSettings wrap) {
        int sx = wrap.shiftIntoRangeX(root.getX());
        int sz = wrap.shiftIntoRangeZ(root.getZ());
        if (sx == 0 && sz == 0) {
            return false;
        }
        shift(root, sx, sz);
        return true;
    }

    private static void shift(Entity entity, int sx, int sz) {
        if (entity instanceof ServerPlayer player && player.connection != null) {
            player.connection.teleport(player.getX() - sx, player.getY(), player.getZ() - sz,
                    player.getYRot(), player.getXRot(), EnumSet.allOf(RelativeMovement.class));
            for (Entity passenger : entity.getPassengers()) {
                shift(passenger, sx, sz);
            }
            return;
        }
        entity.setPos(entity.getX() - sx, entity.getY(), entity.getZ() - sz);
        entity.xo -= sx;
        entity.xOld -= sx;
        entity.zo -= sz;
        entity.zOld -= sz;
        if (entity instanceof Mob mob) {
            // Path nodes and move targets are on the other side of the seam now; pick new ones.
            mob.getNavigation().stop();
            if (mob.getMoveControl().hasWanted()) {
                mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
            }
        }
        for (Entity passenger : entity.getPassengers()) {
            shift(passenger, sx, sz);
        }
        // A vehicle the client drives (boat, horse) is positioned by that client: tell it the new spot.
        if (entity.getControllingPassenger() instanceof ServerPlayer driver && driver.connection != null) {
            driver.connection.send(new ClientboundMoveVehiclePacket(entity));
        }
    }
}
