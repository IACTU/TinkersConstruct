package slimeknights.tconstruct.tools.modifiers.traits.melee;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.TinkerHooks;
import slimeknights.tconstruct.library.modifiers.hook.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.NamespacedNBT;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;

import javax.annotation.Nullable;
import java.util.List;

public class InsatibleModifier extends Modifier implements ProjectileHitModifierHook, ConditionalStatModifierHook, MeleeDamageModifierHook, MeleeHitModifierHook, TooltipModifierHook {
  /** Gets the current bonus for the entity */
  private static float getBonus(LivingEntity attacker, int level, TinkerEffect effect) {
    int effectLevel = effect.getLevel(attacker) + 1;
    return level * effectLevel / 4f;
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addHook(this, TinkerHooks.PROJECTILE_HIT, TinkerHooks.CONDITIONAL_STAT, TinkerHooks.MELEE_DAMAGE, TinkerHooks.MELEE_HIT, TinkerHooks.TOOLTIP);
  }

  @Override
  public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
    // gives +2 damage per level at max
    return damage + (getBonus(context.getAttacker(), modifier.getLevel(), TinkerModifiers.insatiableEffect.get()) * tool.getMultiplier(ToolStats.ATTACK_DAMAGE));
  }

  @Override
  public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
    // 8 hits gets you to max, levels faster at higher levels
    if (!context.isExtraAttack() && context.isFullyCharged()) {
      LivingEntity attacker = context.getAttacker();
      int effectLevel = Math.min(7, TinkerModifiers.insatiableEffect.get().getLevel(attacker) + 1);
      TinkerModifiers.insatiableEffect.get().apply(attacker, 5 * 20, effectLevel, true);
    }
  }

  @Override
  public float modifyStat(IToolStackView tool, ModifierEntry modifier, LivingEntity living, FloatToolStat stat, float baseValue, float multiplier) {
    if (stat == ToolStats.PROJECTILE_DAMAGE) {
      // get bonus is +2 damage per level, but we want to half for the actual damage due to velocity stuff
      baseValue += (getBonus(living, modifier.getLevel(), TinkerModifiers.insatiableRangedEffect.get()) / 2f * multiplier);
    }
    return baseValue;
  }

  @Override
  public boolean onProjectileHitEntity(ModifierNBT modifiers, NamespacedNBT persistentData, ModifierEntry modifier, Projectile projectile, EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
    if (attacker != null) {
      int effectLevel = Math.min(7, TinkerModifiers.insatiableRangedEffect.get().getLevel(attacker) + 1);
      TinkerModifiers.insatiableRangedEffect.get().apply(attacker, 10 * 20, effectLevel, true);
    }
    return false;
  }

  @Override
  public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey key, TooltipFlag tooltipFlag) {
    int level = modifier.getLevel();
    float bonus = level * 2;
    if (player != null && key == TooltipKey.SHIFT) {
      bonus = getBonus(player, level, tool.hasTag(TinkerTags.Items.RANGED) ? TinkerModifiers.insatiableRangedEffect.get() : TinkerModifiers.insatiableEffect.get());
    }
    TooltipModifierHook.addDamageBoost(tool, this, bonus, tooltip);
  }
}
