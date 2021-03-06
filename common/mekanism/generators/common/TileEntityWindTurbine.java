package mekanism.generators.common;

import mekanism.api.Object3D;
import mekanism.common.ChargeUtils;
import mekanism.common.IBoundingBlock;
import mekanism.common.MekanismUtils;
import net.minecraft.item.ItemStack;
import dan200.computer.api.IComputerAccess;

public class TileEntityWindTurbine extends TileEntityGenerator implements IBoundingBlock
{
	/** The angle the blades of this Wind Turbine are currently at. */
	public int angle;
	
	public TileEntityWindTurbine() 
	{
		super("Wind Turbine", 200000, (MekanismGenerators.windGeneration*8)*2);
		inventory = new ItemStack[1];
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		ChargeUtils.charge(0, this);
		
		if(!worldObj.isRemote)
		{
			if(canOperate())
			{
				setActive(true);
				setEnergy(electricityStored + (MekanismGenerators.windGeneration*getMultiplier()));
			}
			else {
				setActive(false);
			}
		}
	}
	
	/** 0 - 8 **/
	public float getMultiplier()
	{
		return worldObj.canBlockSeeTheSky(xCoord, yCoord+4, zCoord) ? (((float)yCoord+4)/(float)256)*8 : 0;
	}
	
	@Override
	public float getVolumeMultiplier()
	{
		return 1.5F;
	}

	@Override
	public String[] getMethodNames() 
	{
		return null;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws Exception 
	{
		return null;
	}

	@Override
	public double getEnvironmentBoost() 
	{
		return getMultiplier();
	}

	@Override
	public boolean canOperate() 
	{
		return electricityStored < MAX_ELECTRICITY && getMultiplier() > 0;
	}

	@Override
	public void onPlace() 
	{
		MekanismUtils.makeBoundingBlock(worldObj, xCoord, yCoord+1, zCoord, Object3D.get(this));
		MekanismUtils.makeBoundingBlock(worldObj, xCoord, yCoord+2, zCoord, Object3D.get(this));
		MekanismUtils.makeBoundingBlock(worldObj, xCoord, yCoord+3, zCoord, Object3D.get(this));
		MekanismUtils.makeBoundingBlock(worldObj, xCoord, yCoord+4, zCoord, Object3D.get(this));
	}

	@Override
	public void onBreak() 
	{
		worldObj.setBlockToAir(xCoord, yCoord+1, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord+2, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord+3, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord+4, zCoord);
		
		worldObj.setBlockToAir(xCoord, yCoord, zCoord);
	}
	
	@Override
	public boolean hasVisual()
	{
		return false;
	}
}
