package pneumaticCraft.common.thirdparty.cofh;

import net.minecraft.entity.EntityCreature;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.ForgeDirection;
import pneumaticCraft.api.drone.IBlockInteractHandler;
import pneumaticCraft.api.drone.ICustomBlockInteract;
import pneumaticCraft.lib.Textures;
import cofh.api.energy.IEnergyReceiver;
import cofh.api.energy.IEnergyStorage;

public class DroneInteractRFExport implements ICustomBlockInteract{

    @Override
    public String getName(){
        return "rfExport";
    }

    @Override
    public ResourceLocation getTexture(){
        return Textures.PROG_WIDGET_RF_EX;
    }

    @Override
    public boolean doInteract(ChunkPosition pos, EntityCreature drone, IBlockInteractHandler interactHandler, boolean simulate){
        TileEntity te = drone.worldObj.getTileEntity(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ);
        if(te instanceof IEnergyReceiver) {
            IEnergyReceiver receiver = (IEnergyReceiver)te;
            IEnergyStorage droneEnergy = CoFHCore.getEnergyStorage(drone);
            for(ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                if(interactHandler.getSides()[d.ordinal()]) {
                    int transferedEnergy = droneEnergy.extractEnergy(receiver.receiveEnergy(d, Math.min(1000, interactHandler.useCount() ? interactHandler.getRemainingCount() : Integer.MAX_VALUE), true), true);
                    if(transferedEnergy > 0) {
                        if(!simulate) {
                            interactHandler.decreaseCount(transferedEnergy);
                            droneEnergy.extractEnergy(transferedEnergy, false);
                            receiver.receiveEnergy(d, transferedEnergy, false);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
}