package mekanism.common;

import net.minecraft.src.*;

public class ItemMekanism extends Item 
{
	public ItemMekanism(int i)
	{
		super(i);
		setCreativeTab(Mekanism.tabMekanism);
	}

	public String getTextureFile() {
		return "/textures/items.png";
	}
}