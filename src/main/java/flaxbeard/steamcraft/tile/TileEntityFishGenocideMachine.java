package flaxbeard.steamcraft.tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.WeightedRandomFishable;
import net.minecraftforge.common.util.ForgeDirection;
import flaxbeard.steamcraft.api.ISteamTransporter;
import flaxbeard.steamcraft.api.UtilSteamTransport;
import flaxbeard.steamcraft.entity.EntityFloatingItem;

public class TileEntityFishGenocideMachine extends TileEntity implements ISteamTransporter {
    private static final List field_146036_f = Arrays.asList(new WeightedRandomFishable[] {new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.COD.func_150976_a()), 60), new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.SALMON.func_150976_a()), 25), new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.CLOWNFISH.func_150976_a()), 2), new WeightedRandomFishable(new ItemStack(Items.fish, 1, ItemFishFood.FishType.PUFFERFISH.func_150976_a()), 13)});

	private int steam = 0;
	
	@Override
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        this.steam = par1NBTTagCompound.getShort("steam");
    }

    @Override
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setShort("steam",(short) this.steam);
    }
	
	@Override
	public Packet getDescriptionPacket()
	{
    	super.getDescriptionPacket();
        NBTTagCompound access = new NBTTagCompound();
        access.setInteger("steam", steam);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, access);
	}
	    

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
    {
    	super.onDataPacket(net, pkt);
    	NBTTagCompound access = pkt.func_148857_g();
    	this.steam = access.getInteger("steam");
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    public int calcSourceBlocks() {
    	int water = 0;
    	for (int x = -3; x<4;x++) {
			for (int z = -3; z<4;z++) {
				if (this.worldObj.getBlock(this.xCoord + x, this.yCoord, this.zCoord + z) == Blocks.water) {
					water++;
				}
			}
		}
    	return water;
    }
	
    public ChunkCoordinates randSourceBlock() {
    	ArrayList<ChunkCoordinates> cc = new ArrayList<ChunkCoordinates>();
    	for (int x = -3; x<4;x++) {
			for (int z = -3; z<4;z++) {
				if (this.worldObj.getBlock(this.xCoord + x, this.yCoord, this.zCoord + z) == Blocks.water) {
					cc.add(new ChunkCoordinates(this.xCoord + x, this.yCoord, this.zCoord + z));
				}
			}
		}
    	return cc.get(this.worldObj.rand.nextInt(cc.size()));
    }
	
    
	@Override
	public void updateEntity() {
		if (!this.worldObj.isRemote) {
			ForgeDirection[] distr = { ForgeDirection.UP, ForgeDirection.DOWN };
			UtilSteamTransport.generalDistributionEvent(worldObj, xCoord, yCoord, zCoord,distr);
			UtilSteamTransport.generalPressureEvent(worldObj,xCoord, yCoord, zCoord, this.getPressure(), this.getCapacity());
		}
		int src = calcSourceBlocks();
		if (this.steam > src) {
			this.steam -= src;
			if (this.worldObj.rand.nextInt((int) (300.0F/src)) == 0 && !this.worldObj.isRemote) {		
				ChunkCoordinates loc = randSourceBlock();

				ItemStack fish = ((WeightedRandomFishable)WeightedRandom.getRandomItem(this.worldObj.rand, field_146036_f)).func_150708_a(this.worldObj.rand);
				ItemStack output = fish;
				if (FurnaceRecipes.smelting().getSmeltingResult(fish) != null) {
					output = FurnaceRecipes.smelting().getSmeltingResult(fish);
				}
                this.dropItem(output,loc.posX+0.5F,this.yCoord+1.0F,loc.posZ+0.5F);
			}
		}
	}
	
	@Override
	public float getPressure() {
		return this.steam/1000.0F;
	}

	@Override
	public boolean canInsert(ForgeDirection face) {
		return face == ForgeDirection.DOWN || face == ForgeDirection.UP;
	}

	@Override
	public int getCapacity() {
		return 1000;
	}

	@Override
	public int getSteam() {
		return this.steam;
	}

	@Override
	public void insertSteam(int amount, ForgeDirection face) {
		this.steam+=amount;
	}

	@Override
	public void decrSteam(int i) {
		this.steam -= i;
	}

	@Override
	public boolean doesConnect(ForgeDirection face) {
		return face == ForgeDirection.DOWN || face == ForgeDirection.UP;
	}
	
	public void dropItem(ItemStack item) {
		EntityItem entityItem = new EntityItem(this.worldObj, this.xCoord+0.5F, this.yCoord + 1.25F, this.zCoord+0.5F, item);
		this.worldObj.spawnEntityInWorld(entityItem);
	}

	public void dropItem(ItemStack item, float x, float y, float z) {
		EntityFloatingItem entityItem = new EntityFloatingItem(this.worldObj, x, y, z, item);
		this.worldObj.spawnEntityInWorld(entityItem);
	}
	
	@Override
	public boolean acceptsGauge(ForgeDirection face) {
		return false;
	}
	
	public void explode(){
		ForgeDirection[] distr = { ForgeDirection.UP, ForgeDirection.DOWN };
		UtilSteamTransport.preExplosion(worldObj, xCoord, yCoord, zCoord,distr);
		this.steam = 0;
	}
}
