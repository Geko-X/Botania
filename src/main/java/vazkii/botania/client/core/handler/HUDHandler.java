/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Jan 25, 2014, 6:11:10 PM (GMT)]
 */
package vazkii.botania.client.core.handler;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import baubles.common.lib.PlayerHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.botania.api.lexicon.ILexicon;
import vazkii.botania.api.lexicon.ILexiconable;
import vazkii.botania.api.lexicon.LexiconEntry;
import vazkii.botania.api.mana.ICreativeManaProvider;
import vazkii.botania.api.mana.IManaItem;
import vazkii.botania.api.mana.IManaUsingItem;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.api.recipe.RecipeManaInfusion;
import vazkii.botania.api.wand.IWandHUD;
import vazkii.botania.api.wiki.IWikiProvider;
import vazkii.botania.api.wiki.WikiHooks;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.lib.LibResources;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileAltar;
import vazkii.botania.common.block.tile.TileRuneAltar;
import vazkii.botania.common.block.tile.corporea.TileCorporeaCrystalCube;
import vazkii.botania.common.block.tile.corporea.TileCorporeaIndex;
import vazkii.botania.common.block.tile.corporea.TileCorporeaIndex.InputHandler;
import vazkii.botania.common.block.tile.mana.TilePool;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.helper.PlayerHelper;
import vazkii.botania.common.item.ItemCraftingHalo;
import vazkii.botania.common.item.ItemSextant;
import vazkii.botania.common.item.ItemTwigWand;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.bauble.ItemDodgeRing;
import vazkii.botania.common.item.equipment.bauble.ItemFlightTiara;
import vazkii.botania.common.item.equipment.bauble.ItemMonocle;
import vazkii.botania.common.lib.LibObfuscation;

public final class HUDHandler {

	private HUDHandler() {}

	public static final ResourceLocation manaBar = new ResourceLocation(LibResources.GUI_MANA_HUD);

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onDrawScreenPre(RenderGameOverlayEvent.Pre event) {
		Minecraft mc = Minecraft.getMinecraft();
		Profiler profiler = mc.mcProfiler;

		if(event.getType() == ElementType.HEALTH) {
			profiler.startSection("botania-hud");
			IInventory baublesInv = PlayerHandler.getPlayerBaubles(mc.thePlayer);
			ItemStack amulet = baublesInv.getStackInSlot(0);
			if(amulet != null && amulet.getItem() == ModItems.flightTiara) {
				profiler.startSection("flugelTiara");
				ItemFlightTiara.renderHUD(event.getResolution(), mc.thePlayer, amulet);
				profiler.endSection();
			}

			dodgeRing: {
				ItemStack ring = baublesInv.getStackInSlot(1);
				if(ring == null || !(ring.getItem() instanceof ItemDodgeRing)) {
					ring = baublesInv.getStackInSlot(2);
					if(ring == null || !(ring.getItem() instanceof ItemDodgeRing))
						break dodgeRing;
				}

				profiler.startSection("dodgeRing");
				ItemDodgeRing.renderHUD(event.getResolution(), mc.thePlayer, ring, event.getPartialTicks());
				profiler.endSection();
			}

			profiler.endSection();
		}
	}

