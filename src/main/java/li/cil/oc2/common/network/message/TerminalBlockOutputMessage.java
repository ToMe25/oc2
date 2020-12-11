package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class TerminalBlockOutputMessage extends AbstractTerminalBlockMessage {
    public TerminalBlockOutputMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public TerminalBlockOutputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    public static boolean handleMessage(final AbstractTerminalBlockMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                tileEntity -> tileEntity.getTerminal().putOutput(ByteBuffer.wrap(message.data))));
        return true;
    }
}
