package com.abdelaziz.pluto.mixin.fast_entity_access;

import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntitySection.class)
public interface EntitySectionAccessor<T> {
    @Accessor
    ClassInstanceMultiMap<T> getStorage();
}