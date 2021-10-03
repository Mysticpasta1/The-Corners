package net.ludocrypt.corners.mixin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.ludocrypt.corners.TheCorners;
import net.ludocrypt.corners.access.BlockRenderManagerAccess;
import net.ludocrypt.corners.access.ChunkBuilderChunkDataAccess;
import net.ludocrypt.corners.client.render.block.SkyboxBlockEntityRenderer;
import net.ludocrypt.corners.client.render.model.FilterSkyQuadBakedModel;
import net.ludocrypt.corners.init.CornerSoundEvents;
import net.ludocrypt.corners.init.CornerWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.profiler.Profiler;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	@Final
	private TextureManager textureManager;

	@Shadow
	private ClientWorld world;

	@Unique
	private HashMap<BlockPos, BlockState> skyboxBlocks = new HashMap<BlockPos, BlockState>();

	@Inject(method = "processWorldEvent", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void corners$processWorldEvent(PlayerEntity source, int eventId, BlockPos pos, int data, CallbackInfo ci, Random random) {
		switch (eventId) {
		case 29848748:
			this.client.getSoundManager().play(PositionedSoundInstance.ambient(CornerSoundEvents.PAINTING_PORTAL_TRAVEL, random.nextFloat() * 0.4F + 0.8F, 0.25F));
			break;
		}
	}

	@Inject(method = "renderSky", at = @At("HEAD"))
	private void corners$renderSky(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable, CallbackInfo ci) {
		if (this.world.getRegistryKey().equals(CornerWorld.YEARNING_CANAL_WORLD_REGISTRY_KEY)) {
			this.corners$renderCubemap(matrices, TheCorners.id("textures/sky/yearning_canal"), f);
		} else if (this.world.getRegistryKey().equals(CornerWorld.COMMUNAL_CORRIDORS_WORLD_REGISTRY_KEY)) {
			this.corners$renderCubemap(matrices, TheCorners.id("textures/sky/snow"), f);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$ChunkData;getBlockEntities()Ljava/util/List;", ordinal = 0, shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
	private void corners$render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci, Profiler profiler, Vec3d vec3d, double d, double e, double f, Matrix4f matrix4f2, boolean bl, Frustum frustum2, boolean bl3, VertexConsumerProvider.Immediate immediate, ObjectListIterator<Entry<SortedSet<BlockBreakingInfo>>> var39, WorldRenderer.ChunkInfo chunkInfo) {
		skyboxBlocks = ((ChunkBuilderChunkDataAccess) ((WorldRendererChunkInfoAccessor) chunkInfo).getChunk().getData()).getSkyboxBlocks();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0, shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
	private void corners$render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci, Profiler profiler, Vec3d vec3d, double d, double e, double f, Matrix4f matrix4f2, boolean bl, Frustum frustum2, boolean bl3, VertexConsumerProvider.Immediate immediate, ObjectListIterator<Entry<SortedSet<BlockBreakingInfo>>> var39, WorldRenderer.ChunkInfo chunkInfo, List<BlockEntity> list) {
		Iterator<java.util.Map.Entry<BlockPos, BlockState>> iterator = skyboxBlocks.entrySet().iterator();
		while (iterator.hasNext()) {
			java.util.Map.Entry<BlockPos, BlockState> entry = iterator.next();
			BlockPos pos = entry.getKey();
			BlockState state = entry.getValue();
			matrices.push();
			matrices.translate(pos.getX() - d, pos.getY() - e, pos.getZ() - f);

			MinecraftClient client = MinecraftClient.getInstance();
			BlockRenderManager blockRenderManager = client.getBlockRenderManager();

			List<BakedQuad> quads = Lists.newArrayList();
			BakedModel model = ((BlockRenderManagerAccess) blockRenderManager).getModelPure(state);
			for (Direction dir : Direction.values()) {
				quads.addAll(model.getQuads(state, dir, new Random(0)).stream().filter((quad) -> quad.getSprite().getId().getPath().startsWith("sky/")).toList());
			}

			Iterator<BakedQuad> quadIterator = quads.iterator();

			while (quadIterator.hasNext()) {
				BakedQuad quad = quadIterator.next();
				Matrix4f matrix = matrices.peek().getModel();
				SkyboxBlockEntityRenderer.SKYBOX_CORE_SHADER.findUniformMat4("TransformMatrix").set(matrix);
				VertexConsumer consumer = immediate.getBuffer(SkyboxBlockEntityRenderer.SKYBOX_CORE_SHADER.getRenderLayer(SkyboxBlockEntityRenderer.SKYBOX_RENDER_LAYER.apply(new Identifier(quad.getSprite().getId().getNamespace(), "textures/" + quad.getSprite().getId().getPath()))));
				FilterSkyQuadBakedModel.quad(consumer, matrix, quad);
			}

			matrices.pop();
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0))
	private boolean corners$render$keepBoning(List<?> in) {
		return in.isEmpty() && skyboxBlocks.isEmpty();
	}

	@Unique
	private void corners$renderCubemap(MatrixStack matrices, Identifier identifier, float tickDelta) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);

		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d color = client.world.method_23777(client.gameRenderer.getCamera().getPos(), tickDelta).multiply(255);
		int r = (int) Math.floor(color.x);
		int g = (int) Math.floor(color.y);
		int b = (int) Math.floor(color.z);
		int a = 255;
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();

		for (int i = 0; i < 6; ++i) {
			matrices.push();
			if (i == 0) {
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
				matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
			}

			if (i == 1) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
			}

			if (i == 2) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
			}

			if (i == 3) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0F));
			}

			if (i == 4) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
			}

			Matrix4f matrix4f = matrices.peek().getModel();

			RenderSystem.setShaderTexture(0, new Identifier(identifier.toString() + "_" + i + ".png"));
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
			bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(r, g, b, a).next();
			bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).texture(0.0F, 1.0F).color(r, g, b, a).next();
			bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).texture(1.0F, 1.0F).color(r, g, b, a).next();
			bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).texture(1.0F, 0.0F).color(r, g, b, a).next();
			tessellator.draw();
			matrices.pop();
		}

		RenderSystem.depthMask(true);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

}