	@SubscribeEvent
	public static void onDrawScreenPost(RenderGameOverlayEvent.Post event) {
		Minecraft mc = Minecraft.getMinecraft();
		Profiler profiler = mc.mcProfiler;
		ItemStack main = mc.thePlayer.getHeldItemMainhand();
		ItemStack offhand = mc.thePlayer.getHeldItemOffhand();

		if(event.getType() == ElementType.ALL) {
			profiler.startSection("botania-hud");
			RayTraceResult pos = mc.objectMouseOver;

			if(pos != null) {
				IBlockState state = pos.typeOfHit == RayTraceResult.Type.BLOCK ? mc.theWorld.getBlockState(pos.getBlockPos()) : null;
				Block block = state == null ? null : state.getBlock();
				TileEntity tile = pos.typeOfHit == RayTraceResult.Type.BLOCK ? mc.theWorld.getTileEntity(pos.getBlockPos()) : null;

				if(PlayerHelper.hasAnyHeldItem(mc.thePlayer)) {
					if(pos != null && PlayerHelper.hasHeldItem(mc.thePlayer, ModItems.twigWand)) {
						renderWandModeDisplay(PlayerHelper.getFirstHeldItem(mc.thePlayer, ModItems.twigWand), event.getResolution());

						if(block instanceof IWandHUD) {
							profiler.startSection("wandItem");
							((IWandHUD) block).renderHUD(mc, event.getResolution(), mc.theWorld, pos.getBlockPos());
							profiler.endSection();
						}
					} else if(block != null && PlayerHelper.hasHeldItemClass(mc.thePlayer, ILexicon.class))
						drawLexiconHUD(PlayerHelper.getFirstHeldItemClass(mc.thePlayer, ILexicon.class), state, pos, event.getResolution());
					if(tile != null && tile instanceof TilePool && mc.thePlayer.getHeldItemMainhand() != null)
						renderPoolRecipeHUD(event.getResolution(), (TilePool) tile, mc.thePlayer.getHeldItemMainhand());
				}
				if(tile != null && tile instanceof TileAltar)
					((TileAltar) tile).renderHUD(mc, event.getResolution());
				else if(tile != null && tile instanceof TileRuneAltar)
					((TileRuneAltar) tile).renderHUD(mc, event.getResolution());

				if(tile != null && tile instanceof TileCorporeaCrystalCube)
					renderCrystalCubeHUD(event.getResolution(), (TileCorporeaCrystalCube) tile);
			}

			TileCorporeaIndex.getInputHandler();
			if(!InputHandler.getNearbyIndexes(mc.thePlayer).isEmpty() && mc.currentScreen != null && mc.currentScreen instanceof GuiChat) {
				profiler.startSection("nearIndex");
				renderNearIndexDisplay(event.getResolution());
				profiler.endSection();
			}

			if(MultiblockRenderHandler.currentMultiblock != null && MultiblockRenderHandler.anchor == null) {
				profiler.startSection("multiblockRightClick");
				String s = I18n.format("botaniamisc.rightClickToAnchor");
				mc.fontRendererObj.drawStringWithShadow(s, event.getResolution().getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(s) / 2, event.getResolution().getScaledHeight() / 2 - 30, 0xFFFFFF);
				profiler.endSection();
			}

			if(main != null && main.getItem() instanceof ItemCraftingHalo) {
				profiler.startSection("craftingHalo_main");
				ItemCraftingHalo.renderHUD(event.getResolution(), mc.thePlayer, main);
				profiler.endSection();
			} else if(offhand != null && offhand.getItem() instanceof ItemCraftingHalo) {
				profiler.startSection("craftingHalo_off");
				ItemCraftingHalo.renderHUD(event.getResolution(), mc.thePlayer, offhand);
				profiler.endSection();
			}

			if(main != null && main.getItem() instanceof ItemSextant) {
				profiler.startSection("sextant");
				ItemSextant.renderHUD(event.getResolution(), mc.thePlayer, main);
				profiler.endSection();
			}

			/*if(equippedStack != null && equippedStack.getItem() == ModItems.flugelEye) {
				profiler.startSection("flugelEye");
				ItemFlugelEye.renderHUD(event.getResolution(), mc.thePlayer, equippedStack);
				profiler.endSection();
			}*/

			if(Botania.proxy.isClientPlayerWearingMonocle()) {
				profiler.startSection("monocle");
				ItemMonocle.renderHUD(event.getResolution(), mc.thePlayer);
				profiler.endSection();
			}

			profiler.startSection("manaBar");

			EntityPlayer player = mc.thePlayer;
			if(!player.isSpectator()) {
				int totalMana = 0;
				int totalMaxMana = 0;
				boolean anyRequest = false;
				boolean creative = false;

				IInventory mainInv = player.inventory;
				IInventory baublesInv = PlayerHandler.getPlayerBaubles(player);

				int invSize = mainInv.getSizeInventory();
				int size = invSize;
				if(baublesInv != null)
					size += baublesInv.getSizeInventory();

				for(int i = 0; i < size; i++) {
					boolean useBaubles = i >= invSize;
					IInventory inv = useBaubles ? baublesInv : mainInv;
					ItemStack stack = inv.getStackInSlot(i - (useBaubles ? invSize : 0));

					if(stack != null) {
						Item item = stack.getItem();
						if(item instanceof IManaUsingItem)
							anyRequest = anyRequest || ((IManaUsingItem) item).usesMana(stack);
					}
				}

				List<ItemStack> items = ManaItemHandler.getManaItems(player);
				for (ItemStack stack : items) {
					Item item = stack.getItem();
					if(!((IManaItem) item).isNoExport(stack)) {
						totalMana += ((IManaItem) item).getMana(stack);
						totalMaxMana += ((IManaItem) item).getMaxMana(stack);
					}
					if(item instanceof ICreativeManaProvider && ((ICreativeManaProvider) item).isCreative(stack))
						creative = true;
				}

				Map<Integer, ItemStack> baubles = ManaItemHandler.getManaBaubles(player);
				for (Entry<Integer, ItemStack> entry : baubles.entrySet()) {
					ItemStack stack = entry.getValue();
					Item item = stack.getItem();
					if(!((IManaItem) item).isNoExport(stack)) {
						totalMana += ((IManaItem) item).getMana(stack);
						totalMaxMana += ((IManaItem) item).getMaxMana(stack);
					}
					if(item instanceof ICreativeManaProvider && ((ICreativeManaProvider) item).isCreative(stack))
						creative = true;
				}

				if(anyRequest)
					renderManaInvBar(event.getResolution(), creative, totalMana, totalMaxMana);
			}

			profiler.endStartSection("itemsRemaining");
			ItemsRemainingRenderHandler.render(event.getResolution(), event.getPartialTicks());
			profiler.endSection();
			profiler.endSection();

			GlStateManager.color(1F, 1F, 1F, 1F);
		}
	}

