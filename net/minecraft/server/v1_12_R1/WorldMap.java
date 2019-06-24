// 
// Decompiled by Procyon v0.5.30
// 

package net.minecraft.server.v1_12_R1;

import org.bukkit.map.MapCursor;
import java.util.ArrayList;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.map.CraftMapRenderer;
import org.bukkit.map.MapRenderer;
import org.bukkit.entity.Player;
import java.util.Collection;
import javax.annotation.Nullable;
import com.google.common.base.Charsets;
import java.util.Iterator;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.Bukkit;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.map.CraftMapView;
import org.bukkit.craftbukkit.v1_12_R1.map.RenderData;
import java.util.UUID;
import java.util.Map;
import java.util.List;

public class WorldMap extends PersistentBase
{
    public int centerX;
    public int centerZ;
    public byte map;
    public boolean track;
    public boolean unlimitedTracking;
    public byte scale;
    public byte[] colors;
    public List<WorldMapHumanTracker> i;
    public final Map<EntityHuman, WorldMapHumanTracker> k;
    public Map<UUID, MapIcon> decorations;
    private RenderData vanillaRender;
    public final CraftMapView mapView;
    private CraftServer server;
    private UUID uniqueId;
    
    public WorldMap(final String s) {
        super(s);
        this.colors = new byte[16384];
        this.i = (List<WorldMapHumanTracker>)Lists.newArrayList();
        this.k = (Map<EntityHuman, WorldMapHumanTracker>)Maps.newHashMap();
        this.decorations = (Map<UUID, MapIcon>)Maps.newLinkedHashMap();
        this.vanillaRender = new RenderData();
        this.uniqueId = null;
        this.mapView = new CraftMapView(this);
        this.server = (CraftServer)Bukkit.getServer();
        this.vanillaRender.buffer = this.colors;
    }
    
    public void a(final double d0, final double d1, final int i) {
        final int j = 128 * (1 << i);
        final int k = MathHelper.floor((d0 + 64.0) / j);
        final int l = MathHelper.floor((d1 + 64.0) / j);
        this.centerX = k * j + j / 2 - 64;
        this.centerZ = l * j + j / 2 - 64;
    }
    
    public void a(final NBTTagCompound nbttagcompound) {
        byte dimension = nbttagcompound.getByte("dimension");
        if (dimension >= 10) {
            final long least = nbttagcompound.getLong("UUIDLeast");
            final long most = nbttagcompound.getLong("UUIDMost");
            if (least != 0L && most != 0L) {
                this.uniqueId = new UUID(most, least);
                final CraftWorld world = (CraftWorld)this.server.getWorld(this.uniqueId);
                if (world == null) {
                    dimension = 127;
                }
                else {
                    dimension = (byte)world.getHandle().dimension;
                }
            }
        }
        this.map = dimension;
        this.centerX = nbttagcompound.getInt("xCenter");
        this.centerZ = nbttagcompound.getInt("zCenter");
        this.scale = nbttagcompound.getByte("scale");
        this.scale = (byte)MathHelper.clamp((int)this.scale, 0, 4);
        if (nbttagcompound.hasKeyOfType("trackingPosition", 1)) {
            this.track = nbttagcompound.getBoolean("trackingPosition");
        }
        else {
            this.track = true;
        }
        this.unlimitedTracking = nbttagcompound.getBoolean("unlimitedTracking");
        final short short0 = nbttagcompound.getShort("width");
        final short short2 = nbttagcompound.getShort("height");
        if (short0 == 128 && short2 == 128) {
            this.colors = nbttagcompound.getByteArray("colors");
        }
        else {
            final byte[] abyte = nbttagcompound.getByteArray("colors");
            this.colors = new byte[16384];
            final int i = (128 - short0) / 2;
            final int j = (128 - short2) / 2;
            for (int k = 0; k < short2; ++k) {
                final int l = k + j;
                if (l >= 0 || l < 128) {
                    for (int i2 = 0; i2 < short0; ++i2) {
                        final int j2 = i2 + i;
                        if (j2 >= 0 || j2 < 128) {
                            this.colors[j2 + l * 128] = abyte[i2 + k * short0];
                        }
                    }
                }
            }
        }
        this.vanillaRender.buffer = this.colors;
    }
    
    public NBTTagCompound b(final NBTTagCompound nbttagcompound) {
        if (this.map >= 10) {
            if (this.uniqueId == null) {
                for (final World world : this.server.getWorlds()) {
                    final CraftWorld cWorld = (CraftWorld)world;
                    if (cWorld.getHandle().dimension == this.map) {
                        this.uniqueId = cWorld.getUID();
                        break;
                    }
                }
            }
            if (this.uniqueId != null) {
                nbttagcompound.setLong("UUIDLeast", this.uniqueId.getLeastSignificantBits());
                nbttagcompound.setLong("UUIDMost", this.uniqueId.getMostSignificantBits());
            }
        }
        nbttagcompound.setByte("dimension", this.map);
        nbttagcompound.setInt("xCenter", this.centerX);
        nbttagcompound.setInt("zCenter", this.centerZ);
        nbttagcompound.setByte("scale", this.scale);
        nbttagcompound.setShort("width", (short)128);
        nbttagcompound.setShort("height", (short)128);
        nbttagcompound.setByteArray("colors", this.colors);
        nbttagcompound.setBoolean("trackingPosition", this.track);
        nbttagcompound.setBoolean("unlimitedTracking", this.unlimitedTracking);
        return nbttagcompound;
    }
    
