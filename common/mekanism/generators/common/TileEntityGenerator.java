package mekanism.generators.common;

import ic2.api.Direction;
import ic2.api.energy.event.EnergyTileSourceEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.tile.IEnergyStorage;

import java.util.ArrayList;
import java.util.EnumSet;

import mekanism.api.ICableOutputter;
import mekanism.api.Object3D;
import mekanism.client.IHasSound;
import mekanism.common.CableUtils;
import mekanism.common.IActiveState;
import mekanism.common.IUniversalCable;
import mekanism.common.Mekanism;
import mekanism.common.MekanismUtils;
import mekanism.common.PacketHandler;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.TileEntityElectricBlock;
import mekanism.common.network.PacketTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import universalelectricity.core.block.IConductor;
import universalelectricity.core.electricity.ElectricityHelper;
import universalelectricity.core.electricity.ElectricityPack;
import universalelectricity.core.grid.IElectricityNetwork;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import buildcraft.api.power.PowerHandler.Type;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;

public abstract class TileEntityGenerator extends TileEntityElectricBlock implements IEnergySource, IEnergyStorage, IPowerReceptor, IPeripheral, IActiveState, IHasSound, ICableOutputter
{
	/** Output per tick this generator can transfer. */
	public double output;
	
	/** Whether or not this block is in it's active state. */
	public boolean isActive;
	
	/** The previous active state for this block. */
	public boolean prevActive;
	
	/**
	 * Generator -- a block that produces energy. It has a certain amount of fuel it can store as well as an output rate.
	 * @param name - full name of this generator
	 * @param maxEnergy - how much energy this generator can store
	 * @param maxFuel - how much fuel this generator can store
	 */
	public TileEntityGenerator(String name, double maxEnergy, double out)
	{
		super(name, maxEnergy);
		
		powerHandler.configure(0, 0, 0, (int)(maxEnergy*Mekanism.TO_BC));
		
		output = out;
		isActive = false;
	}
	
	@Override
	public void onUpdate()
	{	
		super.onUpdate();
		
		if(worldObj.isRemote)
		{
			Mekanism.proxy.registerSound(this);
		}
		
		if(!worldObj.isRemote)
		{
			TileEntity tileEntity = Object3D.get(this).getFromSide(ForgeDirection.getOrientation(facing)).getTileEntity(worldObj);
			
			if(electricityStored > 0)
			{
				if(tileEntity instanceof IUniversalCable)
				{
					setEnergy(electricityStored - (Math.min(electricityStored, output) - CableUtils.emitEnergyToNetwork(Math.min(electricityStored, output), this, ForgeDirection.getOrientation(facing))));
				}
				
				if(!worldObj.isRemote)
				{
					if((tileEntity instanceof IEnergyConductor || tileEntity instanceof IEnergyAcceptor) && Mekanism.hooks.IC2Loaded)
					{
						if(electricityStored >= output)
						{
							EnergyTileSourceEvent event = new EnergyTileSourceEvent(this, (int)(output*Mekanism.TO_IC2));
							MinecraftForge.EVENT_BUS.post(event);
							setEnergy(electricityStored - (output - (event.amount*Mekanism.FROM_IC2)));
						}
					}
					else if(tileEntity instanceof IPowerReceptor && Mekanism.hooks.BuildCraftLoaded)
					{
						PowerReceiver receiver = ((IPowerReceptor)tileEntity).getPowerReceiver(ForgeDirection.getOrientation(facing).getOpposite());
		            	double electricityNeeded = Math.min(receiver.powerRequest(), receiver.getMaxEnergyStored() - receiver.getEnergyStored())*Mekanism.FROM_BC;
		            	double transferEnergy = Math.min(electricityStored, Math.min(electricityNeeded, output));
		            	receiver.receiveEnergy(Type.STORAGE, (float)(transferEnergy*Mekanism.TO_BC), ForgeDirection.getOrientation(facing).getOpposite());
		            	setEnergy(electricityStored - transferEnergy);
					}
				}
			}
			
			if(tileEntity instanceof IConductor)
			{
				ForgeDirection outputDirection = ForgeDirection.getOrientation(facing);
				float provide = getProvide(outputDirection);
	
				if(provide > 0)
				{
					IElectricityNetwork outputNetwork = ElectricityHelper.getNetworkFromTileEntity(tileEntity, outputDirection);
		
					if(outputNetwork != null)
					{
						ElectricityPack request = outputNetwork.getRequest(this);
						
						if(request.getWatts() > 0)
						{
							ElectricityPack sendPack = ElectricityPack.min(ElectricityPack.getFromWatts(getEnergyStored(), getVoltage()), ElectricityPack.getFromWatts(provide, getVoltage()));
							float rejectedPower = outputNetwork.produce(sendPack, this);
							setEnergyStored(getEnergyStored() - (sendPack.getWatts() - rejectedPower));
						}
					}
				}
			}
		}
	}
	
