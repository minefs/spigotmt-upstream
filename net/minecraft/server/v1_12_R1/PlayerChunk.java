// 
// Decompiled by Procyon v0.5.30
// 

package net.minecraft.server.v1_12_R1;

import org.apache.logging.log4j.LogManager;
import com.google.common.collect.Iterables;
import com.google.common.base.Predicate;
import java.util.Iterator;
import org.bukkit.craftbukkit.v1_12_R1.chunkio.ChunkIOExecutor;
import com.google.common.collect.Lists;
import javax.annotation.Nullable;
import java.util.List;
import org.apache.logging.log4j.Logger;

public class PlayerChunk
{
    private static final Logger a;
    private final PlayerChunkMap playerChunkMap;
    public final List<EntityPlayer> c;
    private final ChunkCoordIntPair location;
    private final short[] dirtyBlocks;
    @Nullable
    public Chunk chunk;
    private int dirtyCount;
    private int h;
    private long i;
    private boolean done;
    boolean chunkExists;
    private boolean loadInProgress;
    private Runnable loadedRunnable;
    
    public final void markChunkUsed() {
        if (this.chunk != null && this.chunk.scheduledForUnload != null) {
            this.chunk.scheduledForUnload = null;
        }
    }
    
    public PlayerChunk(final PlayerChunkMap playerchunkmap, final int i, final int j) {
        this.c = (List<EntityPlayer>)Lists.newArrayList();
        this.dirtyBlocks = new short[64];
        this.loadInProgress = false;
        this.loadedRunnable = new Runnable() {
            @Override
            public void run() {
                PlayerChunk.this.loadInProgress = false;
                PlayerChunk.this.chunk = PlayerChunk.this.playerChunkMap.getWorld().getChunkProviderServer().getOrLoadChunkAt(PlayerChunk.this.location.x, PlayerChunk.this.location.z);
                PlayerChunk.this.markChunkUsed();
            }
        };
        this.playerChunkMap = playerchunkmap;
        this.location = new ChunkCoordIntPair(i, j);
        this.loadInProgress = true;
        this.chunk = playerchunkmap.getWorld().getChunkProviderServer().getChunkAt(i, j, this.loadedRunnable, false);
        this.chunkExists = (this.chunk != null || ChunkIOExecutor.hasQueuedChunkLoad((World)this.playerChunkMap.getWorld(), i, j));
        this.markChunkUsed();
    }
    
    public ChunkCoordIntPair a() {
        return this.location;
    }
    
    public void a(final EntityPlayer entityplayer) {
        if (this.c.contains(entityplayer)) {
            PlayerChunk.a.debug("Failed to add player. {} already is in chunk {}, {}", (Object)entityplayer, (Object)this.location.x, (Object)this.location.z);
        }
        else {
            if (this.c.isEmpty()) {
                this.i = this.playerChunkMap.getWorld().getTime();
            }
            this.c.add(entityplayer);
            if (this.done) {
                this.sendChunk(entityplayer);
            }
        }
    }
    
    public void b(final EntityPlayer entityplayer) {
        if (this.c.contains(entityplayer)) {
            if (!this.done) {
                this.c.remove(entityplayer);
                if (this.c.isEmpty()) {
                    ChunkIOExecutor.dropQueuedChunkLoad((World)this.playerChunkMap.getWorld(), this.location.x, this.location.z, this.loadedRunnable);
                    this.playerChunkMap.b(this);
                }
                return;
            }
            if (this.done) {
                entityplayer.playerConnection.sendPacket((Packet)new PacketPlayOutUnloadChunk(this.location.x, this.location.z));
            }
            this.c.remove(entityplayer);
            if (this.c.isEmpty()) {
                this.playerChunkMap.b(this);
            }
        }
    }
    
    public boolean a(final boolean flag) {
        if (this.chunk != null) {
            return true;
        }
        if (!this.loadInProgress) {
            this.loadInProgress = true;
            this.chunk = this.playerChunkMap.getWorld().getChunkProviderServer().getChunkAt(this.location.x, this.location.z, this.loadedRunnable, flag);
            this.markChunkUsed();
        }
        return this.chunk != null;
    }
    
    public boolean b() {
        if (this.done) {
            return true;
        }
        if (this.chunk == null) {
            return false;
        }
        if (!this.chunk.isReady()) {
            return false;
        }
        if (!this.chunk.world.chunkPacketBlockController.onChunkPacketCreate(this.chunk, 65535, false)) {
            return false;
        }
        this.dirtyCount = 0;
        this.h = 0;
        this.done = true;
        final PacketPlayOutMapChunk packetplayoutmapchunk = new PacketPlayOutMapChunk(this.chunk, 65535);
        for (final EntityPlayer entityplayer : this.c) {
            entityplayer.playerConnection.sendPacket((Packet)packetplayoutmapchunk);
            this.playerChunkMap.getWorld().getTracker().a(entityplayer, this.chunk);
        }
        return true;
    }
    