    public void updateSeenPlayers(final EntityHuman entityhuman, final ItemStack itemstack) {
        this.a(entityhuman, itemstack);
    }
    
    public void a(final EntityHuman entityhuman, final ItemStack itemstack) {
        if (!this.k.containsKey(entityhuman)) {
            final WorldMapHumanTracker worldmap_worldmaphumantracker = new WorldMapHumanTracker(entityhuman);
            this.k.put(entityhuman, worldmap_worldmaphumantracker);
            this.i.add(worldmap_worldmaphumantracker);
        }
        if (!entityhuman.inventory.h(itemstack)) {
            this.decorations.remove(entityhuman.getUniqueID());
        }
        for (int i = 0; i < this.i.size(); ++i) {
            final WorldMapHumanTracker worldmap_worldmaphumantracker2 = this.i.get(i);
            if (!worldmap_worldmaphumantracker2.trackee.dead && (worldmap_worldmaphumantracker2.trackee.inventory.h(itemstack) || itemstack.z())) {
                if (!itemstack.z() && worldmap_worldmaphumantracker2.trackee.dimension == this.map && this.track) {
                    this.a(MapIcon.Type.PLAYER, worldmap_worldmaphumantracker2.trackee.world, worldmap_worldmaphumantracker2.trackee.getUniqueID(), worldmap_worldmaphumantracker2.trackee.locX, worldmap_worldmaphumantracker2.trackee.locZ, worldmap_worldmaphumantracker2.trackee.yaw);
                }
            }
            else {
                this.k.remove(worldmap_worldmaphumantracker2.trackee);
                this.i.remove(worldmap_worldmaphumantracker2);
            }
        }
        if (itemstack.z() && this.track) {
            final EntityItemFrame entityitemframe = itemstack.A();
            final BlockPosition blockposition = entityitemframe.getBlockPosition();
            this.a(MapIcon.Type.FRAME, entityhuman.world, UUID.nameUUIDFromBytes(("frame-" + entityitemframe.getId()).getBytes(Charsets.US_ASCII)), blockposition.getX(), blockposition.getZ(), entityitemframe.direction.get2DRotationValue() * 90);
        }
        if (itemstack.hasTag() && itemstack.getTag().hasKeyOfType("Decorations", 9)) {
            final NBTTagList nbttaglist = itemstack.getTag().getList("Decorations", 10);
            for (int j = 0; j < nbttaglist.size(); ++j) {
                final NBTTagCompound nbttagcompound = nbttaglist.get(j);
                final UUID uuid = UUID.nameUUIDFromBytes(nbttagcompound.getString("id").getBytes(Charsets.US_ASCII));
                if (!this.decorations.containsKey(uuid)) {
                    this.a(MapIcon.Type.a(nbttagcompound.getByte("type")), entityhuman.world, uuid, nbttagcompound.getDouble("x"), nbttagcompound.getDouble("z"), nbttagcompound.getDouble("rot"));
                }
            }
        }
    }
    
    public static void a(final ItemStack itemstack, final BlockPosition blockposition, final String s, final MapIcon.Type mapicon_type) {
        NBTTagList nbttaglist;
        if (itemstack.hasTag() && itemstack.getTag().hasKeyOfType("Decorations", 9)) {
            nbttaglist = itemstack.getTag().getList("Decorations", 10);
        }
        else {
            nbttaglist = new NBTTagList();
            itemstack.a("Decorations", (NBTBase)nbttaglist);
        }
        final NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setByte("type", mapicon_type.a());
        nbttagcompound.setString("id", s);
        nbttagcompound.setDouble("x", blockposition.getX());
        nbttagcompound.setDouble("z", blockposition.getZ());
        nbttagcompound.setDouble("rot", 180.0);
        nbttaglist.add((NBTBase)nbttagcompound);
        if (mapicon_type.c()) {
            final NBTTagCompound nbttagcompound2 = itemstack.c("display");
            nbttagcompound2.setInt("MapColor", mapicon_type.d());
        }
    }
    