	@Override
	protected EnumSet<ForgeDirection> getConsumingSides()
	{
		return EnumSet.noneOf(ForgeDirection.class);
	}
	
	@Override
	protected EnumSet<ForgeDirection> getOutputtingSides()
	{
		return EnumSet.of(ForgeDirection.getOrientation(facing));
	}
	
	@Override
	public float getRequest(ForgeDirection direction)
	{
		return 0;
	}
	
	@Override
	public float getProvide(ForgeDirection direction)
	{
		return getOutputtingSides().contains(direction) ? (float)Math.min(getMaxEnergy()-getEnergy(), output) : 0;
	}
	
	@Override
	public void invalidate()
	{
		super.invalidate();
		
		if(worldObj.isRemote)
		{
			Mekanism.proxy.unregisterSound(this);
		}
	}
	
	/**
	 * Gets the boost this generator can receive in it's current location.
	 * @return environmental boost
	 */
	public abstract double getEnvironmentBoost();
	
	/**
	 * Whether or not this generator can operate.
	 * @return if the generator can operate
	 */
	public abstract boolean canOperate();
	
	/**
	 * Gets the scaled energy level for the GUI.
	 * @param i - multiplier
	 * @return
	 */
	public int getScaledEnergyLevel(int i)
	{
		return (int)(electricityStored*i / MAX_ELECTRICITY);
	}
	
	@Override
	public boolean getActive()
	{
		return isActive;
	}
	
	@Override
    public void setActive(boolean active)
    {
    	isActive = active;
    	
    	if(prevActive != active)
    	{
    		PacketHandler.sendPacket(Transmission.ALL_CLIENTS, new PacketTileEntity().setParams(Object3D.get(this), getNetworkedData(new ArrayList())));
    	}
    	
    	prevActive = active;
    }
	
	@Override
	public String getType() 
	{
		return getInvName();
	}

	@Override
	public boolean canAttachToSide(int side) 
	{
		return true;
	}

	@Override
	public void attach(IComputerAccess computer) {}

	@Override
	public void detach(IComputerAccess computer) {}
	
	@Override
	public int getMaxEnergyOutput()
	{
		return (int)(output*Mekanism.TO_IC2);
	}
	
	@Override
	public void setFacing(short orientation)
	{
		super.setFacing(orientation);
		
		worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, MekanismGenerators.generatorID);
	}
	
	@Override
	public boolean canSetFacing(int side)
	{
		return side != 0 && side != 1;
	}
	
	@Override
	public boolean emitsEnergyTo(TileEntity receiver, Direction direction)
	{
		return direction.toForgeDirection() == ForgeDirection.getOrientation(facing);
	}
	
	@Override
	public int getStored() 
	{
		return (int)(electricityStored*Mekanism.TO_IC2);
	}

	@Override
	public int getCapacity() 
	{
		return (int)(MAX_ELECTRICITY*Mekanism.TO_IC2);
	}

	@Override
	public int getOutput() 
	{
		return (int)(output*Mekanism.TO_IC2);
	}
	
	@Override
	public boolean isTeleporterCompatible(Direction side) 
	{
		return side.toForgeDirection() == ForgeDirection.getOrientation(facing);
	}
	
	@Override
	public int addEnergy(int amount)
	{
		setEnergy(electricityStored + amount*Mekanism.FROM_IC2);
		return (int)electricityStored;
	}
	
	@Override
	public void setStored(int energy)
	{
		setEnergy(energy*Mekanism.FROM_IC2);
	}
	
	@Override
	public void handlePacketData(ByteArrayDataInput dataStream)
	{
		super.handlePacketData(dataStream);
		isActive = dataStream.readBoolean();
		MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
	}
	
	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);
		data.add(isActive);
		return data;
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbtTags)
    {
        super.readFromNBT(nbtTags);

        isActive = nbtTags.getBoolean("isActive");
    }

	@Override
    public void writeToNBT(NBTTagCompound nbtTags)
    {
        super.writeToNBT(nbtTags);
        
        nbtTags.setBoolean("isActive", isActive);
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}
	
	@Override
	public boolean canOutputTo(ForgeDirection side)
	{
		return side == ForgeDirection.getOrientation(facing);
	}
	
	@Override
	public String getSoundPath()
	{
		return fullName.replace(" ", "").replace("-","").replace("Advanced", "") + ".ogg";
	}
	
	@Override
	public float getVolumeMultiplier()
	{
		return 1;
	}
	
	@Override
	public boolean hasVisual()
	{
		return true;
	}
}
