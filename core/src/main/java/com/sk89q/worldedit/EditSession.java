/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.MCAWorld;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.HistoryExtent;
import com.boydti.fawe.object.NullChangeSet;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.object.changeset.BlockBagChangeSet;
import com.boydti.fawe.object.changeset.CPUOptimizedChangeSet;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.boydti.fawe.object.clipboard.WorldCopyClipboard;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.object.extent.FaweRegionExtent;
import com.boydti.fawe.object.extent.HeightBoundExtent;
import com.boydti.fawe.object.extent.MultiRegionExtent;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.object.extent.ProcessedWEExtent;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.extent.SingleRegionExtent;
import com.boydti.fawe.object.extent.SlowExtent;
import com.boydti.fawe.object.extent.SourceMaskExtent;
import com.boydti.fawe.object.extent.StripNBTExtent;
import com.boydti.fawe.object.function.SurfaceRegionFunction;
import com.boydti.fawe.object.mask.ResettableMask;
import com.boydti.fawe.object.pattern.ExistingPattern;
import com.boydti.fawe.object.progress.ChatProgressTracker;
import com.boydti.fawe.object.progress.DefaultProgressTracker;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.MaskTraverser;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.Perm;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagExtent;
import com.sk89q.worldedit.extent.world.SurvivalModeExtent;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.block.Naturalizer;
import com.sk89q.worldedit.function.generator.GardenPatchGenerator;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BoundedHeightMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.FuzzyBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.util.RegionOffset;
import com.sk89q.worldedit.function.visitor.DirectionalVisitor;
import com.sk89q.worldedit.function.visitor.DownwardVisitor;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.NonRisingVisitor;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.internal.expression.runtime.RValue;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.regions.shape.ArbitraryBiomeShape;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.regions.shape.RegionShape;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.SimpleWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.WorldData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

/**
 * An {@link Extent} that handles history, {@link BlockBag}s, change limits,
 * block re-ordering, and much more. Most operations in WorldEdit use this class.
 * <p>
 * <p>Most of the actual functionality is implemented with a number of other
 * {@link Extent}s that are chained together. For example, history is logged
 * using the {@link ChangeSetExtent}.</p>
 */
public class EditSession extends AbstractDelegateExtent implements HasFaweQueue, SimpleWorld {
    /**
     * Used by {@link #setBlock(Vector, BaseBlock, Stage)} to
     * determine which {@link Extent}s should be bypassed.
     */
    public enum Stage {
        BEFORE_HISTORY, BEFORE_REORDER, BEFORE_CHANGE
    }

    private World world;
    private String worldName;
    private FaweQueue queue;
    private boolean wrapped;
    private boolean fastMode;
    private AbstractDelegateExtent extent;
    private HistoryExtent history;
    private AbstractDelegateExtent bypassHistory;
    private AbstractDelegateExtent bypassAll;
    private FaweLimit originalLimit;
    private FaweLimit limit;
    private FawePlayer player;
    private FaweChangeSet changeTask;

    private MutableBlockVector mutable = new MutableBlockVector();

    private int changes = 0;
    private BlockBag blockBag;

    private final int maxY;

    public static final UUID CONSOLE = UUID.fromString("1-1-3-3-7");
    public static final BaseBiome nullBiome = new BaseBiome(0);
    public static final BaseBlock nullBlock = new BaseBlock(0, 0);
    private static final Vector[] recurseDirections = {
            PlayerDirection.NORTH.vector(),
            PlayerDirection.EAST.vector(),
            PlayerDirection.SOUTH.vector(),
            PlayerDirection.WEST.vector(),
            PlayerDirection.UP.vector(),
            PlayerDirection.DOWN.vector(),};

    @Deprecated
    public EditSession(@Nonnull World world, @Nullable FaweQueue queue, @Nullable FawePlayer player, @Nullable FaweLimit limit, @Nullable FaweChangeSet changeSet, @Nullable RegionWrapper[] allowedRegions, @Nullable Boolean autoQueue, @Nullable Boolean fastmode, @Nullable Boolean checkMemory, @Nullable Boolean combineStages, @Nullable BlockBag blockBag, @Nullable EventBus bus, @Nullable EditSessionEvent event) {
        this(null, world, queue, player, limit, changeSet, allowedRegions, autoQueue, fastmode, checkMemory, combineStages, blockBag, bus, event);
    }

    @Deprecated
    public EditSession(@Nullable String worldName, @Nullable World world, @Nullable FaweQueue queue, @Nullable FawePlayer player, @Nullable FaweLimit limit, @Nullable FaweChangeSet changeSet, @Nullable RegionWrapper[] allowedRegions, @Nullable Boolean autoQueue, @Nullable Boolean fastmode, @Nullable Boolean checkMemory, @Nullable Boolean combineStages, @Nullable BlockBag blockBag, @Nullable EventBus bus, @Nullable EditSessionEvent event) {
        this(worldName, world, queue, player, limit, changeSet, (Region[]) allowedRegions, autoQueue, fastmode, checkMemory, combineStages, blockBag, bus, event);
    }

    public EditSession(@Nullable String worldName, @Nullable World world, @Nullable FaweQueue queue, @Nullable FawePlayer player, @Nullable FaweLimit limit, @Nullable FaweChangeSet changeSet, @Nullable Region[] allowedRegions, @Nullable Boolean autoQueue, @Nullable Boolean fastmode, @Nullable Boolean checkMemory, @Nullable Boolean combineStages, @Nullable BlockBag blockBag, @Nullable EventBus bus, @Nullable EditSessionEvent event) {
        super(world);
        this.worldName = worldName == null ? world == null ? queue == null ? "" : queue.getWorldName() : Fawe.imp().getWorldName(world) : worldName;
        if (world == null && this.worldName != null) world = FaweAPI.getWorld(this.worldName);
        this.world = world = WorldWrapper.wrap(world);
        if (bus == null) {
            bus = WorldEdit.getInstance().getEventBus();
        }
        if (event == null) {
            event = new EditSessionEvent(world, player == null ? null : (player.getPlayer()), -1, null);
        }
        event.setEditSession(this);
        if (player == null && event.getActor() != null) {
            player = FawePlayer.wrap(event.getActor());
        }
        this.player = player;
        if (limit == null) {
            if (player == null) {
                limit = FaweLimit.MAX;
            } else {
                limit = player.getLimit();
            }
        }
        if (autoQueue == null) {
            autoQueue = true;
        }
        if (fastmode == null) {
            if (player == null) {
                fastmode = !Settings.IMP.HISTORY.ENABLE_FOR_CONSOLE;
            } else {
                fastmode = player.getSession().hasFastMode();
            }
        }
        this.fastMode = fastmode;
        if (checkMemory == null) {
            checkMemory = player != null && !this.fastMode;
        }
        if (checkMemory) {
            if (MemUtil.isMemoryLimitedSlow()) {
                if (Perm.hasPermission(player, "worldedit.fast")) {
                    BBC.WORLDEDIT_OOM_ADMIN.send(player);
                }
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY);
            }
        }
        this.originalLimit = limit;
        this.blockBag = limit.INVENTORY_MODE != 0 ? blockBag : null;
        this.limit = limit.copy();

