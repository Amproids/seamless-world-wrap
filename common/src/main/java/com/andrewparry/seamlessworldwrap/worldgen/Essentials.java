package com.andrewparry.seamlessworldwrap.worldgen;

import com.andrewparry.seamlessworldwrap.SeamlessWorldWrap;
import com.andrewparry.seamlessworldwrap.WrapSettings;
import com.andrewparry.seamlessworldwrap.wrap.SeamWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;

/**
 * Makes small wrapped worlds beatable: worlds too small for a stronghold get an End portal room,
 * and Nethers too small for a fortress get a blaze spawner room. Both go in the world's center
 * chunk, which is generated once.
 */
public final class Essentials {

    /** Strongholds start about 1,280 blocks out; narrower than ~160 chunks, a world may have none. */
    private static final int STRONGHOLD_CHUNKS = 160;
    /** Fortresses are spread roughly one per 27 x 27 chunks, shared with bastions. */
    private static final int FORTRESS_CHUNKS = 64;

    private Essentials() {
    }

    public static void decorate(WorldGenLevel level, ChunkAccess chunk) {
        Level server = level.getLevel();
        WrapSettings wrap = SeamWrap.of(server);
        if (wrap == null || !chunk.getPos().equals(centerChunk(wrap))) {
            return;
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ 0x5EA3L);
        if (server.dimension() == Level.OVERWORLD && tooSmall(wrap, STRONGHOLD_CHUNKS)) {
            portalRoom(level, chunk.getPos(), random);
        } else if (server.dimension() == Level.NETHER && tooSmall(wrap, FORTRESS_CHUNKS)) {
            blazeRoom(level, chunk.getPos(), random);
        }
    }

    private static boolean tooSmall(WrapSettings wrap, int chunks) {
        return wrap.wrapsX() && wrap.widthChunks() < chunks || wrap.wrapsZ() && wrap.heightChunks() < chunks;
    }

    private static ChunkPos centerChunk(WrapSettings wrap) {
        int x = wrap.wrapsX() ? wrap.minX() + wrap.width() / 2 : 0;
        int z = wrap.wrapsZ() ? wrap.minZ() + wrap.height() / 2 : 0;
        return new ChunkPos(new BlockPos(x, 0, z));
    }

    /** An underground stronghold-style room: lava pool, 12 frames (10% chance of an eye each), silverfish spawner on the steps. */
    private static void portalRoom(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        int x0 = chunk.getMinBlockX() + 2;
        int z0 = chunk.getMinBlockZ() + 2;
        int y0 = Math.max(level.getMinBuildHeight() + 10, 20);
        // Shell 11 x 7 x 11 of stone bricks, hollow inside.
        for (int dx = 0; dx <= 10; dx++) {
            for (int dz = 0; dz <= 10; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    boolean shell = dx == 0 || dx == 10 || dz == 0 || dz == 10 || dy == 0 || dy == 6;
                    level.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), shell ? brick(random) : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // Raised platform with a lava pool under the portal, like the real room.
        int cx = x0 + 5;
        int cz = z0 + 6;
        int py = y0 + 1;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(new BlockPos(cx + dx, py, cz + dz), brick(random), 2);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(new BlockPos(cx + dx, py, cz + dz), Blocks.LAVA.defaultBlockState(), 2);
            }
        }
        int fy = py + 1;
        boolean inward = framesWork(level, cx, fy, cz, true);
        if (!inward && !framesWork(level, cx, fy, cz, false)) {
            SeamlessWorldWrap.LOG.warn("Portal room: neither frame orientation forms a portal; placing inward-facing frames");
            inward = true;
        }
        placeFrames(level, cx, fy, cz, inward, random);
        // Steps up to the frames, and the silverfish spawner the real room has.
        BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(new BlockPos(cx + dx, py, cz - 3), step, 2);
        }
        spawner(level, new BlockPos(cx, py, cz - 4), EntityType.SILVERFISH, random);
        SeamlessWorldWrap.LOG.info("Placed an End portal room at {}, {}, {}", cx, fy, cz);
    }

    /** Places all 12 frames with eyes and asks vanilla whether that's a complete portal, then clears them. */
    private static boolean framesWork(WorldGenLevel level, int cx, int y, int cz, boolean inward) {
        forEachFrame(cx, y, cz, inward, (pos, facing) -> level.setBlock(pos,
                Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, facing).setValue(EndPortalFrameBlock.HAS_EYE, true), 2));
        boolean works = EndPortalFrameBlock.getOrCreatePortalShape().find(level, new BlockPos(cx, y, cz)) != null;
        forEachFrame(cx, y, cz, inward, (pos, facing) -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
        return works;
    }

    private static void placeFrames(WorldGenLevel level, int cx, int y, int cz, boolean inward, RandomSource random) {
        forEachFrame(cx, y, cz, inward, (pos, facing) -> level.setBlock(pos,
                Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, facing)
                        .setValue(EndPortalFrameBlock.HAS_EYE, random.nextFloat() < 0.1F), 2));
    }

    private interface FrameAction {
        void at(BlockPos pos, Direction facing);
    }

    private static void forEachFrame(int cx, int y, int cz, boolean inward, FrameAction action) {
        for (int d = -1; d <= 1; d++) {
            action.at(new BlockPos(cx + d, y, cz - 2), inward ? Direction.SOUTH : Direction.NORTH);
            action.at(new BlockPos(cx + d, y, cz + 2), inward ? Direction.NORTH : Direction.SOUTH);
            action.at(new BlockPos(cx - 2, y, cz + d), inward ? Direction.EAST : Direction.WEST);
            action.at(new BlockPos(cx + 2, y, cz + d), inward ? Direction.WEST : Direction.EAST);
        }
    }

    /** A small nether-brick room with a blaze spawner, windows, and soul sand with nether wart. */
    private static void blazeRoom(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        int x0 = chunk.getMinBlockX() + 4;
        int z0 = chunk.getMinBlockZ() + 4;
        int y0 = 64;
        for (int dx = 0; dx <= 8; dx++) {
            for (int dz = 0; dz <= 8; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    boolean wall = dx == 0 || dx == 8 || dz == 0 || dz == 8;
                    boolean floorOrRoof = dy == 0 || dy == 5;
                    boolean window = wall && dy == 2 && (dx == 4 || dz == 4);
                    BlockState state = floorOrRoof || wall ? Blocks.NETHER_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (window) {
                        state = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x0 + dx, y0 + dy, z0 + dz), state, 2);
                }
            }
        }
        spawner(level, new BlockPos(x0 + 4, y0 + 1, z0 + 4), EntityType.BLAZE, random);
        for (int dx = 1; dx <= 2; dx++) {
            level.setBlock(new BlockPos(x0 + dx, y0, z0 + 1), Blocks.SOUL_SAND.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x0 + dx, y0 + 1, z0 + 1), Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 3), 2);
        }
        SeamlessWorldWrap.LOG.info("Placed a blaze spawner room at {}, {}, {}", x0 + 4, y0 + 1, z0 + 4);
    }

    private static void spawner(WorldGenLevel level, BlockPos pos, EntityType<?> type, RandomSource random) {
        level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, random);
        }
    }

    private static BlockState brick(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.2F) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (r < 0.5F) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }
}
