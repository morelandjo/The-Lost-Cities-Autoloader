package com.lostcitiesautoloader;

//? >1.20.1 {
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
//?} >1.18.2 {
/*import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
*///?}
//? >1.18.2 {
import com.mojang.logging.LogUtils;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

// Adds a Lost Cities placed feature to every biome the lost_city_dimension target can produce.
// Lost Cities' own biome modifiers only cover #minecraft:is_overworld, and its feature does nothing
// in dimensions that have no profile, so biomes shared with other dimensions are safe.
// Runs in ServerLifecycleHooks.runModifiers: after the server's registries exist (including the
// world's level stems) and before any level is created, on new and existing worlds alike.
public record TargetDimensionBiomeModifier(HolderSet<PlacedFeature> features, GenerationStep.Decoration step)
        implements BiomeModifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer reportedMissingFor = null;

//?}
    //? >1.20.1 {
    private static final MapCodec<TargetDimensionBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    //?} >1.18.2 {
    /*private static final Codec<TargetDimensionBiomeModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    *///?}
    //? >1.18.2 {
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(TargetDimensionBiomeModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(TargetDimensionBiomeModifier::step)
    ).apply(instance, TargetDimensionBiomeModifier::new));

    public static void register(IEventBus modEventBus) {
    //?}
        //? >1.20.1 {
        DeferredRegister<MapCodec<? extends BiomeModifier>> serializers =
                DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, LostCitiesAutoloaderMod.MODID);
        //?} >1.18.2 {
        /*DeferredRegister<Codec<? extends BiomeModifier>> serializers =
                DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, LostCitiesAutoloaderMod.MODID);
        *///?}
    //? >1.18.2 {
        serializers.register("target_dimension_features", () -> CODEC);
        serializers.register(modEventBus);
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD
                || !AutoloaderConfigSimple.SPEC.isLoaded()
                || !AutoloaderConfigSimple.ENABLE_AUTOLOADER.get()
                || DimensionManager.isOverworldTarget()
                // Lost Cities' own modifier already adds the feature to these; placing it twice would generate twice.
                || biome.is(BiomeTags.IS_OVERWORLD)
                || !isInTargetDimension(biome)) {
            return;
        }
        features.forEach(feature -> builder.getGenerationSettings().addFeature(step, feature));
        LOGGER.info("Added Lost Cities feature {} to biome {} for dimension {}",
                features.unwrapKey().map(Object::toString).orElseGet(features::toString),
                biome.unwrapKey().map(key -> key.location().toString()).orElse("?"),
                DimensionManager.getLostCityDimensionConfiguration());
    }

    private static boolean isInTargetDimension(Holder<Biome> biome) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ResourceKey<Level> target = DimensionManager.getLostCityDimensionKey();
        if (server == null || target == null) {
            return false;
        }
        Registry<LevelStem> stems = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        LevelStem stem = stems.get(target.location());
        if (stem == null) {
            if (reportedMissingFor != server) {
                reportedMissingFor = server;
                LOGGER.error("Lost Cities target dimension {} does not exist in this world; no biomes were changed. Available dimensions: {}",
                        target.location(), stems.keySet());
            }
            return false;
        }
        return stem.generator().getBiomeSource().possibleBiomes().contains(biome);
    }

    @Override
    //?}
    //? >1.20.1 {
    public MapCodec<? extends BiomeModifier> codec() {
    //?} >1.18.2 {
    /*public Codec<? extends BiomeModifier> codec() {
    *///?}
    //? >1.18.2 {
        return CODEC;
    }
}
//?}
