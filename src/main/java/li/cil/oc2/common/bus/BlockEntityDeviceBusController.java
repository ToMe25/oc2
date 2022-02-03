/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public final class BlockEntityDeviceBusController extends CommonDeviceBusController {
    private final Runnable onBusChunkLoadedStateChanged = this::scheduleBusScan;
    private final HashSet<TrackedChunk> trackedChunks = new HashSet<>();
    private final BlockEntity blockEntity;

    ///////////////////////////////////////////////////////////////////

    public BlockEntityDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption, final BlockEntity blockEntity) {
        super(root, baseEnergyConsumption);
        this.blockEntity = blockEntity;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void dispose() {
        super.dispose();

        removeListeners(trackedChunks);
        trackedChunks.clear();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void onAfterBusScan() {
        super.onAfterBusScan();

        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        final HashSet<TrackedChunk> newTrackedChunks = new HashSet<>();
        for (final DeviceBusElement element : getElements()) {
            if (element instanceof final BlockDeviceBusElement blockElement) {
                final LevelAccessor elementLevel = blockElement.getLevel();
                final BlockPos elementPosition = blockElement.getPosition();
                if (elementLevel != null) {
                    newTrackedChunks.add(TrackedChunk.of(elementLevel, elementPosition));
                    newTrackedChunks.add(TrackedChunk.of(elementLevel, elementPosition.relative(Direction.NORTH)));
                    newTrackedChunks.add(TrackedChunk.of(elementLevel, elementPosition.relative(Direction.EAST)));
                    newTrackedChunks.add(TrackedChunk.of(elementLevel, elementPosition.relative(Direction.SOUTH)));
                    newTrackedChunks.add(TrackedChunk.of(elementLevel, elementPosition.relative(Direction.WEST)));
                }
            }
        }

        // Do not track the chunk the controller itself is in -- this is unneeded because
        // we expect the controller to be disposed if its chunk is unloaded.
        newTrackedChunks.remove(TrackedChunk.of(level, blockEntity.getBlockPos()));

        final HashSet<TrackedChunk> removedChunks = new HashSet<>(trackedChunks);
        removedChunks.removeAll(newTrackedChunks);
        removeListeners(removedChunks);

        final HashSet<TrackedChunk> addedChunks = new HashSet<>(newTrackedChunks);
        newTrackedChunks.removeAll(trackedChunks);
        addListeners(addedChunks);

        trackedChunks.removeAll(removedChunks);
        trackedChunks.addAll(newTrackedChunks);
    }

    ///////////////////////////////////////////////////////////////////

    private void addListeners(final Collection<TrackedChunk> trackedChunks) {
        for (final TrackedChunk trackedChunk : trackedChunks) {
            trackedChunk.tryGetLevel().ifPresent(level -> {
                ServerScheduler.scheduleOnLoad(level, trackedChunk.position, onBusChunkLoadedStateChanged);
                ServerScheduler.scheduleOnUnload(level, trackedChunk.position, onBusChunkLoadedStateChanged);
            });
        }
    }

    private void removeListeners(final Collection<TrackedChunk> trackedChunks) {
        for (final TrackedChunk trackedChunk : trackedChunks) {
            trackedChunk.tryGetLevel().ifPresent(level -> {
                ServerScheduler.cancelOnLoad(level, trackedChunk.position, onBusChunkLoadedStateChanged);
                ServerScheduler.cancelOnUnload(level, trackedChunk.position, onBusChunkLoadedStateChanged);
            });
        }
    }

    private record TrackedChunk(WeakReference<LevelAccessor> level, ChunkPos position) {
        public static TrackedChunk of(final LevelAccessor level, final BlockPos position) {
            return new TrackedChunk(new WeakReference<>(level), new ChunkPos(position));
        }

        public Optional<LevelAccessor> tryGetLevel() {
            return Optional.ofNullable(level.get());
        }
    }
}