    private void a(MapIcon.Type mapicon_type, final net.minecraft.server.v1_12_R1.World world, final UUID s, final double d0, final double d1, double d2) {
        final int i = 1 << this.scale;
        final float f = (float)(d0 - this.centerX) / i;
        final float f2 = (float)(d1 - this.centerZ) / i;
        byte b0 = (byte)(f * 2.0f + 0.5);
        byte b2 = (byte)(f2 * 2.0f + 0.5);
        final boolean flag = true;
        byte b3;
        if (f >= -63.0f && f2 >= -63.0f && f <= 63.0f && f2 <= 63.0f) {
            d2 += ((d2 < 0.0) ? -8.0 : 8.0);
            b3 = (byte)(d2 * 16.0 / 360.0);
            if (this.map < 0) {
                final int j = (int)(world.getWorldData().getDayTime() / 10L);
                b3 = (byte)(j * j * 34187121 + j * 121 >> 15 & 0xF);
            }
        }
        else {
            if (mapicon_type != MapIcon.Type.PLAYER) {
                this.decorations.remove(s);
                return;
            }
            final boolean flag2 = true;
            if (Math.abs(f) < 320.0f && Math.abs(f2) < 320.0f) {
                mapicon_type = MapIcon.Type.PLAYER_OFF_MAP;
            }
            else {
                if (!this.unlimitedTracking) {
                    this.decorations.remove(s);
                    return;
                }
                mapicon_type = MapIcon.Type.PLAYER_OFF_LIMITS;
            }
            b3 = 0;
            if (f <= -63.0f) {
                b0 = -128;
            }
            if (f2 <= -63.0f) {
                b2 = -128;
            }
            if (f >= 63.0f) {
                b0 = 127;
            }
            if (f2 >= 63.0f) {
                b2 = 127;
            }
        }
        this.decorations.put(s, new MapIcon(mapicon_type, b0, b2, b3));
    }
    
    @Nullable
    public Packet<?> a(final ItemStack itemstack, final net.minecraft.server.v1_12_R1.World world, final EntityHuman entityhuman) {
        final WorldMapHumanTracker worldmap_worldmaphumantracker = this.k.get(entityhuman);
        return (worldmap_worldmaphumantracker == null) ? null : worldmap_worldmaphumantracker.a(itemstack);
    }
    
    public void flagDirty(final int i, final int j) {
        super.c();
        for (final WorldMapHumanTracker worldmap_worldmaphumantracker : this.i) {
            worldmap_worldmaphumantracker.a(i, j);
        }
    }
    
    public WorldMapHumanTracker a(final EntityHuman entityhuman) {
        WorldMapHumanTracker worldmap_worldmaphumantracker = this.k.get(entityhuman);
        if (worldmap_worldmaphumantracker == null) {
            worldmap_worldmaphumantracker = new WorldMapHumanTracker(entityhuman);
            this.k.put(entityhuman, worldmap_worldmaphumantracker);
            this.i.add(worldmap_worldmaphumantracker);
        }
        return worldmap_worldmaphumantracker;
    }
    
    public class WorldMapHumanTracker
    {
        public final EntityHuman trackee;
        private boolean d;
        private int e;
        private int f;
        private int g;
        private int h;
        private int i;
        public int b;
        
        private void addSeenPlayers(final Collection<MapIcon> icons) {
            final Player player = (Player)this.trackee.getBukkitEntity();
            final Player other;
            final Player player2;
            WorldMap.this.decorations.forEach((uuid, mapIcon) -> {
                other = Bukkit.getPlayer(uuid);
                if (other == null || player2.canSee(other)) {
                    icons.add(mapIcon);
                }
            });
        }
        
        private boolean shouldUseVanillaMap() {
            return WorldMap.this.mapView.getRenderers().size() == 1 && WorldMap.this.mapView.getRenderers().get(0).getClass() == CraftMapRenderer.class;
        }
        
        public WorldMapHumanTracker(final EntityHuman entityhuman) {
            this.d = true;
            this.g = 127;
            this.h = 127;
            this.trackee = entityhuman;
        }
        
        @Nullable
        public Packet<?> a(final ItemStack itemstack) {
            if (!this.d && this.i % 5 != 0) {
                ++this.i;
                return null;
            }
            final boolean vanillaMaps = this.shouldUseVanillaMap();
            final RenderData render = vanillaMaps ? WorldMap.this.vanillaRender : WorldMap.this.mapView.render((CraftPlayer)this.trackee.getBukkitEntity());
            final Collection<MapIcon> icons = new ArrayList<MapIcon>();
            if (vanillaMaps) {
                this.addSeenPlayers(icons);
            }
            for (final MapCursor cursor : render.cursors) {
                if (cursor.isVisible()) {
                    icons.add(new MapIcon(MapIcon.Type.a(cursor.getRawType()), cursor.getX(), cursor.getY(), cursor.getDirection()));
                }
            }
            if (this.d) {
                this.d = false;
                return (Packet<?>)new PacketPlayOutMap(itemstack.getData(), WorldMap.this.scale, WorldMap.this.track, (Collection)icons, render.buffer, this.e, this.f, this.g + 1 - this.e, this.h + 1 - this.f);
            }
            return (Packet<?>)((this.i++ % 5 == 0) ? new PacketPlayOutMap(itemstack.getData(), WorldMap.this.scale, WorldMap.this.track, (Collection)icons, render.buffer, 0, 0, 0, 0) : null);
        }
        
        public void a(final int i, final int j) {
            if (this.d) {
                this.e = Math.min(this.e, i);
                this.f = Math.min(this.f, j);
                this.g = Math.max(this.g, i);
                this.h = Math.max(this.h, j);
            }
            else {
                this.d = true;
                this.e = i;
                this.f = j;
                this.g = i;
                this.h = j;
            }
        }
    }
}
