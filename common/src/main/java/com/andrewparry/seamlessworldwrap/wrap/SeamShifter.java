package com.andrewparry.seamlessworldwrap.wrap;

import com.andrewparry.seamlessworldwrap.WrapSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Moves an entity that has wandered past a seam back into the canonical range. Runs between
 * ticks, never in the middle of one, so code that works out a move over several steps (minecart
 * rails, pathfinding, collisions) always sees continuous coordinates. Passengers move with their
 * vehicle; players also have their client offset adjusted so their client sees no jump.
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
        if (entity instanceof ServerPlayer player) {
            SeamPlayer seam = (SeamPlayer) player;
            seam.seamless$setOffset(seam.seamless$offsetX() + sx, seam.seamless$offsetZ() + sz);
            if (player.connection != null) {
                ((SeamListener) player.connection).seamless$shiftRemembered(sx, sz);
            }
        }
        for (Entity passenger : entity.getPassengers()) {
            shift(passenger, sx, sz);
        }
        if (entity instanceof ServerPlayer player) {
            player.serverLevel().getChunkSource().move(player);
        }
    }
}