	private static void renderWandModeDisplay(ItemStack stack, ScaledResolution res) {
		Minecraft mc = Minecraft.getMinecraft();
		Profiler profiler = mc.mcProfiler;

		profiler.startSection("wandMode");
		int ticks = ReflectionHelper.getPrivateValue(GuiIngame.class, mc.ingameGUI, LibObfuscation.REMAINING_HIGHLIGHT_TICKS);
		ticks -= 15;
		if(ticks > 0) {
			int alpha = Math.min(255, (int) (ticks * 256.0F / 10.0F));
			int color = 0x00CC00 + (alpha << 24);
			String disp = I18n.format(ItemTwigWand.getModeString(stack));

			int x = res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(disp) / 2;
			int y = res.getScaledHeight() - 70;

			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			mc.fontRendererObj.drawStringWithShadow(disp, x, y, color);
			GlStateManager.disableBlend();
		}
		profiler.endSection();
	}

	private static void renderManaInvBar(ScaledResolution res, boolean hasCreative, int totalMana, int totalMaxMana) {
		Minecraft mc = Minecraft.getMinecraft();
		int width = 182;
		int x = res.getScaledWidth() / 2 - width / 2;
		int y = res.getScaledHeight() - ConfigHandler.manaBarHeight;

		if(!hasCreative) {
			if(totalMaxMana == 0)
				width = 0;
			else width *= (double) totalMana / (double) totalMaxMana;
		}

		if(width == 0) {
			if(totalMana > 0)
				width = 1;
			else return;
		}

		Color color = new Color(Color.HSBtoRGB(0.55F, (float) Math.min(1F, Math.sin(System.currentTimeMillis() / 200D) * 0.5 + 1F), 1F));
		GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) (255 - color.getRed()));
		mc.renderEngine.bindTexture(manaBar);

		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		RenderHelper.drawTexturedModalRect(x, y, 0, 0, 251, width, 5);
		GlStateManager.disableBlend();
		GL11.glColor4ub((byte) 255, (byte) 255, (byte) 255, (byte) 255);
	}

	private static void renderPoolRecipeHUD(ScaledResolution res, TilePool tile, ItemStack stack) {
		Minecraft mc = Minecraft.getMinecraft();
		Profiler profiler = mc.mcProfiler;

		profiler.startSection("poolRecipe");
		RecipeManaInfusion recipe = TilePool.getMatchingRecipe(stack, tile.getWorld().getBlockState(tile.getPos().down()));
		if(recipe != null) {
			int x = res.getScaledWidth() / 2 - 11;
			int y = res.getScaledHeight() / 2 + 10;

			int u = tile.getCurrentMana() >= recipe.getManaToConsume() ? 0 : 22;
			int v = mc.thePlayer.getName().equals("haighyorkie") && mc.thePlayer.isSneaking() ? 23 : 8;

			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			mc.renderEngine.bindTexture(manaBar);
			RenderHelper.drawTexturedModalRect(x, y, 0, u, v, 22, 15);
			GlStateManager.color(1F, 1F, 1F, 1F);

			net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
			mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x - 20, y);
			mc.getRenderItem().renderItemAndEffectIntoGUI(recipe.getOutput(), x + 26, y);
			mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, recipe.getOutput(), x + 26, y);
			net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

			GlStateManager.disableLighting();
			GlStateManager.disableBlend();
		}
		profiler.endSection();
	}

	private static void renderCrystalCubeHUD(ScaledResolution res, TileCorporeaCrystalCube tile) {
		Minecraft mc = Minecraft.getMinecraft();
		Profiler profiler = mc.mcProfiler;

		profiler.startSection("crystalCube");
		ItemStack target = tile.getRequestTarget();
		if(target != null) {
			String s1 = target.getDisplayName();
			String s2 = tile.getItemCount() + "x";
			int strlen = Math.max(mc.fontRendererObj.getStringWidth(s1), mc.fontRendererObj.getStringWidth(s2));
			int w = res.getScaledWidth();
			int h = res.getScaledHeight();
			Gui.drawRect(w / 2 + 8, h / 2 - 12, w / 2 + strlen + 32, h / 2 + 10, 0x44000000);
			Gui.drawRect(w / 2 + 6, h / 2 - 14, w / 2 + strlen + 34, h / 2 + 12, 0x44000000);

			mc.fontRendererObj.drawStringWithShadow(target.getDisplayName(), w / 2 + 30, h / 2 - 10, 0x6666FF);
			mc.fontRendererObj.drawStringWithShadow(tile.getItemCount() + "x", w / 2 + 30, h / 2, 0xFFFFFF);
			net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
			GlStateManager.enableRescaleNormal();
			mc.getRenderItem().renderItemAndEffectIntoGUI(target, w / 2 + 10, h / 2 - 10);
			net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
		}

		profiler.endSection();
	}

	private static void drawLexiconHUD(ItemStack stack, IBlockState state, RayTraceResult pos, ScaledResolution res) {
		Minecraft mc = Minecraft.getMinecraft();
		Block block = state.getBlock();
		Profiler profiler = mc.mcProfiler;

		profiler.startSection("lexicon");
		FontRenderer font = mc.fontRendererObj;
		boolean draw = false;
		String drawStr = "";
		String secondLine = "";

		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		int sx = res.getScaledWidth() / 2 - 17;
		int sy = res.getScaledHeight() / 2 + 2;

		if(block instanceof ILexiconable) {
			LexiconEntry entry = ((ILexiconable) block).getEntry(mc.theWorld, pos.getBlockPos(), mc.thePlayer, stack);
			if(entry != null) {
				if(!((ILexicon) stack.getItem()).isKnowledgeUnlocked(stack, entry.getKnowledgeType()))
					font = mc.standardGalacticFontRenderer;

				drawStr = I18n.format(entry.getUnlocalizedName());
				secondLine = TextFormatting.ITALIC + I18n.format(entry.getTagline());
				draw = true;
			}
		}

		if(!draw && pos.entityHit == null) {
			profiler.startSection("wikiLookup");
			if(!block.isAir(state, mc.theWorld, pos.getBlockPos()) && !(block instanceof BlockLiquid)) {
				IWikiProvider provider = WikiHooks.getWikiFor(block);
				String url = provider.getWikiURL(mc.theWorld, pos, mc.thePlayer);
				if(url != null && !url.isEmpty()) {
					String name = provider.getBlockName(mc.theWorld, pos, mc.thePlayer);
					String wikiName = provider.getWikiName(mc.theWorld, pos, mc.thePlayer);
					drawStr = name + " @ " + TextFormatting.AQUA + wikiName;
					draw = true;
				}
			}
			profiler.endSection();
		}

		if(draw) {
			if(!mc.thePlayer.isSneaking()) {
				drawStr = "?";
				secondLine = "";
				font = mc.fontRendererObj;
			}

			mc.getRenderItem().renderItemIntoGUI(new ItemStack(ModItems.lexicon), sx, sy);
			GlStateManager.disableLighting();
			font.drawStringWithShadow(drawStr, sx + 20, sy + 4, 0xFFFFFFFF);
			font.drawStringWithShadow(secondLine, sx + 20, sy + 14, 0xFFAAAAAA);

			if(!mc.thePlayer.isSneaking()) {
				GlStateManager.scale(0.5F, 0.5F, 1F);
				mc.fontRendererObj.drawStringWithShadow(TextFormatting.BOLD + "Shift", (sx + 10) * 2 - 16, (sy + 8) * 2 + 20, 0xFFFFFFFF);
				GlStateManager.scale(2F, 2F, 1F);
			}
		}

		GlStateManager.disableBlend();
		GlStateManager.color(1F, 1F, 1F, 1F);
		profiler.endSection();
	}

	private static void renderNearIndexDisplay(ScaledResolution res) {
		Minecraft mc = Minecraft.getMinecraft();
		String txt0 = I18n.format("botaniamisc.nearIndex0");
		String txt1 = TextFormatting.GRAY + I18n.format("botaniamisc.nearIndex1");
		String txt2 = TextFormatting.GRAY + I18n.format("botaniamisc.nearIndex2");

		int l = Math.max(mc.fontRendererObj.getStringWidth(txt0), Math.max(mc.fontRendererObj.getStringWidth(txt1), mc.fontRendererObj.getStringWidth(txt2))) + 20;
		int x = res.getScaledWidth() - l - 20;
		int y = res.getScaledHeight() - 60;

		Gui.drawRect(x - 6, y - 6, x + l + 6, y + 37, 0x44000000);
		Gui.drawRect(x - 4, y - 4, x + l + 4, y + 35, 0x44000000);
		net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.enableRescaleNormal();
		mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(ModBlocks.corporeaIndex), x, y + 10);
		net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

		mc.fontRendererObj.drawStringWithShadow(txt0, x + 20, y, 0xFFFFFF);
		mc.fontRendererObj.drawStringWithShadow(txt1, x + 20, y + 14, 0xFFFFFF);
		mc.fontRendererObj.drawStringWithShadow(txt2, x + 20, y + 24, 0xFFFFFF);
	}

	public static void drawSimpleManaHUD(int color, int mana, int maxMana, String name, ScaledResolution res) {
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Minecraft mc = Minecraft.getMinecraft();
		int x = res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(name) / 2;
		int y = res.getScaledHeight() / 2 + 10;

		mc.fontRendererObj.drawStringWithShadow(name, x, y, color);

		x = res.getScaledWidth() / 2 - 51;
		y += 10;

		renderManaBar(x, y, color, mana < 0 ? 0.5F : 1F, mana, maxMana);

		if(mana < 0) {
			String text = I18n.format("botaniamisc.statusUnknown");
			x = res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2;
			y -= 1;
			mc.fontRendererObj.drawString(text, x, y, color);
		}

		GlStateManager.disableBlend();
	}

	public static void drawComplexManaHUD(int color, int mana, int maxMana, String name, ScaledResolution res, ItemStack bindDisplay, boolean properlyBound) {
		drawSimpleManaHUD(color, mana, maxMana, name, res);

		Minecraft mc = Minecraft.getMinecraft();

		int x = res.getScaledWidth() / 2 + 55;
		int y = res.getScaledHeight() / 2 + 12;

		net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.enableRescaleNormal();
		mc.getRenderItem().renderItemAndEffectIntoGUI(bindDisplay, x, y);
		net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

		GlStateManager.disableDepth();
		if(properlyBound) {
			mc.fontRendererObj.drawStringWithShadow("\u2714", x + 10, y + 9, 0x004C00);
			mc.fontRendererObj.drawStringWithShadow("\u2714", x + 10, y + 8, 0x0BD20D);
		} else {
			mc.fontRendererObj.drawStringWithShadow("\u2718", x + 10, y + 9, 0x4C0000);
			mc.fontRendererObj.drawStringWithShadow("\u2718", x + 10, y + 8, 0xD2080D);
		}
		GlStateManager.enableDepth();
	}

	public static void renderManaBar(int x, int y, int color, float alpha, int mana, int maxMana) {
		Minecraft mc = Minecraft.getMinecraft();

		GlStateManager.color(1F, 1F, 1F, alpha);
		mc.renderEngine.bindTexture(manaBar);
		RenderHelper.drawTexturedModalRect(x, y, 0, 0, 0, 102, 5);

		int manaPercentage = Math.max(0, (int) ((double) mana / (double) maxMana * 100));

		if(manaPercentage == 0 && mana > 0)
			manaPercentage = 1;

		RenderHelper.drawTexturedModalRect(x + 1, y + 1, 0, 0, 5, 100, 3);

		Color color_ = new Color(color);
		GL11.glColor4ub((byte) color_.getRed(), (byte) color_.getGreen(),(byte) color_.getBlue(), (byte) (255F * alpha));
		RenderHelper.drawTexturedModalRect(x + 1, y + 1, 0, 0, 5, Math.min(100, manaPercentage), 3);
		GL11.glColor4ub((byte) 255, (byte) 255, (byte) 255, (byte) 255);
	}
}
