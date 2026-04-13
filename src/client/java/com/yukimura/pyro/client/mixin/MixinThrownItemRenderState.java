package com.yukimura.pyro.client.mixin;

import com.yukimura.pyro.client.IPyroThrownItemRenderState;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ThrownItemRenderState.class)
public class MixinThrownItemRenderState implements IPyroThrownItemRenderState {

    @Unique
    private long pyro_igniteTime = Long.MIN_VALUE;

    @Override
    public long pyro_getIgniteTime() {
        return pyro_igniteTime;
    }

    @Override
    public void pyro_setIgniteTime(long time) {
        pyro_igniteTime = time;
    }
}
