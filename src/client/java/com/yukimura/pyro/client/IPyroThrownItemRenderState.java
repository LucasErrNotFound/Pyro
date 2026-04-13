package com.yukimura.pyro.client;

/**
 * Mixin interface that adds a pyro_igniteTime field to ThrownItemRenderState,
 * allowing MixinThrownItemRenderer to pass ignition data from extractRenderState
 * (where the entity is available) to submit (where it is not).
 */
public interface IPyroThrownItemRenderState {
    long pyro_getIgniteTime();
    void pyro_setIgniteTime(long time);
}
