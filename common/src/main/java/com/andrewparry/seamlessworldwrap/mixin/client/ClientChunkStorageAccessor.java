package com.andrewparry.seamlessworldwrap.mixin.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkCache.Storage.class)
public interface ClientChunkStorageAccessor {

    @Accessor("chunks")
    AtomicReferenceArray<LevelChunk> seamless$chunks();

    @Accessor("chunkCount")
    int seamless$chunkCount();

    @Accessor("chunkCount")
    void seamless$setChunkCount(int count);

    @Invoker("getIndex")
    int seamless$getIndex(int x, int z);

    @Invoker("inRange")
    boolean seamless$inRange(int x, int z);
}
