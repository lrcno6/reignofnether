package com.solegendary.reignofnether.units.monsters;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.ActionName;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.units.Unit;
import com.solegendary.reignofnether.units.goals.MoveToCursorBlockGoal;
import com.solegendary.reignofnether.units.goals.RangedBowAttackUnitGoal;
import com.solegendary.reignofnether.units.goals.SelectedTargetGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreeperUnit extends Creeper implements Unit {

    public CreeperUnit(EntityType<? extends Creeper> p_32278_, Level p_32279_) { super(p_32278_, p_32279_); }

    // region
    public List<AbilityButton> getAbilities() {return abilities;};

    public MoveToCursorBlockGoal getMoveGoal() {return moveGoal;}
    public void setMoveGoal(MoveToCursorBlockGoal moveGoal) {this.moveGoal = moveGoal;}
    public SelectedTargetGoal<? extends LivingEntity> getTargetGoal() {return targetGoal;}
    public void setTargetGoal(SelectedTargetGoal<? extends LivingEntity> targetGoal) {this.targetGoal = targetGoal;}

    public MoveToCursorBlockGoal moveGoal;
    public SelectedTargetGoal<? extends LivingEntity> targetGoal;

    // flags to not reset particular targets so we can persist them for specific actions
    public boolean getRetainAttackMoveTarget() {return retainAttackMoveTarget;}
    public void setRetainAttackMoveTarget(boolean retainAttackMoveTarget) {this.retainAttackMoveTarget = retainAttackMoveTarget;}
    public boolean getRetainAttackTarget() {return retainAttackTarget;}
    public void setRetainAttackTarget(boolean retainAttackTarget) {this.retainAttackTarget = retainAttackTarget;}
    public boolean getRetainMoveTarget() {return retainMoveTarget;}
    public void setRetainMoveTarget(boolean retainMoveTarget) {this.retainMoveTarget = retainMoveTarget;}
    public boolean getRetainFollowTarget() {return retainFollowTarget;}
    public void setRetainFollowTarget(boolean retainFollowTarget) {this.retainFollowTarget = retainFollowTarget;}
    public boolean getRetainHoldPosition() {return retainHoldPosition;}
    public void setRetainHoldPosition(boolean retainHoldPosition) {this.retainHoldPosition = retainHoldPosition;}

    boolean retainAttackMoveTarget = false;
    boolean retainAttackTarget = false;
    boolean retainMoveTarget = false;
    boolean retainFollowTarget = false;
    boolean retainHoldPosition = false;

    public BlockPos getAttackMoveTarget() { return attackMoveTarget; }
    public LivingEntity getFollowTarget() { return followTarget; }
    public boolean getHoldPosition() { return holdPosition; }
    public void setHoldPosition(boolean holdPosition) { this.holdPosition = holdPosition; }

    // if true causes moveGoal and attackGoal to work together to allow attack moving
    // moves to a block but will chase/attack nearby monsters in range up to a certain distance away
    private BlockPos attackMoveTarget = null;
    private LivingEntity followTarget = null; // if nonnull, continuously moves to the target
    private boolean holdPosition = false;

    // which player owns this unit? this format ensures its synched to client without having to use packets
    public String getOwnerName() { return this.entityData.get(ownerDataAccessor); }
    public void setOwnerName(String name) { this.entityData.set(ownerDataAccessor, name); }
    public static final EntityDataAccessor<String> ownerDataAccessor =
            SynchedEntityData.defineId(CreeperUnit.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ownerDataAccessor, "");
    }

    public void resetTargets() {
        if (!this.getRetainAttackMoveTarget())
            attackMoveTarget = null;
        if (!this.getRetainAttackTarget())
            targetGoal.setTarget(null);
        if (!this.getRetainMoveTarget())
            moveGoal.setMoveTarget(null);
        if (!this.getRetainFollowTarget())
            followTarget = null;
        if (!this.getRetainHoldPosition())
            holdPosition = false;
    }

    public void setMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        moveGoal.setMoveTarget(bp);
    }
    public void setAttackTarget(@Nullable LivingEntity target) {
        resetTargets();
        targetGoal.setTarget(target);
    }
    public void setAttackMoveTarget(@Nullable BlockPos bp) {
        resetTargets();
        this.attackMoveTarget = bp;
    }
    public void setFollowTarget(@Nullable LivingEntity target) {
        resetTargets();
        this.followTarget = target;
    }

    // combat stats
    public boolean getWillRetaliate() {return willRetaliate;}
    public int getAttackCooldown() {return attackCooldown;}
    public float getAggroRange() {return aggroRange;}
    public boolean getAggressiveWhenIdle() {return aggressiveWhenIdle;}
    public float getAttackRange() {return attackRange;}

    // endregion

    final public float attackRange = 0; // only used by ranged units
    final public int attackCooldown = 0; // not used by creepers
    final public float aggroRange = 10;
    final public boolean willRetaliate = true; // will attack when hurt by an enemy
    final public boolean aggressiveWhenIdle = false;

    public MeleeAttackGoal attackGoal;

    public void tick() {
        super.tick();
        Unit.tick(this);
    }

    private static final List<AbilityButton> abilities = Arrays.asList(
            new AbilityButton(
                    "Explode",
                    14,
                    "textures/icons/blocks/tnt.png",
                    Keybinds.keyQ,
                    () -> CursorClientEvents.getLeftClickAction() == ActionName.EXPLODE,
                    () -> CursorClientEvents.setLeftClickAction(ActionName.EXPLODE),
                    0, 0, 3
            )
    );

    @Override
    protected void registerGoals() {
        this.moveGoal = new MoveToCursorBlockGoal(this, 1.0f);
        this.targetGoal = new SelectedTargetGoal(this, true, true);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        // TODO: extend this to make it also compatible with the Explode ability
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, moveGoal);
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // TODO: this doesn't cause the creeper to move to the target before attacking
        this.targetSelector.addGoal(4, targetGoal);
    }

    // TODO: specifically ground target explode ability; for targeting a mob just set target for regular attack goal
    public void explode(BlockPos targetPos) {
    }
}