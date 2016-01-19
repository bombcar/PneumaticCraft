package pneumaticCraft.common.ai;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import pneumaticCraft.api.drone.IPathNavigator;
import pneumaticCraft.common.entity.living.EntityDrone;
import pneumaticCraft.common.network.NetworkHandler;
import pneumaticCraft.common.network.PacketPlaySound;
import pneumaticCraft.common.network.PacketSpawnParticle;
import pneumaticCraft.lib.Sounds;

public class EntityPathNavigateDrone extends PathNavigate implements IPathNavigator{

    private final EntityDrone pathfindingEntity;
    public boolean pathThroughLiquid;
    private boolean forceTeleport;
    private int teleportCounter = -1;
    private BlockPos telPos;
    private static final int TELEPORT_TICKS = 120;

    public EntityPathNavigateDrone(EntityDrone pathfindingEntity, World par2World){
        super(pathfindingEntity, par2World);
        this.pathfindingEntity = pathfindingEntity;
    }

    @Override
    public boolean tryMoveToEntityLiving(Entity p_75497_1_, double p_75497_2_){
        return super.tryMoveToEntityLiving(p_75497_1_, p_75497_2_) || isGoingToTeleport();
    }

    /**
     * Returns the path to the given EntityLiving
     */
    @Override
    public PathEntity getPathToEntityLiving(Entity par1Entity){
        BlockPos pos = new BlockPos(par1Entity.posX, par1Entity.getEntityBoundingBox().minY, par1Entity.posZ);
        return getPathToPos(pos); //TODO 1.8 test
    }

    public void setForceTeleport(boolean forceTeleport){
        this.forceTeleport = forceTeleport;
    }

    @Override
    public PathEntity getPathToPos(BlockPos pos){
        if(!pathfindingEntity.isBlockValidPathfindBlock(pos)) return null;
        PathEntity path = null;
        if(!forceTeleport || pos.equals(new BlockPos(pathfindingEntity))) {
            path = super.getPathToPos(pos.down());
            if(path != null) {
                PathPoint finalPoint = path.getFinalPathPoint();
                if(finalPoint == null || !pos.equals(new BlockPos(finalPoint.xCoord, finalPoint.yCoord, finalPoint.zCoord))) path = null;
            }
        }
        teleportCounter = path != null ? -1 : 0;
        telPos = pos;
        pathfindingEntity.setStandby(false);
        return path;
    }

    @Override
    public float getPathSearchRange(){
        return (float)pathfindingEntity.getRange();
    }

    @Override
    public boolean isGoingToTeleport(){
        return teleportCounter >= 0;
    }

    @Override
    public boolean noPath(){
        return super.noPath() && !isGoingToTeleport();
    }

    @Override
    public void onUpdateNavigation(){
        if(isGoingToTeleport()) {
            if(teleportCounter == 0 || teleportCounter == 60) {
                NetworkHandler.sendToAllAround(new PacketPlaySound(Sounds.HUD_INIT, pathfindingEntity.posX, pathfindingEntity.posY, pathfindingEntity.posZ, 0.1F, teleportCounter == 0 ? 0.7F : 1F, true), pathfindingEntity.worldObj);
            }

            if(teleportCounter < TELEPORT_TICKS - 40) {
                Random rand = pathfindingEntity.getRNG();
                float f = (rand.nextFloat() - 0.5F) * 0.02F * teleportCounter;
                float f1 = (rand.nextFloat() - 0.5F) * 0.02F * teleportCounter;
                float f2 = (rand.nextFloat() - 0.5F) * 0.02F * teleportCounter;
                NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.PORTAL, pathfindingEntity.posX, pathfindingEntity.posY, pathfindingEntity.posZ, f, f1, f2), pathfindingEntity.worldObj);
            }

            if(++teleportCounter > TELEPORT_TICKS) {
                if(pathfindingEntity.isBlockValidPathfindBlock(telPos)) {
                    teleport();
                }
                teleportCounter = -1;
                setPath(null, 0);
                pathfindingEntity.getMoveHelper().setMoveTo(telPos.getX(), telPos.getY(), telPos.getZ(), pathfindingEntity.getSpeed());
                pathfindingEntity.addAir(null, -10000);
            }
        } else {
            // super.onUpdateNavigation();
            if(!noPath()) {
                pathFollow();

                if(!noPath()) {
                    Vec3 vec32 = currentPath.getPosition(theEntity);

                    if(vec32 != null) {
                        theEntity.getMoveHelper().setMoveTo(vec32.xCoord, vec32.yCoord, vec32.zCoord, speed);
                    }
                }
            }
        }
    }

    public void teleport(){

        Random rand = pathfindingEntity.getRNG();
        double width = pathfindingEntity.width;
        double height = pathfindingEntity.height;

        short short1 = 128;

        for(int l = 0; l < short1; ++l) {
            double d6 = l / (short1 - 1.0D);
            float f = (rand.nextFloat() - 0.5F) * 0.2F;
            float f1 = (rand.nextFloat() - 0.5F) * 0.2F;
            float f2 = (rand.nextFloat() - 0.5F) * 0.2F;
            double d7 = pathfindingEntity.posX + (telPos.getX() + 0.5 - pathfindingEntity.posX) * d6 + (rand.nextDouble() - 0.5D) * width * 2.0D;
            double d8 = pathfindingEntity.posY + (telPos.getY() - pathfindingEntity.posY) * d6 + rand.nextDouble() * height;
            double d9 = pathfindingEntity.posZ + (telPos.getZ() + 0.5 - pathfindingEntity.posZ) * d6 + (rand.nextDouble() - 0.5D) * width * 2.0D;
            NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.PORTAL, d7, d8, d9, f, f1, f2), pathfindingEntity.worldObj);
        }

        pathfindingEntity.worldObj.playSoundEffect(pathfindingEntity.posX, pathfindingEntity.posY, pathfindingEntity.posZ, "mob.endermen.portal", 1.0F, 1.0F);
        pathfindingEntity.playSound("mob.endermen.portal", 1.0F, 1.0F);

        pathfindingEntity.setPosition(telPos.getX() + 0.5, telPos.getY() + 0.5, telPos.getZ() + 0.5);
    }

    @Override
    public boolean moveToXYZ(double x, double y, double z){
        return tryMoveToXYZ(x, y, z, pathfindingEntity.getSpeed());
    }

    @Override
    public boolean moveToEntity(Entity entity){
        return tryMoveToEntityLiving(entity, pathfindingEntity.getSpeed());
    }

    @Override
    public boolean hasNoPath(){
        return noPath();
    }

    @Override
    public boolean isDirectPathBetweenPoints(Vec3 p_75493_1_, Vec3 p_75493_2_, int p_75493_3_, int p_75493_4_, int p_75493_5_){
        return false;
    }

    @Override
    protected PathFinder getPathFinder(){
        return new PathFinder(new NodeProcessorDrone());
    }

    @Override
    protected Vec3 getEntityPosition(){
        return pathfindingEntity.getDronePos();//TODO 1.8 test if offset is necessary.
    }

    @Override
    protected boolean canNavigate(){
        return true;
    }
}