    public void sendChunk(final EntityPlayer entityplayer) {
        if (this.done) {
            this.chunk.world.chunkPacketBlockController.onChunkPacketCreate(this.chunk, 65535, true);
            entityplayer.playerConnection.sendPacket((Packet)new PacketPlayOutMapChunk(this.chunk, 65535));
            this.playerChunkMap.getWorld().getTracker().a(entityplayer, this.chunk);
        }
    }
    
    public void c() {
        final long i = this.playerChunkMap.getWorld().getTime();
        if (this.chunk != null) {
            this.chunk.c(this.chunk.x() + i - this.i);
        }
        this.i = i;
    }
    
    public void a(final int i, final int j, final int k) {
        if (this.done) {
            if (this.dirtyCount == 0) {
                this.playerChunkMap.a(this);
            }
            this.h |= 1 << (j >> 4);
            if (this.dirtyCount < 64) {
                final short short0 = (short)(i << 12 | k << 8 | j);
                for (int l = 0; l < this.dirtyCount; ++l) {
                    if (this.dirtyBlocks[l] == short0) {
                        return;
                    }
                }
                this.dirtyBlocks[this.dirtyCount++] = short0;
            }
        }
    }
    
    public void a(final Packet<?> packet) {
        if (this.done) {
            for (int i = 0; i < this.c.size(); ++i) {
                this.c.get(i).playerConnection.sendPacket((Packet)packet);
            }
        }
    }
    
    public void d() {
        if (this.done && this.chunk != null && this.dirtyCount != 0) {
            if (this.dirtyCount == 1) {
                final int i = (this.dirtyBlocks[0] >> 12 & 0xF) + this.location.x * 16;
                final int j = this.dirtyBlocks[0] & 0xFF;
                final int k = (this.dirtyBlocks[0] >> 8 & 0xF) + this.location.z * 16;
                final BlockPosition blockposition = new BlockPosition(i, j, k);
                this.a((Packet<?>)new PacketPlayOutBlockChange((World)this.playerChunkMap.getWorld(), blockposition));
                if (this.playerChunkMap.getWorld().getType(blockposition).getBlock().isTileEntity()) {
                    this.a(this.playerChunkMap.getWorld().getTileEntity(blockposition));
                }
            }
            else if (this.dirtyCount == 64) {
                this.a((Packet<?>)new PacketPlayOutMapChunk(this.chunk, this.h));
            }
            else {
                this.a((Packet<?>)new PacketPlayOutMultiBlockChange(this.dirtyCount, this.dirtyBlocks, this.chunk));
                for (int i = 0; i < this.dirtyCount; ++i) {
                    final int j = (this.dirtyBlocks[i] >> 12 & 0xF) + this.location.x * 16;
                    final int k = this.dirtyBlocks[i] & 0xFF;
                    final int l = (this.dirtyBlocks[i] >> 8 & 0xF) + this.location.z * 16;
                    final BlockPosition blockposition2 = new BlockPosition(j, k, l);
                    if (this.playerChunkMap.getWorld().getType(blockposition2).getBlock().isTileEntity()) {
                        this.a(this.playerChunkMap.getWorld().getTileEntity(blockposition2));
                    }
                }
            }
            this.dirtyCount = 0;
            this.h = 0;
        }
    }
    
    private void a(@Nullable final TileEntity tileentity) {
        if (tileentity != null) {
            final PacketPlayOutTileEntityData packetplayouttileentitydata = tileentity.getUpdatePacket();
            if (packetplayouttileentitydata != null) {
                this.a((Packet<?>)packetplayouttileentitydata);
            }
        }
    }
    
    public boolean d(final EntityPlayer entityplayer) {
        return this.c.contains(entityplayer);
    }
    
    public boolean a(final Predicate<EntityPlayer> predicate) {
        return Iterables.tryFind((Iterable)this.c, (Predicate)predicate).isPresent();
    }
    
    public boolean a(final double d0, final Predicate<EntityPlayer> predicate) {
        for (int i = 0, j = this.c.size(); i < j; ++i) {
            final EntityPlayer entityplayer = this.c.get(i);
            if (predicate.apply((Object)entityplayer) && this.location.a((Entity)entityplayer) < d0 * d0) {
                return true;
            }
        }
        return false;
    }
    
    public boolean e() {
        return this.done;
    }
    
    @Nullable
    public Chunk f() {
        return this.chunk;
    }
    
    public double g() {
        double d0 = Double.MAX_VALUE;
        for (final EntityPlayer entityplayer : this.c) {
            final double d2 = this.location.a((Entity)entityplayer);
            if (d2 < d0) {
                d0 = d2;
            }
        }
        return d0;
    }
    
    static {
        a = LogManager.getLogger();
    }
}
