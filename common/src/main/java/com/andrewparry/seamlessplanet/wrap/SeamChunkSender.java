package com.andrewparry.seamlessplanet.wrap;

/** Added to PlayerChunkSender: pending chunks are tracked per alias (client-frame position). */
public interface SeamChunkSender {

    void seamless$markAlias(long aliasPos);

    boolean seamless$isAliasPending(long aliasPos);
}
