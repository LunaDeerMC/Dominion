package cn.lunadeer.dominion.v26.nms;

import cn.lunadeer.dominion.utils.dialogui.DialogPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NMSDialogCallbackBridgeImplTest {
    @Test
    void decodesSupportedNbtValueTreeWithoutLeakingTags() {
        CompoundTag nested = new CompoundTag();
        nested.putLong("count", 3);
        ListTag list = new ListTag();
        list.add(net.minecraft.nbt.StringTag.valueOf("a"));
        list.add(net.minecraft.nbt.StringTag.valueOf("b"));

        CompoundTag source = new CompoundTag();
        source.putString("text", "hello");
        source.putBoolean("enabled", true);
        source.putDouble("number", 2.5);
        source.put("nested", nested);
        source.put("list", list);

        DialogPayload decoded = NMSDialogCallbackBridgeImpl.decodePayload(source);
        assertEquals("hello", ((DialogPayload.StringValue) decoded.get("text")).value());
        assertTrue(((DialogPayload.BooleanValue) decoded.get("enabled")).value());
        assertEquals(2.5, ((DialogPayload.FloatValue) decoded.get("number")).value());
        assertInstanceOf(DialogPayload.CompoundValue.class, decoded.get("nested"));
        assertInstanceOf(DialogPayload.ListValue.class, decoded.get("list"));
    }

    @Test
    void rejectsOverLimitPayloadBeforeCallbackLookup() {
        CompoundTag source = new CompoundTag();
        for (int index = 0; index < 257; index++) {
            source.putInt("value_" + index, index);
        }
        assertThrows(IllegalArgumentException.class,
                () -> NMSDialogCallbackBridgeImpl.decodePayload(source));
    }
}