        if (queue == null) {
            boolean placeChunks = this.fastMode || this.limit.FAST_PLACEMENT;
            World unwrapped = WorldWrapper.unwrap(world);
            if (unwrapped instanceof FaweQueue) {
                queue = (FaweQueue) unwrapped;
            } else if (unwrapped instanceof MCAWorld) {
                queue = ((MCAWorld) unwrapped).getQueue();
            } else if (player != null && world.equals(player.getWorld())) {
                queue = player.getFaweQueue(placeChunks, autoQueue);
            } else {
                queue = SetQueue.IMP.getNewQueue(world, placeChunks, autoQueue);
            }
        }
        if (combineStages == null) {
            combineStages =
                    // If it's enabled in the settings
                    Settings.IMP.HISTORY.COMBINE_STAGES
                    // If fast placement is disabled, it's slower to perform a copy on each chunk
                    && this.limit.FAST_PLACEMENT
                    // If the specific queue doesn't support it
                    && queue.supports(FaweQueue.Capability.CHANGE_TASKS)
                    // If the edit uses items from the inventory we can't use a delayed task
                    && this.blockBag == null;
        }
        if (Settings.IMP.EXPERIMENTAL.ANVIL_QUEUE_MODE && !(queue instanceof MCAQueue)) {
            queue = new MCAQueue(queue);
        }
        this.queue = queue;
        this.queue.addEditSession(this);
        if (!Settings.IMP.QUEUE.PROGRESS.DISPLAY.equalsIgnoreCase("false") && player != null) {
            switch (Settings.IMP.QUEUE.PROGRESS.DISPLAY.toLowerCase()) {
                case "chat":
                    this.queue.setProgressTask(new ChatProgressTracker(player));
                    break;
                case "title":
                case "true":
                default:
                    this.queue.setProgressTask(new DefaultProgressTracker(player));
            }
        }
        this.bypassAll = wrapExtent(new FastWorldEditExtent(world, queue), bus, event, Stage.BEFORE_CHANGE);
        this.bypassHistory = (this.extent = wrapExtent(bypassAll, bus, event, Stage.BEFORE_REORDER));
        if (!this.fastMode || changeSet != null) {
            if (changeSet == null) {
                if (Settings.IMP.HISTORY.USE_DISK) {
                    UUID uuid = player == null ? CONSOLE : player.getUUID();

                    changeSet = new DiskStorageHistory(world, uuid);
                } else if (combineStages && Settings.IMP.HISTORY.COMPRESSION_LEVEL == 0 && !(queue instanceof MCAQueue)) {
                    changeSet = new CPUOptimizedChangeSet(world);
                } else {
                    changeSet = new MemoryOptimizedHistory(world);
                }
            }
            if (this.limit.SPEED_REDUCTION > 0) {
                this.bypassHistory = new SlowExtent(this.bypassHistory, this.limit.SPEED_REDUCTION);
            }

            if (!(changeSet instanceof NullChangeSet)) {
                if (this.blockBag != null) {
                    changeSet = new BlockBagChangeSet(changeSet, blockBag, limit.INVENTORY_MODE == 1);
                }
                if (combineStages) {
                    changeTask = changeSet;
                    changeSet.addChangeTask(queue);
                } else {
                    this.extent = (history = new HistoryExtent(this, bypassHistory, changeSet, queue));
//                    if (this.blockBag != null) {
//                        this.extent = new BlockBagExtent(this.extent, blockBag, limit.INVENTORY_MODE == 1);
//                    }
                }
            }
        }
        if (allowedRegions == null) {
            if (player != null && !player.hasPermission("fawe.bypass") && !player.hasPermission("fawe.bypass.regions") && !(queue instanceof VirtualWorld)) {
                allowedRegions = player.getCurrentRegions();
            }
        }
        this.maxY = getWorld() == null ? 255 : world.getMaxY();
        if (allowedRegions != null) {
            if (allowedRegions.length == 0) {
                this.extent = new NullExtent(this.extent, BBC.WORLDEDIT_CANCEL_REASON_NO_REGION);
            } else {
                this.extent = new ProcessedWEExtent(this.extent, this.limit);
                if (allowedRegions.length == 1) {
                    Region region = allowedRegions[0];
                    this.extent = new SingleRegionExtent(this.extent, this.limit, allowedRegions[0]);
                } else {
                    this.extent = new MultiRegionExtent(this.extent, this.limit, allowedRegions);
                }
            }
        } else {
            this.extent = new HeightBoundExtent(this.extent, this.limit, 0, maxY);
        }
        if (this.limit.STRIP_NBT != null && !this.limit.STRIP_NBT.isEmpty()) {
            this.extent = new StripNBTExtent(this.extent, this.limit.STRIP_NBT);
        }
        this.extent = wrapExtent(this.extent, bus, event, Stage.BEFORE_HISTORY);
        setExtent(this.extent);
    }

    /**
     * Create a new instance.
     *
     * @param world     a world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @deprecated use {@link WorldEdit#getEditSessionFactory()} to create {@link EditSession}s
     */
    @Deprecated
    public EditSession(final LocalWorld world, final int maxBlocks) {
        this(world, maxBlocks, null);
    }

    /**
     * Create a new instance.
     *
     * @param world     a world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag  the block bag to set, or null to use none
     * @deprecated use {@link WorldEdit#getEditSessionFactory()} to create {@link EditSession}s
     */
    @Deprecated
    public EditSession(final LocalWorld world, final int maxBlocks, @Nullable final BlockBag blockBag) {
        this(WorldEdit.getInstance().getEventBus(), world, maxBlocks, blockBag, new EditSessionEvent(world, null, maxBlocks, null));
    }

    /**
     * Construct the object with a maximum number of blocks and a block bag.
     *
     * @param eventBus  the event bus
     * @param world     the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag  an optional {@link BlockBag} to use, otherwise null
     * @param event     the event to call with the extent
     */
    public EditSession(final EventBus eventBus, World world, final int maxBlocks, @Nullable final BlockBag blockBag, EditSessionEvent event) {
        this(world, null, null, null, null, null, true, null, null, null, blockBag, eventBus, event);
    }

    /**
     * Lazily copy a region
     *
     * @param region
     * @return
     */
    public BlockArrayClipboard lazyCopy(Region region) {
        WorldCopyClipboard faweClipboard = new WorldCopyClipboard(this, region);
        BlockArrayClipboard weClipboard = new BlockArrayClipboard(region, faweClipboard);
        weClipboard.setOrigin(region.getMinimumPoint());
        return weClipboard;
    }

    /**
     * The limit for this specific edit (blocks etc)
     *
     * @return
     */
    public FaweLimit getLimit() {
        return originalLimit;
    }

    public void resetLimit() {
        this.limit.set(this.originalLimit);
        ExtentTraverser<ProcessedWEExtent> find = new ExtentTraverser(extent).find(ProcessedWEExtent.class);
        if (find != null && find.get() != null) {
            find.get().setLimit(this.limit);
        }
    }

    /**
     * Returns a new limit representing how much of this edit's limit has been used so far
     *
     * @return
     */
    public FaweLimit getLimitUsed() {
        FaweLimit newLimit = new FaweLimit();
        newLimit.MAX_ACTIONS = originalLimit.MAX_ACTIONS - limit.MAX_ACTIONS;
        newLimit.MAX_CHANGES = originalLimit.MAX_CHANGES - limit.MAX_CHANGES;
        newLimit.MAX_FAILS = originalLimit.MAX_FAILS - limit.MAX_FAILS;
        newLimit.MAX_CHECKS = originalLimit.MAX_CHECKS - limit.MAX_CHECKS;
        newLimit.MAX_ITERATIONS = originalLimit.MAX_ITERATIONS - limit.MAX_ITERATIONS;
        newLimit.MAX_BLOCKSTATES = originalLimit.MAX_BLOCKSTATES - limit.MAX_BLOCKSTATES;
        newLimit.MAX_ENTITIES = originalLimit.MAX_ENTITIES - limit.MAX_ENTITIES;
        newLimit.MAX_HISTORY = limit.MAX_HISTORY;
        return newLimit;
    }

    /**
     * Returns the remaining limits
     *
     * @return
     */
    public FaweLimit getLimitLeft() {
        return limit;
    }

    /**
     * The region extent restricts block placements to allowed regions
     *
     * @return FaweRegionExtent (may be null)
     */
    public FaweRegionExtent getRegionExtent() {
        ExtentTraverser<FaweRegionExtent> traverser = new ExtentTraverser(this.extent).find(FaweRegionExtent.class);
        return traverser == null ? null : traverser.get();
    }

    public Extent getBypassAll() {
        return bypassAll;
    }

    public Extent getBypassHistory() {
        return bypassHistory;
    }

    public Extent getExtent() {
        return extent;
    }

    public void setExtent(AbstractDelegateExtent extent) {
        this.extent = extent;
        new ExtentTraverser(this).setNext(extent);
    }

    /**
     * Get the FawePlayer or null
     *
     * @return
     */
    @Nullable
    public FawePlayer getPlayer() {
        return player;
    }

    public boolean cancel() {
        ExtentTraverser traverser = new ExtentTraverser(this.extent);
        NullExtent nullExtent = new NullExtent(world, BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
        while (traverser != null) {
            ExtentTraverser next = traverser.next();
            Extent get = traverser.get();
            if (get instanceof AbstractDelegateExtent && !(get instanceof NullExtent)) {
                traverser.setNext(nullExtent);
            }
            traverser = next;
        }
        bypassHistory = nullExtent;
        this.extent = nullExtent;
        bypassAll = nullExtent;
        dequeue();
        queue.clear();
        return true;
    }

    /**
     * Remove this EditSession from the queue<br>
     * - This doesn't necessarily stop it from being queued again
     */
    public void dequeue() {
        if (queue != null) {
            SetQueue.IMP.dequeue(queue);
        }
    }

    /**
     * Add a task to run when this EditSession is done dispatching
     *
     * @param whenDone
     */
    public void addNotifyTask(Runnable whenDone) {
        if (queue != null) {
            queue.addNotifyTask(whenDone);
        }
    }

    /**
     * Send a debug message to the Actor responsible for this EditSession (or Console)
     *
     * @param message
     * @param args
     */
    public void debug(BBC message, Object... args) {
        message.send(player, args);
    }

    /**
     * Get the FaweQueue this EditSession uses to queue the changes<br>
     * - Note: All implementation queues for FAWE are instances of NMSMappedFaweQueue
     *
     * @return
     */
    public FaweQueue getQueue() {
        return queue;
    }

    @Deprecated
    private AbstractDelegateExtent wrapExtent(final AbstractDelegateExtent extent, final EventBus eventBus, EditSessionEvent event, final Stage stage) {
        event = event.clone(stage);
        event.setExtent(extent);
        eventBus.post(event);
        if (event.isCancelled()) {
            return new NullExtent(extent, BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
        }
        final Extent toReturn = event.getExtent();
        if (!(toReturn instanceof AbstractDelegateExtent)) {
            Fawe.debug("Extent " + toReturn + " must be AbstractDelegateExtent");
            return extent;
        }
        if (toReturn != extent) {
            String className = toReturn.getClass().getName().toLowerCase();
            for (String allowed : Settings.IMP.EXTENT.ALLOWED_PLUGINS) {
                if (className.contains(allowed.toLowerCase())) {
                    this.wrapped = true;
                    return (AbstractDelegateExtent) toReturn;
                }
            }
            if (Settings.IMP.EXTENT.DEBUG) {
                Fawe.debug("&cPotentially unsafe extent blocked: " + toReturn.getClass().getName());
                Fawe.debug("&8 - &7For area restrictions, it is recommended to use the FaweAPI");
                Fawe.debug("&8 - &7For block logging, it is recommended to use use BlocksHub");
                Fawe.debug("&8 - &7To allow this plugin add it to the FAWE `allowed-plugins` list");
                Fawe.debug("&8 - &7To hide this message set `debug` to false in the FAWE config.yml");
                if (toReturn.getClass().getName().contains("CoreProtect")) {
                    Fawe.debug("Note on CoreProtect: ");
                    Fawe.debug(" - If you disable CP's WE logger (CP config) and this still shows, please update CP");
                    Fawe.debug(" - Use BlocksHub and set `debug` false in the FAWE config");
                }
            }
        }
        return extent;
    }

    /**
     * Get the world.
     *
     * @return the world
     */
    public World getWorld() {
        return this.world;
    }

    public WorldData getWorldData() {
        return getWorld() != null ? getWorld().getWorldData() : null;
    }

    /**
     * Get the underlying {@link ChangeSet}.
     *
     * @return the change set
     */
    public ChangeSet getChangeSet() {
        return changeTask != null ? changeTask : history != null ? history.getChangeSet() : null;
    }

    public FaweChangeSet getChangeTask() {
        return changeTask;
    }

    /**
     * Set the ChangeSet without hooking into any recording mechanism or triggering any actions.<br/>
     * Used internally to set the ChangeSet during completion to record custom changes which aren't normally recorded
     * @param set
     */
    public void setRawChangeSet(@Nullable FaweChangeSet set) {
        changeTask = set;
        changes++;
    }

    /**
     * Change the ChangeSet being used for this EditSession
     * - If history is disabled, no changeset can be set
     *
     * @param set (null = remove the changeset)
     */
    public void setChangeSet(@Nullable FaweChangeSet set) {
        if (set == null) {
            disableHistory(true);
        } else {
            if (history != null) {
                history.setChangeSet(set);
            } else {
                changeTask = set;
                set.addChangeTask(queue);
            }
        }
        changes++;
    }

    /**
     * @return the limit (&gt;= 0) or -1 for no limit
     * @see #getLimit()
     */
    @Deprecated
    public int getBlockChangeLimit() {
        return originalLimit.MAX_CHANGES;
    }

    /**
     * Set the maximum number of blocks that can be changed.
     *
     * @param limit the limit (&gt;= 0) or -1 for no limit
     */
    public void setBlockChangeLimit(final int limit) {
        // Nothing
    }

    /**
     * Returns queue status.
     *
     * @return whether the queue is enabled
     */
    public boolean isQueueEnabled() {
        return true;
    }

    /**
     * Queue certain types of block for better reproduction of those blocks.
     */
    public void enableQueue() {
    }

    /**
     * Disable the queue. This will close the queue.
     */
    public void disableQueue() {
        if (this.isQueueEnabled()) {
            this.flushQueue();
        }
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getMask() {
        ExtentTraverser<MaskingExtent> maskingExtent = new ExtentTraverser(this.extent).find(MaskingExtent.class);
        return maskingExtent != null ? maskingExtent.get().getMask() : null;
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getSourceMask() {
        ExtentTraverser<SourceMaskExtent> maskingExtent = new ExtentTraverser(this.extent).find(SourceMaskExtent.class);
        return maskingExtent != null ? maskingExtent.get().getMask() : null;
    }

    public void addTransform(ResettableExtent transform) {
        wrapped = true;
        if (transform == null) {
            ExtentTraverser<AbstractDelegateExtent> traverser = new ExtentTraverser(this.extent).find(ResettableExtent.class);
            AbstractDelegateExtent next = extent;
            while (traverser != null && traverser.get() instanceof ResettableExtent) {
                traverser = traverser.next();
                next = traverser.get();
            }
            this.extent = next;
            return;
        } else {
            this.extent = transform.setExtent(extent);
        }
    }

    public @Nullable ResettableExtent getTransform() {
        ExtentTraverser<AbstractDelegateExtent> traverser = new ExtentTraverser(this.extent).find(ResettableExtent.class);
        if (traverser != null) {
            return (ResettableExtent) traverser.get();
        }
        return null;
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setSourceMask(Mask mask) {
        if (mask == null) {
            mask = Masks.alwaysTrue();
        } else {
            new MaskTraverser(mask).reset(this);
        }
        ExtentTraverser<SourceMaskExtent> maskingExtent = new ExtentTraverser(this.extent).find(SourceMaskExtent.class);
        if (maskingExtent != null && maskingExtent.get() != null) {
            Mask oldMask = maskingExtent.get().getMask();
            if (oldMask instanceof ResettableMask) {
                ((ResettableMask) oldMask).reset();
            }
            maskingExtent.get().setMask(mask);
        } else if (mask != Masks.alwaysTrue()) {
            this.extent = new SourceMaskExtent(this.extent, mask);
        }
    }

    public void addSourceMask(Mask mask) {
        checkNotNull(mask);
        Mask existing = getSourceMask();
        if (existing != null) {
            if (existing instanceof MaskIntersection) {
                ((MaskIntersection) existing).add(mask);
                return;
            } else {
                MaskIntersection intersection = new MaskIntersection(existing);
                intersection.add(mask);
                mask = intersection;
            }
        }
        setSourceMask(mask);
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setMask(Mask mask) {
        if (mask == null) {
            mask = Masks.alwaysTrue();
        } else {
            new MaskTraverser(mask).reset(this);
        }
        ExtentTraverser<MaskingExtent> maskingExtent = new ExtentTraverser(this.extent).find(MaskingExtent.class);
        if (maskingExtent != null && maskingExtent.get() != null) {
            Mask oldMask = maskingExtent.get().getMask();
            if (oldMask instanceof ResettableMask) {
                ((ResettableMask) oldMask).reset();
            }
            maskingExtent.get().setMask(mask);
        } else if (mask != Masks.alwaysTrue()) {
            this.extent = new MaskingExtent(this.extent, mask);
        }
    }

    /**
     * Set the mask.
     *
     * @param mask the mask
     * @deprecated Use {@link #setMask(Mask)}
     */
    @Deprecated
    public void setMask(final com.sk89q.worldedit.masks.Mask mask) {
        if (mask == null) {
            this.setMask((Mask) null);
        } else {
            this.setMask(Masks.wrap(mask));
        }
    }

    /**
     * Get the {@link SurvivalModeExtent}.
     *
     * @return the survival simulation extent
     */
    public SurvivalModeExtent getSurvivalExtent() {
        ExtentTraverser<SurvivalModeExtent> survivalExtent = new ExtentTraverser(this.extent).find(SurvivalModeExtent.class);
        if (survivalExtent != null) {
            return survivalExtent.get();
        } else {
            AbstractDelegateExtent extent = this.extent;
            SurvivalModeExtent survival = new SurvivalModeExtent(extent.getExtent(), getWorld());
            new ExtentTraverser(extent).setNext(survival);
            return survival;
        }
    }

    /**
     * Set whether fast mode is enabled.
     * <p>
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @param enabled true to enable
     */
    public void setFastMode(final boolean enabled) {
        this.fastMode = enabled;
        disableHistory(enabled);
    }

    /**
     * Disable history (or re-enable)
     *
     * @param disableHistory
     */
    public void disableHistory(boolean disableHistory) {
        if (history == null) {
            return;
        }
        ExtentTraverser traverseHistory = new ExtentTraverser(this.extent).find(HistoryExtent.class);
        if (disableHistory) {
            if (traverseHistory != null && traverseHistory.exists()) {
                ExtentTraverser beforeHistory = traverseHistory.previous();
                ExtentTraverser afterHistory = traverseHistory.next();
                if (beforeHistory != null && beforeHistory.exists()) {
                    beforeHistory.setNext(afterHistory.get());
                } else {
                    extent = (AbstractDelegateExtent) afterHistory.get();
                }
            }
        } else if (traverseHistory == null || !traverseHistory.exists()) {
            ExtentTraverser traverseBypass = new ExtentTraverser(this.extent).find(bypassHistory);
            if (traverseBypass != null) {
                ExtentTraverser beforeHistory = traverseBypass.previous();
                beforeHistory.setNext(history);
            }
        }
    }

    /**
     * Return fast mode status.
     * <p>
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @return true if enabled
     */
    public boolean hasFastMode() {
        return getChangeSet() == null;
    }

    /**
     * Get the {@link BlockBag} is used.
     *
     * @return a block bag or null
     */
    public BlockBag getBlockBag() {
        return this.blockBag;
    }

    /**
     * Set a {@link BlockBag} to use.
     *
     * @param blockBag the block bag to set, or null to use none
     */
    public void setBlockBag(final BlockBag blockBag) {
        this.blockBag = blockBag;
    }

    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<Integer, Integer> popMissingBlocks() {
        BlockBag bag = getBlockBag();
        if (bag != null) {
            bag.flushChanges();

            Map<Integer, Integer> missingBlocks;
            ChangeSet changeSet = getChangeSet();


            if (changeSet instanceof BlockBagChangeSet) {
                missingBlocks = ((BlockBagChangeSet) changeSet).popMissing();
            } else {
                ExtentTraverser<BlockBagExtent> find = new ExtentTraverser(extent).find(BlockBagExtent.class);
                if (find != null && find.get() != null) {
                    missingBlocks = find.get().popMissing();
                } else {
                    missingBlocks = null;
                }
            }

            if (missingBlocks != null && !missingBlocks.isEmpty()) {
                StringBuilder str = new StringBuilder();
                int size = missingBlocks.size();
                int i = 0;

                for (Map.Entry<Integer, Integer> entry : missingBlocks.entrySet()) {
                    int combined = entry.getKey();
                    int id = FaweCache.getId(combined);
                    int data = FaweCache.getData(combined);
                    int amount = entry.getValue();
                    BlockType type = BlockType.fromID(id);
                    str.append((type != null ? type.getName() : "" + id))
                            .append((data != 0 ? ":" + data : ""))
                            .append((amount != 1 ? "x" + amount : ""));
                    ++i;
                    if (i != size) {
                        str.append(", ");
                    }
                }

                BBC.WORLDEDIT_SOME_FAILS_BLOCKBAG.send(player, str.toString());
            }
        }
        return new HashMap<>();
    }

    @Override
    public String toString() {
        return super.toString() + ":" + extent;
    }

    /**
     * Get the number of blocks changed, including repeated block changes.
     * <p>
     * <p>This number may not be accurate.</p>
     *
     * @return the number of block changes
     */
    public int getBlockChangeCount() {
        return this.changes;
    }

    @Override
    public BaseBiome getBiome(final Vector2D position) {
        return this.extent.getBiome(position);
    }

    @Override
    public boolean setBiome(final Vector2D position, final BaseBiome biome) {
        this.changes++;
        return this.extent.setBiome(position, biome);
    }

    @Override
    public boolean setBiome(int x, int y, int z, BaseBiome biome) {
        this.changes++;
        return this.extent.setBiome(x, y, z, biome);
    }

    @Override
    public int getLight(int x, int y, int z) {
        return queue.getLight(x, y, z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return queue.getEmmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return queue.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return queue.getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return queue.getOpacity(x, y, z);
    }

    @Override
    public BaseBlock getLazyBlock(final Vector position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    public BaseBlock getLazyBlock(int x, int y, int z) {
        return extent.getLazyBlock(x, y, z);
    }

    public BaseBlock getBlock(int x, int y, int z) {
        return getLazyBlock(x, y, z);
    }

    @Override
    public BaseBlock getBlock(final Vector position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    /**
     * Get a block type at the given position.
     *
     * @param position the position
     * @return the block type
     * @deprecated Use {@link #getLazyBlock(Vector)} or {@link #getBlock(Vector)}
     */
    @Deprecated
    public int getBlockType(final Vector position) {
        return getBlockType(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    public int getBlockType(int x, int y, int z) {
        if (!limit.MAX_CHECKS()) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        int combinedId4Data = queue.getCombinedId4DataDebug(x, y, z, 0, this);
        return combinedId4Data >> 4;
    }

    /**
     * Get a block data at the given position.
     *
     * @param position the position
     * @return the block data
     * @deprecated Use {@link #getLazyBlock(Vector)} or {@link #getBlock(Vector)}
     */
    @Deprecated
    public int getBlockData(final Vector position) {
        if (!limit.MAX_CHECKS()) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        int combinedId4Data = queue.getCombinedId4DataDebug(position.getBlockX(), position.getBlockY(), position.getBlockZ(), 0, this);
        return combinedId4Data & 0xF;
    }

    /**
     * Gets the block type at a position.
     *
     * @param position the position
     * @return a block
     * @deprecated Use {@link #getBlock(Vector)}
     */
    @Deprecated
    public BaseBlock rawGetBlock(final Vector position) {
        return bypassAll.getLazyBlock(position);
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block    the block
     * @param stage    the level
     * @return whether the block changed
     * @throws WorldEditException thrown on a set error
     */
    public boolean setBlock(final Vector position, final BaseBlock block, final Stage stage) throws WorldEditException {
        this.changes++;
        switch (stage) {
            case BEFORE_HISTORY:
                return this.extent.setBlock(position, block);
            case BEFORE_CHANGE:
                return this.bypassHistory.setBlock(position, block);
            case BEFORE_REORDER:
                return this.bypassAll.setBlock(position, block);
        }

        throw new RuntimeException("New enum entry added that is unhandled here");
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block    the block
     * @return whether the block changed
     */
    public boolean rawSetBlock(final Vector position, final BaseBlock block) {
        this.changes++;
        try {
            return this.bypassHistory.setBlock(position, block);
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Set a block, bypassing history but still utilizing block re-ordering.
     *
     * @param position the position to set the block at
     * @param block    the block
     * @return whether the block changed
     */
    public boolean smartSetBlock(final Vector position, final BaseBlock block) {
        this.changes++;
        try {
            return this.bypassAll.setBlock(position, block);
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws MaxChangedBlocksException {
        try {
            return setBlock(position, block, Stage.BEFORE_HISTORY);
        } catch (MaxChangedBlocksException e) {
            throw e;
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        this.changes++;
        try {
            return this.extent.setBlock(x, y, z, block);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public boolean setBlock(int x, int y, int z, Pattern pattern) {
        this.changes++;
        try {
            mutable.setComponents(x, y, z);
            return pattern.apply(extent, mutable, mutable);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public boolean setBlock(final Vector position, final BaseBlock block, final boolean ignorePhysics) {
        return setBlockFast(position, block);
    }

    public boolean setBlockFast(final Vector position, final BaseBlock block) {
        this.changes++;
        try {
            return this.extent.setBlock(position, block);
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @SuppressWarnings("deprecation")
    public boolean setBlock(final Vector position, final Pattern pattern) {
        this.changes++;
        try {
            return pattern.apply(this.extent, position, position);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public int setBlocks(final Set<Vector> vset, final Pattern pattern) {
        RegionVisitor visitor = new RegionVisitor(vset, new BlockReplace(extent, pattern), this);
        Operations.completeBlindly(visitor);
        changes += visitor.getAffected();
        return changes;
    }

    /**
     * Set a block (only if a previous block was not there) if {@link Math#random()}
     * returns a number less than the given probability.
     *
     * @param position    the position
     * @param block       the block
     * @param probability a probability between 0 and 1, inclusive
     * @return whether a block was changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public boolean setChanceBlockIfAir(final Vector position, final BaseBlock block, final double probability) {
        return (FaweCache.RANDOM.random(65536) <= (probability * 65536)) && this.setBlockIfAir(position, block);
    }

    /**
     * Set a block only if there's no block already there.
     *
     * @param position the position
     * @param block    the block to set
     * @return if block was changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use your own method
     */
    @Deprecated
    public boolean setBlockIfAir(final Vector position, final BaseBlock block) {
        return this.getBlock(position).isAir() && this.setBlockFast(position, block);
    }

    @Override
    @Nullable
    public Entity createEntity(final com.sk89q.worldedit.util.Location location, final BaseEntity entity) {
        Entity result = this.extent.createEntity(location, entity);
        return result;
    }

    /**
     * Insert a contrived block change into the history.
     *
     * @param position the position
     * @param existing the previous block at that position
     * @param block    the new block
     * @deprecated Get the change set with {@link #getChangeSet()} and add the change with that
     */
    @Deprecated
    public void rememberChange(final Vector position, final BaseBlock existing, final BaseBlock block) {
        ChangeSet changeSet = getChangeSet();
        if (changeSet != null) {
            changeSet.add(new BlockChange(position.toBlockVector(), existing, block));
        }
    }

    /**
     * Restores all blocks to their initial state.
     *
     * @param editSession a new {@link EditSession} to perform the undo in
     */
    public void undo(final EditSession editSession) {
        final UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassAll);
        ChangeSet changeSet = getChangeSet();
        editSession.getQueue().setChangeTask(null);
        Operations.completeBlindly(ChangeSetExecutor.create(changeSet, context, ChangeSetExecutor.Type.UNDO, editSession.getBlockBag(), editSession.getLimit().INVENTORY_MODE));
        flushQueue();
        editSession.changes = 1;
    }

    public void setBlocks(ChangeSet changeSet, ChangeSetExecutor.Type type) {
        final UndoContext context = new UndoContext();
        Extent bypass = (history == null) ? bypassAll : history;
        context.setExtent(bypass);
        Operations.completeBlindly(ChangeSetExecutor.create(changeSet, context, type, getBlockBag(), getLimit().INVENTORY_MODE));
        flushQueue();
        changes = 1;
    }

    /**
     * Sets to new state.
     *
     * @param editSession a new {@link EditSession} to perform the redo in
     */
    public void redo(final EditSession editSession) {
        final UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassAll);
        ChangeSet changeSet = getChangeSet();
        editSession.getQueue().setChangeTask(null);
        Operations.completeBlindly(ChangeSetExecutor.create(changeSet, context, ChangeSetExecutor.Type.REDO, editSession.getBlockBag(), editSession.getLimit().INVENTORY_MODE));
        flushQueue();
        editSession.changes = 1;
    }

    /**
     * Get the number of changed blocks.
     *
     * @return the number of changes
     */
    public int size() {
        return this.getBlockChangeCount();
    }

    public void setSize(int size) {
        this.changes = size;
    }

    @Override
    public Vector getMinimumPoint() {
        if (extent != null) {
            return this.extent.getMinimumPoint();
        } else {
            return new Vector(-30000000, 0, -30000000);
        }
    }

    @Override
    public Vector getMaximumPoint() {
        if (extent != null) {
            return this.extent.getMaximumPoint();
        } else {
            return new Vector(30000000, 255, 30000000);
        }
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return this.extent.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.extent.getEntities();
    }

    /**
     * Finish off the queue.
     */
    public void flushQueue() {
        Operations.completeBlindly(commit());
        // Check fails
        FaweLimit used = getLimitUsed();
        if (used.MAX_FAILS > 0) {
            if (used.MAX_CHANGES > 0 || used.MAX_ENTITIES > 0) {
                BBC.WORLDEDIT_SOME_FAILS.send(player, used.MAX_FAILS);
            } else if (new ExtentTraverser(this).findAndGet(FaweRegionExtent.class) != null){
                BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION.send(player);
            } else {
                BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_LEVEL.send(player);
            }
        }
        // Reset limit
        limit.set(originalLimit);
        // Enqueue it
        if (queue == null || queue.isEmpty()) {
            queue.dequeue();
            return;
        }
        if (Fawe.isMainThread() && (!(queue instanceof MCAQueue) || ((MCAQueue) queue).hasParent())) {
            SetQueue.IMP.flush(queue);
        } else {
            queue.flush();
        }
        if (getChangeSet() != null) {
            if (Settings.IMP.HISTORY.COMBINE_STAGES) {
                ((FaweChangeSet) getChangeSet()).closeAsync();
            } else {
                ((FaweChangeSet) getChangeSet()).close();
            }
        }
    }

    /**
     * Count the number of blocks of a given list of types in a region.
     *
     * @param region    the region
     * @param searchIDs a list of IDs to search
     * @return the number of found blocks
     */
    public int countBlock(final Region region, final Set<Integer> searchIDs) {
        if (searchIDs.isEmpty()) {
            return 0;
        }
        if (searchIDs.size() == 1) {
            final int id = searchIDs.iterator().next();
            RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                @Override
                public boolean apply(Vector position) throws WorldEditException {
                    return getBlockType(position) == id;
                }
            }, this);
            Operations.completeBlindly(visitor);
            return visitor.getAffected();
        }
        final boolean[] ids = new boolean[4096];
        for (final int id : searchIDs) {
            if ((id < 4096) && (id >= 0)) {
                ids[id] = true;
            }
        }
        return this.countBlock(region, ids);
    }

    public int countBlock(final Region region, final boolean[] ids) {
        RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
            @Override
            public boolean apply(Vector position) throws WorldEditException {
                return ids[getBlockType(position)];
            }
        }, this);
        Operations.completeBlindly(visitor);
        return visitor.getAffected();
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region       the region
     * @param searchBlocks the list of blocks to search
     * @return the number of blocks that matched the pattern
     */
    public int countBlocks(final Region region, final Set<BaseBlock> searchBlocks) {
        final BlockMask mask = new BlockMask(extent, searchBlocks);
        RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
            @Override
            public boolean apply(Vector position) throws WorldEditException {
                return mask.test(position);
            }
        }, this);
        Operations.completeBlindly(visitor);
        return visitor.getAffected();
    }

    public int fall(final Region region, boolean fullHeight, final BaseBlock replace) {
        FlatRegion flat = asFlatRegion(region);
        final int startPerformY = region.getMinimumPoint().getBlockY();
        final int startCheckY = fullHeight ? 0 : startPerformY;
        final int endY = region.getMaximumPoint().getBlockY();
        RegionVisitor visitor = new RegionVisitor(flat, new RegionFunction() {
            @Override
            public boolean apply(Vector pos) throws WorldEditException {
                int x = pos.getBlockX();
                int z = pos.getBlockZ();
                int freeSpot = startCheckY;
                for (int y = startCheckY; y <= endY; y++) {
                    if (y < startPerformY) {
                        if (getLazyBlock(x, y, z).getId() != 0) {
                            freeSpot = y + 1;
                        }
                        continue;
                    }
                    BaseBlock block = getLazyBlock(x, y, z);
                    if (block.getId() != 0) {
                        if (freeSpot != y) {
                            setBlock(x, freeSpot, z, block);
                            setBlock(x, y, z, replace);
                        }
                        freeSpot++;
                    }
                }
                return true;
            }
        }, this);
        Operations.completeBlindly(visitor);
        return this.changes;
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin    the location to start from
     * @param pattern     the block to fill with
     * @param radius    the radius of the spherical area to fill
     * @param depth     the maximum depth, starting from the origin
     * @param direction the direction to fill
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fillDirection(final Vector origin, final Pattern pattern, final double radius, final int depth, Vector direction) {
        checkNotNull(origin);
        checkNotNull(pattern);
        checkArgument(radius >= 0, "radius >= 0");
        checkArgument(depth >= 1, "depth >= 1");
        if (direction.equals(new Vector(0, -1, 0))) {
            return fillXZ(origin, pattern, radius, depth, false);
        }
        final MaskIntersection mask = new MaskIntersection(new RegionMask(new EllipsoidRegion(null, origin, new Vector(radius, radius, radius))), Masks.negate(new ExistingBlockMask(EditSession.this)));

        // Want to replace blocks
        final BlockReplace replace = new BlockReplace(EditSession.this, pattern);

        // Pick how we're going to visit blocks
        RecursiveVisitor visitor = new DirectionalVisitor(mask, replace, origin, direction, (int) (radius * 2 + 1), this);

        // Start at the origin
        visitor.visit(origin);

        // Execute
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin    the location to start from
     * @param block     the block to fill with
     * @param radius    the radius of the spherical area to fill
     * @param depth     the maximum depth, starting from the origin
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int fillXZ(final Vector origin, final BaseBlock block, final double radius, final int depth, final boolean recursive) {
        return this.fillXZ(origin, (Pattern) block, radius, depth, recursive);
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin    the origin to start the fill from
     * @param pattern   the pattern to fill with
     * @param radius    the radius of the spherical area to fill, with 0 as the smallest radius
     * @param depth     the maximum depth, starting from the origin, with 1 as the smallest depth
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int fillXZ(final Vector origin, final Pattern pattern, final double radius, final int depth, final boolean recursive) {
        checkNotNull(origin);
        checkNotNull(pattern);
        checkArgument(radius >= 0, "radius >= 0");
        checkArgument(depth >= 1, "depth >= 1");
        final MaskIntersection mask = new MaskIntersection(new RegionMask(new EllipsoidRegion(null, origin, new Vector(radius, radius, radius))), new BoundedHeightMask(Math.max(
                (origin.getBlockY() - depth) + 1, getMinimumPoint().getBlockY()), Math.min(getMaximumPoint().getBlockY(), origin.getBlockY())), Masks.negate(new ExistingBlockMask(EditSession.this)));

        // Want to replace blocks
        final BlockReplace replace = new BlockReplace(EditSession.this, pattern);

        // Pick how we're going to visit blocks
        RecursiveVisitor visitor;
        if (recursive) {
            visitor = new RecursiveVisitor(mask, replace, (int) (radius * 2 + 1), this);
        } else {
            visitor = new DownwardVisitor(mask, replace, origin.getBlockY(), (int) (radius * 2 + 1), this);
        }

        // Start at the origin
        visitor.visit(origin);

        // Execute
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Remove a cuboid above the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem  an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height   the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int removeAbove(final Vector position, final int apothem, final int height) {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
                position.add(-apothem + 1, 0, -apothem + 1), position.add(apothem - 1, height - 1, apothem - 1));
        final Pattern pattern = (new BaseBlock(BlockID.AIR));
        return this.setBlocks(region, pattern);
    }

    /**
     * Remove a cuboid below the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem  an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height   the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int removeBelow(final Vector position, final int apothem, final int height) {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
                position.add(-apothem + 1, 0, -apothem + 1), position.add(apothem - 1, -height + 1, apothem - 1));
        final Pattern pattern = (new BaseBlock(BlockID.AIR));
        return this.setBlocks(region, pattern);
    }

    /**
     * Remove blocks of a certain type nearby a given position.
     *
     * @param position  center position of cuboid
     * @param blockType the block type to match
     * @param apothem   an apothem of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int removeNear(final Vector position, final int blockType, final int apothem) {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");

        final Mask mask = new FuzzyBlockMask(this, new BaseBlock(blockType, -1));
        final Vector adjustment = new Vector(1, 1, 1).multiply(apothem - 1);
        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
                position.add(adjustment.multiply(-1)), position.add(adjustment));
        final Pattern pattern = (new BaseBlock(BlockID.AIR));
        return this.replaceBlocks(region, mask, pattern);
    }

    public boolean canBypassAll(Region region, boolean get, boolean set) {
        if (wrapped) return false;
        if (history != null) return false;
        FaweRegionExtent regionExtent = getRegionExtent();
        if (!(region instanceof CuboidRegion)) return false;
        if (regionExtent != null) {
            if (!(region instanceof CuboidRegion)) return false;
            Vector pos1 = region.getMinimumPoint();
            Vector pos2 = region.getMaximumPoint();
            boolean contains = false;
            for (Region current : regionExtent.getRegions()) {
                if (current.contains((int) pos1.getX(), pos1.getBlockY(), pos1.getBlockZ()) && current.contains(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ())) {
                    contains = true;
                    break;
                }
            }
            if (!contains) return false;
        }
        long area = region.getArea();
        FaweLimit left = getLimitLeft();
        if (!left.isUnlimited() && (((get || getChangeTask() != null) && left.MAX_CHECKS <= area) || (set && left.MAX_CHANGES <= area)))
            return false;
        if (getChangeTask() != getChangeSet()) return false;
        if (!Masks.isNull(getMask()) || !Masks.isNull(getSourceMask())) return false;
        if (getBlockBag() != null) return false;
        return true;
    }

    public boolean hasExtraExtents() {
        return wrapped || getMask() != null || getSourceMask() != null || history != null;
    }

    /**
     * Sets all the blocks inside a region to a given block type.
     *
     * @param region the region
     * @param block  the block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int setBlocks(final Region region, final BaseBlock block) {
        checkNotNull(region);
        checkNotNull(block);
        if (canBypassAll(region, false, true) && !block.hasNbtData()) {
            return changes = queue.setBlocks((CuboidRegion) region, block.getId(), block.getData());
        }
        try {
            if (hasExtraExtents()) {
                RegionVisitor visitor = new RegionVisitor(region, new BlockReplace(extent, (block)), this);
                Operations.completeBlindly(visitor);
                this.changes += visitor.getAffected();
            } else {
                Iterator<BlockVector> iter = region.iterator();
                while (iter.hasNext()) {
                    if (this.extent.setBlock(iter.next(), block)) {
                        changes++;
                    }
                }
            }
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
        return changes;
    }

    /**
     * Sets all the blocks inside a region to a given pattern.
     *
     * @param region  the region
     * @param pattern the pattern that provides the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int setBlocks(final Region region, final Pattern pattern) {
        checkNotNull(region);
        checkNotNull(pattern);
        if (pattern instanceof BlockPattern) {
            return setBlocks(region, ((BlockPattern) pattern).getBlock());
        }
        if (pattern instanceof BaseBlock) {
            return setBlocks(region, (BaseBlock) pattern);
        }
        final BlockReplace replace = new BlockReplace(EditSession.this, pattern);
        final RegionVisitor visitor = new RegionVisitor(region, replace, queue instanceof MappedFaweQueue ? (MappedFaweQueue) queue : null);
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region      the region to replace the blocks within
     * @param filter      a list of block types to match, or null to use {@link com.sk89q.worldedit.masks.ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int replaceBlocks(final Region region, final Set<BaseBlock> filter, final BaseBlock replacement) {
//        if (canBypassAll()) {
//            queue.replaceBlocks(regionWrapper, blocks, block);
//            return changes = region.getArea();
//        }
        // TODO fast replace
        return this.replaceBlocks(region, filter, (Pattern) (replacement));
    }


    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region  the region to replace the blocks within
     * @param filter  a list of block types to match, or null to use {@link com.sk89q.worldedit.masks.ExistingBlockMask}
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int replaceBlocks(final Region region, final Set<BaseBlock> filter, final Pattern pattern) {
//        if (pattern instanceof BaseBlock) {
//            return replaceBlocks(region, filter, ((BaseBlock) pattern));
//        }
        final Mask mask = filter == null ? new ExistingBlockMask(this) : new FuzzyBlockMask(this, filter);
        return this.replaceBlocks(region, mask, pattern);
    }

//    public int replaceBlocks(final Region region, final Mask mask, final BaseBlock block) {
    // TODO fast replace
//    }

    /**
     * Replaces all the blocks matching a given mask, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region  the region to replace the blocks within
     * @param mask    the mask that blocks must match
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int replaceBlocks(final Region region, final Mask mask, final Pattern pattern) {
        checkNotNull(region);
        checkNotNull(mask);
        checkNotNull(pattern);
        final BlockReplace replace = new BlockReplace(EditSession.this, pattern);
        final RegionMaskingFilter filter = new RegionMaskingFilter(mask, replace);
        final RegionVisitor visitor = new RegionVisitor(region, filter, queue instanceof MappedFaweQueue ? (MappedFaweQueue) queue : null);
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Sets the blocks at the center of the given region to the given pattern.
     * If the center sits between two blocks on a certain axis, then two blocks
     * will be placed to mark the center.
     *
     * @param region  the region to find the center of
     * @param pattern the replacement pattern
     * @return the number of blocks placed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int center(final Region region, final Pattern pattern) {
        checkNotNull(region);
        checkNotNull(pattern);

        final Vector center = region.getCenter();
        final Region centerRegion = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
                center.floor(), center.ceil());
        return this.setBlocks(centerRegion, pattern);
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block  the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidFaces(final Region region, final BaseBlock block) {
        return this.makeCuboidFaces(region, (Pattern) (block));
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region  the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidFaces(final Region region, final Pattern pattern) {
        checkNotNull(region);
        checkNotNull(pattern);

        final CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        final Region faces = cuboid.getFaces();
        return this.setBlocks(faces, pattern);
    }

    /**
     * Make the faces of the given region. The method by which the faces are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region  the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeFaces(final Region region, final Pattern pattern) throws WorldEditException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return this.makeCuboidFaces(region, pattern);
        } else {
            return new RegionShape(region).generate(this, pattern, true);
        }
    }

    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block  the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidWalls(final Region region, final BaseBlock block) {
        return this.makeCuboidWalls(region, (Pattern) (block));
    }

    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region  the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidWalls(final Region region, final Pattern pattern) {
        checkNotNull(region);
        checkNotNull(pattern);

        final CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        final Region faces = cuboid.getWalls();
        return this.setBlocks(faces, pattern);
    }

    /**
     * Make the walls of the given region. The method by which the walls are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region  the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeWalls(final Region region, final Pattern pattern) throws WorldEditException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return this.makeCuboidWalls(region, pattern);
        } else {
            final int minY = region.getMinimumPoint().getBlockY();
            final int maxY = region.getMaximumPoint().getBlockY();
            final ArbitraryShape shape = new RegionShape(region) {
                @Override
                public BaseBlock getMaterial(final int x, final int y, final int z, final BaseBlock defaultMaterial) {
                    if ((y > maxY) || (y < minY)) {
                        // Put holes into the floor and ceiling by telling ArbitraryShape that the shape goes on outside the region
                        return defaultMaterial;
                    }

                    return super.getMaterial(x, y, z, defaultMaterial);
                }
            };
            return shape.generate(this, pattern, true);
        }
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param block  the placed block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int overlayCuboidBlocks(final Region region, final BaseBlock block) {
        checkNotNull(block);
        return this.overlayCuboidBlocks(region, (Pattern) (block));
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region  the region
     * @param pattern the placed block pattern
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int overlayCuboidBlocks(final Region region, final Pattern pattern) {
        checkNotNull(region);
        checkNotNull(pattern);
        final BlockReplace replace = new BlockReplace(EditSession.this, pattern);
        final RegionOffset offset = new RegionOffset(new Vector(0, 1, 0), replace);
        int minY = region.getMinimumPoint().getBlockY();
        int maxY = Math.min(getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockY() + 1);
        SurfaceRegionFunction surface = new SurfaceRegionFunction(this, offset, minY, maxY);
        FlatRegionVisitor visitor = new FlatRegionVisitor(asFlatRegion(region), surface, this);
        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Turns the first 3 layers into dirt/grass and the bottom layers
     * into rock, like a natural Minecraft mountain.
     *
     * @param region the region to affect
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int naturalizeCuboidBlocks(final Region region) {
        checkNotNull(region);
        final Naturalizer naturalizer = new Naturalizer(EditSession.this);
        final FlatRegion flatRegion = Regions.asFlatRegion(region);
        final LayerVisitor visitor = new LayerVisitor(flatRegion, minimumBlockY(region), maximumBlockY(region), naturalizer);
        Operations.completeBlindly(visitor);
        return this.changes = naturalizer.getAffected();
    }

    public int stackCuboidRegion(final Region region, final Vector dir, final int count, final boolean copyAir) {
        return stackCuboidRegion(region, dir, count, copyAir, true, false);
    }

    /**
     * Stack a cuboid region.
     *
     * @param region  the region to stack
     * @param dir     the direction to stack
     * @param count   the number of times to stack
     * @param copyAir true to also copy air blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int stackCuboidRegion(final Region region, final Vector dir, final int count, final boolean copyAir, boolean copyEntities, boolean copyBiomes) {
        checkNotNull(region);
        checkNotNull(dir);
        checkArgument(count >= 1, "count >= 1 required");
        final Vector size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        final Vector to = region.getMinimumPoint();
        final ForwardExtentCopy copy = new ForwardExtentCopy(EditSession.this, region, EditSession.this, to);
        copy.setCopyEntities(copyEntities);
        copy.setCopyBiomes(copyBiomes);
        copy.setRepetitions(count);
        copy.setTransform(new AffineTransform().translate(dir.multiply(size)));
        Mask sourceMask = getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(EditSession.this);
            copy.setSourceMask(sourceMask);
            setSourceMask(null);
        }
        if (!copyAir) {
            copy.setSourceMask(new ExistingBlockMask(EditSession.this));
        }
        Operations.completeBlindly(copy);
        return this.changes = copy.getAffected();
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region      the region to move
     * @param dir         the direction
     * @param distance    the distance to move
     * @param copyAir     true to copy air blocks
     * @param replacement the replacement block to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveRegion(final Region region, final Vector dir, final int distance, final boolean copyAir, final BaseBlock replacement) {
        return moveRegion(region, dir, distance, copyAir, true, false, replacement);
    }

    public int moveRegion(final Region region, final Vector dir, final int distance, final boolean copyAir, final boolean copyEntities, final boolean copyBiomes, BaseBlock replacement) {
        return moveRegion(region, dir, distance, copyAir, copyEntities, copyBiomes, (Pattern) replacement);
    }

    public int moveRegion(final Region region, final Vector dir, final int distance, final boolean copyAir, final boolean copyEntities, final boolean copyBiomes, Pattern replacement) {
        checkNotNull(region);
        checkNotNull(dir);
        checkArgument(distance >= 1, "distance >= 1 required");
        final Vector displace = dir.multiply(distance);

        final Vector size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        final Vector to = region.getMinimumPoint().add(dir.multiply(distance));

        Vector disAbs = displace.positive();

        if (disAbs.getBlockX() < size.getBlockX() && disAbs.getBlockY() < size.getBlockY() && disAbs.getBlockZ() < size.getBlockZ()) {
            // Buffer if overlapping
            queue.dequeue();
        }

        final ForwardExtentCopy copy = new ForwardExtentCopy(EditSession.this, region, EditSession.this, to);

        if (replacement == null) replacement = nullBlock;
        final BlockReplace remove = replacement instanceof ExistingPattern ? null : new BlockReplace(EditSession.this, replacement);

        copy.setCopyBiomes(copyBiomes);
        copy.setCopyEntities(copyEntities);
        copy.setSourceFunction(remove);
        copy.setRepetitions(1);
        Mask sourceMask = getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(EditSession.this);
            copy.setSourceMask(sourceMask);
            setSourceMask(null);
        }
        if (!copyAir) {
            copy.setSourceMask(new ExistingBlockMask(EditSession.this));
        }
        Operations.completeBlindly(copy);
        return this.changes = copy.getAffected();
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region      the region to move
     * @param dir         the direction
     * @param distance    the distance to move
     * @param copyAir     true to copy air blocks
     * @param replacement the replacement block to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveCuboidRegion(final Region region, final Vector dir, final int distance, final boolean copyAir, final BaseBlock replacement) {
        return this.moveRegion(region, dir, distance, copyAir, replacement);
    }

    /**
     * Drain nearby pools of water or lava.
     *
     * @param origin the origin to drain from, which will search a 3x3 area
     * @param radius the radius of the removal, where a value should be 0 or greater
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drainArea(final Vector origin, final double radius) {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");
        Mask liquidMask;
        // Not thread safe, use hardcoded liquidmask
//        if (getWorld() != null) {
//            liquidMask = getWorld().createLiquidMask();
//        } else {
        liquidMask = new BlockMask(this,
                new BaseBlock(BlockID.STATIONARY_LAVA, -1),
                new BaseBlock(BlockID.LAVA, -1),
                new BaseBlock(BlockID.STATIONARY_WATER, -1),
                new BaseBlock(BlockID.WATER, -1));
//        }
        final MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(0, EditSession.this.getMaximumPoint().getBlockY()),
                new RegionMask(
                        new EllipsoidRegion(null, origin,
                                new Vector(radius, radius, radius))), liquidMask);

        final BlockReplace replace = new BlockReplace(EditSession.this, new BaseBlock(BlockID.AIR));
        final RecursiveVisitor visitor = new RecursiveVisitor(mask, replace, (int) (radius * 2 + 1), this);

        // Around the origin in a 3x3 block
        for (final BlockVector position : CuboidRegion.fromCenter(origin, 1)) {
            if (mask.test(position)) {
                visitor.visit(position);
            }
        }

        Operations.completeBlindly(visitor);
        return this.changes = visitor.getAffected();
    }

    /**
     * Fix liquids so that they turn into stationary blocks and extend outward.
     *
     * @param origin     the original position
     * @param radius     the radius to fix
     * @param moving     the block ID of the moving liquid
     * @param stationary the block ID of the stationary liquid
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fixLiquid(final Vector origin, final double radius, final int moving, final int stationary) {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");
        // Our origins can only be liquids
        BlockMask liquidMask = new BlockMask(
                this,
                new BaseBlock(moving, -1),
                new BaseBlock(stationary, -1)
        ) {
            @Override
            public boolean test(Vector vector) {
                BaseBlock block = getBlock(vector);
                return block.getId() == moving || block.getId() == stationary;
            }
        };

        BlockMask blockMask = new BlockMask(
                this,
                new BaseBlock(moving, -1),
                new BaseBlock(stationary, -1),
                new BaseBlock(0, 0)
        ) {
            @Override
            public boolean test(Vector vector) {
                BaseBlock block = getBlock(vector);
                return block.getId() == 0 || block.getId() == moving || block.getId() == stationary;
            }
        };

        // There are boundaries that the routine needs to stay in
        MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(0, Math.min(origin.getBlockY(), getMaximumPoint().getBlockY())),
                new RegionMask(new EllipsoidRegion(null, origin, new Vector(radius, radius, radius))),
                blockMask);

        BlockReplace replace = new BlockReplace(this, (FaweCache.getBlock(stationary, 0)));
        NonRisingVisitor visitor = new NonRisingVisitor(mask, replace, (int) (radius * 2 + 1), this);

        // Around the origin in a 3x3 block
        for (BlockVector position : CuboidRegion.fromCenter(origin, 1)) {
            if (liquidMask.test(position)) {
                visitor.visit(position);
            }
        }

        Operations.completeBlindly(visitor);
        return getBlockChangeCount();
    }

    /**
     * Makes a cylinder.
     *
     * @param pos    Center of the cylinder
     * @param block  The block pattern to use
     * @param radius The cylinder's radius
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(final Vector pos, final Pattern block, final double radius, final int height, final boolean filled) {
        return this.makeCylinder(pos, block, radius, radius, height, filled);
    }

    /**
     * Makes a cylinder.
     *
     * @param pos     Center of the cylinder
     * @param block   The block pattern to use
     * @param radiusX The cylinder's largest north/south extent
     * @param radiusZ The cylinder's largest east/west extent
     * @param height  The cylinder's up/down extent. If negative, extend downward.
     * @param filled  If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(Vector pos, final Pattern block, double radiusX, double radiusZ, int height, final boolean filled) {
        return makeCylinder(pos, block, radiusX, radiusZ, height, 0, filled);
    }

    public int makeHollowCylinder(Vector pos, final Pattern block, double radiusX, double radiusZ, int height, double thickness) {
        return makeCylinder(pos, block, radiusX, radiusZ, height, thickness, false);
    }

    private int makeCylinder(Vector pos, final Pattern block, double radiusX, double radiusZ, int height, double thickness, final boolean filled) {
        radiusX += 0.5;
        radiusZ += 0.5;

        if (height == 0) {
            return this.changes;
        } else if (height < 0) {
            height = -height;
            pos.mutY(pos.getY() - height);
        }

        if (pos.getBlockY() < 0) {
            pos.mutY(0);
        } else if (((pos.getBlockY() + height) - 1) > maxY) {
            height = (maxY - pos.getBlockY()) + 1;
        }

        final double invRadiusX = 1 / (radiusX);
        final double invRadiusZ = 1 / (radiusZ);

        int px = pos.getBlockX();
        int py = pos.getBlockY();
        int pz = pos.getBlockZ();
        MutableBlockVector mutable = new MutableBlockVector();

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);
        double dx, dxz, dz;
        double nextXn = 0;

        if (thickness != 0) {
            double nextMinXn = 0;
            final double minInvRadiusX = 1 / (radiusX - thickness);
            final double minInvRadiusZ = 1 / (radiusZ - thickness);
            forX:
            for (int x = 0; x <= ceilRadiusX; ++x) {
                final double xn = nextXn;
                double dx2 = nextMinXn * nextMinXn;
                nextXn = (x + 1) * invRadiusX;
                nextMinXn = (x + 1) * minInvRadiusX;
                double nextZn = 0;
                double nextMinZn = 0;
                dx = xn * xn;
                forZ:
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    double dz2 = nextMinZn * nextMinZn;
                    nextZn = (z + 1) * invRadiusZ;
                    nextMinZn = (z + 1) * minInvRadiusZ;
                    dz = zn * zn;
                    dxz = dx + dz;
                    if (dxz > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break forZ;
                    }

                    if ((dz2 + nextMinXn * nextMinXn <= 1) && (nextMinZn * nextMinZn + dx2 <= 1)) {
                        continue;
                    }

                    for (int y = 0; y < height; ++y) {
                        this.setBlock(mutable.setComponents(px + x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px + x, py + y, pz - z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz - z), block);
                    }
                }
            }
        } else {
            forX:
            for (int x = 0; x <= ceilRadiusX; ++x) {
                final double xn = nextXn;
                nextXn = (x + 1) * invRadiusX;
                double nextZn = 0;
                dx = xn * xn;
                forZ:
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;
                    dz = zn * zn;
                    dxz = dx + dz;
                    if (dxz > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break forZ;
                    }

                    if (!filled) {
                        if ((dz + nextXn * nextXn <= 1) && (nextZn * nextZn + dx <= 1)) {
                            continue;
                        }
                    }

                    for (int y = 0; y < height; ++y) {
                        this.setBlock(mutable.setComponents(px + x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz + z), block);
                        this.setBlock(mutable.setComponents(px + x, py + y, pz - z), block);
                        this.setBlock(mutable.setComponents(px - x, py + y, pz - z), block);
                    }
                }
            }
        }

        return this.changes;
    }

    public int makeCircle(Vector pos, final Pattern block, double radiusX, double radiusY, double radiusZ, boolean filled, Vector normal) {
        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        normal = normal.normalize();
        double nx = normal.getX();
        double ny = normal.getY();
        double nz = normal.getZ();


        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        int px = pos.getBlockX();
        int py = pos.getBlockY();
        int pz = pos.getBlockZ();
        MutableBlockVector mutable = new MutableBlockVector();

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double threshold = 0.5;

        LocalBlockVectorSet set = new LocalBlockVectorSet();

        double nextXn = 0;
        double dx, dy, dz, dxy, dxyz;
        forX:
        for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            dx = xn * xn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY:
            for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                dy = yn * yn;
                dxy = dx + dy;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ:
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    dz = zn * zn;
                    dxyz = dxy + dz;
                    nextZn = (z + 1) * invRadiusZ;
                    if (dxyz > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }
                    if (!filled) {
                        if (nextXn * nextXn + dy + dz <= 1 && nextYn * nextYn + dx + dz <= 1 && nextZn * nextZn + dx + dy <= 1) {
                            continue;
                        }
                    }

                    if (Math.abs((x) * nx + (y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py + y, pz + z), block);
                    if (Math.abs((-x) * nx + (y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py + y, pz + z), block);
                    if (Math.abs((x) * nx + (-y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py - y, pz + z), block);
                    if (Math.abs((x) * nx + (y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py + y, pz - z), block);
                    if (Math.abs((-x) * nx + (-y) * ny + (z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py - y, pz + z), block);
                    if (Math.abs((x) * nx + (-y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px + x, py - y, pz - z), block);
                    if (Math.abs((-x) * nx + (y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py + y, pz - z), block);
                    if (Math.abs((-x) * nx + (-y) * ny + (-z) * nz) < threshold)
                        setBlock(mutable.setComponents(px - x, py - y, pz - z), block);
                }
            }
        }

        return changes;
    }

    /**
     * Makes a sphere.
     *
     * @param pos    Center of the sphere or ellipsoid
     * @param block  The block pattern to use
     * @param radius The sphere's radius
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeSphere(final Vector pos, final Pattern block, final double radius, final boolean filled) {
        return this.makeSphere(pos, block, radius, radius, radius, filled);
    }

    /**
     * Makes a sphere or ellipsoid.
     *
     * @param pos     Center of the sphere or ellipsoid
     * @param block   The block pattern to use
     * @param radiusX The sphere/ellipsoid's largest north/south extent
     * @param radiusY The sphere/ellipsoid's largest up/down extent
     * @param radiusZ The sphere/ellipsoid's largest east/west extent
     * @param filled  If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeSphere(final Vector pos, final Pattern block, double radiusX, double radiusY, double radiusZ, final boolean filled) {
        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        int px = pos.getBlockX();
        int py = pos.getBlockY();
        int pz = pos.getBlockZ();

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = 0;
        double dx, dy, dz, dxy, dxyz;
        forX:
        for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            dx = xn * xn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY:
            for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                dy = yn * yn;
                dxy = dx + dy;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ:
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    dz = zn * zn;
                    dxyz = dxy + dz;
                    nextZn = (z + 1) * invRadiusZ;
                    if (dxyz > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }

                    if (!filled) {
                        if (nextXn * nextXn + dy + dz <= 1 && nextYn * nextYn + dx + dz <= 1 && nextZn * nextZn + dx + dy <= 1) {
                            continue;
                        }
                    }

                    this.setBlock(px + x, py + y, pz + z, block);
                    this.setBlock(px - x, py + y, pz + z, block);
                    this.setBlock(px + x, py - y, pz + z, block);
                    this.setBlock(px + x, py + y, pz - z, block);
                    this.setBlock(px - x, py - y, pz + z, block);
                    this.setBlock(px + x, py - y, pz - z, block);
                    this.setBlock(px - x, py + y, pz - z, block);
                    this.setBlock(px - x, py - y, pz - z, block);
                }
            }
        }

        return changes;
    }

    /**
     * Makes a pyramid.
     *
     * @param position a position
     * @param block    a block
     * @param size     size of pyramid
     * @param filled   true if filled
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePyramid(final Vector position, final Pattern block, int size, final boolean filled) {
        final int height = size;

        for (int y = 0; y <= height; ++y) {
            size--;
            for (int x = 0; x <= size; ++x) {
                for (int z = 0; z <= size; ++z) {
                    if ((filled && (z <= size) && (x <= size)) || (z == size) || (x == size)) {
                        this.setBlock(position.add(x, y, z), block);
                        this.setBlock(position.add(-x, y, z), block);
                        this.setBlock(position.add(x, y, -z), block);
                        this.setBlock(position.add(-x, y, -z), block);
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Thaw blocks in a radius.
     *
     * @param position the position
     * @param radius   the radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int thaw(final Vector position, final double radius) {
        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oy = position.getBlockY();
        final int oz = position.getBlockZ();

        final BaseBlock air = new BaseBlock(0);
        final BaseBlock water = new BaseBlock(BlockID.STATIONARY_WATER);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= (ox + ceilRadius); ++x) {
            int dx = x - ox;
            int dx2 = dx * dx;
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                int dz = z - oz;
                int dz2 = dz * dz;
                if (dx2 + dz2 > radiusSq) {
                    continue;
                }
                for (int y = maxY; y >= 1; --y) {
                    final int id = FaweCache.getId(queue.getCombinedId4Data(x, y, z));
                    switch (id) {
                        case BlockID.ICE:
                            this.setBlock(x, y, z, water);
                            break;
                        case BlockID.SNOW:
                            this.setBlock(x, y, z, air);
                            break;
                        case BlockID.AIR:
                            continue;
                        default:
                            break;
                    }

                    break;
                }
            }
        }

        return changes;
    }

    /**
     * Make snow in a radius.
     *
     * @param position a position
     * @param radius   a radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int simulateSnow(final Vector position, final double radius) {

        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oy = position.getBlockY();
        final int oz = position.getBlockZ();

        final BaseBlock ice = new BaseBlock(BlockID.ICE);
        final BaseBlock snow = new BaseBlock(BlockID.SNOW);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= (ox + ceilRadius); ++x) {
            int dx = x - ox;
            int dx2 = dx * dx;
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                int dz = z - oz;
                int dz2 = dz * dz;
                if (dx2 + dz2 > radiusSq) {
                    continue;
                }
                outer:
                for (int y = maxY; y >= 1; --y) {
                    final int id = FaweCache.getId(queue.getCombinedId4Data(x, y, z));
                    if (id == BlockID.AIR) {
                        continue;
                    }
                    // Ice!
                    if ((id == BlockID.WATER) || (id == BlockID.STATIONARY_WATER)) {
                        this.setBlock(x, y, z, ice);
                        break;
                    }

                    // Snow should not cover these blocks
                    if (BlockType.isTranslucent(id)) {
                        switch (id) {
                            case BlockID.LEAVES:
                            case BlockID.LEAVES2:
                                break;
                            default:
                                break outer;
                        }
                    }

                    // Too high?
                    if (y == maxY) {
                        break;
                    }

                    // add snow cover
                    this.setBlock(x, y + 1, z, snow);
                    break;
                }
            }
        }

        return changes;
    }

    /**
     * Make dirt green.
     *
     * @param position a position
     * @param radius   a radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #green(Vector, double, boolean)}.
     */
    @Deprecated
    public int green(final Vector position, final double radius) {
        return this.green(position, radius, true);
    }

    /**
     * Make dirt green.
     *
     * @param position       a position
     * @param radius         a radius
     * @param onlyNormalDirt only affect normal dirt (data value 0)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int green(final Vector position, final double radius, final boolean onlyNormalDirt) {

        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oy = position.getBlockY();
        final int oz = position.getBlockZ();

        final BaseBlock grass = new BaseBlock(BlockID.GRASS);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= (ox + ceilRadius); ++x) {
            int dx = x - ox;
            int dx2 = dx * dx;
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                int dz = z - oz;
                int dz2 = dz * dz;
                if (dx2 + dz2 > radiusSq) {
                    continue;
                }
                loop:
                for (int y = maxY; y >= 1; --y) {
                    BaseBlock block = getLazyBlock(x, y, z);
                    final int id = block.getId();
                    final int data = block.getData();

                    switch (id) {
                        case BlockID.DIRT:
                            if (onlyNormalDirt && (data != 0)) {
                                break loop;
                            }
                            this.setBlock(x, y, z, grass);
                            break loop;

                        case BlockID.WATER:
                        case BlockID.STATIONARY_WATER:
                        case BlockID.LAVA:
                        case BlockID.STATIONARY_LAVA:
                            // break on liquids...
                            break loop;

                        default:
                            // ...and all non-passable blocks
                            if (!BlockType.canPassThrough(id, data)) {
                                break loop;
                            }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Makes pumpkin patches randomly in an area around the given position.
     *
     * @param position the base position
     * @param apothem  the apothem of the (square) area
     * @return number of patches created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePumpkinPatches(final Vector position, final int apothem) {
        // We want to generate pumpkins
        final GardenPatchGenerator generator = new GardenPatchGenerator(EditSession.this);
        generator.setPlant(GardenPatchGenerator.getPumpkinPattern());

        // In a region of the given radius
        final FlatRegion region = new CuboidRegion(EditSession.this.getWorld(), // Causes clamping of Y range
                position.add(-apothem, -5, -apothem), position.add(apothem, 10, apothem));
        final double density = 0.02;

        final GroundFunction ground = new GroundFunction(new ExistingBlockMask(EditSession.this), generator);
        final LayerVisitor visitor = new LayerVisitor(region, minimumBlockY(region), maximumBlockY(region), ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeBlindly(visitor);
        return this.changes = ground.getAffected();
    }

    /**
     * Makes a forest.
     *
     * @param basePosition  a position
     * @param size          a size
     * @param density       between 0 and 1, inclusive
     * @param treeGenerator the tree genreator
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeForest(final Vector basePosition, final int size, final double density, final TreeGenerator treeGenerator) {
        try {
            for (int x = basePosition.getBlockX() - size; x <= (basePosition.getBlockX() + size); ++x) {
                for (int z = basePosition.getBlockZ() - size; z <= (basePosition.getBlockZ() + size); ++z) {
                    // Don't want to be in the ground
                    if (!this.getLazyBlock(x, basePosition.getBlockY(), z).isAir()) {
                        continue;
                    }
                    // The gods don't want a tree here
                    if (FaweCache.RANDOM.random(65536) >= (density * 65536)) {
                        continue;
                    } // def 0.05
                    this.changes++;
                    for (int y = basePosition.getBlockY(); y >= (basePosition.getBlockY() - 10); --y) {
                        final int t = getLazyBlock(x, y, z).getType();
                        if ((t == BlockID.GRASS) || (t == BlockID.DIRT)) {
                            treeGenerator.generate(EditSession.this, mutable.setComponents(x, y + 1, z));
                            break;
                        } else if (t == BlockID.SNOW) {
                            setBlock(x, y, z, nullBlock);
                        } else if (t != BlockID.AIR) { // Trees won't grow on this!
                            break;
                        }
                    }
                }
            }
        } catch (MaxChangedBlocksException ignore) {
        }
        return this.changes;
    }

    /**
     * Get the block distribution inside a region.
     *
     * @param region a region
     * @return the results
     */
    public List<Countable<Integer>> getBlockDistribution(final Region region) {
        int[] counter = new int[4096];

        if (region instanceof CuboidRegion) {
            // Doing this for speed
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            final int minX = min.getBlockX();
            final int minY = min.getBlockY();
            final int minZ = min.getBlockZ();
            final int maxX = max.getBlockX();
            final int maxY = max.getBlockY();
            final int maxZ = max.getBlockZ();

            if (canBypassAll(region, true, false)) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            int id = FaweCache.getId(queue.getCombinedId4Data(x, y, z));
                            counter[id]++;
                        }
                    }
                }
            } else {
                MutableBlockVector mutable = new MutableBlockVector(minX, minY, minZ);
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            int id = getBlock(x, y, z).getId();
                            counter[id]++;
                        }
                    }
                }
            }

        } else {
            for (final Vector pt : region) {
                final int id = this.getBlock(pt).getId();
                counter[id]++;
            }
        }
        List<Countable<Integer>> distribution = new ArrayList<>();
        for (int i = 0; i < counter.length; i++) {
            int count = counter[i];
            if (count != 0) {
                distribution.add(new Countable<Integer>(i, count));
            }
        }
        Collections.sort(distribution);
        // Collections.reverse(distribution);

        return distribution;
    }

    /**
     * Get the block distribution (with data values) inside a region.
     *
     * @param region a region
     * @return the results
     */
    public List<Countable<BaseBlock>> getBlockDistributionWithData(final Region region) {
        int[] counter = new int[Character.MAX_VALUE + 1];

        if (region instanceof CuboidRegion) {
            // Doing this for speed
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            final int minX = min.getBlockX();
            final int minY = min.getBlockY();
            final int minZ = min.getBlockZ();
            final int maxX = max.getBlockX();
            final int maxY = max.getBlockY();
            final int maxZ = max.getBlockZ();

            if (canBypassAll(region, true, false)) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            counter[queue.getCombinedId4Data(x, y, z)]++;
                        }
                    }
                }
            } else {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            final BaseBlock blk = getBlock(x, y, z);
                            counter[FaweCache.getCombined(blk)]++;
                        }
                    }
                }
            }
        } else {
            for (final Vector pt : region) {
                BaseBlock blk = this.getBlock(pt);
                counter[FaweCache.getCombined(blk)]++;
            }
        }
        List<Countable<BaseBlock>> distribution = new ArrayList<>();
        for (int i = 0; i < counter.length; i++) {
            int count = counter[i];
            if (count != 0) {
                distribution.add(new Countable<BaseBlock>(FaweCache.CACHE_BLOCK[i], count));
            }
        }
        // Collections.reverse(distribution);
        return distribution;
    }

    public int makeShape(final Region region, final Vector zero, final Vector unit, final Pattern pattern, final String expressionString, final boolean hollow) throws ExpressionException,
            MaxChangedBlocksException {
        final Expression expression = Expression.compile(expressionString, "x", "y", "z", "type", "data");
        expression.optimize();

        final RValue typeVariable = expression.getVariable("type", false);
        final RValue dataVariable = expression.getVariable("data", false);

        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(this, unit, zero);
        expression.setEnvironment(environment);

        final ArbitraryShape shape = new ArbitraryShape(region) {
            @Override
            public BaseBlock getMaterial(final int x, final int y, final int z, final BaseBlock defaultMaterial) {
                //TODO Optimize - avoid vector creation (math)
                final Vector current = mutable.setComponents(x, y, z);
                environment.setCurrentBlock(current);
                final Vector scaled = current.subtract(zero).divide(unit);

                try {
                    if (expression.evaluate(scaled.getX(), scaled.getY(), scaled.getZ(), defaultMaterial.getType(), defaultMaterial.getData()) <= 0) {
                        return null;
                    }

                    return FaweCache.getBlock((int) typeVariable.getValue(), (int) dataVariable.getValue());
                } catch (final Exception e) {
                    Fawe.debug("Failed to create shape: " + e);
                    return null;
                }
            }
        };

        try {
            return shape.generate(this, pattern, hollow);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public int deformRegion(final Region region, final Vector zero, final Vector unit, final String expressionString) throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = Expression.compile(expressionString, "x", "y", "z");
        expression.optimize();
        final RValue x = expression.getVariable("x", false).optimize();
        final RValue y = expression.getVariable("y", false).optimize();
        final RValue z = expression.getVariable("z", false).optimize();
        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(this, unit, zero);
        expression.setEnvironment(environment);
        final Vector zero2 = zero.add(0.5, 0.5, 0.5);

        RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {

            private MutableBlockVector mutable = new MutableBlockVector();

            @Override
            public boolean apply(Vector position) throws WorldEditException {
                try {
                    // offset, scale
                    double sx = (position.getX() - zero.getX()) / unit.getX();
                    double sy = (position.getY() - zero.getY()) / unit.getY();
                    double sz = (position.getZ() - zero.getZ()) / unit.getZ();
                    // transform
                    expression.evaluate(sx, sy, sz);
                    int xv = (int) (x.getValue() * unit.getX() + zero2.getX());
                    int yv = (int) (y.getValue() * unit.getY() + zero2.getY());
                    int zv = (int) (z.getValue() * unit.getZ() + zero2.getZ());
                    // read block from world
                    BaseBlock material = FaweCache.CACHE_BLOCK[queue.getCombinedId4DataDebug(xv, yv, zv, 0, EditSession.this)];
                    // queue operation
                    return setBlockFast(position, material);
                } catch (EvaluationException e) {
                    throw new RuntimeException(e);
                }
            }
        }, this);
        Operations.completeBlindly(visitor);
        changes += visitor.getAffected();
        return changes;
    }

    /**
     * Hollows out the region (Semi-well-defined for non-cuboid selections).
     *
     * @param region    the region to hollow out.
     * @param thickness the thickness of the shell to leave (manhattan distance)
     * @param pattern   The block pattern to use
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int hollowOutRegion(final Region region, final int thickness, final Pattern pattern) {


        final Set outside = new LocalBlockVectorSet();

        final Vector min = region.getMinimumPoint();
        final Vector max = region.getMaximumPoint();

        final int minX = min.getBlockX();
        final int minY = min.getBlockY();
        final int minZ = min.getBlockZ();
        final int maxX = max.getBlockX();
        final int maxY = max.getBlockY();
        final int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                this.recurseHollow(region, new BlockVector(x, y, minZ), outside);
                this.recurseHollow(region, new BlockVector(x, y, maxZ), outside);
            }
        }

        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                this.recurseHollow(region, new BlockVector(minX, y, z), outside);
                this.recurseHollow(region, new BlockVector(maxX, y, z), outside);
            }
        }

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                this.recurseHollow(region, new BlockVector(x, minY, z), outside);
                this.recurseHollow(region, new BlockVector(x, maxY, z), outside);
            }
        }

        for (int i = 1; i < thickness; ++i) {
            final Set newOutside = new LocalBlockVectorSet();
            outer:
            for (final BlockVector position : region) {
                for (final Vector recurseDirection : this.recurseDirections) {
                    final BlockVector neighbor = position.add(recurseDirection).toBlockVector();

                    if (outside.contains(neighbor)) {
                        newOutside.add(position);
                        continue outer;
                    }
                }
            }
            outside.addAll(newOutside);
        }

        try {
            outer:
            for (final BlockVector position : region) {
                for (final Vector recurseDirection : this.recurseDirections) {
                    final BlockVector neighbor = position.add(recurseDirection).toBlockVector();

                    if (outside.contains(neighbor)) {
                        continue outer;
                    }
                }
                this.changes++;
                pattern.apply(this.extent, position, position);
            }
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        return changes;
    }

    public int drawLine(final Pattern pattern, final Vector pos1, final Vector pos2, final double radius, final boolean filled) {
        return drawLine(pattern, pos1, pos2, radius, filled, false);
    }

    /**
     * Draws a line (out of blocks) between two vectors.
     *
     * @param pattern The block pattern used to draw the line.
     * @param pos1    One of the points that define the line.
     * @param pos2    The other point that defines the line.
     * @param radius  The radius (thickness) of the line.
     * @param filled  If false, only a shell will be generated.
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawLine(final Pattern pattern, final Vector pos1, final Vector pos2, final double radius, final boolean filled, boolean flat) {

        LocalBlockVectorSet vset = new LocalBlockVectorSet();
        boolean notdrawn = true;

        final int x1 = pos1.getBlockX(), y1 = pos1.getBlockY(), z1 = pos1.getBlockZ();
        final int x2 = pos2.getBlockX(), y2 = pos2.getBlockY(), z2 = pos2.getBlockZ();
        int tipx = x1, tipy = y1, tipz = z1;
        final int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);

        if ((dx + dy + dz) == 0) {
            vset.add(tipx, tipy, tipz);
            notdrawn = false;
        }

        if ((Math.max(Math.max(dx, dy), dz) == dx) && notdrawn) {
            for (int domstep = 0; domstep <= dx; domstep++) {
                tipx = x1 + (domstep * ((x2 - x1) > 0 ? 1 : -1));
                tipy = (int) Math.round(y1 + (((domstep * ((double) dy)) / (dx)) * ((y2 - y1) > 0 ? 1 : -1)));
                tipz = (int) Math.round(z1 + (((domstep * ((double) dz)) / (dx)) * ((z2 - z1) > 0 ? 1 : -1)));
                vset.add(tipx, tipy, tipz);
            }
            notdrawn = false;
        }

        if ((Math.max(Math.max(dx, dy), dz) == dy) && notdrawn) {
            for (int domstep = 0; domstep <= dy; domstep++) {
                tipy = y1 + (domstep * ((y2 - y1) > 0 ? 1 : -1));
                tipx = (int) Math.round(x1 + (((domstep * ((double) dx)) / (dy)) * ((x2 - x1) > 0 ? 1 : -1)));
                tipz = (int) Math.round(z1 + (((domstep * ((double) dz)) / (dy)) * ((z2 - z1) > 0 ? 1 : -1)));

                vset.add(tipx, tipy, tipz);
            }
            notdrawn = false;
        }

        if ((Math.max(Math.max(dx, dy), dz) == dz) && notdrawn) {
            for (int domstep = 0; domstep <= dz; domstep++) {
                tipz = z1 + (domstep * ((z2 - z1) > 0 ? 1 : -1));
                tipy = (int) Math.round(y1 + (((domstep * ((double) dy)) / (dz)) * ((y2 - y1) > 0 ? 1 : -1)));
                tipx = (int) Math.round(x1 + (((domstep * ((double) dx)) / (dz)) * ((x2 - x1) > 0 ? 1 : -1)));
                vset.add(tipx, tipy, tipz);
            }
        }
        Set<Vector> newVset;
        if (flat) {
            newVset = this.getStretched(vset, radius);
            if (!filled) {
                newVset = this.getOutline(newVset);
            }
        } else {
            newVset = this.getBallooned(vset, radius);
            if (!filled) {
                newVset = this.getHollowed(newVset);
            }
        }
        return this.setBlocks(newVset, pattern);
    }

    /**
     * Draws a spline (out of blocks) between specified vectors.
     *
     * @param pattern     The block pattern used to draw the spline.
     * @param nodevectors The list of vectors to draw through.
     * @param tension     The tension of every node.
     * @param bias        The bias of every node.
     * @param continuity  The continuity of every node.
     * @param quality     The quality of the spline. Must be greater than 0.
     * @param radius      The radius (thickness) of the spline.
     * @param filled      If false, only a shell will be generated.
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawSpline(final Pattern pattern, final List<Vector> nodevectors, final double tension, final double bias, final double continuity, final double quality, final double radius, final boolean filled) {
        LocalBlockVectorSet vset = new LocalBlockVectorSet();
        final List<Node> nodes = new ArrayList<Node>(nodevectors.size());

        final KochanekBartelsInterpolation interpol = new KochanekBartelsInterpolation();

        for (final Vector nodevector : nodevectors) {
            final Node n = new Node(nodevector);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }

        interpol.setNodes(nodes);
        final double splinelength = interpol.arcLength(0, 1);
        for (double loop = 0; loop <= 1; loop += 1D / splinelength / quality) {
            final Vector tipv = interpol.getPosition(loop);
            final int tipx = (int) Math.round(tipv.getX());
            final int tipy = (int) Math.round(tipv.getY());
            final int tipz = (int) Math.round(tipv.getZ());
            if (radius == 0) {
                setBlock(tipx, tipy, tipz, pattern.next(tipx, tipy, tipz));
            } else {
                vset.add(tipx, tipy, tipz);
            }
        }
        Set<Vector> newVset;
        if (radius != 0) {
            newVset = this.getBallooned(vset, radius);
            if (!filled) {
                newVset = this.getHollowed(newVset);
            }
            return this.setBlocks(newVset, pattern);
        }
        return changes;
    }

    private Set<Vector> getBallooned(final Set<Vector> vset, final double radius) {
        if (radius < 1) {
            return vset;
        }
        final LocalBlockVectorSet returnset = new LocalBlockVectorSet();
        final int ceilrad = (int) Math.ceil(radius);
        for (final Vector v : vset) {
            final int tipx = v.getBlockX(), tipy = v.getBlockY(), tipz = v.getBlockZ();
            for (int loopx = tipx - ceilrad; loopx <= (tipx + ceilrad); loopx++) {
                for (int loopy = tipy - ceilrad; loopy <= (tipy + ceilrad); loopy++) {
                    for (int loopz = tipz - ceilrad; loopz <= (tipz + ceilrad); loopz++) {
                        if (MathMan.hypot(loopx - tipx, loopy - tipy, loopz - tipz) <= radius) {
                            returnset.add(loopx, loopy, loopz);
                        }
                    }
                }
            }
        }
        return returnset;
    }

    public Set<Vector> getStretched(final Set<Vector> vset, final double radius) {
        if (radius < 1) {
            return vset;
        }
        final LocalBlockVectorSet returnset = new LocalBlockVectorSet();
        final int ceilrad = (int) Math.ceil(radius);
        for (final Vector v : vset) {
            final int tipx = v.getBlockX(), tipy = v.getBlockY(), tipz = v.getBlockZ();
            for (int loopx = tipx - ceilrad; loopx <= (tipx + ceilrad); loopx++) {
                for (int loopz = tipz - ceilrad; loopz <= (tipz + ceilrad); loopz++) {
                    if (MathMan.hypot(loopx - tipx, 0, loopz - tipz) <= radius) {
                        returnset.add(loopx, v.getBlockY(), loopz);
                    }
                }
            }
        }
        return returnset;
    }

    public Set<Vector> getOutline(final Set<Vector> vset) {
        // TODO optimize - vset instanceof LocalBlockVectorSet -> avoid Vector creation
        final LocalBlockVectorSet returnset = new LocalBlockVectorSet();
        for (final Vector v : vset) {
            final double x = v.getX(), y = v.getY(), z = v.getZ();
            if (!(vset.contains(new Vector(x + 1, y, z))
                    && vset.contains(new Vector(x - 1, y, z))
                    && vset.contains(new Vector(x, y, z + 1)) && vset.contains(new Vector(x, y, z - 1)))) {
                returnset.add(v);
            }
        }
        return returnset;
    }

    public Set<Vector> getHollowed(final Set<Vector> vset) {
        //TODO Optimize - avoid vector creation
        final Set returnset = new LocalBlockVectorSet();
        for (final Vector v : vset) {
            final double x = v.getX(), y = v.getY(), z = v.getZ();
            if (!(vset.contains(new Vector(x + 1, y, z))
                    && vset.contains(new Vector(x - 1, y, z))
                    && vset.contains(new Vector(x, y + 1, z))
                    && vset.contains(new Vector(x, y - 1, z))
                    && vset.contains(new Vector(x, y, z + 1)) && vset.contains(new Vector(x, y, z - 1)))) {
                returnset.add(v);
            }
        }
        return returnset;
    }

    public void recurseHollow(final Region region, final BlockVector origin, final Set<BlockVector> outside) {
        //TODO Optimize - avoid vector creation
        final ArrayDeque<BlockVector> queue = new ArrayDeque<BlockVector>();
        queue.addLast(origin);

        while (!queue.isEmpty()) {
            final BlockVector current = queue.removeFirst();
            if (!BlockType.canPassThrough(this.getBlockType(current), this.getBlockData(current))) {
                continue;
            }

            if (!outside.add(current)) {
                continue;
            }

            if (!region.contains(current)) {
                continue;
            }

            for (final Vector recurseDirection : this.recurseDirections) {
                queue.addLast(current.add(recurseDirection).toBlockVector());
            }
        }
    }

    public int makeBiomeShape(final Region region, final Vector zero, final Vector unit, final BaseBiome biomeType, final String expressionString, final boolean hollow) throws ExpressionException,
            MaxChangedBlocksException {
        final Vector2D zero2D = zero.toVector2D();
        final Vector2D unit2D = unit.toVector2D();

        final Expression expression = Expression.compile(expressionString, "x", "z");
        expression.optimize();

        final EditSession editSession = this;
        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(editSession, unit, zero);
        expression.setEnvironment(environment);

        final ArbitraryBiomeShape shape = new ArbitraryBiomeShape(region) {
            @Override
            protected BaseBiome getBiome(final int x, final int z, final BaseBiome defaultBiomeType) {
                final Vector2D current = new Vector2D(x, z);
                environment.setCurrentBlock(current.toVector(0));
                final Vector2D scaled = current.subtract(zero2D).divide(unit2D);

                try {
                    if (expression.evaluate(scaled.getX(), scaled.getZ()) <= 0) {
                        return null;
                    }

                    return defaultBiomeType;
                } catch (final Exception e) {
                    Fawe.debug("Failed to create shape: " + e);
                    return null;
                }
            }
        };

        return shape.generate(this, biomeType, hollow);
    }

    private double lengthSq(final double x, final double y, final double z) {
        return (x * x) + (y * y) + (z * z);
    }

    private double lengthSq(final double x, final double z) {
        return (x * x) + (z * z);
    }


    @Override
    public String getName() {
        return worldName;
    }

    @Override
    public int getBlockLightLevel(Vector position) {
        return queue.getEmmittedLight(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public boolean clearContainerBlockContents(Vector pos) {
        BaseBlock block = getBlock(pos);
        CompoundTag nbt = block.getNbtData();
        if (nbt != null) {
            if (nbt.containsKey("items")) {
                block.setNbtData(null);
                return setBlock(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), block);
            }
        }
        return false;
    }

    public boolean regenerate(final Region region) {
        return regenerate(region, this);
    }

    @Override
    public boolean regenerate(final Region region, final EditSession session) {
        return session.regenerate(region, null, null);
    }

    private void setExistingBlocks(Vector pos1, Vector pos2) {
        for (int x = (int) pos1.getX(); x <= (int) pos2.getX(); x++) {
            for (int z = pos1.getBlockZ(); z <= pos2.getBlockZ(); z++) {
                for (int y = (int) pos1.getY(); y <= (int) pos2.getY(); y++) {
                    int from = queue.getCombinedId4Data(x, y, z);
                    short id = (short) (from >> 4);
                    byte data = (byte) (from & 0xf);
                    queue.setBlock(x, y, z, id, data);
                    if (FaweCache.hasNBT(id)) {
                        CompoundTag tile = queue.getTileEntity(x, y, z);
                        if (tile != null) {
                            queue.setTile(x, y, z, tile);
                        }
                    }
                }
            }
        }
    }

    public boolean regenerate(final Region region, final BaseBiome biome, final Long seed) {
        //TODO Optimize - avoid Vector2D creation (make mutable)
        final FaweQueue queue = this.getQueue();
        queue.setChangeTask(null);
        final FaweChangeSet fcs = (FaweChangeSet) this.getChangeSet();
        final FaweRegionExtent fe = this.getRegionExtent();
        final boolean cuboid = region instanceof CuboidRegion;
        if (fe != null && cuboid) {
            Vector max = region.getMaximumPoint();
            Vector min = region.getMinimumPoint();
            if (!fe.contains(max.getBlockX(), max.getBlockY(), max.getBlockZ()) && !fe.contains(min.getBlockX(), min.getBlockY(), min.getBlockZ())) {
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION);
            }
        }
        final Set<Vector2D> chunks = region.getChunks();
        MutableBlockVector mutable = new MutableBlockVector();
        MutableBlockVector2D mutable2D = new MutableBlockVector2D();
        for (Vector2D chunk : chunks) {
            final int cx = chunk.getBlockX();
            final int cz = chunk.getBlockZ();
            final int bx = cx << 4;
            final int bz = cz << 4;
            final Vector cmin = new Vector(bx, 0, bz);
            final Vector cmax = cmin.add(15, getMaxY(), 15);
            final boolean containsBot1 = (fe == null || fe.contains(cmin.getBlockX(), cmin.getBlockY(), cmin.getBlockZ()));
            final boolean containsBot2 = region.contains(cmin);
            final boolean containsTop1 = (fe == null || fe.contains(cmax.getBlockX(), cmax.getBlockY(), cmax.getBlockZ()));
            final boolean containsTop2 = region.contains(cmax);
            if (((containsBot2 && containsTop2)) && !containsBot1 && !containsTop1) {
                continue;
            }
            boolean conNextX = chunks.contains(mutable2D.setComponents(cx + 1, cz));
            boolean conNextZ = chunks.contains(mutable2D.setComponents(cx, cz + 1));
            boolean containsAny = false;
            if (cuboid && containsBot1 && containsBot2 && containsTop1 && containsTop2 && conNextX && conNextZ) {
                containsAny = true;
                if (fcs != null) {
                    for (int x = 0; x < 16; x++) {
                        int xx = x + bx;
                        for (int z = 0; z < 16; z++) {
                            int zz = z + bz;
                            for (int y = 0; y < getMaxY() + 1; y++) {
                                int from = queue.getCombinedId4DataDebug(xx, y, zz, 0, EditSession.this);
                                if (!FaweCache.hasNBT(from >> 4)) {
                                    fcs.add(xx, y, zz, from, 0);
                                } else {
                                    try {
                                        Vector loc = new Vector(xx, y, zz);
                                        BaseBlock block = getLazyBlock(loc);
                                        fcs.add(loc, block, FaweCache.CACHE_BLOCK[0]);
                                    } catch (Throwable e) {
                                        fcs.add(xx, y, zz, from, 0);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (!conNextX) {
                    setExistingBlocks(new Vector(bx + 16, 0, bz), new Vector(bx + 31, getMaxY(), bz + 15));
                }
                if (!conNextZ) {
                    setExistingBlocks(new Vector(bx, 0, bz + 16), new Vector(bx + 15, getMaxY(), bz + 31));
                }
                if (!chunks.contains(mutable2D.setComponents(cx + 1, cz + 1)) && !conNextX && !conNextZ) {
                    setExistingBlocks(new Vector(bx + 16, 0, bz + 16), new Vector(bx + 31, getMaxY(), bz + 31));
                }
                for (int x = 0; x < 16; x++) {
                    int xx = x + bx;
                    mutable.mutX(xx);
                    for (int z = 0; z < 16; z++) {
                        int zz = z + bz;
                        mutable.mutZ(zz);
                        for (int y = 0; y < getMaxY() + 1; y++) {
                            mutable.mutY(y);
                            boolean contains = (fe == null || fe.contains(xx, y, zz)) && region.contains(mutable);
                            if (contains) {
                                containsAny = true;
                                if (fcs != null) {
                                    int from = queue.getCombinedId4Data(xx, y, zz);
                                    if (!FaweCache.hasNBT(from >> 4)) {
                                        fcs.add(xx, y, zz, from, 0);
                                    } else {
                                        try {
                                            BaseBlock block = getLazyBlock(mutable);
                                            fcs.add(mutable, block, FaweCache.CACHE_BLOCK[0]);
                                        } catch (Throwable e) {
                                            fcs.add(xx, y, zz, from, 0);
                                        }
                                    }
                                }
                            } else {
                                int from = queue.getCombinedId4Data(xx, y, zz);
                                short id = (short) (from >> 4);
                                byte data = (byte) (from & 0xf);
                                queue.setBlock(xx, y, zz, id, data);
                                if (FaweCache.hasNBT(id)) {
                                    CompoundTag tile = queue.getTileEntity(xx, y, zz);
                                    if (tile != null) {
                                        queue.setTile(xx, y, zz, tile);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (containsAny) {
                changes++;
                TaskManager.IMP.sync(new RunnableVal<Object>() {
                    @Override
                    public void run(Object value) {
                        queue.regenerateChunk(cx, cz, biome, seed);
                    }
                });
            }
        }
        if (changes != 0) {
            flushQueue();
            return true;
        } else {
            this.queue.clear();
        }
        return false;
    }

    @Override
    public void dropItem(Vector position, BaseItemStack item) {
        if (getWorld() != null) {
            getWorld().dropItem(position, item);
        }
    }

    public boolean generateTree(TreeGenerator.TreeType type, Vector position) {
        return generateTree(type, this, position);
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, Vector position) {
        if (getWorld() != null) {
            try {
                return getWorld().generateTree(type, editSession, position);
            } catch (MaxChangedBlocksException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public static Class<?> inject() {
        return EditSession.class;
    }
}
