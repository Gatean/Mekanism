package mekanism.client;

import mekanism.common.MekanismUtils;
import mekanism.common.MekanismUtils.ResourceType;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderRobit extends RenderLiving
{
	public RenderRobit() 
	{
		super(new ModelRobit(), 0.5F);
	}

	@Override
	protected ResourceLocation func_110775_a(Entity entity) 
	{
		return MekanismUtils.getResource(ResourceType.RENDER, "Robit.png");
	}
}
