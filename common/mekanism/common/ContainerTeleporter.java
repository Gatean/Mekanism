package mekanism.common;

import ic2.api.item.IElectricItem;
import mekanism.common.SlotEnergy.SlotDischarge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import universalelectricity.core.item.IItemElectric;

public class ContainerTeleporter extends Container
{
    private TileEntityTeleporter tileEntity;

    public ContainerTeleporter(InventoryPlayer inventory, TileEntityTeleporter tentity)
    {
        tileEntity = tentity;
        addSlotToContainer(new SlotDischarge(tentity, 0, 27, 14));
        int slotX;

        for(slotX = 0; slotX < 3; ++slotX)
        {
            for(int slotY = 0; slotY < 9; ++slotY)
            {
                addSlotToContainer(new Slot(inventory, slotY + slotX * 9 + 9, 8 + slotY * 18, 84 + slotX * 18));
            }
        }

        for(slotX = 0; slotX < 9; ++slotX)
        {
            addSlotToContainer(new Slot(inventory, slotX, 8 + slotX * 18, 142));
        }
        
        tileEntity.openChest();
        tileEntity.playersUsing.add(inventory.player);
    }
    
    @Override
    public void onContainerClosed(EntityPlayer entityplayer)
    {
		super.onContainerClosed(entityplayer);
		tileEntity.closeChest();
		tileEntity.playersUsing.remove(entityplayer);
    }

	@Override
    public boolean canInteractWith(EntityPlayer entityplayer)
	{
        return tileEntity.isUseableByPlayer(entityplayer);
    }
    
	@Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotID)
    {
        ItemStack stack = null;
        Slot currentSlot = (Slot)inventorySlots.get(slotID);

        if(currentSlot != null && currentSlot.getHasStack())
        {
            ItemStack slotStack = currentSlot.getStack();
            stack = slotStack.copy();

            if(MekanismUtils.canBeDischarged(slotStack))
            {
	            if(slotID != 0)
	            {
	                if(!mergeItemStack(slotStack, 0, 1, false))
	                {
	                	return null;
	                }
	            }
	            else if(slotID == 0)
	            {
	            	if(!mergeItemStack(slotStack, 1, inventorySlots.size(), true))
	            	{
	            		return null;
	            	}
	            }
            }
            else {
	        	if(slotID >= 1 && slotID <= 27)
	        	{
	        		if(!mergeItemStack(slotStack, 28, inventorySlots.size(), false))
	        		{
	        			return null;
	        		}
	        	}
	        	else if(slotID > 27)
	        	{
	        		if(!mergeItemStack(slotStack, 1, 27, false))
	        		{
	        			return null;
	        		}
	        	}
	        	else {
            		if(!mergeItemStack(slotStack, 1, inventorySlots.size(), true))
            		{
            			return null;
            		}
            	}
            }
            
            if(slotStack.stackSize == 0)
            {
                currentSlot.putStack((ItemStack)null);
            }
            else {
                currentSlot.onSlotChanged();
            }

            if(slotStack.stackSize == stack.stackSize)
            {
                return null;
            }

            currentSlot.onPickupFromSlot(player, slotStack);
        }

        return stack;
    }
}
